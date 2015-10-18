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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import clojure.lang.Keyword;

/**
 * @author Denis Lebedev
 */
public class FGEvolveResultData
{
    public static final FGEvolveResultData EMPTY = new FGEvolveResultData(Collections.emptyMap(), Collections.<List<Keyword>>emptySet());

    private final Map<Object, List<Keyword>> evolveReasonToTargetPath_;

    private final Set<List<Keyword>> changedPaths_;

    public FGEvolveResultData(Map<Object, List<Keyword>> evolveReasonToTargetPath, Set<List<Keyword>> changedPaths)
    {
        evolveReasonToTargetPath_ = evolveReasonToTargetPath;
        changedPaths_ = changedPaths;
    }

    public Map<Object, List<Keyword>> getEvolveReasonToTargetPath()
    {
        return evolveReasonToTargetPath_;
    }

    public Set<List<Keyword>> getChangedPaths()
    {
        return changedPaths_;
    }
}
