/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import clojure.lang.Associative;
import clojure.lang.Keyword;
import flatgui.core.IFGInteropUtil;
import flatgui.core.engine.AppContainer;

import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class FGAppContainer<Interop extends IFGInteropUtil> extends AppContainer<FGClojureContainerParser, FGClojureResultCollector>
{
    protected static final int DFLT_UNIT_SIZE_PX = 64;

    private static final Keyword INTEROP_KW = Keyword.intern("interop");

    private final FGMouseEventParser mouseEventParser_;
    private final Interop interopUtil_;

    public FGAppContainer(Map<Object, Object> container, Interop interopUtil)
    {
        this(container, interopUtil, DFLT_UNIT_SIZE_PX);
    }

    public FGAppContainer(Map<Object, Object> container, Interop interopUtil, int unitSizePx)
    {
        super(new FGClojureContainerParser(),
                new FGClojureResultCollector(unitSizePx), assocInterop(container, interopUtil));

        interopUtil_ = interopUtil;

        mouseEventParser_ = new FGMouseEventParser(unitSizePx);
        getInputEventParser().registerReasonClassParser(MouseEvent.class, mouseEventParser_);
    }

    protected final Interop getInteropUtil()
    {
        return interopUtil_;
    }

    private static Map<Object, Object> assocInterop(Map<Object, Object> container, IFGInteropUtil interopUtil)
    {
        return (Map<Object, Object>) ((Associative)container).assoc(INTEROP_KW, interopUtil);
    }
}
