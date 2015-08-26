/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */
package flatgui.core;

import javax.swing.*;
import java.awt.*;

/**
 * @author Denys Lebediev
 */
public class FGWebInteropUtil implements IFGInteropUtil
{
    // TODO This is still a dummy implementation

    private final double unitSizePx_;
    private Font referenceFont_;
    private FontMetrics referenceFontMetrics_;

    public FGWebInteropUtil(int unitSizePx)
    {
        unitSizePx_ = unitSizePx;
        referenceFont_ = new Font("Tahoma", Font.PLAIN, 12);
        referenceFontMetrics_ = getDefaultReferenceFontMetrics(referenceFont_);
    }

    public double getStringWidth(String str)
    {
        double widthPx = SwingUtilities.computeStringWidth(referenceFontMetrics_, str);
        return widthPx / unitSizePx_;
    }

    public double getFontAscent()
    {
        double heightPx = referenceFontMetrics_.getAscent();
        //@todo what does this 0.75 mean?
        return 0.75 * heightPx / unitSizePx_;
    }

    private static FontMetrics getDefaultReferenceFontMetrics(Font font)
    {
        // This will give FontMetrics constructed basing on default FontRenderContext
        // (identity transformation, no antialiasing, no fractional metrics) which is
        // a rendering device state that current FlatGUI implementation counts on
        return new Container().getFontMetrics(font);
    }
}
