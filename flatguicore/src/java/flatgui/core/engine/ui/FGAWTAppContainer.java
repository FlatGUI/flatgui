/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import flatgui.core.awt.AbstractHostComponent;
import flatgui.core.websocket.FGWebInteropUtil;

import java.awt.*;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class FGAWTAppContainer extends FGAppContainer<FGWebInteropUtil>
{
    private final HostComponent hostComponent_;
    private final PaintListIterable paintListIterable_;

    public FGAWTAppContainer(Map<Object, Object> container)
    {
        this(container, DFLT_UNIT_SIZE_PX);
    }

    public FGAWTAppContainer(Map<Object, Object> container, int unitSizePx)
    {
        super(container, new FGWebInteropUtil(unitSizePx));

        hostComponent_ = new HostComponent();
        paintListIterable_ = new PaintListIterable();
    }

    public final Component getComponent()
    {
        return hostComponent_;
    }

    // Inner classes

    private class PaintListIterator implements Iterator<Object>
    {
        private final Iterator<Integer> naturalOrderIterator_;

        public PaintListIterator()
        {
            naturalOrderIterator_ = getContainer().getComponentNaturalOrder().iterator();
        }

        @Override
        public boolean hasNext()
        {
            return naturalOrderIterator_.hasNext();
        }

        @Override
        public Object next()
        {
            Integer nextIndex = naturalOrderIterator_.next();
            return getResultCollector().getLookVector(nextIndex);
        }
    }

    private class PaintListIterable implements Iterable<Object>
    {
        @Override
        public Iterator<Object> iterator()
        {
            return new PaintListIterator();
        }
    }

    private class HostComponent extends AbstractHostComponent
    {
        @Override
        protected void changeCursorIfNeeded() throws Exception
        {
            // TODO
        }

        @Override
        protected Iterable<Object> getPaintList(double clipX, double clipY, double clipW, double clipH) throws Exception
        {
            return paintListIterable_;
        }

        @Override
        protected void acceptEvolveReason(Object evolveReason)
        {
            evolve(evolveReason);
        }
    }
}
