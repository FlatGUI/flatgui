/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core;

import flatgui.core.awt.FGKeyEventParser;
import flatgui.core.awt.FGMouseEventParser;
import flatgui.core.websocket.FGPaintVectorBinaryCoder;
import flatgui.core.websocket.FGWebInteropUtil;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Denis Lebedev
 */
public class FGWebContainer implements IFGContainer
{
    //@todo
    public static final int UNIT_SIZE_PX = 64;

    public static final byte POSITION_MATRIX_MAP_COMMAND_CODE = 0;
    public static final byte VIEWPORT_MATRIX_MAP_COMMAND_CODE = 1;
    public static final byte CLIP_SIZE_MAP_COMMAND_CODE = 2;
    public static final byte LOOK_VECTOR_MAP_COMMAND_CODE = 3;
    public static final byte CHILD_COUNT_MAP_COMMAND_CODE = 4;
    public static final byte BOOLEAN_STATE_FLAGS_COMMAND_CODE = 5;

    public static final byte PAINT_ALL_LIST_COMMAND_CODE = 6;

    public static final byte REPAINT_CACHED_COMMAND_CODE = 7;


    private final FGRepaintReasonParser reasonParser_;

    private static Set<String> RECT_COMMANDS;
    static
    {
        RECT_COMMANDS = new HashSet<>();
        RECT_COMMANDS.add("drawLine");
        RECT_COMMANDS.add("drawRect");
        RECT_COMMANDS.add("fillRect");
        RECT_COMMANDS.add("drawOval");
        RECT_COMMANDS.add("fillOval");
        RECT_COMMANDS.add("clipRect");
        RECT_COMMANDS.add("setClip");
    }

    private final FGWebInteropUtil interopUtil_;

    private final Map<String, Object> containerProperties_;
    private final Map<String, Object> targetedProperties_;

    private final IFGModule module_;

    private final FGContainerStateTransmitter stateTransmitter_;

    private ExecutorService evolverExecutorService_;

    public FGWebContainer(IFGModule module)
    {
        reasonParser_ = new FGRepaintReasonParser();
        reasonParser_.registerReasonClassParser(MouseEvent.class, new FGMouseEventParser(UNIT_SIZE_PX));
        reasonParser_.registerReasonClassParser(MouseWheelEvent.class, new FGMouseEventParser(UNIT_SIZE_PX));
        reasonParser_.registerReasonClassParser(KeyEvent.class, new FGKeyEventParser());
        reasonParser_.registerReasonClassParser(FGContainer.FGTimerEvent.class, new IFGRepaintReasonParser<FGContainer.FGTimerEvent>() {
            @Override
            public Map<String, Object> initialize(IFGModule fgModule) {
                return null;
            }

            @Override
            public Map<String, Object> getTargetedPropertyValues(FGContainer.FGTimerEvent fgTimerEvent) {
                return new HashMap<>();
            }

            @Override
            public Map<FGContainer.FGTimerEvent, Collection<Object>> getTargetCellIds(FGContainer.FGTimerEvent fgTimerEvent, IFGModule fgModule, Map<String, Object> generalPropertyMap) {
                return Collections.emptyMap();
            }
        });

        module_ = module;
        stateTransmitter_ = new FGContainerStateTransmitter(module);

        interopUtil_ = new FGWebInteropUtil(UNIT_SIZE_PX);

        containerProperties_ = new HashMap<>();
        containerProperties_.put(GENERAL_PROPERTY_UNIT_SIZE, UNIT_SIZE_PX);

        targetedProperties_ = new HashMap<>();

//        Timer repaintTimer = new Timer("FlatGUI Blink Helper Timer", true);
//        repaintTimer.schedule(new TimerTask()
//        {
//            @Override
//            public void run()
//            {
//                //EventQueue.invokeLater(() -> cycle(new FGTimerEvent()));
//            }
//        }, 250, 250);

        Map<String, Object> initialGeneralProperties = reasonParser_.initialize(module_);
        if (initialGeneralProperties != null)
        {
            containerProperties_.putAll(initialGeneralProperties);
        }
    }

    @Override
    public void initialize()
    {
        evolverExecutorService_ = Executors.newSingleThreadExecutor();
    }

    @Override
    public void unInitialize()
    {
        evolverExecutorService_.shutdown();
    }

    @Override
    public Component getContainerComponent()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IFGModule getFGModule()
    {
        return module_;
    }

    ///

    @Override
    public Object getGeneralProperty(String propertyName)
    {
        return containerProperties_.get(propertyName);
    }

    @Override
    public Object getAWTUtil()
    {
        return interopUtil_;
    }

    ///

    private void cycle(Object repaintReason)
    {
        if (repaintReason == null)
        {
            throw new IllegalArgumentException();
        }

        targetedProperties_.clear();
        if (repaintReason != null)
        {
            Map<String, Object> propertiesFromReason = reasonParser_.getTargetedPropertyValues(repaintReason);
            for (String propertyName : propertiesFromReason.keySet())
            {
                targetedProperties_.put(propertyName,
                        propertiesFromReason.get(propertyName));
            }
        }

        Map<String, Object> generalProperties = new HashMap<>();
        Map<Object, Collection<Object>> reasonMap = reasonParser_.getTargetCellIds(repaintReason, module_, generalProperties);
        if (generalProperties != null)
        {
            containerProperties_.putAll(generalProperties);
        }
        for (Object reason : reasonMap.keySet())
        {
            Collection<Object> targetCellIds_ = reasonMap.get(reason);

            if (targetCellIds_ == null || !targetCellIds_.isEmpty())
            {
                module_.evolve(targetCellIds_, reason);
            }
        }
    }

//    public synchronized java.util.List<Object> getPaintAllVector()
//    {
//        Future<java.util.List<Object>> paintFuture = evolverExecutorService_.submit(() -> {
//            long t = System.currentTimeMillis();
//            //System.out.println("-------------------------- Started painting...");
//
//            // @todo these 0, 0, 1024, 768 coords are just for demo
//            java.util.List<Object> result = module_.getPaintAllSequence(0, 0, 1024, 768);
//
//            //System.out.println("---------------------------- ...done painting in " + (System.currentTimeMillis() - t) + " millis.");
//
//            return result;
//        });
//        try
//        {
//            return paintFuture.get();
//        }
//        catch (InterruptedException | ExecutionException e)
//        {
//            e.printStackTrace();
//            return null;
//        }
//    }

//    public synchronized java.util.List<Object> getPaintVector()
//    {
//        Future<java.util.List<Object>> paintFuture = evolverExecutorService_.submit(() -> {
//            long t = System.currentTimeMillis();
//            //System.out.println("-------------------------- Started painting...");
//
//            java.util.List<Object> result = module_.getPaintChangesSequence(null);
//
//            java.util.List<Object> compressed = compressPaintVector(result);
//
//
//            //System.out.println("---------------------------- ...done painting in " + (System.currentTimeMillis() - t) + " millis.");
//
//            return compressed;
//        });
//        try
//        {
//            return paintFuture.get();
//        }
//        catch (InterruptedException | ExecutionException e)
//        {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public synchronized void feedEvent(Object repaintReason)
    {
        evolverExecutorService_.submit(() -> cycle(repaintReason));
    }

    public synchronized Collection<ByteBuffer> getResponseForClient()
    {
        Future<Collection<ByteBuffer>> responseFuture =
                evolverExecutorService_.submit(() -> stateTransmitter_.computeDataDiffsToTransmit());

        try
        {
            return responseFuture.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public synchronized void resetCache()
    {
        stateTransmitter_.resetDataCache();
    }

    private java.util.List<Object> compressPaintVector(java.util.List<Object> commandVector)
    {
        // TODO paint.clj: bother with clipping only when viewport size is different from clip size

        commandVector = commandVector.stream().filter(cmd -> {
            Object command = ((List) cmd).get(0);
//            if (command.equals("pushCurrentClip") || command.equals("popCurrentClip") ||
//                    command.equals("clipRect") || command.equals("setClip")) {
//                return false;
//            }
            if (RECT_COMMANDS.contains(command)) {
                double x = getCoord(((List) cmd), 1);
                double y = getCoord(((List) cmd), 2);
                double w = getCoord(((List) cmd), 3);
                double h = getCoord(((List) cmd), 4);
                if (x == 0 && y == 0 && w == 0 && h == 0) return false;
            }
            return true;
        }).collect(Collectors.toList());

        java.util.List<Object> compressed = new ArrayList<>(commandVector.size());

        int size = commandVector.size();
        for (int i = 0; i < size;)
        {
            List singleCommand = new ArrayList<>((List) commandVector.get(i));
            String command = (String) singleCommand.get(0);

            if (i < size-1)
            {
//                if (getCommand(commandVector, i).equals("popCurrentClip") &&
//                        getCommand(commandVector, i+1).equals("pushCurrentClip"))
//                {
//                    // Skip
//                    i = i+2;
//                }
                if (getCommand(commandVector, i).equals("transform"))
                {
                    double ctx = ((AffineTransform) singleCommand.get(1)).getTranslateX();
                    double cty = ((AffineTransform) singleCommand.get(1)).getTranslateY();
                    boolean coalesce = false;

                    int j=i+1;
                    while (j < size && getCommand(commandVector, j).equals("transform"))
                    {
                        List jCommand = new ArrayList<>((List) commandVector.get(j));
                        ctx += ((AffineTransform) jCommand.get(1)).getTranslateX();
                        cty += ((AffineTransform) jCommand.get(1)).getTranslateY();
                        coalesce = true;
                        j++;
                    }

                    if (coalesce)
                    {
                        compressed.add(Arrays.asList("transform", AffineTransform.getTranslateInstance(ctx, cty)));
                        i=j;
                    }
                    else
                    {
                        compressed.add(singleCommand);
                        i++;
                    }
                }
//                else if (getCommand(commandVector, i).equals("drawString"))
//                {
//                    singleCommand.set(1, "");
//                    i++;
//                }
                else
                {
                    compressed.add(singleCommand);
                    i++;
                }
            }
            else
            {
                compressed.add(singleCommand);
                i++;
            }
        }

        return compressed;
    }

    private static String getCommand(java.util.List<Object> commandVector, int i)
    {
        List singleCommand = new ArrayList<>((List) commandVector.get(i));
        return (String) singleCommand.get(0);
    }

    private static double getCoord(List singleCommand, int i)
    {
        return ((Number) singleCommand.get(i)).doubleValue();
    }

    private java.util.List<Object> compressPaintVector2(java.util.List<Object> commandVector)
    {
        java.util.List<Object> compressed = new ArrayList<>(commandVector.size());

        double ctx = 0;
        double cty = 0;

        for (int i=0; i<commandVector.size(); i++)
        {
            List singleCommand = new ArrayList<>((List)commandVector.get(i));
            String command = (String) singleCommand.get(0);
            if (command.equals("transform"))
            {
                ctx += ((AffineTransform) singleCommand.get(1)).getTranslateX();
                cty += ((AffineTransform) singleCommand.get(1)).getTranslateY();
            }
//            else if (command.equals("pushCurrentClip") || command.equals("popCurrentClip"))
//            {
//                // Skip
//            }
            else
            {
                double ctxScaled = ctx / UNIT_SIZE_PX;
                double ctyScaled = cty / UNIT_SIZE_PX;

                switch (command)
                {
                    case "drawString":
                        double x = ((Number)singleCommand.get(2)).doubleValue();
                        double y = ((Number)singleCommand.get(3)).doubleValue();
                        singleCommand.set(2, Double.valueOf(x + ctxScaled));
                        singleCommand.set(3, Double.valueOf(y + ctyScaled));
                        break;
                    case "drawLine":
                        x = ((Number)singleCommand.get(1)).doubleValue();
                        y = ((Number)singleCommand.get(2)).doubleValue();
                        singleCommand.set(1, Double.valueOf(x + ctxScaled));
                        singleCommand.set(2, Double.valueOf(y + ctyScaled));
                        double x2 = ((Number)singleCommand.get(3)).doubleValue();
                        double y2 = ((Number)singleCommand.get(4)).doubleValue();
                        singleCommand.set(3, Double.valueOf(x2 + ctxScaled));
                        singleCommand.set(4, Double.valueOf(y2 + ctyScaled));
                        break;
                    case "drawRect":
                    case "fillRect":
                    case "drawOval":
                    case "fillOval":
                    case "clipRect":
                    case "setClip":
                        x = ((Number)singleCommand.get(1)).doubleValue();
                        y = ((Number)singleCommand.get(2)).doubleValue();
                        singleCommand.set(1, Double.valueOf(x + ctxScaled));
                        singleCommand.set(2, Double.valueOf(y + ctyScaled));
                }

                compressed.add(singleCommand);
            }
        }

        return compressed;
    }

    private static boolean eq(Object a, Object b)
    {
        if (a == null)
        {
            return b == null;
        }
        else
        {
            return a.equals(b);
        }
    }

    interface IDataTransmitter<S>
    {
        public byte getCommandCode();

        public Supplier<S> getEmptyDataSupplier();

        public Supplier<S> getSourceDataSupplier();

        public S getDiffToTransmit(S previousData, S newData);

        public ByteBuffer convertToBinary(byte commandCode, S data);
    }

    static abstract class AbstractTransmitter<S> implements IDataTransmitter<S>
    {
        @Override
        public ByteBuffer convertToBinary(byte commandCode, S data)
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(commandCode);

            writeBinary(stream, 1, data);

            return ByteBuffer.wrap(stream.toByteArray());

//            // TODO ZIP only if >10 bytes
//            try
//            {
//                ByteArrayOutputStream bo = new ByteArrayOutputStream();
//                DeflaterOutputStream go = new GZIPOutputStream(bo);
//                go.write(stream.toByteArray());
//                go.close();
//                byte[] result = bo.toByteArray();
//                return ByteBuffer.wrap(result);
//            }
//            catch (IOException ex)
//            {
//                ex.printStackTrace();
//                return null;
//            }
        }

        public abstract int writeBinary(ByteArrayOutputStream stream, int n, S data);
    }

    interface IKeyCache
    {
        int getUniqueId(Object key);
    }

    static abstract class MapTransmitter<V> extends AbstractTransmitter<Map<Object, Object>>
    {
        private IKeyCache keyCache_;
        private Supplier<Map<Object, Object>> sourceMapSupplier_;

        public MapTransmitter(IKeyCache keyCache, Supplier<Map<Object, Object>> sourceMapSupplier)
        {
            keyCache_ = keyCache;
            sourceMapSupplier_ = sourceMapSupplier;
        }

        @Override
        public Map<Object, Object> getDiffToTransmit(Map<Object, Object> previousData, Map<Object, Object> newData)
        {
            Map<Object, Object> diff = newData.entrySet().stream()
                    .filter(e -> !eq(previousData.get(e.getKey()), e.getValue()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            return diff.isEmpty() ? null : diff;
        }

        @Override
        public int writeBinary(ByteArrayOutputStream stream, int n, Map<Object, Object> data)
        {
            for (Map.Entry<Object, Object> e : data.entrySet())
            {
                int uid = keyCache_.getUniqueId(e.getKey());
                stream.write((byte)(uid & 0xFF));
                n++;
                stream.write((byte)((uid >> 8) & 0xFF));
                n++;
                int bytesWritten = writeValue(stream, n, (V)e.getValue());
                n += bytesWritten;
            }
            return n;
        }

        @Override
        public Supplier<Map<Object, Object>> getEmptyDataSupplier()
        {
            return HashMap::new;
        }

        @Override
        public Supplier<Map<Object, Object>> getSourceDataSupplier()
        {
            return sourceMapSupplier_;
        }

        protected abstract int writeValue(ByteArrayOutputStream stream, int n, V value);
    }

    static abstract class TransformMatrixMapTransmitter extends MapTransmitter<AffineTransform>
    {
        TransformMatrixMapTransmitter(IKeyCache keyCache, Supplier<Map<Object, Object>> sourceMapSupplier)
        {
            super(keyCache, sourceMapSupplier);
        }

        @Override
        protected int writeValue(ByteArrayOutputStream stream, int n, AffineTransform matrix)
        {
            int tx = getTranslateX(matrix);
            int ty = getTranslateY(matrix);
            stream.write((byte)(tx & 0xFF));
            stream.write((byte)(ty & 0xFF));
            stream.write((byte)((tx >> 8) & 0x0F | (ty >> 4) & 0xF0));
            return 3;
        }

        protected abstract int getTranslateX(AffineTransform matrix);

        protected abstract int getTranslateY(AffineTransform matrix);
    }

    static class PositionMatrixMapTrasmitter extends TransformMatrixMapTransmitter
    {
        PositionMatrixMapTrasmitter(IKeyCache keyCache, Supplier<Map<Object, Object>> sourceMapSupplier)
        {
            super(keyCache, sourceMapSupplier);
        }

        @Override
        protected int getTranslateX(AffineTransform matrix)
        {
            return (int)matrix.getTranslateX();
        }

        @Override
        protected int getTranslateY(AffineTransform matrix)
        {
            return (int)matrix.getTranslateY();
        }

        @Override
        public byte getCommandCode()
        {
            return POSITION_MATRIX_MAP_COMMAND_CODE;
        }
    }

    static class ViewportMatrixMapTrasmitter extends TransformMatrixMapTransmitter
    {
        ViewportMatrixMapTrasmitter(IKeyCache keyCache, Supplier<Map<Object, Object>> sourceMapSupplier)
        {
            super(keyCache, sourceMapSupplier);
        }

        @Override
        protected int getTranslateX(AffineTransform matrix)
        {
            return -(int)matrix.getTranslateX();
        }

        @Override
        protected int getTranslateY(AffineTransform matrix)
        {
            return -(int)matrix.getTranslateY();
        }

        @Override
        public byte getCommandCode()
        {
            return VIEWPORT_MATRIX_MAP_COMMAND_CODE;
        }
    }

    static abstract class ShortMapTrasmitter extends MapTransmitter<Number>
    {
        public ShortMapTrasmitter(IKeyCache keyCache, Supplier<Map<Object, Object>> sourceMapSupplier)
        {
            super(keyCache, sourceMapSupplier);
        }

        protected int writeValue(ByteArrayOutputStream stream, int n, Number value)
        {
            short number = value.shortValue();
            stream.write((byte)(number & 0xFF));
            stream.write((byte)((number >> 8) & 0xFF));
            return 2;
        }
    }

    static abstract class ByteMapTrasmitter extends MapTransmitter<Number>
    {
        public ByteMapTrasmitter(IKeyCache keyCache, Supplier<Map<Object, Object>> sourceMapSupplier)
        {
            super(keyCache, sourceMapSupplier);
        }

        protected int writeValue(ByteArrayOutputStream stream, int n, Number value)
        {
            byte number = value.byteValue();
            stream.write(number);
            return 1;
        }
    }

    static class ChildCountMapTransmitter extends ShortMapTrasmitter
    {
        public ChildCountMapTransmitter(IKeyCache keyCache, Supplier<Map<Object, Object>> sourceMapSupplier)
        {
            super(keyCache, sourceMapSupplier);
        }

        @Override
        public byte getCommandCode()
        {
            return CHILD_COUNT_MAP_COMMAND_CODE;
        }
    }

    static class BooleanFlagsMapTransmitter extends ByteMapTrasmitter
    {
        public BooleanFlagsMapTransmitter(IKeyCache keyCache, Supplier<Map<Object, Object>> sourceMapSupplier)
        {
            super(keyCache, sourceMapSupplier);
        }

        @Override
        public byte getCommandCode()
        {
            return BOOLEAN_STATE_FLAGS_COMMAND_CODE;
        }
    }

    static class ClipRectTransmitter extends MapTransmitter<List<Number>>
    {
        public ClipRectTransmitter(IKeyCache keyCache, Supplier<Map<Object, Object>> sourceMapSupplier)
        {
            super(keyCache, sourceMapSupplier);
        }

        @Override
        protected int writeValue(ByteArrayOutputStream stream, int n, List<Number> value)
        {
            int w = (int)(value.get(0).doubleValue() * UNIT_SIZE_PX);
            int h = (int)(value.get(1).doubleValue() * UNIT_SIZE_PX);
            stream.write((byte)(w & 0xFF));
            stream.write((byte)(h & 0xFF));
            stream.write((byte)((w >> 8) & 0x0F | (h >> 4) & 0xF0));
            return 3;
        }

        @Override
        public byte getCommandCode()
        {
            return CLIP_SIZE_MAP_COMMAND_CODE;
        }
    }

    static class LookVectorTransmitter extends MapTransmitter<List<Object>>
    {
        private FGPaintVectorBinaryCoder coder_;

        public LookVectorTransmitter(IKeyCache keyCache, Supplier<Map<Object, Object>> sourceMapSupplier)
        {
            super(keyCache, sourceMapSupplier);
            coder_ = new FGPaintVectorBinaryCoder();
        }

        @Override
        protected int writeValue(ByteArrayOutputStream stream, int n, List<Object> value)
        {
            byte[] array = new byte[131072];
            int lookVecLen = coder_.writeCoded(array, 0, value);
            stream.write((byte)(lookVecLen & 0xFF));
            stream.write((byte)((lookVecLen >> 8) & 0xFF));
            stream.write(array, 0, lookVecLen);
            return lookVecLen + 2;
        }

        @Override
        public byte getCommandCode()
        {
            return LOOK_VECTOR_MAP_COMMAND_CODE;
        }
    }

    static abstract class ListTransmitter<V> extends AbstractTransmitter<List<Object>>
    {
        private Supplier<List<Object>> sourceListSupplier_;

        ListTransmitter(Supplier<List<Object>> sourceListSupplier)
        {
            sourceListSupplier_ = sourceListSupplier;
        }

        public List<Object> getDiffToTransmit(List<Object> previousData, List<Object> newData)
        {
            return newData.isEmpty() || newData.equals(previousData) ? null : newData;
        }

        @Override
        public int writeBinary(ByteArrayOutputStream stream, int n, List<Object> data)
        {
            for (Object e : data)
            {
                int bytesWritten = writeValue(stream, n, (V)e);
                n += bytesWritten;
            }
            return n;
        }

        @Override
        public Supplier<List<Object>> getEmptyDataSupplier()
        {
            return ArrayList::new;
        }

        @Override
        public Supplier<List<Object>> getSourceDataSupplier()
        {
            return sourceListSupplier_;
        }

        protected abstract int writeValue(ByteArrayOutputStream stream, int n, V value);
    }

    static abstract class KeyListTransmitter extends ListTransmitter<Object>
    {
        private IKeyCache keyCache_;

        public KeyListTransmitter(IKeyCache keyCache, Supplier<List<Object>> sourceListSupplier)
        {
            super(sourceListSupplier);
            keyCache_ = keyCache;
        }

        @Override
        protected int writeValue(ByteArrayOutputStream stream, int n, Object value)
        {
            int uid = keyCache_.getUniqueId(value);
            stream.write((byte)(uid & 0xFF));
            stream.write((byte)((uid >> 8) & 0xFF));
            return 2;
        }
    }

    static class PaintAllTransmitter extends KeyListTransmitter
    {
        public PaintAllTransmitter(IKeyCache keyCache, Supplier<List<Object>> sourceListSupplier)
        {
            super(keyCache, sourceListSupplier);
        }

        @Override
        public byte getCommandCode()
        {
            return PAINT_ALL_LIST_COMMAND_CODE;
        }
    }

    static class KeyCahe implements IKeyCache
    {
        private int uid_ = 0;
        private Map<Object, Integer> cache_;

        KeyCahe()
        {
            cache_ = new HashMap<>();
        }

        @Override
        public int getUniqueId(Object key)
        {
            Integer cache = cache_.get(key);
            if (cache == null)
            {
                cache = Integer.valueOf(uid_);
                cache_.put(key, cache);

                //System.out.println("-DLTEMP- KeyCahe.getUniqueId CACHE " + key + " -> " + cache.intValue());

                uid_++;
            }
            return cache.intValue();
        }
    }

    static class FGContainerStateTransmitter
    {
        private static BinaryOperator THROWING_MERGER = (u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u));};

        private IKeyCache keyCache_;
        private Map<Byte, ?> cmdToLastData_;
        private Map<Byte, IDataTransmitter<Object>> cmdToDataTransmitter_;

        public FGContainerStateTransmitter(IFGModule fgModule)
        {
            keyCache_ = new KeyCahe();
            cmdToDataTransmitter_ = new LinkedHashMap<>();

            addDataTransmitter(new PositionMatrixMapTrasmitter(keyCache_, () -> fgModule.getComponentIdPathToPositionMatrix()));
            addDataTransmitter(new ViewportMatrixMapTrasmitter(keyCache_, () -> fgModule.getComponentIdPathToViewportMatrix()));
            addDataTransmitter(new ClipRectTransmitter(keyCache_, () -> fgModule.getComponentIdPathToClipRect()));
            addDataTransmitter(new LookVectorTransmitter(keyCache_, () -> fgModule.getComponentIdPathToLookVector()));
            addDataTransmitter(new ChildCountMapTransmitter(keyCache_, () -> fgModule.getComponentIdPathToChildCount()));
            addDataTransmitter(new BooleanFlagsMapTransmitter(keyCache_, () -> fgModule.getComponentIdPathToBooleanStateFlags()));
            addDataTransmitter(new PaintAllTransmitter(keyCache_, () -> fgModule.getPaintAllSequence2()));

            resetDataCache();
        }

        public Collection<ByteBuffer> computeDataDiffsToTransmit()
        {
            Map<Byte, Object> newDatas = cmdToDataTransmitter_.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> {
                        Object sourceData = e.getValue().getSourceDataSupplier().get();
                        if (sourceData == null)
                        {
                            System.out.println(
                                    "-DLTEMP- FGContainerStateTransmitter.computeDataDiffsToTransmit null source data for cmd " + e.getKey());
                        }
                        return sourceData;
                    },
                    THROWING_MERGER,
                    LinkedHashMap::new));
            Collection<ByteBuffer> result = newDatas.entrySet().stream()
                    .map(e -> {
                        IDataTransmitter<Object> transmitter = cmdToDataTransmitter_.get(e.getKey());
                        Object diff = transmitter.getDiffToTransmit(cmdToLastData_.get(e.getKey()), e.getValue());
                        return diff != null ? transmitter.convertToBinary(e.getKey().byteValue(), diff) : null;
                    })
                    .filter(b -> b != null)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            cmdToLastData_ = newDatas;
            return result;
        }

        public void resetDataCache()
        {
            cmdToLastData_ = cmdToDataTransmitter_.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getEmptyDataSupplier().get()));
        }

        private void addDataTransmitter(IDataTransmitter<?> dataTransmitter)
        {
            cmdToDataTransmitter_.put(Byte.valueOf(dataTransmitter.getCommandCode()), (IDataTransmitter<Object>) dataTransmitter);
        }
    }
}
