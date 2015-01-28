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

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Denys Lebediev
 *         Date: 10/1/13
 *         Time: 9:51 PM
 */
public interface IFGModule
{
    public void evolve(Collection<Object> targetCellIds, Object inputEvent);

    public FGMouseTargetComponentInfo getMouseTargetInfoAt(double x, double y, FGComponentPath knownPath);

    public Object getContainerObject();

    // Painting

    // old approach

    public List<Object> getPaintAllSequence();

    public List<Object> getPaintAllSequence(double clipX, double clipY, double clipW, double clipH);

    public Object getDirtyRectFromContainer();

    public List<Object> getPaintChangesSequence(Collection dirtyRects);

    // new approach

    public Map<Object, Object> getComponentIdPathToPositionMatrix();

    public Map<Object, Object> getComponentIdPathToViewportMatrix();

    public Map<Object, Object> getComponentIdPathToClipRect();

    public Map<Object, Object> getComponentIdPathToLookVector();

    public Map<Object, Object> getComponentIdPathToChildCount();

    public Map<Object, Object> getComponentIdPathToBooleanStateFlags();

    public List<Object> getPaintAllSequence2();

    public List<Object> getPaintChangedSequence2();

    //

    public static class FGComponentPath
    {
        private final Object targetComponentPath_;
        private final Object targetIdPath_;

        public FGComponentPath(Object targetComponentPath, Object targetIdPath)
        {
            if (targetComponentPath == null || targetIdPath == null)
            {
                throw new IllegalArgumentException();
            }
            targetComponentPath_ = targetComponentPath;
            targetIdPath_ = targetIdPath;
        }

        public Object getTargetComponentPath()
        {
            return targetComponentPath_;
        }

        public Object getTargetIdPath()
        {
            return targetIdPath_;
        }
    }

}
