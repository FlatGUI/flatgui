/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine;

import java.util.List;

/**
 * @author Denis Lebedev
 */
public interface IResultCollector
{
    void componentAdded(Integer componentUid);

    void componentRemoved(Integer componentUid);

    void componentInitialized(Container container, Integer componentUid);

    void appendResult(Integer parentComponentUid, List<Object> path, Container.Node node, Object newValue);

    void postProcessAfterEvolveCycle(Container.IContainerAccessor containerAccessor, Container.IContainerMutator containerMutator);
}
