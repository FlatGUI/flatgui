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

import clojure.lang.Keyword;
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
    public Collection<Container.SourceNode> processComponent(List<Object> componentPath, Map<Object, Object> component, List<Object> propertyValueVec, Function<List<Object>, Integer> indexProvider)
    {
        Map<Object, Object> evolvers = (Map<Object, Object>) component.get(EVOLVERS_KEY);

        Map<Object, Collection<List<Object>>> propertyIdToDependencies =
                (Map<Object, Collection<List<Object>>>) collectAllEvolverDependencies_.invoke(component);

        Collection<Container.SourceNode> result = new ArrayList<>(propertyIdToDependencies.size());

        for (Object propertyId : propertyIdToDependencies.keySet())
        {
            List<Object> nodePath = new ArrayList<>(componentPath.size()+1);
            nodePath.addAll(componentPath);
            nodePath.add(propertyId);

            Object evolverCode = evolvers.get(propertyId);
            List<Object> evolverInputDependencies = (List<Object>) getInputDependencies_.invoke(evolverCode);
            Var evolverFn = (Var) compileEvolver_.invoke(evolverCode, propertyValueVec, indexProvider);

            result.add(new Container.SourceNode(
                    propertyId,
                    nodePath,
                    propertyIdToDependencies.get(propertyId),
                    evolvers.get(propertyId) != null
                            ? componentAccessor -> evolverFn.invoke(componentAccessor)
                            : null,
                    evolverInputDependencies));
        }

        return result;
    }

    @Override
    public boolean isInterestedIn(Collection<Object> inputDependencies, Object evolveReason)
    {
        Keyword kw = INPUT_EVENT_KEYS.get(evolveReason.getClass());
        return kw != null ? inputDependencies.contains(kw) : true;
    }
}
