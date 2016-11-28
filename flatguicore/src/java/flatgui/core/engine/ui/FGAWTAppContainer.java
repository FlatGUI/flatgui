/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import clojure.lang.Var;
import flatgui.core.awt.AbstractHostComponent;
import flatgui.core.engine.*;
import flatgui.core.engine.Container;
import flatgui.core.websocket.FGWebInteropUtil;

import java.awt.*;
import java.awt.geom.NoninvertibleTransformException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * @author Denis Lebedev
 */
public class FGAWTAppContainer extends FGAppContainer<FGWebInteropUtil>
{
    private final HostComponent hostComponent_;
//    private final PaintListIterable paintListIterable_;

    public FGAWTAppContainer(Map<Object, Object> container)
    {
        this(container, DFLT_UNIT_SIZE_PX);
    }

    public FGAWTAppContainer(Map<Object, Object> container, int unitSizePx)
    {
        super(container, new FGWebInteropUtil(unitSizePx), unitSizePx);

        hostComponent_ = new HostComponent();
//        paintListIterable_ = new PaintListIterable();
    }

    public final Component getComponent()
    {
        return hostComponent_;
    }

    // Inner classes

//    private class PaintListIterator implements Iterator<Object>
//    {
//        private final Iterator<Integer> naturalOrderIterator_;
//
//        public PaintListIterator()
//        {
//            naturalOrderIterator_ = getContainer().getComponentNaturalOrder().iterator();
//        }
//
//        @Override
//        public boolean hasNext()
//        {
//            return naturalOrderIterator_.hasNext();
//        }
//
//        @Override
//        public Object next()
//        {
//            Integer nextIndex = naturalOrderIterator_.next();
//            return getResultCollector().getLookVector(nextIndex);
//        }
//    }
//
//    private class PaintListIterable implements Iterable<Object>
//    {
//        @Override
//        public Iterator<Object> iterator()
//        {
//            return new PaintListIterator();
//        }
//    }
//

    public final void paintAllFromRoot(Consumer<List<Object>> primitivePainter) throws NoninvertibleTransformException
    {
        Container.IContainerAccessor containerAccessor = getContainer().getContainerAccessor();
        Container.IPropertyValueAccessor propertyValueAccessor = getContainer().getPropertyValueAccessor();
        getResultCollector().paintComponentWithChildren(
                primitivePainter,
                containerAccessor,
                propertyValueAccessor,
                Integer.valueOf(0));
    }

    private class HostComponent extends AbstractHostComponent
    {
        @Override
        protected void changeCursorIfNeeded() throws Exception
        {
            // TODO
        }

        @Override
        protected void paintAll(Graphics bg, double clipX, double clipY, double clipW, double clipH) throws Exception
        {
            paintAllFromRoot(primitive ->
            {
                if (primitive.size() > 0)
                {
                    if (primitive.get(0) instanceof String)
                    {
                        getPrimitivePainter().paintPrimitive(bg, primitive);
                    }
                    else
                    {
                        for (Object p : primitive)
                        {
                            getPrimitivePainter().paintPrimitive(bg, (List<Object>) p);
                        }
                    }
                }
            });
        }

        @Override
        protected void acceptEvolveReason(Object evolveReason)
        {
            try
            {
                evolve(evolveReason).get();
                repaint();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
