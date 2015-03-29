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
import java.util.Map;

/**
 * @author Denys Lebediev
 *         Date: 8/15/13
 *         Time: 9:24 PM
 */
public class FGClipboardEventEventParser implements IFGInputEventParser<FGClipboardEvent>
{
    public static final String CHANNEL_NAME = "clipboard";
    public static final String FOCUS_ORDER_VECTOR_NAME = "focus-order";

    public static final String FOCUS_OWNER_GENERAL_PROPERTY = "FocusOwner";

    @Override
    public Map<String, Object> initialize(IFGModule fgModule)
    {
        return null;
    }

    @Override
    public Map<String, Object> getTargetedPropertyValues(FGClipboardEvent keyEvent)
    {
        Map<String, Object> map = new HashMap<>();
        //map.put(CHANNEL_NAME, keyEvent);
        return map;
    }

    @Override
    public Map<FGClipboardEvent, Collection<Object>> getTargetCellIds(FGClipboardEvent keyEvent, IFGModule fgModule, Map<String, Object> generalPropertyMap)
    {
        // TODO see key event parser
        Map<FGClipboardEvent, Collection<Object>> map = new HashMap<>();
        System.out.println("-DLTEMP- FGClipboardEventEventParser.getTargetCellIds " + FGMouseEventParser.latestPressedTargetCellIds_);
        map.put(keyEvent, FGMouseEventParser.latestPressedTargetCellIds_);
        return map;
    }

}
