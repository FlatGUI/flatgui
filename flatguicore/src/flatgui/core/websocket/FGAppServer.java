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

import flatgui.core.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class FGAppServer
{
    private static Server server_;
    private static Map<InetSocketAddress, FGContainerSessionHolder> serverContext_;
    private static Map<String, FGClientApp> applications_ = new HashMap<>();
    private static FGLogger logger_ = new FGLogger();

    // TODO temporary
    private static String defaultApplicationName_;
    private static boolean baseInit_ = false;

    private FGAppServer()
    {
    }

    public static void start(int port) throws Exception
    {
        serverContext_ = new HashMap<>();

        server_ = new Server(port);

        ServletHandler handler = new ServletHandler();
        server_.setHandler(handler);

        handler.addServletWithMapping(FGWebSocketServlet.class, "/*");

        server_.start();
        server_.join();
    }

    public static void deployApplication(
            String clientNs,
            String clientContainerVarName,
            String applicationName,
            String sourceCode)
    {
        if (!baseInit_)
        {
            FGContainerBase.setRefContainer(new FGContainer(new FGModule(null)));
            baseInit_ = true;
        }
        FGClientAppLoader.initializeFromContent(sourceCode);
        applications_.put(applicationName, new FGClientApp(clientNs, clientContainerVarName));
    }

    public static void deployApplication1(
            String clientNs,
            String clientContainerVarName,
            String applicationName)
    {
        if (!baseInit_)
        {
            FGContainerBase.setRefContainer(new FGContainer(new FGModule(null)));
            baseInit_ = true;
        }

        applications_.put(applicationName, new FGClientApp(clientNs, clientContainerVarName));
    }

    public static void instantiateContainer(String applicationName, String containerName)
    {
        FGClientApp app = applications_.get(applicationName);
        if (app != null)
        {
            FGClientAppLoader.registerContainer(app.getContainerNs(), app.getContainerVarName(), containerName);
        }
        else
        {
            throw new IllegalStateException("No application with name '" + applicationName + "' deployed.");
        }
    }

    //TODO temporary

    public static void setDefaultApplicationName(String defaultApplicationName)
    {
        defaultApplicationName_ = defaultApplicationName;
    }

    public static String getDefaultApplicationName()
    {
        return defaultApplicationName_;
    }

    // Private

    static synchronized FGContainerSessionHolder getSessionHolder(InetSocketAddress localAddr)
    {
        ensureInitialized();
        FGContainerSessionHolder holder = serverContext_.get(localAddr);
        if (holder == null)
        {
            holder = new FGContainerSessionHolder(localAddr);
            serverContext_.put(localAddr, holder);
        }
        return holder;
    }
    
    static synchronized FGContainerSession getSession(
            InetSocketAddress localAddr,
            String applicationName,
            InetSocketAddress remoteAddr)
    {
        FGContainerSessionHolder sessionHolder = getSessionHolder(localAddr);
        return sessionHolder.getSession(applicationName, remoteAddr.getAddress());
    }

    static FGLogger getFGLogger()
    {
        return logger_;
    }

    private static void ensureInitialized()
    {
        if (serverContext_ == null)
        {
            throw new IllegalStateException("Not initialized");
        }
    }
}
