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

import java.awt.event.KeyEvent;
import java.util.*;

/**
 * @author Denys Lebediev
 *         Date: 8/15/13
 *         Time: 9:24 PM
 */
public class FGKeyEventParser implements IFGInputEventParser<KeyEvent>
{
    public static final String CHANNEL_NAME = "keyboard";
    public static final String FOCUS_ORDER_VECTOR_NAME = "focus-order";

    public static final String FOCUS_OWNER_GENERAL_PROPERTY = "FocusOwner";

    private IFGModule fgModule_;

    @Override
    public Map<String, Object> initialize(IFGModule fgModule)
    {
        fgModule_ = fgModule;
        return null;
    }

    @Override
    public Map<String, Object> getTargetedPropertyValues(KeyEvent keyEvent)
    {
        Map<String, Object> map = new HashMap<>();
        map.put(CHANNEL_NAME, keyEvent);
        return map;
    }

    @Override
    public Map<KeyEvent, Collection<Object>> getTargetCellIds(KeyEvent keyEvent, IFGModule fgModule, Map<String, Object> generalPropertyMap)
    {
//        Set<Object> targetCellIds = new HashSet<>();
//        Object focusOwnerId = fgModule_.getFocusOwnerId();
//        if (focusOwnerId != null)
//        {
//            targetCellIds.add(fgModule_.getFocusOwnerId());
//        }
//        return targetCellIds;


        // temporary for debug
//        if (FGMouseEventParser.latestPressedTargetCellIds_ != null)
//        {
//            System.out.println("-DLTEMP- FGKeyEventParser.getTargetCellIds: " + FGMouseEventParser.latestPressedTargetCellIds_.size());
//        }
//        else
//        {
//            System.out.println("-DLTEMP- FGKeyEventParser.getTargetCellIds: no letest cells");
//        }

        Map<KeyEvent, Collection<Object>> map = new HashMap<>();
        map.put(keyEvent, FGMouseEventParser.latestPressedTargetCellIds_);
        return map;
    }

}
