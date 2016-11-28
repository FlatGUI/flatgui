/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine;

/**
 * Property value accessor function - implementation of (get-property [..] ..)
 *
 * @author Denis Lebedev
 */
public class GetPropertyStaticClojureFn extends GetPropertyClojureFn
{
    @Override
    protected ClojureContainerParser.GetPropertyDelegate getDelegate(Object path, Object property)
    {
        return currentEvolverWrapper_.get().getDelegateById(getterId_);
    }
}
