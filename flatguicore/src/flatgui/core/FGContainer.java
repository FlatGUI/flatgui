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

import flatgui.core.awt.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * @author Denis Lebedev
 */
public class FGContainer implements IFGContainer
{
    //@todo
    private static final int UNIT_SIZE_PX = 64;

    private static final FGRepaintReasonParser REASON_PARSER = new FGRepaintReasonParser();
    static
    {
        REASON_PARSER.registerReasonClassParser(MouseEvent.class, new FGMouseEventParser(UNIT_SIZE_PX));
        REASON_PARSER.registerReasonClassParser(MouseWheelEvent.class, new FGMouseEventParser(UNIT_SIZE_PX));
        REASON_PARSER.registerReasonClassParser(KeyEvent.class, new FGKeyEventParser());
        REASON_PARSER.registerReasonClassParser(FGTimerEvent.class, new IFGRepaintReasonParser<FGTimerEvent>()
        {
            @Override
            public Map<String, Object> initialize(IFGModule fgModule)
            {return null;}
            @Override
            public Map<String, Object> getTargetedPropertyValues(FGTimerEvent fgTimerEvent)
            {return new HashMap<>();}
            @Override
            public Map<FGTimerEvent, Collection<Object>> getTargetCellIds(FGTimerEvent fgTimerEvent, IFGModule fgModule, Map<String, Object> generalPropertyMap)
            {return Collections.emptyMap();}
        });
    }

    private HostComponent hostComponent_;
    private FGAWTInteropUtil awtInteropUtil_;

    private Map<String, Object> containerProperties_;
    private Map<String, Object> targetedProperties_;

    private IFGModule module_;

    // @todo shutdown when needed (introduce uninitialize method here)
    private ExecutorService evolverExecutorService_;
    private ExecutorService painterExecutorService_;

    //private Future<?> cycleResult_;

    // Result: dirty rect
    private List<Future<Region>> cycleResults_;

    private Future<java.util.List<Object>> paintResult_;

    private DirtyRegionCollection dirtyRegionCollection_;

    public FGContainer(IFGModule module)
    {
        module_ = module;

        dirtyRegionCollection_ = new DirtyRegionCollection();

        evolverExecutorService_ = Executors.newSingleThreadExecutor();
        painterExecutorService_ = Executors.newSingleThreadExecutor();

        cycleResults_ = new ArrayList<>(256);

        hostComponent_ = new HostComponent(module_);
        awtInteropUtil_ = new FGAWTInteropUtil(hostComponent_, UNIT_SIZE_PX);

        containerProperties_ = new HashMap<>();
        containerProperties_.put(GENERAL_PROPERTY_UNIT_SIZE, UNIT_SIZE_PX);

        targetedProperties_ = new HashMap<>();

        hostComponent_.setFocusable(true);

        Consumer<Object> eventConsumer = this::cycle;

        hostComponent_.addMouseListener(new ContainerMouseListener(eventConsumer));
        hostComponent_.addMouseMotionListener(new ContainerMouseMotionListener(eventConsumer));
        hostComponent_.addMouseWheelListener(new ContainerMouseWheelListener(eventConsumer));
        hostComponent_.addKeyListener(new ContainerKeyListener(eventConsumer));

        Timer repaintTimer = new Timer("FlatGUI Blink Helper Timer", true);
        repaintTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                //EventQueue.invokeLater(() -> cycle(new FGTimerEvent()));
            }
        }, 250, 250);

        Map<String, Object> initialGeneralProperties = REASON_PARSER.initialize(module_);
        if (initialGeneralProperties != null)
        {
            containerProperties_.putAll(initialGeneralProperties);
        }
    }

    @Override
    public void initialize()
    {
    }

    @Override
    public void unInitialize()
    {
    }

    @Override
    public Component getContainerComponent()
    {
        return hostComponent_;
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
        return awtInteropUtil_;
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
            Map<String, Object> propertiesFromReason = REASON_PARSER.getTargetedPropertyValues(repaintReason);
            for (String propertyName : propertiesFromReason.keySet())
            {
                targetedProperties_.put(propertyName,
                        propertiesFromReason.get(propertyName));
            }
        }

        Map<String, Object> generalProperties = new HashMap<>();
        Map<Object, Collection<Object>> reasonMap = REASON_PARSER.getTargetCellIds(repaintReason, module_, generalProperties);
        if (generalProperties != null)
        {
            containerProperties_.putAll(generalProperties);
        }
        for (Object reason : reasonMap.keySet())
        {
            Collection<Object> targetCellIds_ = reasonMap.get(reason);

            if (targetCellIds_ == null || !targetCellIds_.isEmpty())
            {
                //cycleResult_ = evolverExecutorService_.submit(() -> module_.evolve(targetCellIds_, reason));
                cycleResults_.add(evolverExecutorService_.submit(() ->
                {
                    try
                    {
                        module_.evolve(targetCellIds_, reason);
                        java.util.List dirtyRect = (java.util.List) module_.getDirtyRectFromContainer();
                        if (dirtyRect != null)
                        {
                            double x = ((Double) dirtyRect.get(0)).doubleValue() * UNIT_SIZE_PX;
                            double y = ((Double) dirtyRect.get(1)).doubleValue() * UNIT_SIZE_PX;
                            double w = ((Double) dirtyRect.get(2)).doubleValue() * UNIT_SIZE_PX;
                            double h = ((Double) dirtyRect.get(3)).doubleValue() * UNIT_SIZE_PX;
                            return new Region(x, y, w, h);
                        }
                        else
                        {
                            return null;
                        }
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        return null;
                    }
                }));

                hostComponent_.repaint();
            }
        }
    }

    private class HostComponent extends Canvas
    {
        private IFGModule module_;
        private IFGPrimitivePainter primitivePainter_;

        private Image bufferImage_;

        private boolean appTriggered_ = false;

        HostComponent(IFGModule module)
        {
            setFocusTraversalKeysEnabled(false);

            module_ = module;
            primitivePainter_ = new FGDefaultPrimitivePainter(UNIT_SIZE_PX);
        }

        @Override
        public void paint(Graphics g)
        {
            //AffineTransform saveAT = ((Graphics2D)g).getTransform();
            //((Graphics2D)g).setTransform(saveAT);

            if (appTriggered_)
            {
                Collection<Region> dirtyRects = dirtyRegionCollection_.getRegionsToRepaint();
                if (dirtyRects != null)
                {
                    for (Region dirtyRect : dirtyRects)
                    {
                        int x = (int) dirtyRect.getX();
                        int y = (int) dirtyRect.getY();
                        int w = (int) dirtyRect.getW();
                        int h = (int) dirtyRect.getH();

                        g.drawImage(getPendingImage(), x, y, x + w, y + h, x, y, x + w, y + h, null);

                        //g.setColor(Color.WHITE);
                        //g.drawRect(x, y, w, h);

                        //System.out.println("-DLTEMP- HostComponent.paint ----------- container dirty rect: " + dirtyRect);
                    }
                }
                else
                {
                    //System.out.println("-DLTEMP- HostComponent.paint ----------- no container dirty rect");
                    g.drawImage(getPendingImage(), 0, 0, null);
                }
            }
            else
            {
                g.drawImage(getPendingImage(), 0, 0, null);
            }

            dirtyRegionCollection_.clear();
            appTriggered_ = false;

//            try
//            {
//                java.util.List<Object> pList = paintResult_.get();
//                paintSequence(g, pList);
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
        }

        @Override
        public void update(Graphics g)
        {
            try
            {
                //if (cycleResult_ != null && cycleResult_.get() == null)
                if (cycleResults_.size() > 0)
                {
                    for (Future<Region> f : cycleResults_)
                    {
                        Region r = f.get();
                        if (r != null)
                        {
                            dirtyRegionCollection_.addRegion(r);
                        }
                    }
                    cycleResults_.clear();

//                    java.util.List dirtyRect = (java.util.List) module_.getDirtyRectFromContainer();
//                    if (dirtyRect != null)
//                    {
//                        double x = ((Double) dirtyRect.get(0)).doubleValue() * UNIT_SIZE_PX;
//                        double y = ((Double) dirtyRect.get(1)).doubleValue() * UNIT_SIZE_PX;
//                        double w = ((Double) dirtyRect.get(2)).doubleValue() * UNIT_SIZE_PX;
//                        double h = ((Double) dirtyRect.get(3)).doubleValue() * UNIT_SIZE_PX;
//                        dirtyRegionCollection_.addRegion(new Region(x, y, w, h));
//                    }


                    Rectangle clipBounds = g.getClipBounds();
                    double clipX = clipBounds.getX() / UNIT_SIZE_PX;
                    double clipY = clipBounds.getY() / UNIT_SIZE_PX;
                    double clipW = clipBounds.getWidth() / UNIT_SIZE_PX;
                    double clipH = clipBounds.getHeight() / UNIT_SIZE_PX;

                    paintResult_ = painterExecutorService_.submit(() -> module_.getPaintAllSequence(clipX, clipY, clipW, clipH));
                    //paintResult_ = painterExecutorService_.submit(() -> module_.getPaintChangesSequence(null));

                    Graphics bg = getBufferGraphics();
                    ((Graphics2D)bg).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    //AffineTransform saveAT = ((Graphics2D)g).getTransform();

                    java.util.List<Object> pList = paintResult_.get();
                    paintSequence(bg, pList);

                    appTriggered_ = true;
                    paint(g);

                    //((Graphics2D)g).setTransform(saveAT);
                }
            }
            catch (InterruptedException | ExecutionException e)
            {
                e.printStackTrace();
            }
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
    }

    static class Region
    {
        private final double x_;
        private final double y_;
        private final double w_;
        private final double h_;

        Region(double x, double y, double w, double h)
        {
            x_ = x;
            h_ = h;
            w_ = w;
            y_ = y;
        }

        public double getX()
        {
            return x_;
        }

        public double getY()
        {
            return y_;
        }

        public double getW()
        {
            return w_;
        }

        public double getH()
        {
            return h_;
        }

        @Override
        public String toString()
        {
            return "Dirty rect [x="+x_+"; y="+y_+"; w="+w_+"; h="+h_+"]";
        }
    }

    static class DirtyRegionCollection
    {
        private Region compoundRegion_;

        //private List<Region> regions_;

        DirtyRegionCollection()
        {
            //regions_ = new ArrayList<>();
        }

        void addRegion(Region r)
        {
            if (compoundRegion_ == null)
            {
                compoundRegion_ = r;
            }
            else
            {
                compoundRegion_ = combine(compoundRegion_, r);
            }
            //regions_.add(r);
        }

        Collection<Region> getRegionsToRepaint()
        {
           return compoundRegion_ != null ? Arrays.asList(compoundRegion_) : null;
           //return regions_;
        }

        void clear()
        {
            compoundRegion_ = null;
            //regions_.clear();
        }

        private static Region combine(Region r1, Region r2)
        {
            double x1 = Math.min(r1.getX(), r2.getX());
            double y1 = Math.min(r1.getY(), r2.getY());
            double x2 = Math.max(r1.getX()+r1.getW(), r2.getX()+r2.getW());
            double y2 = Math.max(r1.getY()+r1.getH(), r2.getY()+r2.getH());
            return new Region(x1, y1, x2-x1, y2-y1);
        }
    }


    static class FGTimerEvent
    {

    }

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
}
