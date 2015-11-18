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

import flatgui.core.IFGContainer;

/**
 * @author Denis Lebedev
 */
public class FGDummyInteropUtil extends FGAWTInteropUtil
{
    public FGDummyInteropUtil()
    {
        // TODO Some initial default unit size here
        super(IFGContainer.UNIT_SIZE_PX);
    }
}
