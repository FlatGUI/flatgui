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

import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.SwingUtilities;

import clojure.lang.Var;
import flatgui.core.IFGInteropUtil;

/**
 * @author Denys Lebediev
 *         Date: 8/14/13
 *         Time: 11:03 PM
 */
public class FGAWTInteropUtil implements IFGInteropUtil
{
    private static final Var strToFont_ = clojure.lang.RT.var("flatgui.awt", "str->font");
    private static final Container CONTAINER = new Container();

    private final double unitSizePx_;
    private String referenceFontStr_;
    private Font referenceFont_;
    private FontMetrics referenceFontMetrics_;
    private Graphics referenceGraphics_;

    public FGAWTInteropUtil(int unitSizePx)
    {
        unitSizePx_ = unitSizePx;
        referenceFont_ = getDefaultFont();
        referenceFontMetrics_ = getDefaultReferenceFontMetrics(referenceFont_);
    }

    @Override
    public double getStringWidth(String str, String font)
    {
        if (font != null && !font.equals(referenceFontStr_))
        {
            setReferenceFont(font, (Font) strToFont_.invoke(font));
        }
        double widthPx = SwingUtilities.computeStringWidth(referenceFontMetrics_, str);
        return widthPx / unitSizePx_;
    }

    @Override
    public double getFontAscent(String font)
    {
        if (font != null && !font.equals(referenceFontStr_))
        {
            setReferenceFont(font, (Font) strToFont_.invoke(font));
        }
        double heightPx = referenceFontMetrics_.getAscent();
        //@todo what does this 0.75 mean?
        return 0.75 * heightPx / unitSizePx_;
    }

    // Non-public

    void setReferenceFont(String fontStr, Font font)
    {
        referenceFontStr_ = fontStr;
        referenceFont_ = font;
        updateReferenceFontMetrics();
    }

    void setReferenceGraphics(Graphics g)
    {
        referenceGraphics_ = g;
        updateReferenceFontMetrics();
    }

    private void updateReferenceFontMetrics()
    {
        if (referenceGraphics_ != null)
        {
            referenceFontMetrics_ = referenceGraphics_.getFontMetrics(referenceFont_);
        }
        else
        {
             referenceFontMetrics_ = CONTAINER.getFontMetrics(referenceFont_);
        }
    }

    // Defaults to start with for any (trouble) case when nothing else provided

    private static Font getDefaultFont()
    {
        return new Font("Tahoma", Font.PLAIN, 12);
    }

    private static FontMetrics getDefaultReferenceFontMetrics(Font font)
    {
        // This will give FontMetrics constructed basing on default FontRenderContext
        // (identity transformation, no antialiasing, no fractional metrics) which is
        // a rendering device state that current FlatGUI implementation counts on
        return CONTAINER.getFontMetrics(font);
    }
}
