/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */
package flatgui.core;

import clojure.lang.Keyword;
import clojure.lang.Var;

import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * @author Denis Lebedev
 *
 * // TODO synchronization here?
 *
 */
public class FGContainer implements IFGContainer
{
    private static final String REGISTER_FN_NAME = "register-container";

    private static final String FGC_NS = "flatgui.comlogic";
    private static final String PROPERTY_CHANGED_FN = "property-changed?";
    private static final String GET_CHANGED_PROPERTIES_BY_PATH = "get-changed-properties-by-path";

    private static final Var propetyChanged_ = clojure.lang.RT.var(FGC_NS, PROPERTY_CHANGED_FN);
    private static final Var getChangedPropertiesByPath_ = clojure.lang.RT.var(FGC_NS, GET_CHANGED_PROPERTIES_BY_PATH);

    private static final Var clearForks_ = clojure.lang.RT.var(IFGModule.FG_CORE_NAMESPACE, "app-clear-forks");
    private static final Var useFork_ = clojure.lang.RT.var(IFGModule.FG_CORE_NAMESPACE, "app-use-fork");

    private final FGInputEventParser reasonParser_;

    private final String containerId_;
    private final IFGModule module_;
    private final Map<Object, IFGModule> forks_;
    private final Map<Object, Future<FGEvolveResultData>> forkResults_;
    private final IFGInteropUtil interopUtil_;

    private boolean active_ = false;

    private ExecutorService evolverExecutorService_;

    private ActionListener eventFedCallback_;

    private List<IFGEvolveConsumer> evolveConsumers_;

    private FGMouseEventParser mouseEventParser_;

    public FGContainer(IFGTemplate template, IFGInteropUtil interopUtil)
    {
        this(template, template.getContainerVarName(), interopUtil);
    }

    public FGContainer(IFGTemplate template, String containerId, IFGInteropUtil interopUtil)
    {
        Var containerVar = clojure.lang.RT.var(template.getContainerNamespace(), template.getContainerVarName());
        Var registerFn = clojure.lang.RT.var(FGModule.FG_CORE_NAMESPACE, REGISTER_FN_NAME);
        Object container = containerVar.get();
        registerFn.invoke(containerId, container, interopUtil);

        Keyword containerIdInternal = (Keyword) ((Map)container).get(Keyword.intern("id"));

        containerId_ = containerId;
        module_ = new FGModule(containerId);
        forks_ = new HashMap<>();
        forkResults_ = new HashMap<>();

        interopUtil_ = interopUtil;

        mouseEventParser_ = new FGMouseEventParser(UNIT_SIZE_PX);
        reasonParser_ = new FGInputEventParser();
        reasonParser_.registerReasonClassParser(MouseEvent.class, mouseEventParser_);
        reasonParser_.registerReasonClassParser(MouseWheelEvent.class, new FGMouseEventParser(UNIT_SIZE_PX));
        reasonParser_.registerReasonClassParser(KeyEvent.class, new FGKeyEventParser());
        reasonParser_.registerReasonClassParser(FGClipboardEvent.class, new FGClipboardEventEventParser());
        reasonParser_.registerReasonClassParser(FGTimerEvent.class, new FGTimerEventParser());
        reasonParser_.registerReasonClassParser(FGHostStateEvent.class, (fgHostStateEvent, fgModule) -> {
            Map<FGHostStateEvent, List<Keyword>> map = new HashMap<>();
            map.put(fgHostStateEvent, Arrays.asList(containerIdInternal));
            return map;
        });

        evolveConsumers_ = new ArrayList<>();
    }

    @Override
    public String getId()
    {
        return containerId_;
    }

    @Override
    public void initialize()
    {
        evolverExecutorService_ = Executors.newSingleThreadExecutor();
        active_ = true;
    }

    @Override
    public void unInitialize()
    {
        active_ = false;
        evolverExecutorService_.shutdown();
    }

    @Override
    public boolean isActive()
    {
        return active_;
    }

    @Override
    public void addEvolveConsumer(IFGEvolveConsumer consumer)
    {
        evolveConsumers_.add(consumer);
    }

    @Override
    public IFGModule getFGModule()
    {
        return module_;
    }

    @Override
    public IFGModule getForkedFGModule(Object evolveReason)
    {
        return forks_.get(evolveReason);
    }

    @Override
    public IFGInteropUtil getInterop()
    {
        return interopUtil_;
    }

    @Override
    public Function<FGEvolveInputData, Future<FGEvolveResultData>> connect(ActionListener eventFedCallback, Object hostContext)
    {
        eventFedCallback_ = eventFedCallback;
        return this::feedEvent;
    }

    @Override
    public <T> Future<T> submitTask(Callable<T> callable)
    {
        return evolverExecutorService_.submit(callable);
    }

    @Override
    public List<Keyword> getLastMouseTargetIdPath()
    {
        return (List<Keyword>) mouseEventParser_.getLastTargetIdPath();
    }

    @Override
    public synchronized Future<FGEvolveResultData> feedEvent(FGEvolveInputData inputData)
    {
        Object evolveReason = inputData != null ? inputData.getEvolveReason() : null;
        return feedEventImpl(inputData, m -> cycle(evolveReason, reasonParser_, m));
    }

    @Override
    public synchronized Future<FGEvolveResultData> feedTargetedEvent(List<Keyword> targetCellIdPath, FGEvolveInputData inputData)
    {
        Object evolveReason = inputData != null ? inputData.getEvolveReason() : null;
        return feedEventImpl(inputData, m -> cycleTargeted(targetCellIdPath, evolveReason, m));
    }

    @Override
    public Future<FGEvolveResultData> feedTargetedEvent(List<Keyword> targetCellIdPath, Object evolveReason)
    {
        return feedTargetedEvent(targetCellIdPath, new FGEvolveInputData(evolveReason, false));
    }

    // Private

    private Future<FGEvolveResultData> feedEventImpl(FGEvolveInputData inputData, Function<IFGModule, FGEvolveResultData> cycleFn)
    {
        IFGModule module = resolveModuleToWorkOn(inputData);
        Object evolveReason = inputData.getEvolveReason();
        boolean useFork = false;

        if (!inputData.shouldFork())
        {
            if (forks_.containsKey(evolveReason))
            {
                useFork_.invoke(containerId_, evolveReason);
                useFork = true;
                System.out.println("Hit prediction for " + evolveReason);
            }
        }

        Future<FGEvolveResultData> resultFuture;
        if (useFork)
        {
            //resultFuture = forkResults_.get(evolveReason);
            // Do not send the same result: already sent is as a prediction.
            return null;
        }
        else
        {
            resultFuture = evolverExecutorService_.submit(() -> {
                try
                {
                    FGEvolveResultData resultData = cycleFn.apply(module);
                    if (!inputData.shouldFork())
                    {
                        notifyEvolveConsumers(module);
                    }
                    return resultData;
                }
                catch (Throwable ex)
                {
                    ex.printStackTrace();
                    return FGEvolveResultData.EMPTY;
                }
            });
            if (inputData.shouldFork())
            {
                forkResults_.put(evolveReason, resultFuture);
            }
        }

        if (!inputData.shouldFork())
        {
            clearForks_.invoke(containerId_);
            forks_.clear();
            forkResults_.clear();
        }

        eventFedCallback_.actionPerformed(null);

        return resultFuture;
    }

    private IFGModule resolveModuleToWorkOn(FGEvolveInputData inputData)
    {
        if (inputData.shouldFork())
        {
            IFGModule forkModule = new FGForkModule(containerId_);
            forks_.put(inputData.getEvolveReason(), forkModule);
            return forkModule;
        }
        else
        {
            return module_;
        }
    }

    private static FGEvolveResultData cycle(Object repaintReason, FGInputEventParser reasonParser, IFGModule module)
    {
        if (repaintReason == null)
        {
            throw new IllegalArgumentException();
        }

        Map<Object, List<Keyword>> reasonMap;
        try
        {
            reasonMap = reasonParser.getTargetCellIds(repaintReason, module);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            reasonMap = Collections.emptyMap();
        }

        Set<List<Keyword>> changedPaths = new HashSet<>();
        for (Object reason : reasonMap.keySet())
        {
            List<Keyword> targetCellIds = reasonMap.get(reason);

            if (targetCellIds == null || !targetCellIds.isEmpty())
            {
                try
                {
                    module.evolve(targetCellIds, reason);
                    Set<List<Keyword>> changed = module.getChangedComponentIdPaths();
                    if (changed != null)
                    {
                        changedPaths.addAll(changed);
                    }
                }
                catch (Throwable ex)
                {
                    ex.printStackTrace();
                }
            }
        }
        return new FGEvolveResultData(reasonMap, changedPaths);
    }

    private static FGEvolveResultData cycleTargeted(List<Keyword> targetIdPath, Object repaintReason, IFGModule module)
    {
        try
        {
            module.evolve(targetIdPath, repaintReason);
            Set<List<Keyword>> changed = module.getChangedComponentIdPaths();
            if (changed != null)
            {
                Map<Object, List<Keyword>> reasonMap = new HashMap<>();
                reasonMap.put(repaintReason, targetIdPath);
                return new FGEvolveResultData(reasonMap, changed);
            }
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
        return FGEvolveResultData.EMPTY;
    }

    private void notifyEvolveConsumers(IFGModule module)
    {
        evolveConsumers_.stream()
            .filter(consumer -> shouldInvokeEvolveConsumer(module, consumer))
            .forEach(consumer ->
                new Thread(() ->
                    consumer.acceptEvolveResult(containerId_, module.getContainer()),
                    "FlatGUI Evolver Consumer Notifier").start());
    }

    private static boolean shouldInvokeEvolveConsumer(IFGModule module, IFGEvolveConsumer consumer)
    {
        Map<Object, Object> container = (Map<Object, Object>) module.getContainer();

        for (List<Keyword> path : consumer.getTargetPaths())
        {
            Collection<Keyword> properties = consumer.getTargetProperties(path);
            if (properties != null)
            {
                for (Keyword property : properties)
                {
                    Boolean evolved = (Boolean) propetyChanged_.invoke(container, path, property);
                    if (evolved.booleanValue())
                    {
                        return true;
                    }
                }
            }
            else
            {
                Object changedProperties = getChangedPropertiesByPath_.invoke(container, path);
                return changedProperties != null;
            }
        }
        return false;
    }
}
