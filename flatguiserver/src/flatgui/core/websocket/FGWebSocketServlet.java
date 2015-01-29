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

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
* @author Denis Lebedev
*/
public class FGWebSocketServlet extends WebSocketServlet
{

    @Override
    public void configure(WebSocketServletFactory factory)
    {
        factory.register(FGContainerSessionProvider.class);
        factory.getPolicy().setIdleTimeout(24 * 60 * 60 * 1000);
    }
}
