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
import java.util.HashMap;
import java.util.Map;

import clojure.lang.Var;
import flatgui.core.IFGInteropUtil;

/**
 * @author Denys Lebediev
 */
public class FGWebInteropUtil implements IFGInteropUtil
{
    private static final Var strToFont_ = clojure.lang.RT.var("flatgui.awt", "str->font");
    private static final Container CONTAINER = new Container();

    private final double unitSizePx_;
    private Font referenceFont_;
    private String referenceFontStr_;
    private Map<String, byte[]> fontStrToCharMetrics_;
    private FontMetrics referenceFontMetrics_;

    public FGWebInteropUtil(int unitSizePx)
    {
        unitSizePx_ = unitSizePx;
        referenceFont_ = getDefaultFont();
        fontStrToCharMetrics_ = new HashMap<>();
        updateFontMetrics();
    }

    @Override
    public double getStringWidth(String str, String font)
    {
        if (font != null && !font.equals(referenceFontStr_))
        {
            setReferenceFont(font, (Font) strToFont_.invoke(font));
        }
        if (str != null)
        {
            double widthPx;
            byte[] charMetrics = fontStrToCharMetrics_.get(referenceFontStr_);
            if (charMetrics != null)
            {
                widthPx = 0;
                for (int i = 0; i < str.length(); i++)
                {
                    // TODO bug here:
                    // At least with Firefox, arrow buttons cause text field generate text string containing zeros
                    // or control chars. So below gives AIOB and that exception happens to prevent garbage to get
                    // into text field model
                    //if (str.charAt(i) >= 32)
                    {
                        widthPx += charMetrics[str.charAt(i) - 32];
                    }
                }
            }
            else
            {
                widthPx = SwingUtilities.computeStringWidth(referenceFontMetrics_, str);
            }
            return widthPx / unitSizePx_;
        }
        else
        {
            return 0;
        }
    }

    @Override
    public double getFontHeight(String font)
    {
        double heightPx = referenceFontMetrics_.getHeight();
        return heightPx / unitSizePx_;
    }

    @Override
    public double getFontAscent(String font)
    {
        double heightPx = referenceFontMetrics_.getAscent();
        return heightPx / unitSizePx_;
    }

    public String setMetricsTransmission(byte[] metricsTransmission)
    {
        int fontStrLen = metricsTransmission[1];
        int charCount = metricsTransmission.length-1-1-fontStrLen;

        byte[] fontStrBytes = new byte[fontStrLen];
        System.arraycopy(metricsTransmission, 1+1, fontStrBytes, 0, fontStrLen);
        char[] fontStrChars = new char[fontStrLen];
        for (int i=0; i<fontStrLen; i++)
        {
            fontStrChars[i] = (char) fontStrBytes[i];
        }
        String fontStr = String.valueOf(fontStrChars);

        System.out.println("Received metrics for font " + fontStr);

        byte[] charMetrics = new byte[charCount];
        System.arraycopy(metricsTransmission, 1+1+fontStrLen, charMetrics, 0, charCount);
        fontStrToCharMetrics_.put(fontStr, charMetrics);

        return fontStr;
    }

    public void setReferenceFont(String fontStr, Font font)
    {
        referenceFontStr_ = fontStr;
        referenceFont_ = font;
        updateFontMetrics();
    }

    // Non-public

    private void updateFontMetrics()
    {
        referenceFontMetrics_ = getDefaultReferenceFontMetrics(referenceFont_);
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
