/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import clojure.lang.PersistentHashMap;
import clojure.lang.Var;
import flatgui.core.engine.Container;
import flatgui.core.engine.IResultCollector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Denis Lebedev
 */
public class FGClojureResultCollector implements IResultCollector
{
    // TODO
    // paint all vector; look vectors; string pools; and all that GUI specific stuff

    private static final String FG_NS = "flatgui.core";
    private static final Var rebuildLook_ = clojure.lang.RT.var(FG_NS, "rebuild-look");

    private Set<Integer> changedComponents_;

    public FGClojureResultCollector()
    {
        changedComponents_ = new HashSet<>();
    }

    @Override
    public void appendResult(List<Object> path, Integer componentUid, Object property, Object newValue)
    {
        changedComponents_.add(componentUid);
    }

    @Override
    public void postProcessAfterEvolveCycle(Container.IContainerAccessor containerAccessor, Container.IContainerMutator containerMutator)
    {
        // TODO (*1) maybe rework deflookfn to utilize indexed access to properties

        for (Integer changedComponentUid : changedComponents_)
        {
            Container.IComponent componentAccessor = containerAccessor.getComponent(changedComponentUid.intValue());
            FGClojureContainerParser.FGComponentDataCache componentDataCache =
                    (FGClojureContainerParser.FGComponentDataCache) componentAccessor.getCustomData();

            // TODO creating Clojure map should not be needed after (*1)
            Object componentClj = PersistentHashMap.create(componentAccessor);
            Object lookVec = rebuildLook_.invoke(componentClj);

            containerMutator.setValue(componentDataCache.getLookVecIndex(), lookVec);
        }

        changedComponents_.clear();
    }
}
