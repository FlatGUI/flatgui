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

import clojure.lang.Var;

import java.util.*;

/**
 * @author Denis Lebedev
 */
public class FGModule implements IFGModule
{
    static final String FG_CORE_NAMESPACE = "flatgui.appcontainer";
    static final String GET_CONTAINER_FN_NAME = "get-container";

    private String containerName_;

    public FGModule(String containerName)
    {
        containerName_ = containerName;
    }

//    @Override
//    public void start(String... source)
//    {
//        for (String src : source)
//        {
//            File srcFile = new File(src);
//
//            String content = null;
//            try
//            {
//                content = new Scanner(srcFile).useDelimiter("\\Z").next();
//            }
//            catch (FileNotFoundException e)
//            {
//                e.printStackTrace();
//            }
//            initializeFromContent(content);
//        }
//    }
//
//    @Override
//    public void initializeFromContent(String content)
//    {
//        // Ensure RT class is initialized before compiler
//        System.out.println(clojure.lang.RT.byteCast(1));
//
//        try
//        {
//            clojure.lang.Compiler.load(new StringReader(content));
//
//            registerContainer();
//        }
//        catch(Exception ex)
//        {
//            System.out.println("Unable to parse source content: " + ex.getMessage());
//            ex.printStackTrace();
//        }
//    }

    @Override
    public void evolve(Collection<Object> targetCellIds, Object repaintReason)
    {
        Var evolver = getEvolver();
        evolver.invoke(containerName_, targetCellIds, repaintReason);
    }

    @Override
    public List<Object> getPaintAllSequence()
    {
        Object container = getContainer();
        Var paintAll = clojure.lang.RT.var(
                "flatgui.paint", "paint-all");

        List<Object> pList = (List<Object>) paintAll.invoke(container);

//        System.out.println("-DLTEMP- FGModule.getPaintAllSequence paint-all" +
//                ": " + pList);

        return pList;
    }

    @Override
    public List<Object> getPaintAllSequence(double clipX, double clipY, double clipW, double clipH)
    {
        Object container = getContainer();
        Var paintAll = clojure.lang.RT.var(
                "flatgui.paint", "paint-all");

        List<Object> pList = (List<Object>) paintAll.invoke(container, clipX, clipY, clipW, clipH);

//        System.out.println("-DLTEMP- FGModule.getPaintAllSequence paint-all" +
//                ": " + pList);

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
    public Object getFocusOwnerId()
    {
        Object container = getContainer();
        Var getFocusOwner = clojure.lang.RT.var(
                "flatgui.focusmanagement", "get-focus-owner-id");
        return getFocusOwner.invoke(container);
    }

    @Override
    public List<Object> getPaintChangesSequence(Collection dirtyRects)
    {
        Object container = getContainer();
        Var paintChanged = clojure.lang.RT.var(
                "flatgui.paint", /*"paint-changed"*/"paint-dirty");

        List<Object> pList = (List<Object>) paintChanged.invoke(container, dirtyRects, false);

//        System.out.println("-DLTEMP- FGModule.getPaintChangesSequence paint-changed" +
//                ": " + pList);

        return pList;
    }

    @Override
    public FGMouseTargetComponentInfo getCellIdsAt(double x, double y, FGComponentPath knownPath)
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

            //System.out.println("-DLTEMP- FGModule.getCellIdsAt " + ((Collection)targetPath).size() + " " + targetIds + " - " + xRelativeVec + " - " + yRelativeVec);

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

    @Override
    public Map<Object, Object> getCell(String cellId)
    {
        return null;
    }

//    @Override
//    public Object getObjectFromContainer(String name)
//    {
//        Var var = clojure.lang.RT.var(
//                namespace_, name);
//        return var.isBound() ? var.get() : null;
//    }

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
        // @todo container-name

//        Var globalContainerMap = clojure.lang.RT.var(
//                FG_CORE_NAMESPACE,
//                "CONTAINERS");
//        return globalContainerMap.invoke(containerName_);
        Var getContainerFn = clojure.lang.RT.var(
                FG_CORE_NAMESPACE,
                GET_CONTAINER_FN_NAME);
        return getContainerFn.invoke(containerName_);
    }

    private Var getEvolver()
    {
        // @todo container-name

        return clojure.lang.RT.var(
                FG_CORE_NAMESPACE,
                "app-evolve-container");
    }
}
