/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine;

import clojure.lang.Keyword;

import java.util.List;

/**
 * Property value accessor function - implementation of (get-property [..] ..)
 *
 * @author Denis Lebedev
 */
public class GetDynPropertyDynPathClojureFn extends GetPropertyClojureFn
{
    @Override
    protected ClojureContainerParser.GetPropertyDelegate getDelegate(Object path, Object property)
    {
        return currentEvolverWrapper_.get().getDelegateByIdPathAndProperty(getterId_, (List<Object>) path, (Keyword) property);
    }
}
