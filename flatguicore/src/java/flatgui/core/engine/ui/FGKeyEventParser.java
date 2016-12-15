/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import java.awt.event.KeyEvent;

/**
 * @author Denis Lebedev
 */
public class FGKeyEventParser extends FGFocusTargetedEventParser<KeyEvent, KeyEvent>
{
    @Override
    protected KeyEvent reasonToEvent(KeyEvent r)
    {
        return r;
    }
}
