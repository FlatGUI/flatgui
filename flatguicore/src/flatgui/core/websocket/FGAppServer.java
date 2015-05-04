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
import flatgui.core.IFGTemplate;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Denis Lebedev
 */
public class FGAppServer
{
    private static final String DEFAULT_MAPPING = "/*";

    private static FGLogger logger_ = new FGLogger();

    private final Server server_;
    private final ServletHandler handler_;

    private final Map<String, FGWebSocketServlet> mappingToAppTemplateMap_;

    public FGAppServer(IFGTemplate template, int port) throws Exception
    {
        this(template, port, DEFAULT_MAPPING, null);
    }

    public FGAppServer(IFGTemplate template, int port, String mapping, Consumer<IFGContainer> containerConsumer) throws Exception
    {
        server_ = new Server(port);

        handler_ = new ServletHandler();
        server_.setHandler(handler_);

        mappingToAppTemplateMap_ = new HashMap<>();

        addApplication(mapping, template, containerConsumer);
    }

    public synchronized void addApplication(String mapping, IFGTemplate template)
    {
        addApplication(mapping, template, null);
    }

    public synchronized void addApplication(String mapping, IFGTemplate template, Consumer<IFGContainer> containerConsumer)
    {
        mapping = ensureMapping(mapping);
        FGWebSocketServlet servlet = new FGWebSocketServlet(template, containerConsumer);
        ServletHolder h = new ServletHolder(servlet);
        handler_.addServletWithMapping(h, mapping);
        mappingToAppTemplateMap_.put(mapping, servlet);
    }

    public synchronized void setTemplateByMapping(String mapping, IFGTemplate template, Consumer<IFGContainer> containerConsumer)
    {
        mapping = ensureMapping(mapping);
        FGWebSocketServlet servlet = mappingToAppTemplateMap_.get(mapping);
        if (servlet != null)
        {
            servlet.setTemplate(template);
            if (containerConsumer != null)
            {
                servlet.setContainerConsumer(containerConsumer);
            }
        }
        else
        {
            addApplication(mapping, template);
        }
    }

    public void start() throws Exception
    {
        server_.start();
    }

    public void join() throws Exception
    {
        server_.join();
    }

    // Private

    static FGLogger getFGLogger()
    {
        return logger_;
    }

    private static String ensureMapping(String mapping)
    {
        if (mapping != null && !mapping.startsWith("/"))
        {
            mapping = "/"+mapping;
        }
        if (mapping == null)
        {
            mapping = "/*";
        }
        return mapping;
    }

    // Inner classes

    private static class FGWebSocketServlet extends WebSocketServlet
    {
        private IFGTemplate template_;
        private final FGContainerSessionHolder sessionHolder_;
        private Consumer<IFGContainer> containerConsumer_;

        FGWebSocketServlet(IFGTemplate template, Consumer<IFGContainer> containerConsumer)
        {
            setTemplate(template);
            sessionHolder_ = new FGContainerSessionHolder(new FGSessionContainerHost());
            setContainerConsumer(containerConsumer);
        }

        @Override
        public void configure(WebSocketServletFactory factory)
        {
            factory.getPolicy().setIdleTimeout(24 * 60 * 60 * 1000);
            factory.setCreator(this::createWebSocket);
        }

        final void setTemplate(IFGTemplate template)
        {
            template_ = template;
        }

        final void setContainerConsumer(Consumer<IFGContainer> containerConsumer)
        {
            containerConsumer_ = containerConsumer;
        }

        private Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
        {
            return new FGContainerWebSocket(template_, sessionHolder_, containerConsumer_);
        }
    }
}
