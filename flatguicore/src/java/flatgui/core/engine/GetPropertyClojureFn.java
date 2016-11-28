/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine;

import clojure.lang.AFunction;

import java.util.List;

/**
 * @author Denis Lebedev
 */
public abstract class GetPropertyClojureFn extends AFunction
{
    private static Integer counter_ = Integer.valueOf(0);

    protected final Integer getterId_;

    protected static ThreadLocal<ClojureContainerParser.EvolverWrapper> currentEvolverWrapper_ = new ThreadLocal<>();

    public GetPropertyClojureFn()
    {
        getterId_ = getNewId();
    }

    @Override
    public Object invoke(Object path, Object property)
    {
        ClojureContainerParser.GetPropertyDelegate delegate = getDelegate(path, property);
        if (!delegate.isLinked())
        {
            delegate.link((List<Object>) path, property);
        }
        return delegate.getProperty();
    }

    @Override
    public Object invoke(Object component, Object path, Object property)
    {
        return invoke(path, property);
    }

    static void visit(ClojureContainerParser.EvolverWrapper evolverWrapper)
    {
        currentEvolverWrapper_.set(evolverWrapper);
    }

    private static synchronized Integer getNewId()
    {
        Integer id = counter_;
        counter_ = Integer.valueOf(counter_.intValue()+1);
        return id;
    }

    protected abstract ClojureContainerParser.GetPropertyDelegate getDelegate(Object path, Object property);
}
