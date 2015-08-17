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
public class FGKeyEventParser extends FGFocusTargetedEventParser<KeyEvent>
{
    public static final String CHANNEL_NAME = "keyboard";
    public static final String FOCUS_ORDER_VECTOR_NAME = "focus-order";

    public static final String FOCUS_OWNER_GENERAL_PROPERTY = "FocusOwner";

    @Override
    public Map<String, Object> initialize(IFGModule fgModule)
    {
        return null;
    }

    @Override
    public Map<String, Object> getTargetedPropertyValues(KeyEvent keyEvent)
    {
        Map<String, Object> map = new HashMap<>();
        map.put(CHANNEL_NAME, keyEvent);
        return map;
    }
}
