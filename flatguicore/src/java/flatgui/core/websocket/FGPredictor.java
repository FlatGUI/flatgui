/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core.websocket;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import flatgui.core.FGMouseEventParser;
import flatgui.core.util.Tuple;

/**
 * @author Denis Lebedev
 */
class FGPredictor
{
    private MouseEvent latestMouseEvent_;

    private int lastX_ = -1;
    private int lastY_ = -1;
    private int predictX_ = 0;
    private int predictY_ = 0;
    private int lastDX_ = -1;
    private int lastDY_ = -1;

    void considerInputEvent(Object e)
    {
        if (e instanceof MouseEvent)
        {
            latestMouseEvent_ = (MouseEvent) e;

            int mouseX = latestMouseEvent_.getX();
            int mouseY = latestMouseEvent_.getY();

            lastDX_ = (mouseX-lastX_);
            lastDY_ = (mouseY-lastY_);

            predictX_ = lastX_ == -1 ? mouseX : mouseX + (mouseX-lastX_);
            predictY_ = lastY_ == -1 ? mouseY : mouseY + (mouseY-lastY_);

//            System.out.println("-DLTEMP- FGPredictor.considerInputEvent "
//                + (latestMouseEvent_.getX()-lastX_) + " "
//                + (latestMouseEvent_.getY()-lastY_));

            lastX_ = mouseX;
            lastY_ = mouseY;
        }
    }

//    double getNearestPredictedMouseX()
//    {
//        return predictX_;
//    }
//
//    double getNearestPredictedMouseY()
//    {
//        return predictY_;
//    }

    List<Tuple> moveAroundTheLatestEvent()
    {
        int axisDeltaX = Math.abs(lastDX_) > 0 ? 1 : 0;
        int axisDeltaY = Math.abs(lastDY_) > 0 ? 1 : 0;

        if (latestMouseEvent_ != null
            && (latestMouseEvent_.getID() == MouseEvent.MOUSE_MOVED || latestMouseEvent_.getID() == MouseEvent.MOUSE_DRAGGED))
        {
            int minDx = Math.min(Math.max(lastDX_-axisDeltaX, -8), 7);
            int maxDx = Math.min(Math.max(lastDX_+axisDeltaX, -8), 7);
            int minDy = Math.min(Math.max(lastDY_-axisDeltaY, -8), 7);
            int maxDy = Math.min(Math.max(lastDY_+axisDeltaY, -8), 7);

            List<Tuple> eventList = new ArrayList<>();

            for (int dx = minDx; dx <= maxDx; dx++)
            {
                for (int dy = minDy; dy <= maxDy; dy++)
                {
                    eventList.add(Tuple.triple(dx, dy, FGMouseEventParser.deriveWithCoordAdjustment(latestMouseEvent_, dx, dy)));
                }
            }

            return eventList;
        }
        else
        {
            return null;
        }
    }

    List<MouseEvent> leftClickInLatestPosition()
    {
        if (latestMouseEvent_ != null && latestMouseEvent_.getButton() == MouseEvent.NOBUTTON)
        {
            List<MouseEvent> eventList = new ArrayList<>();
            eventList.add(FGMouseEventParser.deriveWithIdAndButton(latestMouseEvent_, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1));
            eventList.add(FGMouseEventParser.deriveWithIdAndButton(latestMouseEvent_, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1));
            eventList.add(FGMouseEventParser.deriveWithIdAndButton(latestMouseEvent_, MouseEvent.MOUSE_CLICKED, MouseEvent.BUTTON1));
            return eventList;
        }
        else
        {
            return null;
        }
    }
}
