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

/**
 * @author Denis Lebedev
 */
class FGPredictor
{
    private MouseEvent latestMouseEvent_;

    void considerInputEvent(Object e)
    {
        if (e instanceof MouseEvent)
        {
            latestMouseEvent_ = (MouseEvent) e;
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
