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

import flatgui.core.FGComponentPath;

/**
 * @author Denis Lebedev
 */
public class FGMouseTargetComponentInfo
{
    private FGComponentPath componentPath_;
    private Object xRelativeVec_;
    private Object yRelativeVec_;

    public FGMouseTargetComponentInfo(FGComponentPath componentPath,
                                      Object xRelativeVec, Object yRelativeVec)
    {
        componentPath_ = componentPath;
        xRelativeVec_ = xRelativeVec;
        yRelativeVec_ = yRelativeVec;
    }


    public FGComponentPath getComponentPath()
    {
        return componentPath_;
    }

    public Object getXRelativeVec()
    {
        return xRelativeVec_;
    }

    public Object getYRelativeVec()
    {
        return yRelativeVec_;
    }
}
