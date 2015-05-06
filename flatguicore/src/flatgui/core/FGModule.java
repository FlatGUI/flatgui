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

    private static final int STRING_POOL_PER_COMPONENT_CAPACITY = 16;

    private final String containerName_;

    private final Map<Object, FGStringPool> stringPoolMap_;

    public FGModule(String containerName)
    {
        containerName_ = containerName;
        stringPoolMap_ = new HashMap<>();
    }

    @Override
    public void evolve(Collection<Object> targetCellIds, Object inputEvent)
    {
        Var evolver = getEvolver();
        evolver.invoke(containerName_, targetCellIds, inputEvent);
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

        Var getMousePointedPath = clojure.lang.RT.var("flatgui.access", "get-mouse-pointed-path");
        Var getIdsFromPointedPath = clojure.lang.RT.var("flatgui.access", "get-ids-from-pointed-path");
        Var getMouseRelXFromPath = clojure.lang.RT.var("flatgui.access", "get-mouse-rel-x-from-path");
        Var getMouseRelYFromPath = clojure.lang.RT.var("flatgui.access", "get-mouse-rel-y-from-path");

        Object targetPath = null;
        Object targetIds = null;
        if (knownPath != null)
        {
            targetPath = knownPath.getTargetComponentPath();
            targetIds = knownPath.getTargetIdPath();
        }
        else
        {
            targetPath = getMousePointedPath.invoke(container, Double.valueOf(x), Double.valueOf(y));
            if (targetPath != null)
            {
                targetIds = getIdsFromPointedPath.invoke(targetPath);
            }
        }

        if (targetIds != null)
        {
            Object xRelativeVec = getMouseRelXFromPath.invoke(targetPath);
            Object yRelativeVec = getMouseRelYFromPath.invoke(targetPath);

            return new FGMouseTargetComponentInfo(new FGComponentPath(targetPath, targetIds), xRelativeVec, yRelativeVec);
        }
        else
        {
            return new FGMouseTargetComponentInfo(null, null, null);
        }
    }

    @Override
    public Object getContainerObject()
    {
        return getContainer();
    }

    //
    // New painting approach optimized for web
    //

    @Override
    public Map<Object, Object> getComponentIdPathToPositionMatrix()
    {
        // @todo Do not call clojure.lang.RT.var each time

        Object container = getContainer();
        Var fn = clojure.lang.RT.var("flatgui.paint", "get-component-id-path-to-position-matrix");
        return (Map<Object, Object>) fn.invoke(container);
    }

    @Override
    public Map<Object, Object> getComponentIdPathToViewportMatrix()
    {
        Object container = getContainer();
        Var fn = clojure.lang.RT.var("flatgui.paint", "get-component-id-path-to-viewport-matrix");
        return (Map<Object, Object>) fn.invoke(container);
    }

    @Override
    public Map<Object, Object> getComponentIdPathToClipRect()
    {
        Object container = getContainer();
        Var fn = clojure.lang.RT.var("flatgui.paint", "get-component-id-path-to-clip-size");
        return (Map<Object, Object>) fn.invoke(container);
    }

    @Override
    public Map<Object, Object> getComponentIdPathToLookVector()
    {
        Object container = getContainer();
        Var fn = clojure.lang.RT.var("flatgui.paint", "get-component-id-path-to-look-vector");
        return (Map<Object, Object>) fn.invoke(container);
    }

    @Override
    public Map<Object, Object> getComponentIdPathToChildCount()
    {
        Object container = getContainer();
        Var fn = clojure.lang.RT.var("flatgui.paint", "get-component-id-path-to-child-count");
        return (Map<Object, Object>) fn.invoke(container);
    }

    @Override
    public Map<Object, Object> getComponentIdPathToBooleanStateFlags()
    {
        Object container = getContainer();
        Var fn = clojure.lang.RT.var("flatgui.paint", "get-component-id-path-to-flags");
        return (Map<Object, Object>) fn.invoke(container);
    }

    @Override
    public Map<Object, Object> getStringPoolDiffs()
    {
        Map<List<Keyword>, List<String>> idPathToStrings = getComponentIdPathToStrings();
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

    public byte getStringPoolId(String s, Object componentId)
    {
        return (byte)(stringPoolMap_.get(componentId).getIndexOfString(s).intValue());
    }

    private Map<List<Keyword>, List<String>> getComponentIdPathToStrings()
    {
        Object container = getContainer();
        Var fn = clojure.lang.RT.var("flatgui.paint", "get-component-id-path-to-strings");
        return (Map<List<Keyword>, List<String>>) fn.invoke(container);
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
        Var fn = clojure.lang.RT.var("flatgui.paint", "get-paint-all-sequence");

        List<Object> s = (List<Object>) fn.invoke(container);

        return s;
    }

    @Override
    public List<Object> getPaintChangedSequence2()
    {
        return null;
    }

    private Object getContainer()
    {
        Var getContainerFn = clojure.lang.RT.var(
                FG_CORE_NAMESPACE,
                GET_CONTAINER_FN_NAME);
        return getContainerFn.invoke(containerName_);
    }

    private Var getEvolver()
    {
        return clojure.lang.RT.var(
                FG_CORE_NAMESPACE,
                "app-evolve-container");
    }
}
