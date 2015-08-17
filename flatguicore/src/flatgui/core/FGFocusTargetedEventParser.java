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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public abstract class FGFocusTargetedEventParser<E> implements IFGInputEventParser<E>
{
    @Override
    public Map<E, Collection<Object>> getTargetCellIds(E event, IFGModule fgModule, Map<String, Object> generalPropertyMap)
    {
        Map<E, Collection<Object>> map = new HashMap<>();
        List focusedPath = fgModule.getFocusedPath();
        if (focusedPath != null)
        {
            map.put(event, focusedPath);
        }
        return map;
    }
}
