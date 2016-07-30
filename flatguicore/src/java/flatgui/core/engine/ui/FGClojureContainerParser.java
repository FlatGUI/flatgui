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
    private static final Keyword LOOK_VEK_KW = Keyword.intern("look-vec");

    @Override
    public void processComponentAfterIndexing(Container.IComponent component)
    {
        Integer lookVecIndex = component.getPropertyIndex(LOOK_VEK_KW);
        if (lookVecIndex == null)
        {
            throw new IllegalArgumentException(getClass().getSimpleName() +
                    ": requires container to have " + LOOK_VEK_KW + " property");
        }

        FGComponentDataCache componentDataCache = new FGComponentDataCache();
        componentDataCache.setLookVecIndex(lookVecIndex);

        component.setCustomData(componentDataCache);
    }

    public static class FGComponentDataCache
    {
        private int lookVecIndex_;

        public int getLookVecIndex()
        {
            return lookVecIndex_;
        }

        public void setLookVecIndex(int lookVecIndex)
        {
            lookVecIndex_ = lookVecIndex;
        }
    }
}
