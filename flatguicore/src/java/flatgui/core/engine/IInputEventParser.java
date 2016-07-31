/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine;

import java.util.Map;

/**
 * @author Denis Lebedev
 */
public interface IInputEventParser<Reason, FGEvent>
{
    Map<FGEvent, Integer> parseInputEvent(Container container, Reason inputEvent);
}
