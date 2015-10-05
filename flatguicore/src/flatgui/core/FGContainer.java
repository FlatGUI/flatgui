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
import flatgui.core.awt.FGAWTInteropUtil;

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

    private final FGInputEventParser reasonParser_;

    private final String containerId_;
    private final IFGModule module_;
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

        interopUtil_ = interopUtil;

        mouseEventParser_ = new FGMouseEventParser(UNIT_SIZE_PX);
        reasonParser_ = new FGInputEventParser();
        reasonParser_.registerReasonClassParser(MouseEvent.class, mouseEventParser_);
        reasonParser_.registerReasonClassParser(MouseWheelEvent.class, new FGMouseEventParser(UNIT_SIZE_PX));
        reasonParser_.registerReasonClassParser(KeyEvent.class, new FGKeyEventParser());
        reasonParser_.registerReasonClassParser(FGClipboardEvent.class, new FGClipboardEventEventParser());
        reasonParser_.registerReasonClassParser(FGContainer.FGTimerEvent.class, (fgTimerEvent, fgModule) -> Collections.emptyMap());
        reasonParser_.registerReasonClassParser(FGHostStateEvent.class, (fgHostStateEvent, fgModule) -> {
            Map<FGHostStateEvent, Collection<Object>> map = new HashMap<>();
            map.put(fgHostStateEvent, Arrays.asList(containerIdInternal));
            return map;
        });

//        Timer repaintTimer = new Timer("FlatGUI Blink Helper Timer", true);
//        repaintTimer.schedule(new TimerTask()
//        {
//            @Override
//            public void run()
//            {
//                //EventQueue.invokeLater(() -> cycle(new FGTimerEvent()));
//            }
//        }, 250, 250);

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
    public IFGInteropUtil getInterop()
    {
        return interopUtil_;
    }

    @Override
    public Function<Object, Future<Set<List<Keyword>>>> connect(ActionListener eventFedCallback, Object hostContext)
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

    // Private

    private Set<List<Keyword>> cycle(Object repaintReason)
    {
        if (repaintReason == null)
        {
            throw new IllegalArgumentException();
        }

        Map<Object, Collection<Object>> reasonMap;
        try
        {
            reasonMap = reasonParser_.getTargetCellIds(repaintReason, module_);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            reasonMap = Collections.emptyMap();
        }

        Set<List<Keyword>> changedPaths = new HashSet<>();
        for (Object reason : reasonMap.keySet())
        {
            Collection<Object> targetCellIds_ = reasonMap.get(reason);

            if (targetCellIds_ == null || !targetCellIds_.isEmpty())
            {
                try
                {
                    module_.evolve(targetCellIds_, reason);
                    Set<List<Keyword>> changed = module_.getChangedComponentIdPaths();
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
        return changedPaths;
    }

    private Set<List<Keyword>> cycleTargeted(Collection<Object> targetIdPath, Object repaintReason)
    {
        try
        {
            module_.evolve(targetIdPath, repaintReason);
            Set<List<Keyword>> changed = module_.getChangedComponentIdPaths();
            if (changed != null)
            {
                return changed;
            }
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
        return Collections.EMPTY_SET;
    }

    @Override
    public synchronized Future<Set<List<Keyword>>> feedEvent(Object repaintReason)
    {
        Future<Set<List<Keyword>>> resultFuture = evolverExecutorService_.submit(() -> {
            try
            {
                Set<List<Keyword>> changedPaths = cycle(repaintReason);

                evolveConsumers_.stream()
                        .filter(this::shouldInvokeEvolveConsumer)
                        .forEach(consumer ->
                                new Thread(() ->
                                    consumer.acceptEvolveResult(null, module_.getContainerObject()),
                                    "Evolver consumer notifier").start());

                return changedPaths;
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
                return Collections.EMPTY_SET;
            }
        });

        eventFedCallback_.actionPerformed(null);

        return resultFuture;
    }

    @Override
    public synchronized Future<Set<List<Keyword>>> feedTargetedEvent(Collection<Object> targetCellIdPath, Object repaintReason)
    {
        Future<Set<List<Keyword>>> resultFuture = evolverExecutorService_.submit(() -> {
            try
            {
                Set<List<Keyword>> changedPaths = cycleTargeted(targetCellIdPath, repaintReason);

                evolveConsumers_.stream()
                        .filter(this::shouldInvokeEvolveConsumer)
                        .forEach(consumer ->
                                new Thread(() ->
                                        consumer.acceptEvolveResult(null, module_.getContainerObject()),
                                        "Evolver consumer notifier").start());

                return changedPaths;
            }
            catch(Throwable ex)
            {
                ex.printStackTrace();
                return Collections.EMPTY_SET;
            }
        });

        eventFedCallback_.actionPerformed(null);

        return resultFuture;
    }

    private boolean shouldInvokeEvolveConsumer(IFGEvolveConsumer consumer)
    {
        Map<Object, Object> container = (Map<Object, Object>) module_.getContainerObject();

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

    static class FGTimerEvent
    {

    }

}
