/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.remote;

import clojure.lang.Keyword;
import flatgui.core.*;
import flatgui.core.awt.FGMouseTargetComponentInfo;
import flatgui.core.engine.ui.FGRemoteAppContainer;

import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Temporary code for transition to new core. TODO Refactor further
 *
 * @author Denis Lebedev
 */
public class FGLegacyCoreGlue implements IFGContainer, IFGModule
{
    private FGAbstractModule glueModule_;
    private FGRemoteAppContainer container_;

    public FGLegacyCoreGlue(FGRemoteAppContainer container, GlueModule glueModule)
    {
        container_ = container;
        glueModule_ = glueModule;
    }

    public Collection<ByteBuffer> getDiffsToTransmit()
    {
        return container_.getDiffsToTransmit();
    }

    public Collection<ByteBuffer> getInitialDataToTransmit()
    {
        return container_.getInitialDataToTransmit();
    }

    //
    // Needed for FGWebContainerWrapper and FGContainerStateTransmitter
    //

    //  IFGModule

    // See flatgui.core.FGWebContainerWrapper.FGContainerStateTransmitter.stringPoolIdSupplier_
    // It supplies string pools ids for strings contained in look vector commands.
    // It accepts string+componentUid, and componentUid is index (from KeyCache) there.
    //
    // getStringPoolId method as passed as the implementation for stringPoolIdSupplier_
    //
    // It is not clear how FGAbstractModule#getStringPoolDiffs works because it does no convert
    // id paths (map keys) into indices (from KeyCache). Impl in this class does so.

    @Override
    public Integer getStringPoolId(String s, Object componentId)
    {
        if (!(componentId instanceof Integer))
        {
            throw new IllegalStateException();
        }
        //Integer componentUid = container_.getComponentUid((List<Object>) componentId);
        //return glueModule_.getStringPoolId(s, componentUid);

        // TODO Component UID -> KeyCacheIndex

        return glueModule_.getStringPoolId(s, componentId);
    }

    @Override
    public Map<Object, Object> getStringPoolDiffs(Map<Object, List<String>> idPathToStrings)
    {
        // TODO Component UID -> KeyCacheIndex

        return glueModule_.getStringPoolDiffs(idPathToStrings);


//        Map<Object, List<String>> componentUidToStrings = new HashMap<>(idPathToStrings.size());
//        for (List idPath : idPathToStrings.keySet())
//        {
//            Integer componentUid = container_.getComponentUid(idPath);
//            componentUidToStrings.put(componentUid, idPathToStrings.get(idPath));
//        }
//        // From this point this duplicates FGAppContainer#getStringPoolDiffs
//        Map<Object, Object> result = new HashMap<>();
//        componentUidToStrings.entrySet().stream().forEach(e -> {
//            Map<Integer, String> valDiff = glueModule_.putStrings(e.getKey(), e.getValue());
//            if (!valDiff.isEmpty())
//            {
//                result.put(e.getKey(), valDiff);
//            }
//        });
//        return result;
    }

    @Override
    public List<Object> getPaintAllSequence2()
    {
        // In old core impl, objects here were id paths. However uniqueness is
        // the only what matters in this case so new impl's indices are good.
        // Moreover, indices will exactly correspond to those in KeyCache
        return container_.getPaintAllSequence();
    }

    @Override
    public Map<Object, Map<Keyword, Object>> getComponentIdPathToComponent(Collection<List<Keyword>> paths)
    {
        // Next step of transition will be to get rid of id paths. It's enough to have indices to transfer data to remote client
        return container_.getComponentIdPathToComponent(paths);
    }

    // IFGContainer

    @Override
    public Function<FGEvolveInputData, Future<FGEvolveResultData>> connect(ActionListener eventFedCallback, Object hostContext)
    {
        return container_::evolveRemote;
    }

    @Override
    public Future<FGEvolveResultData> feedEvent(FGEvolveInputData inputData)
    {
        return container_.evolveRemote(inputData.getEvolveReason());
    }

    @Override
    public Future<FGEvolveResultData> feedTargetedEvent(List<Keyword> targetCellIdPath, FGEvolveInputData inputData)
    {
        return container_.evolveRemote(targetCellIdPath, inputData.getEvolveReason());
    }

    @Override
    public Future<FGEvolveResultData> feedTargetedEvent(List<Keyword> targetCellIdPath, Object evolveReason)
    {
        return container_.evolveRemote(targetCellIdPath, evolveReason);
    }

    @Override
    public <T> Future<T> submitTask(Callable<T> callable)
    {
        return container_.submitTask(callable);
    }

    // IFGContainer - needed for FGContainerWebSocket

    @Override
    public IFGInteropUtil getInterop()
    {
        return container_.getInteropUtil();
    }

    @Override
    public int getQueueSizeWaiting()
    {
        return container_.getQueueSizeWaiting();

    }

    @Override
    public void initialize()
    {
        container_.initialize();
    }

    @Override
    public void unInitialize()
    {
        container_.unInitialize();
    }

    @Override
    public boolean isActive()
    {
        return container_.isActive();
    }

    @Override
    public String getId()
    {
        return container_.getContainerId();
    }

    @Override
    public void addEvolveConsumer(IFGEvolveConsumer consumer)
    {
        // TODO
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public IFGModule getFGModule()
    {
        return this;
    }

    //
    //
    //

    // // // Innner

    public static class GlueModule extends FGAbstractModule
    {
        public GlueModule(String containerName)
        {
            super(containerName);
        }

        @Override
        public void evolve(List<Keyword> targetCellIds, Object inputEvent)
        {

        }

        @Override
        public Object getContainer()
        {
            return null;
        }
    }

    //
    // Unsupported methods
    //

    @Override
    public IFGModule getForkedFGModule(Object evolveReason)
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public List<Keyword> getLastMouseTargetIdPath()
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public void initInstance()
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public void evolve(List<Keyword> targetCellIds, Object inputEvent)
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public FGMouseTargetComponentInfo getMouseTargetInfoAt(double x, double y, FGComponentPath knownPath)
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public Object getContainer()
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public List<Keyword> getFocusedPath()
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public Map<List<Keyword>, Collection<Keyword>> getInputChannelSubscribers(Keyword channel)
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public List<Object> getPaintAllSequence()
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public List<Object> getPaintAllSequence(double clipX, double clipY, double clipW, double clipH)
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public Object getDirtyRectFromContainer()
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public List<Object> getPaintChangesSequence(Collection dirtyRects)
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }

    @Override
    public Set<List<Keyword>> getChangedComponentIdPaths()
    {
        throw new UnsupportedOperationException("Glue impl: method not implemented");
    }
}
