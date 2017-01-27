/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core.websocket;

import clojure.lang.Keyword;

import clojure.lang.Var;
import flatgui.core.*;
import flatgui.core.awt.HostComponent;

import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Denis Lebedev
 */
public class FGContainerWebSocket implements WebSocketListener
{
    private static final long SEND_PREDICTIONS_THRESHOLD = 500;

    private static final Keyword fontKey_ = Keyword.intern("font");
    private static final Keyword childrenKey_ = Keyword.intern("children");

    private static final int METRICS_INPUT_CODE = 407;
    private static final int PING_INPUT_CODE = 408;

    private enum Phase {CollectingMetrics, Live};

    private final FGContainerSessionHolder sessionHolder_;
    // TODO have some template provider instead
    private final IFGTemplate template_;
    private final Consumer<IFGContainer> containerConsumer_;
    private final BiConsumer<Object, IFGContainer> sessionCloseConsumer_;
    private ExecutorService endpointTransportService_;
    private final FGPredictor predictor_;

    private volatile Session session_;
    private volatile FGWebContainerWrapper container_;
    private volatile FGInputEventDecoder parser_;
    private volatile FGContainerSession fgSession_;
    private volatile Timer blinkHelperTimer_;
    //private volatile Timer predictorTimer_;
    private volatile long latestInputEventTimestamp_;
    private volatile boolean predictionsSent_;

    private long totalInboundMessages_ = 0;
    private double avgProcessingTime_ = 0;

    private final ContainerAccessor containerAccessor_;

    private int fontsWaitingForMetrics_;
    private Phase currentPhase_;
    private List<byte[]> initialFontMetrics_;
    private List<FGInputEventDecoder.BinaryInput> pendingEvents_;

    private static boolean acceptingRequests_ = true;

    public FGContainerWebSocket(IFGTemplate template,
                                FGContainerSessionHolder sessionHolder,
                                Consumer<IFGContainer> containerConsumer,
                                BiConsumer<Object, IFGContainer> sessionCloseConsumer)
    {
        template_ = template;
        sessionHolder_ = sessionHolder;
        containerConsumer_ = containerConsumer;
        sessionCloseConsumer_ = sessionCloseConsumer;
        predictor_ = new FGPredictor();

        containerAccessor_ = new ContainerAccessor();

        blinkHelperTimer_ = HostComponent.setupBlinkHelperTimer(this::processInputEvent);

        //predictorTimer_ = new Timer("FlatGUI User Input Predictor Timer", true);
//        predictorTimer_.schedule(new TimerTask()
//        {
//            @Override
//            public void run()
//            {
//                if (System.currentTimeMillis() - latestInputEventTimestamp_ > SEND_PREDICTIONS_THRESHOLD && !predictionsSent_)
//                {
//                    sendPredictionsIfNeeded();
//                }
//            }
//        }, SEND_PREDICTIONS_THRESHOLD, SEND_PREDICTIONS_THRESHOLD);

        FGAppServer.getFGLogger().info("WS Listener created " + System.identityHashCode(this));
    }

    public static void setAcceptingRequests(boolean accept)
    {
        acceptingRequests_ = accept;
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        FGAppServer.getFGLogger().info("WS Close " + System.identityHashCode(this) +
                " session: " + fgSession_ +
                " remote: " + session_.getRemoteAddress() +
                " reason = " + reason);

        blinkHelperTimer_.cancel();
        //predictorTimer_.cancel();

        if (sessionCloseConsumer_ != null)
        {
            sessionCloseConsumer_.accept(new FGSessionInfo(container_.getContainer().getId(), session_.getRemoteAddress()), container_.getContainer());
        }

        container_.unInitialize();
        fgSession_.markIdle();
        session_ = null;
        endpointTransportService_.shutdown();
    }

    @Override
    public void onWebSocketConnect(Session session)
    {
        if (!acceptingRequests_)
        {
            session.close(new CloseStatus(1000, "Server maintenance. Use alternative server."));
            FGAppServer.getFGLogger().info("Refused remote endpoint due to maintenance mode: " +
                    session.getRemoteAddress());
        }

        session_ = session;
        endpointTransportService_ = Executors.newSingleThreadExecutor();
        FGAppServer.getFGLogger().info("WS Connect " + System.identityHashCode(this) +
                " session: " + fgSession_ +
                " remote: " + session_.getRemoteAddress());

        Set<String> fonts = findAllFonts();
        fontsWaitingForMetrics_ = fonts.size();

        initialFontMetrics_ = new ArrayList<>(fontsWaitingForMetrics_);
        pendingEvents_ = new ArrayList<>();
        if (fontsWaitingForMetrics_ > 0)
        {
            currentPhase_ = Phase.CollectingMetrics;

            fonts.stream().forEach(f -> {
                FGAppServer.getFGLogger().info(session_.getRemoteAddress() + " Requesting metrics for font: " + f);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                FGWebContainerWrapper.StringTransmitter.writeString(stream, 1, f);
                byte[] textBytes = stream.toByteArray();
                byte[] cmd = new byte[textBytes.length + 1];
                cmd[0] = FGWebContainerWrapper.METRICS_REQUEST;
                for (int i = 0; i < textBytes.length; i++)
                {
                    cmd[i + 1] = textBytes[i];
                }
                sendBytesToRemote(ByteBuffer.wrap(cmd));
            });
        }
        else
        {
            currentPhase_ = Phase.Live;
            sendBytesToRemote(ByteBuffer.wrap(new byte[]{FGWebContainerWrapper.FINISH_PREDICTION_TRANSMISSION}));
            startContainer();
        }
    }

    @Override
    public void onWebSocketError(Throwable cause)
    {
        FGAppServer.getFGLogger().error(fgSession_ + " WS error: " + cause.getMessage());
    }

    // TODO https://bugs.eclipse.org/bugs/show_bug.cgi?id=474488

    @Override
    public synchronized void onWebSocketBinary(byte[] payload, int offset, int len)
    {
        if (currentPhase_ == Phase.CollectingMetrics)
        {
            if (payload[0] == METRICS_INPUT_CODE - 400)
            {
                initialFontMetrics_.add(payload);
                fontsWaitingForMetrics_--;
                if (fontsWaitingForMetrics_ == 0)
                {
                    currentPhase_ = Phase.Live;
                    sendBytesToRemote(ByteBuffer.wrap(new byte[]{FGWebContainerWrapper.FINISH_PREDICTION_TRANSMISSION}));
                    startContainer();
                }
            }
            else
            {
                pendingEvents_.add(new FGInputEventDecoder.BinaryInput(payload, offset, len));
            }
            return;
        }

        long startTime = System.currentTimeMillis();

        fgSession_.markAccesed();

        if (payload.length > 0)
        {
            if (payload[0] == METRICS_INPUT_CODE - 400)
            {
                FGWebInteropUtil interop = (FGWebInteropUtil) container_.getContainer().getInterop();
                String fontStr = interop.setMetricsTransmission(payload);
                container_.markFontAsHavingReceivedMetrics(fontStr);
            }
            else if (payload[0] == PING_INPUT_CODE - 400)
            {
                sendBytesToRemote(ByteBuffer.wrap(new byte[]{FGWebContainerWrapper.PING_RESPONSE}));
            }
            else
            {
                latestInputEventTimestamp_ = System.currentTimeMillis();
                Object e = parser_.getInputEvent(new FGInputEventDecoder.BinaryInput(payload, offset, len));
                predictor_.considerInputEvent(e);
                processInputEvent(e);
                container_.clearForks();
                predictionsSent_ = false;
// TODO This is still experimental
//        if (e instanceof MouseEvent)
//        {
//            sendPredictionsIfNeeded();
//        }
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        avgProcessingTime_ = (avgProcessingTime_*totalInboundMessages_ + processingTime) / (totalInboundMessages_+1);
        totalInboundMessages_++;
    }

    @Override
    public void onWebSocketText(String message)
    {
        //logger_.debug("Received message #" + debugMessageCount_ + ": " + message);
    }

    private Set<String> findAllFonts()
    {
        Var containerVar = clojure.lang.RT.var(template_.getContainerNamespace(), template_.getContainerVarName());
        Map<Keyword, Object> container = (Map<Keyword, Object>) containerVar.get();
        return getFonts(container);
    }

    private Set<String> getFonts(Map<Keyword, Object> container)
    {
        Set<String> fonts = new HashSet<>();

        Object fontObj = container.get(fontKey_);
        String containerFont = (String) fontObj;
        if (containerFont != null)
        {
            fonts.add(containerFont);
        }

        Map<Keyword, Map<Keyword, Object>> children = (Map<Keyword, Map<Keyword, Object>>) container.get(childrenKey_);
        if (children != null)
        {
            children.values().stream()
                    .forEach(v -> fonts.addAll(getFonts(v)));

        }

        return fonts;
    }

    private void startContainer()
    {
        StringBuilder statusMessage = new StringBuilder("Creating session...");
        setTextToRemote(statusMessage.toString());

        Set<String> fonts = new HashSet<>();
        fgSession_ = sessionHolder_.getSession(template_, session_.getRemoteAddress().getAddress(), initialFontMetrics_, fonts);
        fgSession_.setAccosiatedWebSocket(this);

        statusMessage.append("|created session");
        setTextToRemote(statusMessage.toString());

        FGAppServer.getFGLogger().info("Initializing container " + System.identityHashCode(this) +
                " session: " + fgSession_ +
                " remote: " + session_.getRemoteAddress());

        container_ = fgSession_.getContainer();

        fonts.forEach(container_::markFontAsHavingReceivedMetrics);

        statusMessage.append("|created app");
        setTextToRemote(statusMessage.toString());

        container_.initialize();
        if (containerConsumer_ != null)
        {
            containerConsumer_.accept(containerAccessor_);
        }

        statusMessage.append("|initialized app");
        setTextToRemote(statusMessage.toString());

        parser_ = fgSession_.getParser();

        statusMessage.append("|retrieving initial state...");
        setTextToRemote(statusMessage.toString());

        container_.resetCache();

        pendingEvents_.forEach(b -> onWebSocketBinary(b.getPayload(), b.getOffset(), b.getLen()));
        pendingEvents_.clear();

        collectAndSendResponse(null, false);
    }

    private boolean isStarted()
    {
        return container_ != null;
    }

    private void processInputEvent(Object e)
    {
        if (!isStarted())
        {
            return;
        }

        if (e == null)
        {
            //logger_.debug("Processed message #" + debugMessageCount_ + ": not an input event.");
            //debugMessageCount_++;
            return;
        }

        //
        // Feed input event received from the remote endpoint to the engine
        //

        Future<FGEvolveResultData> evolveResultFuture = container_.feedEvent(new FGEvolveInputData(e, false));
        if (evolveResultFuture != null)
        {
            collectAndSendResponse(evolveResultFuture, e instanceof FGHostStateEvent);
        }

        //debugMessageCount_++;
    }

    void collectAndSendResponse(Future<FGEvolveResultData> evolveResultFuture, boolean forceRepaint)
    {
        Collection<ByteBuffer> response = container_.getResponseForClient(evolveResultFuture);

        if (response.size() > 0)
        {
            response.forEach(this::sendBytesToRemote);
            sendBytesToRemote(ByteBuffer.wrap(new byte[]{FGWebContainerWrapper.REPAINT_CACHED_COMMAND_CODE}));

            //logger_.debug("Finished sending " + response.size() + " responses and repaint cmd for #" + debugMessageCount_);
        }
        else if (forceRepaint)
        {
            sendBytesToRemote(ByteBuffer.wrap(new byte[]{FGWebContainerWrapper.REPAINT_CACHED_COMMAND_CODE}));
        }
    }

    void sendPredictionsIfNeeded()
    {
        synchronized (this)
        {
            int sentForClick = 0;
            int sentForMove = 0;
            int movePredictionCount = 0;

            boolean anyPrediction = false;
            List<MouseEvent> clickEvents = predictor_.leftClickInLatestPosition();
            if (clickEvents != null)
            {
                for (int i = 0; i < FGWebContainerWrapper.MOUSE_LEFT_CLICK_PREDICTION_SEQUENCE.length; i++)
                {
                    Object evolveReason = clickEvents.get(i);
                    Future<FGEvolveResultData> evolveResultFuture = container_.feedEvent(new FGEvolveInputData(evolveReason, true));
                    Collection<ByteBuffer> response = container_.getForkedResponseForClient(evolveReason, evolveResultFuture);
                    if (response.size() > 0)
                    {
                        //response.forEach(b -> System.out.print(b.capacity() + "|"));

                        sendBytesToRemote(ByteBuffer.wrap(new byte[]{FGWebContainerWrapper.MOUSE_LEFT_CLICK_PREDICTION_SEQUENCE[i]}));
                        response.forEach(this::sendBytesToRemote);

                        sentForClick += 1 + response.stream().map(r -> r.capacity()).reduce((a,b) -> a+b).get();

                        anyPrediction = true;
                    }
                }
            }

//            List<Tuple> moveAroundEvents = predictor_.moveAroundTheLatestEvent();
//            if (moveAroundEvents != null)
//            {
//                List<Tuple> predictionsPerPoint = new ArrayList<>(moveAroundEvents.size());
//
//                for (Tuple moveEvent : moveAroundEvents)
//                {
//                    Integer dx = moveEvent.getFirst();
//                    Integer dy = moveEvent.getSecond();
//                    Object evolveReason = moveEvent.getThird();
//
//                    Future<FGEvolveResultData> evolveResultFuture = container_.feedEvent(new FGEvolveInputData(evolveReason, true));
//                    Collection<ByteBuffer> response = container_.getForkedResponseForClient(evolveReason, evolveResultFuture);
//                    if (response.size() > 0)
//                    {
//                        predictionsPerPoint.add(Tuple.triple(dx, dy, response));
//                        anyPrediction = true;
//                    }
//                }
//
//                if (!predictionsPerPoint.isEmpty())
//                {
//                    List<Collection<ByteBuffer>> uniqueResponses = new ArrayList<>(moveAroundEvents.size());
//
//                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
//
//                    stream.write(FGWebContainerWrapper.MOUSE_MOVE_OR_DRAG_PREDICTION_HEADER);
//                    stream.write((byte)predictionsPerPoint.size());
//
//                    for (Tuple predictionPerPoint : predictionsPerPoint)
//                    {
//                        Integer dx = predictionPerPoint.getFirst();
//                        Integer dy = predictionPerPoint.getSecond();
//                        Collection<ByteBuffer> response = predictionPerPoint.getThird();
//                        int index = uniqueResponses.indexOf(response);
//                        if (index < 0)
//                        {
//                            index = uniqueResponses.size();
//                            uniqueResponses.add(response);
//                        }
//                        // Support from -8 to 7 for both axis deltas, and 256 unique indices
//
//                        stream.write((byte)((dx + 8) | ((dy + 8) << 4)));
//                        stream.write((byte)index);
//
//                        sentForMove += 3;
//                    }
//                    stream.write((byte)uniqueResponses.size());
//                    sendBytesToRemote(ByteBuffer.wrap(stream.toByteArray()));
//                    // Here the remote automatically switches to MOUSE_MOVE_OR_DRAG_PREDICTION mode
//
//                    movePredictionCount = uniqueResponses.size();
//
//                    for (Collection<ByteBuffer> response : uniqueResponses)
//                    {
//                        response.forEach(this::sendBytesToRemote);
//                        for (ByteBuffer b : response)
//                        {
//                            sentForMove += b.capacity();
//                        }
//                    }
//                }
//            }

            if (anyPrediction)
            {
                sendBytesToRemote(ByteBuffer.wrap(new byte[]{FGWebContainerWrapper.FINISH_PREDICTION_TRANSMISSION}));
                predictionsSent_ = true;
                if (sentForMove > 0)
                {
                    //System.out.println("Sent predictions Click=" + sentForClick + "b Move=" + sentForMove + "b MoveCnt=" + movePredictionCount);
                }
            }
        }
    }

    double getAvgProcessingTime()
    {
        return avgProcessingTime_;
    }

    int getQueueSizeWaiting()
    {
        return container_.getContainer().getQueueSizeWaiting();
    }

    private void sendBytesToRemote(ByteBuffer bytes)
    {
        endpointTransportService_.submit(() -> {
            try
            {
                session_.getRemote().sendBytes(bytes);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        });
    }

    private void setTextToRemote(String text)
    {
        endpointTransportService_.submit(() -> {
            try
            {
                session_.getRemote().sendString(text);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }

    public static class FGSessionInfo
    {
        private final Object containerId_;
        private final Object removeAddr_;

        public FGSessionInfo(Object containerId, Object remoteAddr)
        {
            containerId_ = containerId;
            removeAddr_ = remoteAddr;
        }

        @Override
        public String toString()
        {
            return containerId_ + "/" + removeAddr_;
        }
    }

    // TODO Refactor. Probably it does not have to accept IFGContainer type
    public class ContainerAccessor implements IFGContainer
    {
        @Override
        public Future<FGEvolveResultData> feedTargetedEvent(List<Keyword> targetCellIdPath, FGEvolveInputData inputData)
        {
            Future <FGEvolveResultData> changedPathsFuture = container_.feedTargetedEvent(targetCellIdPath, inputData);
            collectAndSendResponse(changedPathsFuture, false);
            return null;
        }

        @Override
        public Future<FGEvolveResultData> feedTargetedEvent(List<Keyword> targetCellIdPath, Object evolveReason)
        {
            return feedTargetedEvent(targetCellIdPath, new FGEvolveInputData(evolveReason, false));
        }

        @Override
        public String getId()
        {
            return container_.getContainer().getId();
        }

        @Override
        public void initialize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unInitialize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isActive()
        {
            return container_.getContainer().isActive();
        }

        @Override
        public void addEvolveConsumer(IFGEvolveConsumer consumer) {
            container_.getContainer().addEvolveConsumer(consumer);
        }

        @Override
        public IFGModule getFGModule()
        {
            return container_.getContainer().getFGModule();
        }

        @Override
        public IFGModule getForkedFGModule(Object evolveReason)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public IFGInteropUtil getInterop()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Function<FGEvolveInputData, Future<FGEvolveResultData>> connect(ActionListener eventFedCallback, Object hostContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> submitTask(Callable<T> callable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<FGEvolveResultData> feedEvent(FGEvolveInputData inputData) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Keyword> getLastMouseTargetIdPath()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getQueueSizeWaiting()
        {
            throw new UnsupportedOperationException();
        }
    }
}
