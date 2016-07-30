/*
 * Copyright (c) 2013 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */
package flatgui.core.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Denys Lebediev
 *         Date: 8/11/13
 *         Time: 4:06 PM
 */
public class InputEventParser implements IInputEventParser<Object, Object>
{
    private Map<Object, IInputEventParser<?, ?>> implMap_;

    public InputEventParser()
    {
        implMap_ = new HashMap<>();
    }

    public <C> void registerReasonClassParser(Class<C> reasonClass,
                                         IInputEventParser<? super C, ?> parser)
    {
        implMap_.put(getMapKeyForClass(reasonClass), parser);
    }

    @Override
    public Map<Object, List<Object>> parseInputEvent(Object inputEvent)
    {
        IInputEventParser<Object, Object> parser = getEventParser(inputEvent);
        if (parser != null)
        {
            return parser.parseInputEvent(inputEvent);
        }
        else
        {
            throw new IllegalArgumentException("No parser for input event: " +
                    (inputEvent != null ? inputEvent + " of class " + inputEvent.getClass().getName()  : "null"));
        }
    }

    private Object getMapKeyForClass(Class c)
    {
        return c.getName();
    }

    private Object getMapKey(Object reason)
    {
        return reason != null ? getMapKeyForClass(reason.getClass()) : "<null>";
    }

    private IInputEventParser<Object, Object> getEventParser(Object reason)
    {
        return (IInputEventParser<Object, Object>) implMap_.get(getMapKey(reason));
    }
}
