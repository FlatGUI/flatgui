/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import clojure.lang.Keyword;
import flatgui.core.engine.ClojureContainerParser;
import flatgui.core.engine.Container;

import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class FGClojureContainerParser extends ClojureContainerParser
{
    private static final Keyword LOOK_VEC_KW = Keyword.intern("look-vec");
    private static final Keyword POSITION_MATRIX_KW = Keyword.intern("position-matrix");
    private static final Keyword CLIP_SIZE_KW = Keyword.intern("clip-size");
    private static final Keyword POPUP_KW = Keyword.intern("popup");
    private static final Keyword VIEWPORT_MATRIX_KW = Keyword.intern("viewport-matrix");
    private static final Keyword CHILDREN_KW = Keyword.intern("children");
    private static final Keyword VISIBLE_KW = Keyword.intern("visible");

    private static final Keyword FOCUS_STATE_KW = Keyword.intern("focus-state");
    private static final Keyword MODE_KW = Keyword.intern("mode");
    private static final Keyword HAS_FOCUS_KW = Keyword.intern("has-focus");
    private static final Keyword PARENT_OF_FOCUSED_KW = Keyword.intern("parent-of-focused");
    private static final Keyword FOCUSED_CHILD_KW = Keyword.intern("focused-child");

    @Override
    public void processComponentAfterIndexing(Container.IComponent component)
    {
        FGComponentDataCache componentDataCache = new FGComponentDataCache();

        componentDataCache.setLookVecIndex(getPropertyIndex(component, LOOK_VEC_KW));
        componentDataCache.setPositionMatrixIndex(getPropertyIndex(component, POSITION_MATRIX_KW));
        componentDataCache.setClipSizeIndex(getPropertyIndex(component, CLIP_SIZE_KW));
        componentDataCache.setPopupIndex(getPropertyIndex(component, POPUP_KW));
        componentDataCache.setViewportMatrixIndex(getPropertyIndex(component, VIEWPORT_MATRIX_KW));
        componentDataCache.setChildrenIndex(component.getPropertyIndex(CHILDREN_KW));
        componentDataCache.setVisibleIndex(component.getPropertyIndex(VISIBLE_KW));

        componentDataCache.setFocusStateIndex(component.getPropertyIndex(FOCUS_STATE_KW));

        component.setCustomData(componentDataCache);
    }

    public static Object getFocusMode(Map<Object, Object> focusState)
    {
        return focusState.get(MODE_KW);
    }

    public static boolean hasFocus(Object focusMode)
    {
        return HAS_FOCUS_KW.equals(focusMode);
    }

    public static boolean parentOfFocused(Object focusMode)
    {
        return PARENT_OF_FOCUSED_KW.equals(focusMode);
    }

    public static Object getFocusedChildId(Map<Object, Object> focusState)
    {
        return focusState.get(FOCUSED_CHILD_KW);
    }

    private static Integer getPropertyIndex(Container.IComponent component, Object property)
    {
        Integer index = component.getPropertyIndex(property);
        if (index == null)
        {
            throw new IllegalArgumentException(FGClojureContainerParser.class.getSimpleName() +
                    ": requires container to have " + property + " property");
        }
        return index;
    }

    public static class FGComponentDataCache
    {
        private Integer lookVecIndex_;
        private Integer positionMatrixIndex_;
        private Integer clipSizeIndex_;
        private Integer popupIndex_;
        private Integer viewportMatrixIndex_;
        private Integer childrenIndex_;
        private Integer visibleIndex_;

        private Integer focusStateIndex_;


        public Integer getLookVecIndex()
        {
            return lookVecIndex_;
        }

        public void setLookVecIndex(Integer lookVecIndex)
        {
            lookVecIndex_ = lookVecIndex;
        }

        public Integer getPositionMatrixIndex()
        {
            return positionMatrixIndex_;
        }

        public void setPositionMatrixIndex(Integer positionMatrixIndex)
        {
            positionMatrixIndex_ = positionMatrixIndex;
        }

        public Integer getClipSizeIndex()
        {
            return clipSizeIndex_;
        }

        public void setClipSizeIndex(Integer clipSizeIndex)
        {
            clipSizeIndex_ = clipSizeIndex;
        }

        public Integer getPopupIndex()
        {
            return popupIndex_;
        }

        public void setPopupIndex(Integer popupIndex)
        {
            popupIndex_ = popupIndex;
        }

        public Integer getViewportMatrixIndex()
        {
            return viewportMatrixIndex_;
        }

        public void setViewportMatrixIndex(Integer viewportMatrixIndex)
        {
            viewportMatrixIndex_ = viewportMatrixIndex;
        }

        public Integer getChildrenIndex()
        {
            return childrenIndex_;
        }

        public void setChildrenIndex(Integer childrenIndex)
        {
            childrenIndex_ = childrenIndex;
        }

        public Integer getVisibleIndex()
        {
            return visibleIndex_;
        }

        public void setVisibleIndex(Integer visibleIndex)
        {
            visibleIndex_ = visibleIndex;
        }

        public Integer getFocusStateIndex()
        {
            return focusStateIndex_;
        }

        public void setFocusStateIndex(Integer focusStateIndex)
        {
            focusStateIndex_ = focusStateIndex;
        }
    }
}
