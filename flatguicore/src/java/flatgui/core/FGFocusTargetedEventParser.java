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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import clojure.lang.Keyword;

/**
 * @author Denis Lebedev
 */
public abstract class FGFocusTargetedEventParser<E> implements IFGInputEventParser<E>
{
    @Override
    public Map<E, List<Keyword>> getTargetCellIds(List<Keyword> knownTargetIdPath, E event, IFGModule fgModule)
    {
        Map<E, List<Keyword>> map = new HashMap<>();
        if (knownTargetIdPath != null)
        {
            map.put(event, knownTargetIdPath);
        }
        else
        {
            List<Keyword> focusedPath = fgModule.getFocusedPath();
            if (focusedPath != null)
            {
                map.put(event, focusedPath);
            }
        }
        return map;
    }
}
