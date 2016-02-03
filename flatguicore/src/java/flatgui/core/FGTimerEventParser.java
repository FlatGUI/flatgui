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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Denys Lebediev
 *         Date: 8/15/13
 *         Time: 9:24 PM
 */
public class FGTimerEventParser implements IFGInputEventParser<FGTimerEvent>
{
    private static final Keyword CHANNEL = Keyword.intern("timer");

    // TODO
    // 1. Somehow have different [sub]channels: timer, focused timer
    // 2. Use timer ids
    // 3. Use the list of properties (which is known already) instead of having
    //    componentbase calculate it again
    // 4. How to target one event into several components? This is not possible right now
    //    (well, possible with a hack) since it is a key in the map

    @Override
    public Map<FGTimerEvent, List<Keyword>> getTargetCellIds(List<Keyword> knownTargetIdPath, FGTimerEvent event, IFGModule fgModule)
    {
        Map<FGTimerEvent, List<Keyword>> result = new HashMap<>();

        Map<List<Keyword>, Collection<Keyword>> subscriberPathToProperies = fgModule.getInputChannelSubscribers(CHANNEL);

        // this is a hack
        int i = 0;
        for (List<Keyword> componentIdPath : subscriberPathToProperies.keySet())
        {
            result.put(new FGTimerEvent(event.getTimestamp()+i), componentIdPath);
            i++;
        }

        return result;
    }
}
