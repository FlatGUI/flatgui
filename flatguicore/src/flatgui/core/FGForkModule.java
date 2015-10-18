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
public class FGForkModule extends FGAbstractModule
{
    private static final Var forkFn_ = clojure.lang.RT.var(FG_CORE_NAMESPACE, "app-fork-container");
    private static final Var getForkFn_ = clojure.lang.RT.var(FG_CORE_NAMESPACE, "get-fork");

    private volatile Object evolveReason_;

    public FGForkModule(String containerName)
    {
        super(containerName);
    }

    @Override
    public void evolve(List<Keyword> targetCellIds, Object inputEvent)
    {
        evolveReason_ = inputEvent;
        forkFn_.invoke(containerName_, targetCellIds, inputEvent);
    }

    @Override
    public Object getContainer()
    {
        if (evolveReason_ == null)
        {
            return FGModule.getContainer(containerName_);
        }
        else
        {
            return getForkFn_.invoke(containerName_, evolveReason_);
        }
    }
}
