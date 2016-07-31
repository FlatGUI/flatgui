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

    @Override
    public void processComponentAfterIndexing(Container.IComponent component)
    {
        FGComponentDataCache componentDataCache = new FGComponentDataCache();

        componentDataCache.setLookVecIndex(getPropertyIndex(component, LOOK_VEC_KW));
        componentDataCache.setPositionMatrixIndex(getPropertyIndex(component, POSITION_MATRIX_KW));
        componentDataCache.setClipSizeIndex(getPropertyIndex(component, CLIP_SIZE_KW));

        component.setCustomData(componentDataCache);
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
        private int lookVecIndex_;
        private int positionMatrixIndex_;
        private int clipSizeIndex_;

        public int getLookVecIndex()
        {
            return lookVecIndex_;
        }

        public void setLookVecIndex(int lookVecIndex)
        {
            lookVecIndex_ = lookVecIndex;
        }

        public int getPositionMatrixIndex()
        {
            return positionMatrixIndex_;
        }

        public void setPositionMatrixIndex(int positionMatrixIndex)
        {
            positionMatrixIndex_ = positionMatrixIndex;
        }

        public int getClipSizeIndex()
        {
            return clipSizeIndex_;
        }

        public void setClipSizeIndex(int clipSizeIndex)
        {
            clipSizeIndex_ = clipSizeIndex;
        }
    }
}
