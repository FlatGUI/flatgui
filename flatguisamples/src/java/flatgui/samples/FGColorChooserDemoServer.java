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

import java.io.File;
import java.net.URL;
import java.util.Scanner;
import java.util.function.BiConsumer;

import clojure.lang.RT;
import clojure.lang.Var;
import flatgui.core.FGTemplate;
import flatgui.core.IFGContainer;
import flatgui.core.IFGTemplate;
import flatgui.core.engine.remote.FGLegacyGlueTemplate;
import flatgui.core.websocket.FGAppServer;

/**
 * @author Denis Lebedev
 */
public class FGColorChooserDemoServer
{
    private static final String CONTAINER_NS = "colorchooser";
    private static final String CONTAINER_VAR_NAME = "colorpanel";
    private static final String STATS_REPORTER_VR_NAME = "usage-stats-reporter";
    private static final int PORT = 13100;


    public static void main(String[] args) throws Exception
    {
        URL formUrl = ClassLoader.getSystemResource("flatgui/samples/forms/colorchooser.clj");
        String sourceCode = new Scanner(new File(formUrl.toURI())).useDelimiter("\\Z").next();

        //IFGTemplate appTemplate = new FGTemplate(sourceCode, CONTAINER_NS, CONTAINER_VAR_NAME);
        IFGTemplate appTemplate = new FGLegacyGlueTemplate(sourceCode, CONTAINER_NS, CONTAINER_VAR_NAME);

        FGAppServer server = new FGAppServer(appTemplate, PORT);

//        Var statsReporter = RT.var(CONTAINER_NS, STATS_REPORTER_VR_NAME);
//        server.setSessionCloseConsumer(FGAppServer.DEFAULT_MAPPING, (BiConsumer<Object, IFGContainer>) statsReporter.get());

        server.start();
        server.join();
    }
}

