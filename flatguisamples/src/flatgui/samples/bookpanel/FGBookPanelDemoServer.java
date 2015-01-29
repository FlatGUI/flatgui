/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.samples.bookpanel;

import flatgui.core.websocket.FGAppServer;
import flatgui.core2.FGTemplate;
import flatgui.core2.IFGTemplate;

import java.io.*;
import java.net.URL;
import java.util.Scanner;

/**
 * @author Denis Lebedev
 */
public class FGBookPanelDemoServer
{
    private static final String CONTAINER_NS = "bookpanelmain";
    private static final String CONTAINER_VAR_NAME = "bookpanel";
    private static final int PORT = 13100;


    public static void main(String[] args) throws Exception
    {
        URL formUrl = ClassLoader.getSystemResource("flatgui/samples/forms/bookpanelmain.clj");
        String sourceCode = new Scanner(new File(formUrl.toURI())).useDelimiter("\\Z").next();

        IFGTemplate bookPanelTemplate = new FGTemplate(sourceCode, CONTAINER_NS, CONTAINER_VAR_NAME);

        FGAppServer server = new FGAppServer(bookPanelTemplate, PORT);
        server.start();
        server.join();
    }
}

