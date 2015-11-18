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

import flatgui.core.FGTemplate;
import flatgui.core.IFGTemplate;
import flatgui.core.websocket.FGAppServer;

import java.io.InputStream;
import java.util.Scanner;

/**
 * @author Denis Lebedev
 */
public class FGCompoundDemoServer
{
    public static final String CONTAINER_NS = "compound";
    public static final String CONTAINER_VAR_NAME = "compoundpanel";
    private static final int PORT = 13100;

    private static final String FOCUS_CONTAINER_NS = "fdemo";
    private static final String FOCUS_CONTAINER_VAR_NAME = "fdemopanel";
    private static final String FOCUS_MAPPING = "focus_sample";

    private static final String LAYOUT_CONTAINER_NS = "layoutdemo2";
    private static final String LAYOUT_CONTAINER_VAR_NAME = "layoutpanel";
    private static final String LAYOUT_MAPPING = "layout_sample";

    public static void main(String[] args) throws Exception
    {
        InputStream compoundSampleIs = FGCompoundDemoServer.class.getClassLoader().getResourceAsStream("flatgui/samples/forms/compound.clj");
        String compoundSampleSourceCode = new Scanner(compoundSampleIs).useDelimiter("\\Z").next();
        IFGTemplate compondSampleTemplate = new FGTemplate(compoundSampleSourceCode, CONTAINER_NS, CONTAINER_VAR_NAME);

        FGAppServer server = new FGAppServer(compondSampleTemplate, PORT);


        InputStream focusSampleIs = FGCompoundDemoServer.class.getClassLoader().getResourceAsStream("flatgui/samples/forms/fdemo.clj");
        String focusSampleSourceCode = new Scanner(focusSampleIs).useDelimiter("\\Z").next();
        IFGTemplate focusSampleTemplate = new FGTemplate(focusSampleSourceCode, FOCUS_CONTAINER_NS, FOCUS_CONTAINER_VAR_NAME);
        server.addApplication(FOCUS_MAPPING, focusSampleTemplate);

        InputStream layoutSampleIs = FGCompoundDemoServer.class.getClassLoader().getResourceAsStream("flatgui/samples/forms/layoutdemo2.clj");
        String layoutSampleSourceCode = new Scanner(layoutSampleIs).useDelimiter("\\Z").next();
        IFGTemplate layoutSampleTemplate = new FGTemplate(layoutSampleSourceCode, LAYOUT_CONTAINER_NS, LAYOUT_CONTAINER_VAR_NAME);
        server.addApplication(LAYOUT_MAPPING, layoutSampleTemplate);

        server.start();
        server.join();
    }
}

