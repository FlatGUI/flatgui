/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core2;

import flatgui.core.IFGContainer;
import flatgui.core.websocket.FGContainerSession;

/**
 * @author Denis Lebedev
 */
public class FGSessionContainerHost implements IFGContainerHost<FGContainerSession>
{
    @Override
    public FGContainerSession hostContainer(IFGContainer container)
    {
        return new FGContainerSession(container);
    }
}
