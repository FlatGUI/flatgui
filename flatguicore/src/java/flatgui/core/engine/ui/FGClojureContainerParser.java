/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import flatgui.core.engine.ClojureContainerParser;
import flatgui.core.engine.Container;

import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class FGClojureContainerParser extends ClojureContainerParser
{
    @Override
    public void processComponentAfterIndexing(Map<Object, Object> componentData, Container.IComponent component)
    {
        // TODO cache indices of look, look-vec
    }
}
