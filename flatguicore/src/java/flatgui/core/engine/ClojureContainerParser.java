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

import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.Obj;
import clojure.lang.Var;
import flatgui.core.FGTimerEvent;
import flatgui.core.awt.FGMouseEvent;

import java.util.*;
import java.util.function.Function;

/**
 * @author Denis Lebedev
 */
public class ClojureContainerParser implements Container.IContainerParser
{
    private static final Keyword ID_KEY = Keyword.intern("id");
    private static final Keyword CHILDREN_KEY = Keyword.intern("children");
    private static final Keyword EVOLVERS_KEY = Keyword.intern("evolvers");

    private static final String FG_NS = "flatgui.core";
    private static final String FG_DEP_NS = "flatgui.dependency"; // TODO move to core?

    private static final Var collectAllEvolverDependencies_ = clojure.lang.RT.var(FG_NS, "collect-all-evolver-dependencies");
    private static final Var compileEvolver_ = clojure.lang.RT.var(FG_NS, "compile-evolver");
    private static final Var getInputDependencies_ = clojure.lang.RT.var(FG_DEP_NS, "get-input-dependencies");

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
    public Collection<Container.SourceNode> processComponent(List<Object> componentPath, Map<Object, Object> component)
    {
        Map<Object, Object> evolvers = (Map<Object, Object>) component.get(EVOLVERS_KEY);

        Map<Object, Collection<List<Object>>> propertyIdToDependencies =
                (Map<Object, Collection<List<Object>>>) collectAllEvolverDependencies_.invoke(component);

        Collection<Container.SourceNode> result = new ArrayList<>(component.size());

        for (Object propertyId : component.keySet())
        {
            List<Object> nodePath = new ArrayList<>(componentPath.size()+1);
            nodePath.addAll(componentPath);
            nodePath.add(propertyId);

            boolean hasEvolver = evolvers.get(propertyId) != null;

            Object evolverCode = evolvers.get(propertyId);
            List<Object> evolverInputDependencies = (List<Object>) getInputDependencies_.invoke(evolverCode);

            result.add(new Container.SourceNode(
                    propertyId,
                    nodePath,
                    hasEvolver
                            ? propertyIdToDependencies.get(propertyId)
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
    public Function<Map<Object, Object>, Object> compileEvolverCode(Object evolverCode, Function<List<Object>, Integer> indexProvider)
    {
        IFn evolverFn = (IFn) compileEvolver_.invoke(evolverCode, indexProvider);
        return componentAccessor -> evolverFn.invoke(componentAccessor);
    }

    @Override
    public boolean isInterestedIn(Collection<Object> inputDependencies, Object evolveReason)
    {
        Keyword kw = INPUT_EVENT_KEYS.get(evolveReason.getClass());
        return kw != null ? inputDependencies.contains(kw) : true;
    }
}
