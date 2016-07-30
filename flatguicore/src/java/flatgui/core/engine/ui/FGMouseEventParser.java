/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import flatgui.core.awt.FGMouseEvent;
import flatgui.core.engine.IInputEventParser;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public abstract class FGMouseEventParser implements IInputEventParser<MouseEvent, FGMouseEvent>
{
    @Override
    public Map<FGMouseEvent, List<Object>> parseInputEvent(MouseEvent inputEvent, Map<Object, Object> container)
    {
        return null;
    }

    // TODO
    // regarding previous impl: note that vertors were not need for x-rel and y-rel. This is the only way
    // it was accessed:
    //   :target-id-path-index (dec (count target-id-path))

    //protected abstract
}
