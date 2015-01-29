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
 *         Date: 8/11/13
 *         Time: 4:06 PM
 */
public class FGInputEventParser implements IFGInputEventParser<Object>
{
    private Map<Object, IFGInputEventParser<?>> implMap_;

    public FGInputEventParser()
    {
        implMap_ = new HashMap<>();
    }

    public <C> void registerReasonClassParser(Class<C> reasonClass,
                                         IFGInputEventParser<? super C> parser)
    {
        implMap_.put(getMapKeyForClass(reasonClass), parser);
    }

    @Override
    public Map<String, Object> initialize(IFGModule fgModule)
    {
        Map<String, Object> allProperties = new HashMap<>();
        for (IFGInputEventParser<?> impl : implMap_.values())
        {
            Map<String, Object> implMap = impl.initialize(fgModule);
            if (implMap != null)
            {
                allProperties.putAll(implMap);
            }
        }
        return allProperties;
    }

    @Override
    public Map<String, Object> getTargetedPropertyValues(Object reason)
    {
        return getRepaintReasonParser(reason).getTargetedPropertyValues(reason);
    }

    @Override
    public Map<Object, Collection<Object>> getTargetCellIds(Object reason, IFGModule fgModule, Map<String, Object> generalPropertyMap)
    {
        return getRepaintReasonParser(reason).getTargetCellIds(reason, fgModule, generalPropertyMap);
    }

    private Object getMapKeyForClass(Class c)
    {
        return c.getName();
    }

    private Object getMapKey(Object reason)
    {
        return reason != null ? getMapKeyForClass(reason.getClass()) : "<null>";
    }

    private IFGInputEventParser<Object> getRepaintReasonParser(Object reason)
    {
        IFGInputEventParser<Object> parser =
                (IFGInputEventParser<Object>) implMap_.get(getMapKey(reason));
        if (parser != null)
        {
            return parser;
        }
        else
        {
            // Internal core inconsistency
            throw new IllegalArgumentException("No parser for repaint reason: " +
                    (reason != null ? reason + " of class " + reason.getClass().getName()  : "null"));
        }
    }
}
