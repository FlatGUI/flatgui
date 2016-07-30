/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine;

import java.util.List;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public interface IInputEventParser<Reason, FGEvent>
{
    Map<FGEvent, List<Object>> parseInputEvent(Reason inputEvent);
}
