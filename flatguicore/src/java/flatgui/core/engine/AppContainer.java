/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Denis Lebedev
 */
public class AppContainer<ContainerParser extends Container.IContainerParser, ResultCollector extends IResultCollector>
{
    private final String containerId_;

    private ContainerParser containerParser_;
    private ResultCollector resultCollector_;
    private Map<Object, Object> containerMap_;
    private Container container_;

    private final InputEventParser reasonParser_;
    private ThreadPoolExecutor evolverExecutorService_;

    private boolean active_ = false;

    public AppContainer(String containerId, ContainerParser containerParser, ResultCollector resultCollector, Map<Object, Object> container)
    {
        containerId_ = containerId;
        containerParser_ = containerParser;
        resultCollector_ = resultCollector;
        containerMap_ = container;
        reasonParser_ = new InputEventParser();
    }

    public final String getContainerId()
    {
        return containerId_;
    }

    public void initialize()
    {
        evolverExecutorService_ = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        Future<Container> containerFuture =
                evolverExecutorService_.submit(() -> new Container(containerParser_, resultCollector_, containerMap_));
        try
        {
            container_ = containerFuture.get();
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
        active_ = true;
    }

    public void unInitialize()
    {
        active_ = false;
        evolverExecutorService_.shutdown();
    }

    public boolean isActive()
    {
        return active_;
    }

    public final Container.IContainerAccessor getContainerAccessor()
    {
        return container_.getContainerAccessor();
    }

    public final Integer getComponentUid(List<Object> componentPath)
    {
        return container_.getComponentUid(componentPath);
    }

    public void evolve(List<Object> targetPath, Object evolveReason)
    {
        evolverExecutorService_.submit(() -> container_.evolve(targetPath, evolveReason));
    }

    public void evolve(Integer componentUid, Object evolveReason)
    {
        evolverExecutorService_.submit(() -> container_.evolve(componentUid, evolveReason));
    }

    public Future<?> evolve(Object evolveReason)
    {
        return evolverExecutorService_.submit(() -> evolveImpl(evolveReason));
    }

    protected void evolveImpl(Object evolveReason)
    {
        Map<Object, Integer> eventsToTargetIndex;
        try
        {
            eventsToTargetIndex = reasonParser_.parseInputEvent(getContainer(), evolveReason);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            eventsToTargetIndex = Collections.emptyMap();
        }

        for (Object event : eventsToTargetIndex.keySet())
        {
            container_.evolve(eventsToTargetIndex.get(event), event);
        }
    }

    protected void evolveImpl(List<Object> targetPath, Object evolveReason)
    {
        container_.evolve(targetPath, evolveReason);
    }

    protected final ThreadPoolExecutor getEvolverExecutorService()
    {
        return evolverExecutorService_;
    }

    protected final InputEventParser getInputEventParser()
    {
        return reasonParser_;
    }

    protected final Container getContainer()
    {
        return container_;
    }

    protected final ContainerParser getContainerParser()
    {
        return containerParser_;
    }

    protected final ResultCollector getResultCollector()
    {
        return resultCollector_;
    }
}
