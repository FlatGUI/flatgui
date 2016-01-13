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
    public Map<Object, List<Keyword>> getTargetCellIds(List<Keyword> knownTargetIdPath, Object reason, IFGModule fgModule)
    {
        IFGInputEventParser<Object> parser = getRepaintReasonParser(reason);
        if (parser != null)
        {
            return parser.getTargetCellIds(knownTargetIdPath, reason, fgModule);
        }
        else
        {
            if (knownTargetIdPath != null)
            {
                Map<Object, List<Keyword>> direct = new HashMap<>();
                direct.put(reason, knownTargetIdPath);
                return direct;
            }
            else
            {
                throw new IllegalArgumentException("No parser for repaint reason: " +
                        (reason != null ? reason + " of class " + reason.getClass().getName()  : "null")
                        + " while known target id path is not provided");
            }
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

    private IFGInputEventParser<Object> getRepaintReasonParser(Object reason)
    {
        return (IFGInputEventParser<Object>) implMap_.get(getMapKey(reason));
    }
}
