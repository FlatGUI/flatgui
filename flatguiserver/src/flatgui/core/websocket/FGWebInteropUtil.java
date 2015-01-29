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

import javax.swing.*;
import java.awt.*;

/**
 * @author Denys Lebediev
 *         Date: 8/14/13
 *         Time: 11:03 PM
 */
public class FGWebInteropUtil
{
    private final double unitSizePx_;
    private final Font referenceFont_;
    private final FontMetrics referenceFontMetrics_;

    public FGWebInteropUtil(int unitSizePx)
    {
        unitSizePx_ = unitSizePx;
        referenceFont_ = new Font("Tahoma", Font.PLAIN, 12);
        referenceFontMetrics_ = new Container().getFontMetrics(referenceFont_);
    }

    public double getStringWidth(String str)
    {
        return getStringWidth(str, referenceFont_);
    }

    public double getStringWidth(String str, Font font)
    {
        double widthPx = SwingUtilities.computeStringWidth(referenceFontMetrics_, str);
        return widthPx / unitSizePx_;
    }

    public double getFontAscent()
    {
        return getFontAscent(referenceFont_);
    }

    public double getFontAscent(Font font)
    {
        double heightPx = referenceFontMetrics_.getAscent();
        //@todo what does this 0.75 mean?
        return 0.75 * heightPx / unitSizePx_;
    }
}
