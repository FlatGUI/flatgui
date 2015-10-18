/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core.awt;

import clojure.lang.Keyword;
import clojure.lang.Var;
import flatgui.core.*;
import flatgui.core.util.Tuple;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author Denis Lebedev
*/
public class HostComponent extends Canvas
{
    private static final Var extractCursor_ = clojure.lang.RT.var(IFGModule.RESPONSE_FEED_NS, "extract-cursor");
    private static final Var getDataForClipboard_ = clojure.lang.RT.var(IFGModule.RESPONSE_FEED_NS, "get-data-for-clipboard");

    private static final Map<Keyword, Integer> FG_TO_AWT_CUSROR_MAP;
    static
    {
        Map<Keyword, Integer> m = new HashMap<>();

        m.put(Keyword.intern("wait"), Cursor.WAIT_CURSOR);
        m.put(Keyword.intern("text"), Cursor.TEXT_CURSOR);
        m.put(Keyword.intern("ns-resize"), Cursor.N_RESIZE_CURSOR);
        m.put(Keyword.intern("ew-resize"), Cursor.W_RESIZE_CURSOR);
        m.put(Keyword.intern("nesw-resize"), Cursor.NE_RESIZE_CURSOR);
        m.put(Keyword.intern("nwse-resize"), Cursor.NW_RESIZE_CURSOR);

        FG_TO_AWT_CUSROR_MAP = Collections.unmodifiableMap(m);
    }


    private IFGContainer fgContainer_;
    private IFGPrimitivePainter primitivePainter_;

    private Image bufferImage_;

    private boolean appTriggered_ = false;

    private final FGAWTInteropUtil interopUtil_;

    private Font lastUserDefinedFont_ = null;

    private Function<Object, Future<FGEvolveResultData>> feedFn_;

    Future<FGEvolveResultData> changedPathsFuture_;

    public HostComponent()
    {
        setFocusTraversalKeysEnabled(false);
        interopUtil_ = new FGAWTInteropUtil(FGContainer.UNIT_SIZE_PX);
        primitivePainter_ = new FGDefaultPrimitivePainter(FGContainer.UNIT_SIZE_PX);
        primitivePainter_.addFontChangeListener(e -> {
            lastUserDefinedFont_ = e.getNewValue();
            interopUtil_.setReferenceFont(lastUserDefinedFont_);});
        setFocusable(true);
    }

    public final IFGInteropUtil getInterop()
    {
        return interopUtil_;
    }

    public void initialize(IFGContainer fgContainer)
    {
        fgContainer_ = fgContainer;
    }

    public ActionListener getEventFedCallback()
    {
        return e -> repaint();
    }

    public void setInputEventConsumer(Function<Object, Future<FGEvolveResultData>> feedFn)
    {
        feedFn_ = feedFn;

        Consumer<Object> eventConsumer = this::acceptEvolveReason;

        addMouseListener(new ContainerMouseListener(eventConsumer));
        addMouseMotionListener(new ContainerMouseMotionListener(eventConsumer));
        addMouseWheelListener(new ContainerMouseWheelListener(eventConsumer));
        addKeyListener(new ContainerKeyListener(eventConsumer));
        addComponentListener(new ContainerComponentListener(eventConsumer));
        setupBlinkHelperTimer(eventConsumer);
    }

    public static Timer setupBlinkHelperTimer(Consumer<Object> timerEventConsumer)
    {
        Timer blinkTimer = new Timer("FlatGUI Blink Helper Timer", true);
        blinkTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                timerEventConsumer.accept(new FGTimerEvent(System.currentTimeMillis()));
            }
        }, 530, 530);
        return blinkTimer;
    }

    @Override
    public void paint(Graphics g)
    {
        if (appTriggered_)
        {
            // TODO possibly use dirty rects to optimize
            g.drawImage(getPendingImage(), 0, 0, null);
        }
        else
        {
            g.drawImage(getPendingImage(), 0, 0, null);
        }

        appTriggered_ = false;
    }

    @Override
    public void update(Graphics g)
    {
        try
        {
            Rectangle clipBounds = g.getClipBounds();
            double clipX = clipBounds.getX() / FGContainer.UNIT_SIZE_PX;
            double clipY = clipBounds.getY() / FGContainer.UNIT_SIZE_PX;
            double clipW = clipBounds.getWidth() / FGContainer.UNIT_SIZE_PX;
            double clipH = clipBounds.getHeight() / FGContainer.UNIT_SIZE_PX;

            Future<java.util.List<Object>> paintResult = fgContainer_.submitTask(() -> fgContainer_.getFGModule().getPaintAllSequence(clipX, clipY, clipW, clipH));

            Graphics bg = getBufferGraphics();
            ((Graphics2D) bg).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (lastUserDefinedFont_ != null)
            {
                bg.setFont(lastUserDefinedFont_);
            }
            else
            {
                interopUtil_.setReferenceFont(bg.getFont());
            }
            interopUtil_.setReferenceGraphics(bg);

            java.util.List<Object> pList = paintResult.get();
            paintSequence(bg, pList);

            appTriggered_ = true;
            paint(g);

            FGEvolveResultData evolveResultData = changedPathsFuture_.get();
            Set<Object> reasons = evolveResultData.getEvolveReasonToTargetPath().keySet();
            if (!reasons.isEmpty() && reasons.stream().anyMatch(r -> r instanceof MouseEvent))
            {
                Collection<List<Keyword>> targetComponentPaths = evolveResultData.getEvolveReasonToTargetPath().values();
                Map<List<Keyword>, Map<Keyword, Object>> targetIdPathToComponent = fgContainer_.getFGModule().getComponentIdPathToComponent(targetComponentPaths);
                Keyword c = resolveCursor(targetIdPathToComponent, fgContainer_);
                Integer cursor = c != null ? FG_TO_AWT_CUSROR_MAP.get(c) : null;
                setCursor(cursor != null ? Cursor.getPredefinedCursor(cursor.intValue()) : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
        catch (InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }
    }

    public static Keyword resolveCursor(Map<java.util.List<Keyword>, Map<Keyword, Object>> idPathToComponent,
                                        IFGContainer fgContainer)
    {
        Map<java.util.List<Keyword>, Keyword> componentToCursor =
            idPathToComponent.entrySet().stream()
                .map(e -> Tuple.pair(e.getKey(), extractCursor_.invoke(e.getValue())))
                .filter(t -> t.getSecond() != null)
                .collect(Collectors.toMap(t -> t.getFirst(), t -> t.getSecond()));

        if (componentToCursor.isEmpty())
        {
            // Default cursor
            return null;
        }
        else
        {
            Keyword c = componentToCursor.get(fgContainer.getLastMouseTargetIdPath());
            if (c == null)
            {
                c = componentToCursor.values().stream().findAny().orElse(null);
            }
            return c;
        }
    }

    // TODO add content type info; support other content types
    public static String getTextForClipboard(IFGContainer fgContainer)
    {
        Object data = getDataForClipboard_.invoke(fgContainer.getFGModule().getContainerObject());
        return data != null ? data.toString() : null;
    }

    Image getPendingImage()
    {
        return bufferImage_;
    }

    private Graphics getBufferGraphics()
    {
        if (bufferImage_ == null)
        {
            bufferImage_ = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        }
        return bufferImage_.getGraphics();
    }

    private int paintSequence(Graphics g, java.util.List<Object> paintingSequence)
    {
       // System.out.println("paintSequence starts at " + System.currentTimeMillis());

        int painted = 0;

        for (Object obj : paintingSequence)
        {
            // Null is possible here. It is allowed for look functions to
            // be able to use condtionals easy

            if (obj instanceof java.util.List)
            {
                primitivePainter_.paintPrimitive(g, (java.util.List<Object>)obj);
                painted++;
            }
            else if (obj != null)
            {
                System.out.println("Error: not a list: " + paintingSequence);
            }
        }

      //  System.out.println("paintSequence ends at " + System.currentTimeMillis());

        return painted;
    }

    private void acceptEvolveReason(Object evolveReason)
    {
        changedPathsFuture_ = feedFn_.apply(evolveReason);
    }

    // Inner classes

    private static class ContainerListener
    {
        private final Consumer<Object> eventConsumer_;

        ContainerListener(Consumer<Object> eventConsumer)
        {
            eventConsumer_ = eventConsumer;
        }

        protected final void eventImpl(Object e)
        {
            eventConsumer_.accept(e);
        }
    }

    private static class ContainerMouseListener extends ContainerListener implements MouseListener
    {
        ContainerMouseListener(Consumer<Object> eventConsumer)
        {super(eventConsumer);}
        @Override
        public void mouseClicked(MouseEvent e)
        {eventImpl(e);}
        @Override
        public void mousePressed(MouseEvent e)
        {eventImpl(e);}
        @Override
        public void mouseReleased(MouseEvent e)
        {eventImpl(e);}
        @Override
        public void mouseEntered(MouseEvent e)
        {eventImpl(e);}
        @Override
        public void mouseExited(MouseEvent e)
        {eventImpl(e);}
    }

    private static class ContainerMouseMotionListener extends ContainerListener implements MouseMotionListener
    {
        ContainerMouseMotionListener(Consumer<Object> eventConsumer)
        {super(eventConsumer);}
        @Override
        public void mouseDragged(MouseEvent e)
        {eventImpl(e);}
        @Override
        public void mouseMoved(MouseEvent e)
        {eventImpl(e);}
    }

    private static class ContainerMouseWheelListener extends ContainerListener implements MouseWheelListener
    {
        ContainerMouseWheelListener(Consumer<Object> eventConsumer)
        {super(eventConsumer);}
        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {eventImpl(e);}
    }

    private static class ContainerKeyListener extends ContainerListener implements KeyListener
    {
        ContainerKeyListener(Consumer<Object> eventConsumer)
        {super(eventConsumer);}
        @Override
        public void keyTyped(KeyEvent e)
        {eventImpl(e);}
        @Override
        public void keyPressed(KeyEvent e)
        {eventImpl(e);}
        @Override
        public void keyReleased(KeyEvent e)
        {eventImpl(e);}
    }

    private static class ContainerComponentListener extends ContainerListener implements ComponentListener
    {
        ContainerComponentListener(Consumer<Object> eventConsumer)
        {super(eventConsumer);}
        @Override
        public void componentResized(ComponentEvent e)
        {eventImpl(parseResizeEvent(e));}
        @Override
        public void componentMoved(ComponentEvent e) {}
        @Override
        public void componentShown(ComponentEvent e) {}
        @Override
        public void componentHidden(ComponentEvent e) {}

        private static FGHostStateEvent parseResizeEvent(ComponentEvent e)
        {
            return FGHostStateEvent.createHostSizeEvent(e.getComponent().getSize());
        }
    }
}
