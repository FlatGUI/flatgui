/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import flatgui.core.awt.FGMouseEvent;
import flatgui.core.engine.Container;
import flatgui.core.engine.IInputEventParser;

import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class FGMouseEventParser implements IInputEventParser<MouseEvent, FGMouseEvent>
{
    private final int unitSizePx_;

    public FGMouseEventParser(int unitSizePx)
    {
        unitSizePx_ = unitSizePx;
    }

    @Override
    public Map<FGMouseEvent, Integer> parseInputEvent(Container container, MouseEvent mouseEvent)
    {
        boolean newLeftButtonDown = (mouseEvent.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK;

        double mouseX = ((double)mouseEvent.getX()) / ((double)unitSizePx_);
        double mouseY = ((double)mouseEvent.getY()) / ((double)unitSizePx_);

        Integer targetComponentUid = getTargetComponentUid(0, container, mouseX, mouseY);
        if (targetComponentUid != null)
        {
            return null;
        }
        else
        {
            return Collections.emptyMap();
        }
    }

    private Integer getTargetComponentUid(Integer componentUid, Container rootContainer, double mouseX, double mouseY)
    {
        Container.IComponent component = rootContainer.getComponent(componentUid);

        FGClojureContainerParser.FGComponentDataCache componentDataCache =
                (FGClojureContainerParser.FGComponentDataCache) component.getCustomData();
        Integer pmIndex = componentDataCache.getPositionMatrixIndex();
        Integer csIndex = componentDataCache.getClipSizeIndex();

        List<List<Double>> positionMatrix = rootContainer.getPropertyValue(pmIndex);
        List<List<Double>> clipSize = rootContainer.getPropertyValue(csIndex);

        double x = positionMatrix.get(0).get(3).doubleValue();
        double y = positionMatrix.get(1).get(3).doubleValue();
        double w = clipSize.get(0).get(0).doubleValue();
        double h = clipSize.get(0).get(1).doubleValue();

        if (in(mouseX, x, x+w) && in(mouseY, y, y+h))
        {
            Iterable<Integer> childIndices = component.getChildIndices();
            if (childIndices != null)
            {
                for (Integer childIndex : childIndices)
                {
                    Integer target = getTargetComponentUid(childIndex, rootContainer, mouseX - x, mouseY - y);
                    if (target != null)
                    {
                        return target;
                    }
                }
            }
            return componentUid;
        }
        else
        {
            return null;
        }
    }

    private static boolean in(double n, double min, double max)
    {
        return n >= min && n < max;
    }
}
