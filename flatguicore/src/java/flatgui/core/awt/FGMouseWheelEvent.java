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

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.Collection;

/**
 * @author Denis Lebedev
 */
public class FGMouseWheelEvent extends MouseWheelEvent
{
    /**
     * Vector of mouse x relative coord for clicked target component path
     */
    private final Object xRelativeVec_;

    /**
     * Vector of mouse x relative coord for clicked target component path
     */
    private final Object yRelativeVec_;

    private final Double xRel_;

    private final Double yRel_;

    private final Collection<Object> targetIdPath_;

    public FGMouseWheelEvent (Component source, int id, long when, int modifiers,
        int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger,
        int scrollType, int scrollAmount, int wheelRotation, double preciseWheelRotation,
        Object xRelativeVec, Object yRelativeVec,
        Double xRel, Double yRel,
        Collection<Object> targetIdPath)
    {
        super(source, id, when, modifiers,
                x, y, xAbs, yAbs, clickCount, popupTrigger,
                scrollType, scrollAmount, wheelRotation, preciseWheelRotation);
        xRelativeVec_ = xRelativeVec;
        yRelativeVec_ = yRelativeVec;
        xRel_ = xRel;
        yRel_ = yRel;
        targetIdPath_ = targetIdPath;
    }

    public Object getXRelativeVec()
    {
        return xRelativeVec_;
    }

    public Object getYRelativeVec()
    {
        return yRelativeVec_;
    }

    public Double getXRel()
    {
        return xRel_;
    }

    public Double getYRel()
    {
        return yRel_;
    }

    public Collection<Object> getTargetIdPath()
    {
        return targetIdPath_;
    }
}
