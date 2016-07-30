/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import flatgui.core.engine.Container;
import flatgui.core.engine.IResultCollector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Denis Lebedev
 */
public class FGResultCollector implements IResultCollector
{
    // TODO
    // paint all vector; look vectors; string pools; and all that GUI specific stuff

    private Set<Integer> changedComponents_;

    public FGResultCollector()
    {
        changedComponents_ = new HashSet<>();
    }

    @Override
    public void appendResult(List<Object> path, Integer componentUid, Object property, Object newValue)
    {

    }

    @Override
    public void postProcessAfterEvolveCycle(Container.IContainerAccessor containerAccessor, Container.IContainerMutator containerMutator)
    {
        // TODO re-generate look vectors in changed components


        changedComponents_.clear();
    }
}
