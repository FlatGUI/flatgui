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
import flatgui.core.awt.FGMouseTargetComponentInfo;
import flatgui.core.util.FGStringPool;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Denis Lebedev
 */
class FGModule implements IFGModule
{
    static final String FG_CORE_NAMESPACE = "flatgui.appcontainer";
    static final String GET_CONTAINER_FN_NAME = "get-container";

    private static final Var getPaintAllSequence_ = clojure.lang.RT.var("flatgui.paint", "get-paint-all-sequence");
    private static final Var getComponentIdPathToComponent_ = clojure.lang.RT.var("flatgui.paint", "get-component-id-path-to-component");
    private static final Var evolverFn_ = clojure.lang.RT.var(FG_CORE_NAMESPACE, "app-evolve-container");
    private static final Var getContainerFn_ = clojure.lang.RT.var(FG_CORE_NAMESPACE, GET_CONTAINER_FN_NAME);

    private static final Var getMousePointedPath_ = clojure.lang.RT.var("flatgui.access", "get-mouse-pointed-path");
    private static final Var getIdsFromPointedPath_ = clojure.lang.RT.var("flatgui.access", "get-ids-from-pointed-path");
    private static final Var getMouseRelXFromPath_ = clojure.lang.RT.var("flatgui.access", "get-mouse-rel-x-from-path");
    private static final Var getMouseRelYFromPath_ = clojure.lang.RT.var("flatgui.access", "get-mouse-rel-y-from-path");
    private static final Var getPressedCoorCaptureNeededFromPath_ = clojure.lang.RT.var("flatgui.access", "get-pressed-coord-capture-from-path");

    private static final Var getFocusedPath_ = clojure.lang.RT.var("flatgui.access", "get-focused-path");

    private static final int STRING_POOL_PER_COMPONENT_CAPACITY = 256;

    private static final Keyword CHANGED_PATHS_KEY = Keyword.intern("_changed-paths");


    private final String containerName_;

    private final Map<Object, FGStringPool> stringPoolMap_;

    public FGModule(String containerName)
    {
        containerName_ = containerName;
        stringPoolMap_ = new HashMap<>();
    }

    @Override
    public void evolve(List<Keyword> targetCellIds, Object inputEvent)
    {
        evolverFn_.invoke(containerName_, targetCellIds, inputEvent);
    }

    @Override
    public List<Object> getPaintAllSequence()
    {
        Object container = getContainer();
        Var paintAll = clojure.lang.RT.var(
                "flatgui.paint", "paint-all");

        List<Object> pList = (List<Object>) paintAll.invoke(container);
        return pList;
    }

    @Override
    public List<Object> getPaintAllSequence(double clipX, double clipY, double clipW, double clipH)
    {
        Object container = getContainer();
        Var paintAll = clojure.lang.RT.var(
                "flatgui.paint", "paint-all");

        List<Object> pList = (List<Object>) paintAll.invoke(container, clipX, clipY, clipW, clipH);
        return pList;
    }

    @Override
    public Object getDirtyRectFromContainer()
    {
        Object container = getContainer();
        Var f = clojure.lang.RT.var("flatgui.paint", "get-dirty-rect-from-container");
        return f.invoke(container);
    }

    @Override
    public List<Object> getPaintChangesSequence(Collection dirtyRects)
    {
        Object container = getContainer();
        Var paintChanged = clojure.lang.RT.var(
                "flatgui.paint", /*"paint-changed"*/"paint-dirty");

        List<Object> pList = (List<Object>) paintChanged.invoke(container, dirtyRects, false);
        return pList;
    }

    @Override
    public FGMouseTargetComponentInfo getMouseTargetInfoAt(double x, double y, FGComponentPath knownPath)
    {
        Object container = getContainer();

        Object targetPath = getMousePointedPath_.invoke(container, Double.valueOf(x), Double.valueOf(y));
        boolean captureNeeded = ((Boolean)getPressedCoorCaptureNeededFromPath_.invoke(targetPath)).booleanValue();
        Object targetIds = null;
        if (knownPath != null && captureNeeded)
        {
            targetPath = knownPath.getTargetComponentPath();
            targetIds = knownPath.getTargetIdPath();
        }
        else
        {
            if (targetPath != null)
            {
                targetIds = getIdsFromPointedPath_.invoke(targetPath);
            }
        }

        if (targetIds != null)
        {
            Object xRelativeVec = getMouseRelXFromPath_.invoke(targetPath);
            Object yRelativeVec = getMouseRelYFromPath_.invoke(targetPath);
            return new FGMouseTargetComponentInfo(new FGComponentPath(targetPath, targetIds), xRelativeVec, yRelativeVec, captureNeeded);
        }
        else
        {
            return new FGMouseTargetComponentInfo(null, null, null, captureNeeded);
        }
    }

    @Override
    public Object getContainerObject()
    {
        return getContainer();
    }

    @Override
    public List<Keyword> getFocusedPath()
    {
        return (List<Keyword>) getFocusedPath_.invoke(getContainer());
    }


    //
    // New painting approach optimized for web
    //

    @Override
    public Set<List<Keyword>> getChangedComponentIdPaths()
    {
        Object container = getContainer();
        return (Set<List<Keyword>>) ((Map<Object, Object>)container).get(CHANGED_PATHS_KEY);
    }

    @Override
    public Map<List<Keyword>, Map<Keyword, Object>> getComponentIdPathToComponent(Collection<List<Keyword>> paths)
    {
        Object container = getContainer();
        return (Map<List<Keyword>, Map<Keyword, Object>>) getComponentIdPathToComponent_.invoke(container, paths);
    }

    @Override
    public Map<Object, Object> getStringPoolDiffs(Map<List<Keyword>, List<String>> idPathToStrings)
    {
        Map<Object, Object> result = new HashMap<>();

        idPathToStrings.entrySet().stream().forEach(e -> {
            Map<Integer, String> valDiff = putStrings(e.getKey(), e.getValue());
            if (!valDiff.isEmpty())
            {
                result.put(e.getKey(), valDiff);
            }
        });

        return result;
    }

    public Integer getStringPoolId(String s, Object componentId)
    {
        return stringPoolMap_.get(componentId).getIndexOfString(s);
    }

    private Map<Integer, String> putStrings(Object componentId, Collection<String> strings)
    {
        FGStringPool pool = stringPoolMap_.get(componentId);
        if (pool == null)
        {
            pool = new FGStringPool(STRING_POOL_PER_COMPONENT_CAPACITY);
            stringPoolMap_.put(componentId, pool);
        }
        return pool.addStrings(strings.stream().filter(s -> s != null).collect(Collectors.toSet()));
    }

    @Override
    public List<Object> getPaintAllSequence2()
    {
        Object container = getContainer();
        List<Object> s = (List<Object>) getPaintAllSequence_.invoke(container);
        return s;
    }

    private Object getContainer()
    {
        return getContainerFn_.invoke(containerName_);
    }
}
