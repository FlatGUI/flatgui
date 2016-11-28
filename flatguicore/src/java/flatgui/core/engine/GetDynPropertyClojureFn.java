/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine;

import clojure.lang.Keyword;

/**
 * Property value accessor function - implementation of (get-property [..] ..)
 *
 * @author Denis Lebedev
 */
public class GetDynPropertyClojureFn extends GetPropertyClojureFn
{
    @Override
    protected ClojureContainerParser.GetPropertyDelegate getDelegate(Object path, Object property)
    {
        return currentEvolverWrapper_.get().getDelegateByIdAndProperty(getterId_, (Keyword) property);
    }
}
