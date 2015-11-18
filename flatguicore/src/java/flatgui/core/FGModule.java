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

import java.util.List;

import clojure.lang.Keyword;
import clojure.lang.Var;

/**
 * @author Denis Lebedev
 */
class FGModule extends FGAbstractModule
{
    //static final String GET_CONTAINER_FN_NAME = "get-container";
    private static final Var evolverFn_ = clojure.lang.RT.var(FG_CORE_NAMESPACE, "app-evolve-container");
    private static final Var getContainerFn_ = clojure.lang.RT.var(FG_CORE_NAMESPACE, "get-container");


    public FGModule(String containerName)
    {
        super(containerName);
    }

    @Override
    public Object getContainer()
    {
        return getContainer(containerName_);
    }

    @Override
    public void evolve(List<Keyword> targetCellIds, Object inputEvent)
    {
        evolverFn_.invoke(containerName_, targetCellIds, inputEvent);
    }

    public static Object getContainer(String containerName)
    {
        return getContainerFn_.invoke(containerName);
    }
}
