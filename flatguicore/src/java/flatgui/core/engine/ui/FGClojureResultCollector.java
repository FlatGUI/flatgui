/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import clojure.lang.PersistentHashMap;
import clojure.lang.Var;
import flatgui.core.engine.Container;
import flatgui.core.engine.IResultCollector;

import java.util.*;

/**
 * @author Denis Lebedev
 */
public class FGClojureResultCollector implements IResultCollector
{
    // TODO
    // paint all vector; look vectors; string pools; and all that GUI specific stuff

    private static final String FG_NS = "flatgui.core";
    private static final Var rebuildLook_ = clojure.lang.RT.var(FG_NS, "rebuild-look");

    private final Set<Integer> changedComponents_;

    private final List<List<Object>> lookVectors_;

    public FGClojureResultCollector()
    {
        changedComponents_ = new HashSet<>();
        lookVectors_ = new ArrayList<>();
    }

    @Override
    public void componentAdded(Integer componentUid)
    {
        if (lookVectors_.size() < componentUid.intValue())
        {
            int add = componentUid.intValue() - lookVectors_.size();
            for (int i=0; i<add; i++)
            {
                lookVectors_.add(null);
            }
        }
    }

    @Override
    public void componentRemoved(Integer componentUid)
    {
        lookVectors_.set(componentUid.intValue(), null);
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
            List<Object> lookVec = (List<Object>) rebuildLook_.invoke(componentClj);

            containerMutator.setValue(componentDataCache.getLookVecIndex(), lookVec);
        }

        changedComponents_.clear();
    }

    public List<Object> getLookVector(Integer componentUid)
    {
        return lookVectors_.get(componentUid.intValue());
    }
}
