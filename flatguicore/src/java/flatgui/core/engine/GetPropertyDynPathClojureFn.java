/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine;

import java.util.List;

/**
 * Property value accessor function - implementation of (get-property [..] ..)
 *
 * @author Denis Lebedev
 */
public class GetPropertyDynPathClojureFn extends GetPropertyClojureFn
{
    @Override
    protected ClojureContainerParser.GetPropertyDelegate getDelegate(Object path, Object property)
    {
        return currentEvolverWrapper_.get().getDelegateByIdAndPath(getterId_, (List<Object>) path);
    }
}
