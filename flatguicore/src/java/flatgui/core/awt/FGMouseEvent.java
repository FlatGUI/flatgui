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
import java.awt.event.MouseEvent;
import java.util.Collection;

/**
 * @author Denis Lebedev
 */
public class FGMouseEvent extends MouseEvent
{
    /**
     * Vector of mouse x relative coord for clicked target component path
     */
    private final Object xRelativeVec_;

    /**
     * Vector of mouse y relative coord for clicked target component path
     */
    private final Object yRelativeVec_;

    private final Collection<Object> targetIdPath_;

    public FGMouseEvent(
            Component source,
            int id,
            long when,
            int modifiers,
            int x,
            int y,
            int xAbs,
            int yAbs,
            int clickCount,
            boolean popupTrigger,
            int button,
            Object xRelativeVec,
            Object yRelativeVec,
            Collection<Object> targetIdPath)
    {
        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger, button);
        xRelativeVec_ = xRelativeVec;
        yRelativeVec_ = yRelativeVec;
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

    public Collection<Object> getTargetIdPath()
    {
        return targetIdPath_;
    }
}
