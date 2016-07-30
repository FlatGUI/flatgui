/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import flatgui.core.engine.AppContainer;

import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class FGAppContainer extends AppContainer
{
    public FGAppContainer(Map<Object, Object> container)
    {
        super(new FGClojureContainerParser(),
                new FGClojureResultCollector(), container);
    }


}
