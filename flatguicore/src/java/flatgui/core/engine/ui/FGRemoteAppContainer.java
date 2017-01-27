/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import clojure.lang.Keyword;
import flatgui.core.FGEvolveResultData;
import flatgui.core.engine.Container;
import flatgui.core.websocket.FGWebInteropUtil;

import java.awt.event.ActionListener;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author Denis Lebedev
 */
public class FGRemoteAppContainer extends FGAppContainer<FGWebInteropUtil>
{
    public FGRemoteAppContainer(String sessionId, Map<Object, Object> container)
    {
        this(sessionId, container, DFLT_UNIT_SIZE_PX);
    }

    public FGRemoteAppContainer(String sessionId, Map<Object, Object> container, int unitSizePx)
    {
        super(sessionId, container, new FGWebInteropUtil(unitSizePx), unitSizePx);
    }

    //
    // Special methods needed for FGLegacyCoreGlue - will be refactored
    //

    public Future<FGEvolveResultData> evolveRemote(Object evolveReason)
    {
        Future<FGEvolveResultData> future = getEvolverExecutorService().submit(() -> {
            evolveImpl(evolveReason);
            Set<Integer> changedComponentUids = getResultCollector().getChangedComponentsForRemote();
            Set<List> changedComponentPaths = new HashSet<>(changedComponentUids.size());
            for (Integer changedComponentUid : changedComponentUids)
            {
                List changedComponentPath = ((Container.ComponentAccessor)getContainer().getComponent(changedComponentUid)).getComponentPath();
                changedComponentPaths.add(changedComponentPath);
            }
            getResultCollector().clearChangedComponentsForWeb();
            return new FGLegacyGlueEvolveResultData(changedComponentPaths);
        });
        return future;
    }

    public Future<FGEvolveResultData> evolveRemote(List<Keyword> targetCellIdPath, Object evolveReason)
    {
        Future<FGEvolveResultData> future = getEvolverExecutorService().submit(() -> {
            evolveImpl((List)targetCellIdPath, evolveReason);
            Set<Integer> changedComponentUids = getResultCollector().getChangedComponentsForRemote();
            Set<List> changedComponentPaths = new HashSet<>(changedComponentUids.size());
            for (Integer changedComponentUid : changedComponentUids)
            {
                List changedComponentPath = ((Container.ComponentAccessor)getContainer().getComponent(changedComponentUid)).getComponentPath();
                changedComponentPaths.add(changedComponentPath);
            }
            getResultCollector().clearChangedComponentsForWeb();
            return new FGLegacyGlueEvolveResultData(changedComponentPaths);
        });
        return future;
    }

    public <T> Future<T> submitTask(Callable<T> callable)
    {
        return getEvolverExecutorService().submit(callable);
    }

    public int getQueueSizeWaiting()
    {
        return (int) (getEvolverExecutorService().getTaskCount() -
                getEvolverExecutorService().getCompletedTaskCount());
    }

    public List<Object> getPaintAllSequence()
    {
        // TODO 1 can know list size in advance
        // TODO 2 no need to do this on each cycle
        List<Object> sequence = new ArrayList<>();
        Container.IContainerAccessor containerAccessor = getContainer().getContainerAccessor();
        getResultCollector().collectPaintAllSequence(
                sequence,
                containerAccessor,
                Integer.valueOf(0));
        //System.out.println("PaintAllSeq: " + sequence);
        return sequence;
    }

    public Map<Object, Map<Keyword, Object>> getComponentIdPathToComponent(Collection<List<Keyword>> paths)
    {
        Map m = new HashMap<>();

        if (paths == null)
        {
            Collection<List<Objects>> allIdPaths = (Collection) getContainer().getAllIdPaths();
            paths = new HashSet<>();
            for (List<Objects> p : allIdPaths)
            {
                List pWithoutProperty  = new ArrayList<>(p.size());
                pWithoutProperty.addAll(p);
                pWithoutProperty.remove(pWithoutProperty.size()-1);
                paths.add(pWithoutProperty);
            }
        }

        for (List path : paths)
        {
            Integer index = getContainer().getComponentUid(path);
            Map component = getContainer().getComponent(index);
            m.put(index, component);
        }

        return m;
    }

    // getEvolveReasonToTargetPath was used only for transmitting cursor changes and only to
    // find out if there was mouse event among evolve reasons
    private static class FGLegacyGlueEvolveResultData extends FGEvolveResultData
    {
        public FGLegacyGlueEvolveResultData(Set changedPaths)
        {
            super(null, changedPaths);
        }

        @Override
        public Map<Object, List<Keyword>> getEvolveReasonToTargetPath()
        {
            throw new UnsupportedOperationException();
        }
    }


    //
    //
    //
}
