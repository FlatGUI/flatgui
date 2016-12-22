/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import flatgui.core.awt.FGMouseEvent;
import flatgui.core.engine.Container;
import flatgui.core.engine.IInputEventParser;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.*;
import java.util.List;

/**
 * @author Denis Lebedev
 */
public class FGMouseEventParser implements IInputEventParser<MouseEvent, FGMouseEvent>
{
    // TODO
    // It looks like with touch screen it is possible to click component without entering it with mouse-move first. So
    // we probably need to generate MOUSE_ENTER in this case

    private final int unitSizePx_;

    private boolean targetReached_ = false;
    private double mouseXRel_;
    private double mouseYRel_;

    private boolean leftButtonDown_;
    private Integer pressedComponentId_;

    private MouseEvent lastMouseEvent_;
    private Integer lastComponentId_;

    public FGMouseEventParser(int unitSizePx)
    {
        unitSizePx_ = unitSizePx;
    }

    @Override
    public Map<FGMouseEvent, Integer> parseInputEvent(Container container, MouseEvent mouseEvent)
    {
        boolean newLeftButtonDown = (mouseEvent.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK;

        double mouseX = ((double)mouseEvent.getX()) / ((double)unitSizePx_);
        double mouseY = ((double)mouseEvent.getY()) / ((double)unitSizePx_);

        targetReached_ = false;
        Integer targetComponentUid = getTargetComponentUid(0, container, mouseX, mouseY);

        if (newLeftButtonDown)
        {
            if (!leftButtonDown_)
            {
                pressedComponentId_ = targetComponentUid;
            }
            else
            {
                boolean captureNeeded = true; // TODO false for textfield?
                if (captureNeeded)
                {
                    targetComponentUid = pressedComponentId_;
                }
            }
        }
        leftButtonDown_ = newLeftButtonDown;


        if (targetComponentUid != null)
        {
            Map<FGMouseEvent, Integer> m = new HashMap<>();

            boolean targetChanged = lastComponentId_ != null && !lastComponentId_.equals(targetComponentUid);
            if (targetChanged)
            {
                m.put(deriveFGEvent(deriveWithIdAndNoButton(lastMouseEvent_, MouseEvent.MOUSE_EXITED),
                        mouseXRel_,
                        mouseYRel_), lastComponentId_);
                m.put(deriveFGEvent(deriveWithIdAndNoButton(mouseEvent, MouseEvent.MOUSE_ENTERED),
                        mouseXRel_,
                        mouseYRel_), targetComponentUid);
            }
            m.put(deriveFGEvent(mouseEvent, mouseXRel_, mouseYRel_), targetComponentUid);

            lastMouseEvent_ = mouseEvent;
            lastComponentId_ = targetComponentUid;
            return m;
        }
        else
        {
            return Collections.emptyMap();
        }
    }

    private Integer getTargetComponentUid(Integer componentUid, Container rootContainer, double mouseX, double mouseY)
    {
        Container.IComponent component = rootContainer.getComponent(componentUid);

        FGClojureContainerParser.FGComponentDataCache componentDataCache =
                (FGClojureContainerParser.FGComponentDataCache) component.getCustomData();
        Integer pmIndex = componentDataCache.getPositionMatrixIndex();
        Integer csIndex = componentDataCache.getClipSizeIndex();

        List<List<Number>> positionMatrix = rootContainer.getPropertyValue(pmIndex);
        List<List<Number>> clipSize = rootContainer.getPropertyValue(csIndex);

        double x = positionMatrix.get(0).get(3).doubleValue();
        double y = positionMatrix.get(1).get(3).doubleValue();
        double w = clipSize.get(0).get(0).doubleValue();
        double h = clipSize.get(1).get(0).doubleValue();

        if (((FGClojureResultCollector)rootContainer.getResultCollector()).hasVisiblePopupChildren(componentUid) ||
                in(mouseX, x, x+w) && in(mouseY, y, y+h))
        {
            List<Integer> childIndices = component.getChildIndices();
            if (childIndices != null)
            {
                for (int i=childIndices.size()-1; i >= 0; i--)
                {
                    Integer childIndex = childIndices.get(i);
                    Container.IComponent childComponent = rootContainer.getComponent(childIndex);
                    FGClojureContainerParser.FGComponentDataCache childComponentDataCache =
                            (FGClojureContainerParser.FGComponentDataCache) childComponent.getCustomData();
                    Integer childVisibleIndex = childComponentDataCache.getVisibleIndex();
                    Object childVisible = rootContainer.getPropertyValue(childVisibleIndex);
                    boolean childVisibleBool = childVisible != null && !(childVisible instanceof Boolean && !((Boolean) childVisible).booleanValue());

                    if (childVisibleBool)
                    {
                        double mouseXRel = mouseX - x;
                        double mouseYRel = mouseY - y;
                        Integer target = getTargetComponentUid(childIndex, rootContainer, mouseXRel, mouseYRel);
                        if (target != null)
                        {
                            if (!targetReached_)
                            {
                                mouseXRel_ = mouseXRel;
                                mouseYRel_ = mouseYRel;
//
//                                System.out.println("-DLTEMP- FGMouseEventParser.getTargetComponentUid TGT(1) " + ((Container.ComponentAccessor) component).getComponentPath());
//
                                targetReached_ = true;
                            }
                            return target;
                        }
                    }
                }
            }
            if (!targetReached_)
            {
                mouseXRel_ = mouseX-x;
                mouseYRel_ = mouseY-y;
//
//                System.out.println("-DLTEMP- FGMouseEventParser.getTargetComponentUid TGT(2) " + ((Container.ComponentAccessor)component).getComponentPath());
//
                targetReached_ = true;
            }
            return componentUid;
        }
        else
        {
            return null;
        }
    }

    private static boolean in(double n, double min, double max)
    {
        return n >= min && n < max;
    }

    private static MouseEvent deriveWithIdAndNoButton(MouseEvent e, int id)
    {
        return new MouseEvent((Component) e.getSource(), id, e.getWhen(), 0,
                e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(),
                0, false, MouseEvent.NOBUTTON);
    }

    private static FGMouseEvent deriveFGEvent(MouseEvent e, double mouseXRel, double mouseYRel)
    {
//        public MouseWheelEvent (Component source, int id, long when, int modifiers,
//        int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger,
//        int scrollType, int scrollAmount, int wheelRotation, double preciseWheelRotation) {

        if (e instanceof MouseWheelEvent)
        {
//            return new FGMouseWheelEvent(
//                    (Component) e.getSource(),
//                    e.getID(),
//                    e.getWhen(),
//                    e.getModifiers(),
//                    e.getX(),
//                    e.getY(),
//                    e.getXOnScreen(),
//                    e.getYOnScreen(),
//                    e.getClickCount(),
//                    e.isPopupTrigger(),
//                    ((MouseWheelEvent) e).getScrollType(),
//                    ((MouseWheelEvent) e).getScrollAmount(),
//                    ((MouseWheelEvent) e).getWheelRotation(),
//                    ((MouseWheelEvent) e).getPreciseWheelRotation(),
//                    null,
//                    null,
//                    Double.valueOf(mouseXRel),
//                    Double.valueOf(mouseYRel),
//                    /*targetIdPath*/null);
            throw new UnsupportedOperationException("todo");
        }
        else
        {
            return new FGMouseEvent(
                    (Component) e.getSource(),
                    e.getID(),
                    e.getWhen(),
                    e.getModifiers(),
                    e.getX(),
                    e.getY(),
                    e.getXOnScreen(),
                    e.getYOnScreen(),
                    e.getClickCount(),
                    e.isPopupTrigger(),
                    e.getButton(),
                    null,
                    null,
                    Double.valueOf(mouseXRel),
                    Double.valueOf(mouseYRel),
                    /*targetIdPath*/null);
        }
    }
}
