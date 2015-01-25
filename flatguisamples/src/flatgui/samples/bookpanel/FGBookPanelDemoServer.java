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

import flatgui.core.FlatGUI;
import flatgui.core.websocket.FGAppServer;

import java.io.*;
import java.net.URL;
import java.util.Scanner;

/**
 * @author Denis Lebedev
 */
public class FGBookPanelDemoServer
{
    public static final String CONTAINER_NS = "bookpanelmain";
    public static final String CONTAINER_VAR_NAME = "bookpanel";
    public static final String APP_NAME = "bookpanel";
    public static final String CONTAINER_NAME = "bookpanel";


    public static void main(String[] args) throws Exception
    {
        FlatGUI flatGui = new FlatGUI();

        URL formUrl = ClassLoader.getSystemResource("flatgui/samples/forms/bookpanelmain.clj");
        flatGui.loadAppFromString(new Scanner(new File(formUrl.toURI())).useDelimiter("\\Z").next(),
                APP_NAME,
                CONTAINER_NAME,
                CONTAINER_NS,
                CONTAINER_VAR_NAME);

        flatGui.startContainerServer(APP_NAME, CONTAINER_NAME, 13100);
    }
}

