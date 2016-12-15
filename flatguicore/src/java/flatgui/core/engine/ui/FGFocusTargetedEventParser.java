/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import flatgui.core.engine.Container;
import flatgui.core.engine.IInputEventParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public abstract class FGFocusTargetedEventParser<Reason, FGEvent> implements IInputEventParser<Reason, FGEvent>
{
    @Override
    public Map<FGEvent, Integer> parseInputEvent(Container container, Reason inputEvent)
    {
        Integer targetUid = getFocusedComponentUid(0, container);
        System.out.println("-DLTEMP- FGFocusTargetedEventParser.parseInputEvent " + targetUid);
        if (targetUid != null)
        {
            FGEvent event = reasonToEvent(inputEvent);
            Map<FGEvent, Integer> m = new HashMap<>();
            m.put(event, targetUid);
            return m;
        }
        else
        {
            return Collections.emptyMap();
        }
    }

    protected abstract FGEvent reasonToEvent(Reason r);

    private Integer getFocusedComponentUid(Integer componentUid, Container rootContainer)
    {
        Container.IComponent component = rootContainer.getComponent(componentUid);

        FGClojureContainerParser.FGComponentDataCache componentDataCache =
                (FGClojureContainerParser.FGComponentDataCache) component.getCustomData();
        Integer focusStateIndex = componentDataCache.getFocusStateIndex();
        Map<Object, Object> focusState = rootContainer.getPropertyValue(focusStateIndex);

        Object focusMode = FGClojureContainerParser.getFocusMode(focusState);
        if (FGClojureContainerParser.hasFocus(focusMode))
        {
            return componentUid;
        }
        else if (FGClojureContainerParser.parentOfFocused(focusMode))
        {
            Object focusedChildId = FGClojureContainerParser.getFocusedChildId(focusState);
            Integer focusedChildIndex = component.getChildIndex(focusedChildId);
            if (focusedChildIndex != null)
            {
                return getFocusedComponentUid(focusedChildIndex, rootContainer);
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }
}
