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
import java.util.function.Consumer;

/**
 * @author Denis Lebedev
 */
public class FGContainer implements IFGContainer
{
    private static final String REGISTER_FN_NAME = "register-container";




    private final FGInputEventParser reasonParser_;

    private IFGInteropUtil interopUtil_;

    private final Map<String, Object> containerProperties_;
    private final Map<String, Object> targetedProperties_;

    private final String containerId_;
    private final IFGModule module_;

    private ExecutorService evolverExecutorService_;

    private ActionListener eventFedCallback_;

    private List<IFGEvolveConsumer> evolveConsumers_;

    public FGContainer(IFGTemplate template)
    {
        this(template, template.getContainerVarName());
    }

    public FGContainer(IFGTemplate template, String containerId)
    {
        Var containerVar = clojure.lang.RT.var(template.getContainerNamespace(), template.getContainerVarName());
        Var registerFn = clojure.lang.RT.var(FGModule.FG_CORE_NAMESPACE, REGISTER_FN_NAME);
        Object container = containerVar.get();
        registerFn.invoke(containerId, container);

        Keyword containerIdInternal = (Keyword) ((Map)container).get(Keyword.intern("id"));

        containerId_ = containerId;
        module_ = new FGModule(containerId);

        reasonParser_ = new FGInputEventParser();
        reasonParser_.registerReasonClassParser(MouseEvent.class, new FGMouseEventParser(UNIT_SIZE_PX));
        reasonParser_.registerReasonClassParser(MouseWheelEvent.class, new FGMouseEventParser(UNIT_SIZE_PX));
        reasonParser_.registerReasonClassParser(KeyEvent.class, new FGKeyEventParser());
        reasonParser_.registerReasonClassParser(FGClipboardEvent.class, new FGClipboardEventEventParser());
        reasonParser_.registerReasonClassParser(FGContainer.FGTimerEvent.class, new IFGInputEventParser<FGTimerEvent>() {
            @Override
            public Map<String, Object> initialize(IFGModule fgModule) {
                return null;
            }

            @Override
            public Map<String, Object> getTargetedPropertyValues(FGContainer.FGTimerEvent fgTimerEvent) {return Collections.emptyMap();}

            @Override
            public Map<FGContainer.FGTimerEvent, Collection<Object>> getTargetCellIds(FGContainer.FGTimerEvent fgTimerEvent, IFGModule fgModule, Map<String, Object> generalPropertyMap) {
                return Collections.emptyMap();
            }
        });
        reasonParser_.registerReasonClassParser(FGHostStateEvent.class, new IFGInputEventParser<FGHostStateEvent>() {
            @Override
            public Map<String, Object> initialize(IFGModule fgModule) {return null;}

            @Override
            public Map<String, Object> getTargetedPropertyValues(FGHostStateEvent fgHostStateEvent) {return Collections.emptyMap();}

            @Override
            public Map<FGHostStateEvent, Collection<Object>> getTargetCellIds(FGHostStateEvent fgHostStateEvent, IFGModule fgModule, Map<String, Object> generalPropertyMap) {
                Map<FGHostStateEvent, Collection<Object>> map = new HashMap<>();
                map.put(fgHostStateEvent, Arrays.asList(containerIdInternal));
                return map;
            }
        });

        containerProperties_ = new HashMap<>();
        containerProperties_.put(GENERAL_PROPERTY_UNIT_SIZE, UNIT_SIZE_PX);

        targetedProperties_ = new HashMap<>();

//        Timer repaintTimer = new Timer("FlatGUI Blink Helper Timer", true);
//        repaintTimer.schedule(new TimerTask()
//        {
//            @Override
//            public void run()
//            {
//                //EventQueue.invokeLater(() -> cycle(new FGTimerEvent()));
//            }
//        }, 250, 250);

        Map<String, Object> initialGeneralProperties = reasonParser_.initialize(module_);
        if (initialGeneralProperties != null)
        {
            containerProperties_.putAll(initialGeneralProperties);
        }

        //interopUtil_ = new FGAWTInteropUtil((Component)hostContext, UNIT_SIZE_PX);
        // TODO temporary
        interopUtil_ = new FGDummyInteropUtil(UNIT_SIZE_PX);

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
    }

    @Override
    public void unInitialize()
    {
        evolverExecutorService_.shutdown();
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
    public Consumer<Object> connect(ActionListener eventFedCallback, Object hostContext)
    {
        eventFedCallback_ = eventFedCallback;
        return this::feedEvent;
    }

    public <T> Future<T> submitTask(Callable<T> callable)
    {
        return evolverExecutorService_.submit(callable);
    }

    ///

    @Override
    public Object getGeneralProperty(String propertyName)
    {
       return containerProperties_.get(propertyName);
    }

    @Override
    public Object getAWTUtil()
    {
        return interopUtil_;
    }

    ///

    private Map<Object, Collection<Object>> cycle(Object repaintReason)
    {
        if (repaintReason == null)
        {
            throw new IllegalArgumentException();
        }

        targetedProperties_.clear();
        if (repaintReason != null)
        {
            Map<String, Object> propertiesFromReason = reasonParser_.getTargetedPropertyValues(repaintReason);
            for (String propertyName : propertiesFromReason.keySet())
            {
                targetedProperties_.put(propertyName,
                        propertiesFromReason.get(propertyName));
            }
        }

        Map<String, Object> generalProperties = new HashMap<>();
        Map<Object, Collection<Object>> reasonMap;
        try
        {
            reasonMap = reasonParser_.getTargetCellIds(repaintReason, module_, generalProperties);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            reasonMap = Collections.emptyMap();
        }

        if (generalProperties != null)
        {
            containerProperties_.putAll(generalProperties);
        }
        for (Object reason : reasonMap.keySet())
        {
            Collection<Object> targetCellIds_ = reasonMap.get(reason);

            if (targetCellIds_ == null || !targetCellIds_.isEmpty())
            {
                module_.evolve(targetCellIds_, reason);
            }
        }
        return reasonMap;
    }

    private void cycleTargeted(Collection<Object> targetIdPath, Object repaintReason)
    {
        module_.evolve(targetIdPath, repaintReason);
    }

    @Override
    public synchronized void feedEvent(Object repaintReason)
    {
        evolverExecutorService_.submit(() -> {
            Map<Object, Collection<Object>> reasonMap = cycle(repaintReason);
            for (Collection<Object> idPath : reasonMap.values())
            {
                evolveConsumers_.stream()
                        .filter(consumer -> consumer.getTargetPaths().contains(idPath))
                        .forEach(consumer -> consumer.acceptEvolveResult(null, module_.getContainerObject()));
            }
        });

        eventFedCallback_.actionPerformed(null);
    }

    @Override
    public void feedTargetedEvent(Collection<Object> targetCellIdPath, Object repaintReason)
    {
        evolverExecutorService_.submit(() -> {
            cycleTargeted(targetCellIdPath, repaintReason);
            evolveConsumers_.stream()
                    .filter(consumer -> consumer.getTargetPaths().contains(targetCellIdPath))
                    .forEach(consumer -> consumer.acceptEvolveResult(null, module_.getContainerObject()));
        });

        eventFedCallback_.actionPerformed(null);
    }

    static class FGTimerEvent
    {

    }

}
