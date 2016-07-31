/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Denis Lebedev
 */
public class AppContainer<ContainerParser extends Container.IContainerParser, ResultCollector extends IResultCollector>
{
    private ContainerParser containerParser_;
    private ResultCollector resultCollector_;
    private Map<Object, Object> containerMap_;
    private Container container_;

    private final InputEventParser reasonParser_;
    private ThreadPoolExecutor evolverExecutorService_;

    private boolean active_ = false;

    public AppContainer(ContainerParser containerParser, ResultCollector resultCollector, Map<Object, Object> container)
    {
        containerParser_ = containerParser;
        resultCollector_ = resultCollector;
        containerMap_ = container;
        reasonParser_ = new InputEventParser();
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

    public Container.IContainerAccessor getContainerAccessor()
    {
        return container_.getContainerAccessor();
    }

    public void evolve(List<Object> targetPath, Object evolveReason)
    {
        evolverExecutorService_.submit(() -> container_.evolve(targetPath, evolveReason));
    }

    public void evolve(Integer componentUid, Object evolveReason)
    {
        evolverExecutorService_.submit(() -> container_.evolve(componentUid, evolveReason));
    }

    public void evolve(Object evolveReason)
    {
        evolverExecutorService_.submit(() -> {
            Map<Object, Integer> eventsToTargetIndex = reasonParser_.parseInputEvent(getContainer(), evolveReason);
            for (Object event : eventsToTargetIndex.keySet())
            {
                evolve(eventsToTargetIndex.get(event), event);
            }
        });
    }

    protected final InputEventParser getInputEventParser()
    {
        return reasonParser_;
    }

    protected final Container getContainer()
    {
        return container_;
    }

    protected final ContainerParser getReasonParser()
    {
        return containerParser_;
    }

    protected final ResultCollector getResultCollector()
    {
        return resultCollector_;
    }
}
