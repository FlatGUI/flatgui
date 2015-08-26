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

import flatgui.core.FGContainer;
import flatgui.core.FGHostStateEvent;
import flatgui.core.IFGContainer;
import flatgui.core.IFGInteropUtil;
import flatgui.core.awt.FGDefaultPrimitivePainter;
import flatgui.core.awt.IFGPrimitivePainter;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
* @author Denis Lebedev
*/
public class HostComponent extends Canvas
{
    private IFGContainer fgContainer_;
    private IFGPrimitivePainter primitivePainter_;

    private Image bufferImage_;

    private boolean appTriggered_ = false;

    private final FGAWTInteropUtil interopUtil_;

    public HostComponent()
    {
        setFocusTraversalKeysEnabled(false);
        interopUtil_ = new FGAWTInteropUtil(FGContainer.UNIT_SIZE_PX);
        primitivePainter_ = new FGDefaultPrimitivePainter(FGContainer.UNIT_SIZE_PX);
        primitivePainter_.addFontChangeListener(e -> interopUtil_.setReferenceFont(e.getNewValue()));
        setFocusable(true);
    }

    public void initialize(IFGContainer fgContainer)
    {
        fgContainer_ = fgContainer;
    }

    public ActionListener getEventFedCallback()
    {
        return e -> repaint();
    }

    public void setInputEventConsumer(Consumer<Object> eventConsumer)
    {
        addMouseListener(new ContainerMouseListener(eventConsumer));
        addMouseMotionListener(new ContainerMouseMotionListener(eventConsumer));
        addMouseWheelListener(new ContainerMouseWheelListener(eventConsumer));
        addKeyListener(new ContainerKeyListener(eventConsumer));
        addComponentListener(new ContainerComponentListener(eventConsumer));
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
            ((Graphics2D)bg).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            interopUtil_.setReferenceGraphics(bg);

            java.util.List<Object> pList = paintResult.get();
            paintSequence(bg, pList);

            appTriggered_ = true;
            paint(g);
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
