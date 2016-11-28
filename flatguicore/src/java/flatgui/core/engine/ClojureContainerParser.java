/*
; Copyright (c) 2016 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.
 */
package flatgui.core.engine;

import clojure.lang.*;
import flatgui.core.FGTimerEvent;
import flatgui.core.awt.FGMouseEvent;
import flatgui.core.util.Tuple;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Denis Lebedev
 */
public class ClojureContainerParser implements Container.IContainerParser
{
    private static final Keyword ID_KEY = Keyword.intern("id");
    private static final Keyword CHILDREN_KEY = Keyword.intern("children");
    private static final Keyword EVOLVERS_KEY = Keyword.intern("evolvers");

    private static final Keyword SOURCE_META_KEY = Keyword.intern("source");
    private static final Keyword SYMBOL_META_KEY = Keyword.intern("ns-qualified-sym");
    private static final Keyword INPUT_DEPENDENCIES_META_KEY = Keyword.intern("input-channel-dependencies");
    private static final Keyword RELATIVE_DEPENDENCIES_META_KEY = Keyword.intern("relative-dependencies");

    private static final Keyword THIS_KW = Keyword.intern("this");
    private static final Keyword UP_LEVEL_KW = Keyword.intern("_");

    private static final String FG_NS = "flatgui.core";
    private static final String FG_DEP_NS = "flatgui.dependency"; // TODO move to core?

    private static final Var collectAllEvolverDependencies_ = clojure.lang.RT.var(FG_NS, "collect-all-evolver-dependencies");
    private static final Var compileEvolver_ = clojure.lang.RT.var(FG_NS, "compile-evolver");
    private static final Var getInputDependencies_ = clojure.lang.RT.var(FG_DEP_NS, "get-input-dependencies");

    private static final Keyword WILDCARD_KEY = Keyword.intern("*");

    private static final Map<Class<?>, Keyword> INPUT_EVENT_KEYS;
    static
    {
        Map<Class<?>, Keyword> m = new HashMap<>();
        m.put(FGMouseEvent.class, Keyword.intern("mouse"));
        m.put(FGTimerEvent.class, Keyword.intern("timer"));
        INPUT_EVENT_KEYS = Collections.unmodifiableMap(m);
    }

    @Override
    public Object getComponentId(Map<Object, Object> container)
    {
        return container.get(ID_KEY);
    }

    @Override
    public Map<Object, Map<Object, Object>> getChildren(Map<Object, Object> container)
    {
        return (Map<Object, Map<Object, Object>>) container.get(CHILDREN_KEY);
    }

    @Override
    public Collection<Container.SourceNode> processComponent(List<Object> componentPath, Map<Object, Object> component, Container.IPropertyValueAccessor propertyValueAccessor)
    {
        Map<Object, Object> evolvers = (Map<Object, Object>) component.get(EVOLVERS_KEY);

// TODO(IND)
//        Map<Object, Collection<List<Object>>> propertyIdToDependencies =
//                (Map<Object, Collection<List<Object>>>) collectAllEvolverDependencies_.invoke(component);

        Collection<Container.SourceNode> result = new ArrayList<>(component.size());

        Set<Object> allPropertyIds = new HashSet<>();
        allPropertyIds.addAll(component.keySet());
        if (evolvers != null)
        {
            allPropertyIds.addAll(evolvers.keySet());
        }
        for (Object propertyId : allPropertyIds)
        {
            List<Object> nodePath = new ArrayList<>(componentPath.size()+1);
            nodePath.addAll(componentPath);
            nodePath.add(propertyId);

            boolean hasEvolver = evolvers != null && evolvers.get(propertyId) != null;

            Object evolverCode = evolvers != null ? evolvers.get(propertyId) : null;

            //List<Object> evolverInputDependencies = evolvers != null ? (List<Object>) getInputDependencies_.invoke(evolverCode) : null;
            List<Object> evolverInputDependencies = null;
            Collection<List<Object>> dependencyPaths = Collections.emptySet(); // TODO(IND) evolver's meta should have relative dependencies
            Collection<Tuple> relAndAbsDependencyPaths = Collections.emptySet();
            // TODO(IND)
            if (hasEvolver)
            {
                IFn evolverFn = (IFn) evolverCode;
                Map<Object, Object> evolverFnMeta = (Map<Object, Object>) ((IMeta)evolverFn).meta();
                //Symbol symbol = (Symbol) evolverFnMeta.get(SYMBOL_META_KEY);
                if (evolverFnMeta == null)
                {
                    throw new IllegalStateException("null meta of evolver fn: " + componentPath + " " + propertyId);
                }
                evolverInputDependencies = (List<Object>) evolverFnMeta.get(INPUT_DEPENDENCIES_META_KEY);
                dependencyPaths = (Collection<List<Object>>) evolverFnMeta.get(RELATIVE_DEPENDENCIES_META_KEY);
                relAndAbsDependencyPaths = dependencyPaths.stream()
                        .map(relDep -> Tuple.pair(relDep, buildAbsPath(componentPath, relDep)))
                        .collect(Collectors.toList());
                //dependencyPaths = GetPropertyClojureFnRegistry.attachToInstance(symbol, componentPath, propertyValueAccessor);
            }

            result.add(new Container.SourceNode(
                    propertyId,
                    nodePath,
                    hasEvolver
                            ? /*propertyIdToDependencies.get(propertyId)*/relAndAbsDependencyPaths
                            : Collections.emptySet(),
                    hasEvolver
                            ? evolverCode
                            : null,
                    hasEvolver
                            ? evolverInputDependencies
                            : null));
        }

        return result;
    }

    @Override
    public Function<Map<Object, Object>, Object> compileEvolverCode(
            Object propertyId, Object evolverCode, Function<List<Object>, Integer> indexProvider, List<Object> path, Container.IPropertyValueAccessor propertyValueAccessor)
    {
        if (evolverCode != null)
        {
            try
            {
                IFn evolverFn = (IFn) evolverCode;
//                return componentAccessor -> {
//                    try
//                    {
//                        GetPropertyClojureFn.setAbsPath(path);
//                        GetPropertyClojureFn.setIndexProvider(indexProvider);
//                        GetPropertyClojureFn.setPropertyValueAccessor(propertyValueAccessor);
//                        return evolverFn.invoke(componentAccessor);
//                    }
//                    catch (Throwable ex)
//                    {
//                        throw new RuntimeException("Failed evolver for " + path + " " + propertyId + ": ", ex);
//                    }
//                };
                return new EvolverWrapper(evolverFn, path, indexProvider, propertyValueAccessor);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return componentAccessor -> {throw new IllegalStateException("Could not instantiate evolver for " + path + " " + propertyId);};
            }
        }
        else
        {
            return componentAccessor -> {throw new IllegalStateException("No evolver for " + path + " " + propertyId);};
        }

// TODO(IND)
//
//        IFn evolverFn = (IFn) compileEvolver_.invoke(evolverCode, indexProvider, path, propertyId);
//        if (evolverFn != null)
//        {
//            return componentAccessor -> {
//                try
//                {
//                    return evolverFn.invoke(componentAccessor);
//                }
//                catch (Throwable ex)
//                {
//                    Map<Object, Object> evolverFnMeta = (Map<Object, Object>) ((IMeta)evolverFn).meta();
//                    Object src = evolverFnMeta.get(SOURCE_META_KEY);
//                    throw new RuntimeException("Failed evolver for " + path + " " + propertyId + ": " + (src != null ? src : "<null>"), ex);
//                }
//            };
//        }
//        else
//        {
//            // Error is already logged in this case
//            return componentAccessor -> {throw new IllegalStateException("Evolver is not compiled for " + path + " " + propertyId);};
//        }
    }

    @Override
    public boolean isInterestedIn(Collection<Object> inputDependencies, Object evolveReason)
    {
        Keyword kw = INPUT_EVENT_KEYS.get(evolveReason.getClass());
        return kw != null ? inputDependencies.contains(kw) : true;
    }

    @Override
    public boolean isWildcardPathElement(Object e)
    {
        return WILDCARD_KEY.equals(e);
    }

    @Override
    public void processComponentAfterIndexing(Container.IComponent component)
    {
    }

    static List<Object> buildAbsPath(List<Object> componentPath, List<Object> relPath)
    {
        List<Object> absPath = new ArrayList<>(componentPath);
        if (relPath.isEmpty())
        {
            absPath.remove(absPath.size()-1);
        }
        else if (relPath.get(0).equals(THIS_KW))
        {
            List<Object> next = new ArrayList<>(relPath);
            next.remove(0);
            absPath.addAll(next);
        }
        else if (relPath.get(0).equals(UP_LEVEL_KW))
        {
            int upLevelCount = 0;
            while (upLevelCount < relPath.size() && relPath.get(upLevelCount).equals(UP_LEVEL_KW))
            {
                upLevelCount++;
            }
            int componentPathCountToTake = componentPath.size() - upLevelCount - 1;
            absPath = absPath.subList(0, componentPathCountToTake);
            absPath.addAll(relPath.subList(upLevelCount, relPath.size()));
        }
        else
        {
            absPath.remove(absPath.size()-1);
            absPath.addAll(relPath);
        }
        return absPath;
    }

    /**
     * One instance per evolver use case (unlike GetPropertyClojureFn that has one instance per evolver function)
     * and per application/thread.
     * Keeps the index of referred property which GetPropertyClojureFn cannot keep because it's different depending
     * on where evolver is used.
     */
    static class GetPropertyDelegate
    {
        private boolean linked_;
        private Integer accessedPropertyIndex_;

        private final List<Object> evolvedComponentPath_;
        private final Function<List<Object>, Integer> indexProvider_;
        private final Container.IPropertyValueAccessor propertyValueAccessor_;

        public GetPropertyDelegate(List<Object> evolvedComponentPath, Function<List<Object>, Integer> indexProvider, Container.IPropertyValueAccessor propertyValueAccessor)
        {
            evolvedComponentPath_ = evolvedComponentPath;
            indexProvider_ = indexProvider;
            propertyValueAccessor_ = propertyValueAccessor;
        }

        Object getProperty()
        {
            return propertyValueAccessor_.getPropertyValue(accessedPropertyIndex_);
        }

        boolean isLinked()
        {
            return linked_;
        }

        void link(List<Object> accessedPropertyRelPath, Object accessedProperty)
        {
            List<Object> accessedPropertyAbsPath = buildAbsPath(evolvedComponentPath_, accessedPropertyRelPath);
            accessedPropertyAbsPath.add(accessedProperty);
            accessedPropertyIndex_ = indexProvider_.apply(accessedPropertyAbsPath);
            Container.log(evolvedComponentPath_ + " linked " + accessedPropertyAbsPath + " -> " + accessedPropertyIndex_ + " Delegate: " + this);
            linked_ = true;
        }
    }


    /**
     * One instance per evolver use case and per application/thread (unlike actual Clojure evolver
     * that has one instance per process)
     */
    static class EvolverWrapper implements Function<Map<Object, Object>, Object>
    {
        private HashMap<Integer, GetPropertyDelegate> delegateByIdMap_;
        private HashMap<Integer, Map<List<Object>, GetPropertyDelegate>> delegateByIdAndPathMap_;
        private HashMap<Integer, Map<Keyword, GetPropertyDelegate>> delegateByIdAndPropertyMap_;
        private HashMap<Integer, Map<List<Object>, Map<Keyword, GetPropertyDelegate>>> delegateByIdPathAndPropertyMap_;

        private final IFn evolverFn_;
        private final List<Object> evolvedComponentPath_;
        private final Function<List<Object>, Integer> indexProvider_;
        private final Container.IPropertyValueAccessor propertyValueAccessor_;

        public EvolverWrapper(IFn evolverFn, List<Object> evolvedComponentPath, Function<List<Object>, Integer> indexProvider, Container.IPropertyValueAccessor propertyValueAccessor)
        {
            evolverFn_ = evolverFn;
            evolvedComponentPath_ = evolvedComponentPath;
            indexProvider_ = indexProvider;
            propertyValueAccessor_ = propertyValueAccessor;
            delegateByIdMap_ = new HashMap<>();
            delegateByIdAndPathMap_ = new HashMap<>();
            delegateByIdAndPropertyMap_ = new HashMap<>();
            delegateByIdPathAndPropertyMap_ = new HashMap<>();
        }

        @Override
        public Object apply(Map<Object, Object> component)
        {
            GetPropertyStaticClojureFn.visit(this);
            return evolverFn_.invoke(component);
        }

        GetPropertyDelegate getDelegateById(Integer getterId)
        {
            GetPropertyDelegate delegate = delegateByIdMap_.get(getterId);
            if (delegate == null)
            {
                delegate = createDelegate();
                delegateByIdMap_.put(getterId, delegate);
            }
            return delegate;
        }

        GetPropertyDelegate getDelegateByIdAndPath(Integer getterId, List<Object> path)
        {
            Map<List<Object>, GetPropertyDelegate> pathToDelegate = delegateByIdAndPathMap_.get(getterId);
            if (pathToDelegate == null)
            {
                pathToDelegate = new HashMap<>();
                delegateByIdAndPathMap_.put(getterId, pathToDelegate);
            }
            GetPropertyDelegate delegate = pathToDelegate.get(path);
            if (delegate == null)
            {
                delegate = createDelegate();
                pathToDelegate.put(path, delegate);
            }
            return delegate;
        }

        GetPropertyDelegate getDelegateByIdAndProperty(Integer getterId, Keyword property)
        {
            Map<Keyword, GetPropertyDelegate> propertyToDelegate = delegateByIdAndPropertyMap_.get(getterId);
            if (propertyToDelegate == null)
            {
                propertyToDelegate = new HashMap<>();
                delegateByIdAndPropertyMap_.put(getterId, propertyToDelegate);
            }
            GetPropertyDelegate delegate = propertyToDelegate.get(property);
            if (delegate == null)
            {
                delegate = createDelegate();
                propertyToDelegate.put(property, delegate);
            }
            return delegate;
        }

        GetPropertyDelegate getDelegateByIdPathAndProperty(Integer getterId, List<Object> path, Keyword property)
        {
            Map<List<Object>, Map<Keyword, GetPropertyDelegate>> mapByPath = delegateByIdPathAndPropertyMap_.get(getterId);
            if (mapByPath == null)
            {
                mapByPath = new HashMap<>();
                delegateByIdPathAndPropertyMap_.put(getterId, mapByPath);
            }
            Map<Keyword, GetPropertyDelegate> propertyToDelegate = mapByPath.get(path);
            if (propertyToDelegate == null)
            {
                propertyToDelegate = new HashMap<>();
                mapByPath.put(path, propertyToDelegate);
            }
            GetPropertyDelegate delegate = propertyToDelegate.get(property);
            if (delegate == null)
            {
                delegate = createDelegate();
                propertyToDelegate.put(property, delegate);
            }
            return delegate;
        }

        private GetPropertyDelegate createDelegate()
        {
            return new GetPropertyDelegate(evolvedComponentPath_, indexProvider_, propertyValueAccessor_);
        }
    }
}
