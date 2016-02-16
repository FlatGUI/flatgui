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

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;

import flatgui.core.FGEvolveInputData;
import flatgui.core.FGLogger;
import flatgui.core.IFGContainer;
import flatgui.core.IFGTemplate;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Denis Lebedev
 */
public class FGAppServer
{
    public static final String DEFAULT_MAPPING = "/*";
    private static final String API_MAPPING = "/api";

    private static final String API_PARAM_UID = "uid";
    private static final String API_PARAM_SERVICE_NAME = "service";
    private static final String API_PARAM_PATH = "path";

    private static FGLogger logger_ = new FGLogger();

    private final Server server_;
    private final ServletHandler handler_;

    private final Map<String, FGWebSocketServlet> mappingToAppTemplateMap_;

    private final Map<String, TextHtmlServlet> mappingToTextHtmlServletMap_;

    private final Map<String, IFGCustomServlet> mappingToCustomServletMap_;

    public FGAppServer(IFGTemplate template, int port) throws Exception
    {
        this(template, port, DEFAULT_MAPPING, null);
    }

    public FGAppServer(IFGTemplate template, int port, String mapping, Consumer<IFGContainer> containerConsumer) throws Exception
    {
        server_ = new Server(port);

        handler_ = new ServletHandler();

        // Enable CORS for API
        FilterHolder holder = new FilterHolder(CrossOriginFilter.class);
        holder.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        holder.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        holder.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD");
        holder.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");
        holder.setName("cross-origin");
        FilterMapping fm = new FilterMapping();
        fm.setFilterName("cross-origin");
        fm.setPathSpec("*");
        handler_.addFilter(holder, fm);

        server_.setHandler(handler_);

        mappingToAppTemplateMap_ = new HashMap<>();
        mappingToTextHtmlServletMap_ = new HashMap<>();
        mappingToCustomServletMap_ = new HashMap<>();

        addApplication(mapping, template, containerConsumer);

        Servlet apiServlet = new ApiServlet();
        ServletHolder h = new ServletHolder(apiServlet);
        handler_.addServletWithMapping(h, API_MAPPING);
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

    public synchronized void setTextHtmlServerByMapping(String mapping, Consumer<OutputStream> writer)
    {
        mapping = ensureMapping(mapping);

        TextHtmlServlet servlet = mappingToTextHtmlServletMap_.get(mapping);
        if (servlet == null)
        {
            servlet = new TextHtmlServlet();
            ServletHolder h = new ServletHolder(servlet);
            handler_.addServletWithMapping(h, mapping);
            mappingToTextHtmlServletMap_.put(mapping, servlet);
        }
        servlet.setWriter(writer);
    }

    public synchronized void setCustomServlet(String mapping, IFGCustomServlet servlet)
    {
        mapping = ensureMapping(mapping);

        mappingToCustomServletMap_.put(mapping, servlet);
        ServletHolder h = new ServletHolder(servlet);
        handler_.addServletWithMapping(h, mapping);
    }

    public synchronized void setSessionCloseConsumer(String mapping, BiConsumer<Object, IFGContainer> sessionCloseConsumer)
    {
        mapping = ensureMapping(mapping);
        FGWebSocketServlet servlet = mappingToAppTemplateMap_.get(mapping);
        if (servlet != null)
        {
            servlet.setSessionCloseConsumer(sessionCloseConsumer);
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

    public Map<String, FGServerAppStats> getAppNameToStatsMap()
    {
        Map<String, FGServerAppStats> statsMap = new TreeMap<>();
        for (String appName : mappingToAppTemplateMap_.keySet())
        {
            FGWebSocketServlet servlet = mappingToAppTemplateMap_.get(appName);

            Map<Object, Double> sessionToAvgProcessingTime = new HashMap<>();

            servlet.getSessionHolder().forEachActiveSession(
                    (id, s) -> sessionToAvgProcessingTime.put(
                            id,
                            Double.valueOf(s.getAccosiatedWebSocket().getAvgProcessingTime())));

            statsMap.put(appName, new FGServerAppStats(
                    servlet.getSessionHolder().getActiveOrIdleSessionCount(),
                    servlet.getSessionHolder().getActiveSessionCount(),
                    sessionToAvgProcessingTime));
        }
        return statsMap;
    }

    // Private

    static FGLogger getFGLogger()
    {
        return logger_;
    }

    private static String ensureMapping(String mapping)
    {
        if (API_MAPPING.equals(mapping))
        {
            throw new IllegalArgumentException(API_MAPPING + " mapping is reserved for internal use.");
        }
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

    public static class FGServerAppStats
    {
        private long totalSessions_;
        private long activeSessions_;
        private Map<Object, Double> sessionToAvgProcessingTime_;

        public FGServerAppStats(long totalSessions, long activeSessions, Map<Object, Double> sessionToAvgProcessingTime)
        {
            totalSessions_ = totalSessions;
            activeSessions_ = activeSessions;
            sessionToAvgProcessingTime_ = sessionToAvgProcessingTime;
        }

        public long getTotalSessions()
        {
            return totalSessions_;
        }

        public long getActiveSessions()
        {
            return activeSessions_;
        }

        public Map<Object, Double> getSessionToAvgProcessingTime()
        {
            return sessionToAvgProcessingTime_;
        }

        @Override
        public String toString()
        {
            return "total: " + totalSessions_ +
                    " active: " + activeSessions_;
        }
    }


    private static class FGWebSocketServlet extends WebSocketServlet
    {
        private IFGTemplate template_;
        private final FGContainerSessionHolder sessionHolder_;
        private Consumer<IFGContainer> containerConsumer_;
        private BiConsumer<Object, IFGContainer> sessionCloseConsumer_;

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

        final void setSessionCloseConsumer(BiConsumer<Object, IFGContainer> sessionCloseConsumer)
        {
            sessionCloseConsumer_ = sessionCloseConsumer;
        }

        void feedEventToAllInstancesAndSendUpdates(List<Keyword> targetCellIdPath, Object inputEvent)
        {
            FGAppServer.getFGLogger().debug("Started feeding event to all(" + sessionHolder_.getActiveSessionCount()
                +  " at the moment) active sessions. Event: " + inputEvent);
            sessionHolder_.forEachActiveSession(s -> {
                FGAppServer.getFGLogger().debug(" session " + s.toString());
                s.getAccosiatedWebSocket().collectAndSendResponse(
                    s.getContainer().feedTargetedEvent(targetCellIdPath, new FGEvolveInputData(inputEvent, false)), false);
            });
            FGAppServer.getFGLogger().debug("Done feeding event.");
        }

        FGContainerSessionHolder getSessionHolder()
        {
            return sessionHolder_;
        }

        private Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
        {
            return new FGContainerWebSocket(template_, sessionHolder_, containerConsumer_, sessionCloseConsumer_);
        }
    }

    private static class TextHtmlServlet extends HttpServlet
    {
        private Consumer<OutputStream> writer_;

        void setWriter(Consumer<OutputStream> writer)
        {
            writer_ = writer;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            resp.setContentType("text/html");
            OutputStream o=resp.getOutputStream();
            writer_.accept(o);
            o.flush();
            o.close();
        }
    }

    private class ApiServlet extends HttpServlet
    {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            System.out.println("-DLTEMP- ApiServlet.doPost================================ ");
            System.out.println(req.getParameterMap());
            System.out.println("Apps: " + mappingToAppTemplateMap_.keySet());

            Map<String, String[]> paramMap = new HashMap<>(req.getParameterMap());

            String[] uidArr = paramMap.get(API_PARAM_UID);
            if (uidArr == null || uidArr.length != 1)
            {
                throw new IllegalArgumentException("Param map must contain '" + API_PARAM_UID + "' params with single value");
            }
            String uid = uidArr[0];

            String[] serviceArr = paramMap.get(API_PARAM_SERVICE_NAME);
            if (serviceArr == null || serviceArr.length != 1)
            {
                throw new IllegalArgumentException("Param map must contain '" + API_PARAM_SERVICE_NAME + "' params with single value");
            }

            FGWebSocketServlet fgWebSocketServlet = mappingToAppTemplateMap_.get("/" + uid + "/" + serviceArr[0]);
            if (fgWebSocketServlet != null)
            {
                String[] pathParam = paramMap.get(API_PARAM_PATH);
                if (pathParam == null || pathParam.length == 0)
                {
                    throw new IllegalArgumentException("'" + API_PARAM_PATH + "' " +
                        "param with at least one value must be defined in case of passing event to a FlatGUI container");
                }

                paramMap.remove(API_PARAM_UID);
                paramMap.remove(API_PARAM_SERVICE_NAME);
                paramMap.remove(API_PARAM_PATH);

                fgWebSocketServlet.feedEventToAllInstancesAndSendUpdates(toTargetIdPath(pathParam), toClojureMap(paramMap));
            }
            else
            {
                // TODO uid here?

                IFGCustomServlet customServlet = mappingToCustomServletMap_.get("/" + serviceArr[0]);
                if (customServlet != null)
                {
                    paramMap.remove(API_PARAM_UID);
                    paramMap.remove(API_PARAM_SERVICE_NAME);

                    customServlet.acceptInputEvent(paramMap);
                }
                else
                {
                    throw new IllegalArgumentException("Service '" + serviceArr[0] + "' cannot be identified");
                }
            }
        }

        private IPersistentMap toClojureMap(Map<String, String[]> params)
        {
            Map<String, String> m = new HashMap<>();
            for (String k : params.keySet())
            {
                String[] vArr = params.get(k);
                if (vArr.length != 1)
                {
                    throw new IllegalArgumentException("Parameters must have single values in case of passing event to a FlatGUI container");
                }
                m.put(k, vArr[0]);
            }
            return PersistentHashMap.create(m);
        }

        private List<Keyword> toTargetIdPath(String[] param)
        {
            List<Keyword> result = new ArrayList<>(param.length);
            for (String p : param)
            {
                result.add(Keyword.intern(p));
            }
            return result;
        }

    }
}
