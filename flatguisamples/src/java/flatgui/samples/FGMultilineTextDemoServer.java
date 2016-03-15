/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.samples;

import java.io.InputStream;
import java.util.Scanner;

import flatgui.core.FGTemplate;
import flatgui.core.IFGTemplate;
import flatgui.core.websocket.FGAppServer;

/**
 * @author Denis Lebedev
 */
public class FGMultilineTextDemoServer
{
    public static final String CONTAINER_NS = "text";
    public static final String CONTAINER_VAR_NAME = "textpanel";
    private static final int PORT = 13100;

    public static void main(String[] args) throws Exception
    {
        InputStream sampleIs = FGCompoundDemoServer.class.getClassLoader().getResourceAsStream("flatgui/samples/forms/text.clj");
        String sourceCode = new Scanner(sampleIs).useDelimiter("\\Z").next();

        IFGTemplate template = new FGTemplate(sourceCode, CONTAINER_NS, CONTAINER_VAR_NAME);

        FGAppServer server = new FGAppServer(template, PORT);
        server.start();
        server.join();
    }
}

