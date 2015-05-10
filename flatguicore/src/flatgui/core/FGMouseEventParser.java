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

import flatgui.core.awt.FGMouseEvent;
import flatgui.core.awt.FGMouseTargetComponentInfo;
import flatgui.core.awt.FGMouseWheelEvent;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.*;

/**
 * @author Denys Lebediev
 *         Date: 8/11/13
 *         Time: 4:19 PM
 */
class FGMouseEventParser implements IFGInputEventParser<MouseEvent>
{
    public static final String CHANNEL_NAME = "mouse";

//    public static final String MOUSE_X_VEC_GENERAL_PROPERTY = "MouseXVec";
//    public static final String MOUSE_Y_VEC_GENERAL_PROPERTY = "MouseYVec";


    private final int unitSizePx_;

    private boolean leftButtonDown_;

    // temporary, for debug
    static Collection<Object> latestPressedTargetCellIds_ = new HashSet<>();

    private FGComponentPath pressedPath_;

    private Object lastTargetIdPath_;
    private Object lastXRelativeVec_;
    private Object lastYRelativeVec_;
    private MouseEvent lastMouseEvent_;

    public FGMouseEventParser(int unitSizePx)
    {
        unitSizePx_ = unitSizePx;
    }

    @Override
    public Map<String, Object> initialize(IFGModule fgModule)
    {
        return null;
    }

    @Override
    public Map<String, Object> getTargetedPropertyValues(MouseEvent mouseEvent)
    {
        Map<String, Object> map = new HashMap<>();

        map.put(CHANNEL_NAME, mouseEvent);

        return map;
    }

    @Override
    public Map<MouseEvent, Collection<Object>> getTargetCellIds(MouseEvent mouseEvent, IFGModule fgModule, Map<String, Object> generalPropertyMap)
    {
        boolean newLeftButtonDown = (mouseEvent.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK;

        double mouseX = ((double)mouseEvent.getX()) / ((double)unitSizePx_);
        double mouseY = ((double)mouseEvent.getY()) / ((double)unitSizePx_);

        FGMouseTargetComponentInfo componentInfo = fgModule.getMouseTargetInfoAt(
                mouseX, mouseY, newLeftButtonDown && leftButtonDown_ ? pressedPath_ : null);
        FGComponentPath targetPath = componentInfo.getComponentPath();
        Object xRelativeVec = componentInfo.getXRelativeVec();
        Object yRelativeVec = componentInfo.getYRelativeVec();

        if (newLeftButtonDown)
        {
            latestPressedTargetCellIds_ = (Collection<Object>)targetPath.getTargetIdPath();

            if (!leftButtonDown_)
            {
                pressedPath_ = targetPath;
            }
            else
            {
                targetPath = pressedPath_;
            }
        }

        leftButtonDown_ = newLeftButtonDown;

        //System.out.println("-DLTEMP- FGMouseEventParser.getTargetCellIds PROCESSING " + mouseEvent);

        Map<MouseEvent, Collection<Object>> map = new LinkedHashMap<>();
        if (targetPath != null)
        {
            boolean targetChanged = lastTargetIdPath_ != null && !lastTargetIdPath_.equals(targetPath.getTargetIdPath());
            if (targetChanged)
            {
                map.put(deriveFGEvent(deriveWithIdAndNoButton(lastMouseEvent_, MouseEvent.MOUSE_EXITED),
                        lastXRelativeVec_,
                        lastYRelativeVec_,
                        (Collection<Object>) lastTargetIdPath_), (Collection<Object>) lastTargetIdPath_);
                map.put(deriveFGEvent(deriveWithIdAndNoButton(mouseEvent, MouseEvent.MOUSE_ENTERED),
                        xRelativeVec,
                        yRelativeVec,
                        (Collection<Object>) targetPath.getTargetIdPath()), (Collection<Object>) targetPath.getTargetIdPath());
            }
            map.put(deriveFGEvent(mouseEvent, xRelativeVec, yRelativeVec, (Collection<Object>) targetPath.getTargetIdPath()), (Collection<Object>) targetPath.getTargetIdPath());
            lastTargetIdPath_ = targetPath.getTargetIdPath();
            lastXRelativeVec_ = xRelativeVec;
            lastYRelativeVec_ = yRelativeVec;
            lastMouseEvent_ = mouseEvent;

//        for (MouseEvent e : map.keySet())
//        {
//            System.out.println("-DLTEMP- FGMouseEventParser.getTargetCellIds mouse target " + e.paramString() + " - " + map.get(e));
//        }
        }
        return map;
    }

    private MouseEvent deriveWithIdAndNoButton(MouseEvent e, int id)
    {
        return new MouseEvent((Component) e.getSource(), id, e.getWhen(), 0,
            e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(),
            0, false, MouseEvent.NOBUTTON);
    }

    private MouseEvent deriveFGEvent(MouseEvent e, Object xRelativeVec, Object yRelativeVec, Collection<Object> targetIdPath)
    {
//        public MouseWheelEvent (Component source, int id, long when, int modifiers,
//        int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger,
//        int scrollType, int scrollAmount, int wheelRotation, double preciseWheelRotation) {

        if (e instanceof MouseWheelEvent)
        {
            return new FGMouseWheelEvent(
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
                    ((MouseWheelEvent) e).getScrollType(),
                    ((MouseWheelEvent) e).getScrollAmount(),
                    ((MouseWheelEvent) e).getWheelRotation(),
                    ((MouseWheelEvent) e).getPreciseWheelRotation(),
                    xRelativeVec,
                    yRelativeVec,
                    targetIdPath);
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
                    xRelativeVec,
                    yRelativeVec,
                    targetIdPath);
        }
    }


}
