/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */
package flatgui.core.awt;

import javax.swing.*;
import java.awt.*;

/**
 * @author Denys Lebediev
 *         Date: 8/14/13
 *         Time: 11:03 PM
 */
public class FGAWTInteropUtil
{
    // TODO temporary
    private final Component hostComponent_;
    // TODO temporary
    private final Font font_;

    private final double unitSizePx_;

    public FGAWTInteropUtil(Component hostComponent, int unitSizePx)
    {
        hostComponent_ = hostComponent;
        font_ = UIManager.getFont("Label.font");
        unitSizePx_ = unitSizePx;
    }

    public double getStringWidth(String str)
    {
        return getStringWidth(str, font_);
    }

    public double getStringWidth(String str, Font font)
    {
        if (font == null)
        {
            throw new NullPointerException("Font is null");
        }

        double widthPx = SwingUtilities.computeStringWidth(
                hostComponent_.getFontMetrics(font), str);
        return widthPx / unitSizePx_;
    }

    public double getFontAscent()
    {
        return getFontAscent(font_);
    }

    public double getFontAscent(Font font)
    {
        //Component c = hostComponent_ != null ? hostComponent_ : dummyComponent_;
        Component c = hostComponent_;
        FontMetrics fm = c.getFontMetrics(font);
        double heightPx = fm.getAscent();
        //@todo what does this 0.75 mean?
        return 0.75 * heightPx / unitSizePx_;
    }

}
