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
import flatgui.core.util.Tuple;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Denys Lebediev
 *         Date: 8/15/13
 *         Time: 9:24 PM
 */
public class FGKeyEventParser extends FGFocusTargetedEventParser<KeyEvent>
{
    private static final Var getStats_ = clojure.lang.RT.var("flatgui.widgets.componentbase", "get-stats");
    private Map<Object, Object> lastStats_;

    @Override
    public Map<KeyEvent, List<Keyword>> getTargetCellIds(KeyEvent event, IFGModule fgModule)
    {
//        if (event.getKeyCode() == KeyEvent.VK_S && event.getID() == KeyEvent.KEY_PRESSED &&
//                event.isControlDown() && event.isShiftDown())
//        {
//            Map<Object, Object> stats = (Map<Object, Object>) getStats_.invoke();
//            System.out.println("--- Stats --------------------------------------");
//            List<Tuple> statList = new ArrayList<>(stats.size());
//            for (Object k : stats.keySet())
//            {
//                Number newStats = (Number) stats.get(k);
//                Number lastStats = lastStats_ != null ? (Number) lastStats_.get(k) : null;
//                Long diff = lastStats != null ? Long.valueOf(newStats.longValue()-lastStats.longValue()) : Long.valueOf(-1);
//
//                statList.add(Tuple.pair(
//                        diff,
//                        k + " " + stats.get(k) + (lastStats != null ? " (+"+diff+")" : "")));
//            }
//            Collections.sort(statList, (a,b) -> Long.compare(((Long)a.getFirst()).longValue(), ((Long)b.getFirst()).longValue()));
//            for (Tuple s : statList)
//            {
//                System.out.println((String) s.getSecond());
//            }
//            lastStats_ = stats;
//        }

        return super.getTargetCellIds(event, fgModule);
    }
}
