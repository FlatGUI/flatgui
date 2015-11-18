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

import java.awt.*;
import java.util.List;

import flatgui.core.util.IFGChangeListener;
import flatgui.core.util.Tuple;

/**
 * @author Denys Lebediev
 *         Date: 9/3/13
 *         Time: 6:54 PM
 */
public interface IFGPrimitivePainter
{
    /**
     * @param fontChangeListener - a pair of: font string, Font instance
     */
    void addFontChangeListener(IFGChangeListener<Tuple> fontChangeListener);

    void paintPrimitive(Graphics g, List<Object> primitive);
}
