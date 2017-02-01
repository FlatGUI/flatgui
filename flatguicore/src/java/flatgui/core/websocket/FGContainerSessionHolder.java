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

import clojure.lang.Var;
import flatgui.core.*;
import flatgui.core.engine.remote.FGLegacyCoreGlue;
import flatgui.core.engine.remote.FGLegacyGlueTemplate;
import flatgui.core.engine.ui.FGAppContainer;
import flatgui.core.engine.ui.FGRemoteAppContainer;
import flatgui.core.engine.ui.FGRemoteClojureResultCollector;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Denis Lebedev
 */
class FGContainerSessionHolder
{
    private static final String SPECIAL_CHARS = ".:/=";
    private static final long IDLE_SESSION_TIMEOUT = 15 * 60 * 1000;

    private final IFGContainerHost<FGContainerSession> sessionHost_;
    private final Map<Object, FGContainerSession> sessionMap_;

    FGContainerSessionHolder(IFGContainerHost<FGContainerSession> sessionHost)
    {
        sessionHost_ = sessionHost;
        sessionMap_ = new ConcurrentHashMap<>();

        Timer repaintTimer = new Timer("FlatGUI web container idle session cleaner", true);
        repaintTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                synchronized (FGContainerSessionHolder.this)
                {
                    int sessionCount = sessionMap_.size();
                    sessionMap_.entrySet().removeIf(e -> e.getValue().isIdle(IDLE_SESSION_TIMEOUT));
                    int newSessionCount = sessionMap_.size();
                    if (newSessionCount != sessionCount)
                    {
                        FGAppServer.getFGLogger().info(toString() +
                            " cleaned sessions. Before: " + sessionCount + "; after: " + newSessionCount + ".");
                    }
                }
            }
        }, IDLE_SESSION_TIMEOUT, IDLE_SESSION_TIMEOUT);
    }

    FGContainerSession getSession(IFGTemplate template, InetAddress remoteAddress,
                                  List<byte[]> initialFontMetricsTransmissions,
                                  Set<String> fontCollector)
    {
        Object sessionId = getSessionId(template, remoteAddress);

        // TODO always create new since font metrics may change?

        FGContainerSession s = sessionMap_.computeIfAbsent(
                sessionId,
                k -> {
                    if (template instanceof FGLegacyGlueTemplate)
                    {
                        Var containerVar = clojure.lang.RT.var(template.getContainerNamespace(), template.getContainerVarName());
                        Map<Object, Object> container = (Map<Object, Object>) containerVar.get();

                        FGLegacyCoreGlue.GlueModule glueModule = new FGLegacyCoreGlue.GlueModule(sessionId.toString());

                        // TODO it returns identity coerced to int so it does not matter that this is different instance
                        FGWebContainerWrapper.KeyCache keyCache = new FGWebContainerWrapper.KeyCache();
                        Set<String> fontsWithMetricsAlreadyReceived = new HashSet<>();

                        FGRemoteClojureResultCollector resultCollector =
                                new FGRemoteClojureResultCollector(FGAppContainer.DFLT_UNIT_SIZE_PX,
                                        keyCache, glueModule, fontsWithMetricsAlreadyReceived);

                        FGRemoteAppContainer fgContainer = new FGRemoteAppContainer(sessionId.toString(), container, resultCollector);

                        FGLegacyCoreGlue glueContainer = new FGLegacyCoreGlue(fgContainer, glueModule);
                        glueContainer.initialize();
                        FGWebInteropUtil interop = fgContainer.getInteropUtil();
                        initialFontMetricsTransmissions.forEach(t -> fontCollector.add(interop.setMetricsTransmission(t)));
                        return sessionHost_.hostContainer(glueContainer, fontsWithMetricsAlreadyReceived);
                    }
                    else
                    {
                        FGWebInteropUtil interop = new FGWebInteropUtil(IFGContainer.UNIT_SIZE_PX);
                        initialFontMetricsTransmissions.forEach(t -> fontCollector.add(interop.setMetricsTransmission(t)));
                        FGContainer container = new FGContainer(template, sessionId.toString(), interop);
                        Set<String> fontsWithMetricsAlreadyReceived = new HashSet<>();
                        return sessionHost_.hostContainer(container, fontsWithMetricsAlreadyReceived);
                    }
                });

        FGAppServer.getFGLogger().debug(toString() + " state:");
        FGAppServer.getFGLogger().debug(sessionMap_.toString());
        FGAppServer.getFGLogger().debug(toString() +
            " returning for remoteAddress=" + remoteAddress + " session: " + s);

        return s;
    }

    synchronized long getActiveOrIdleSessionCount()
    {
        return sessionMap_.size();
    }

    synchronized long getActiveSessionCount()
    {
        return sessionMap_.values().stream().filter(FGContainerSession::isActive).count();
    }

    synchronized void forEachActiveSession(Consumer<FGContainerSession> sessionConsumer)
    {
        sessionMap_.values().forEach(s -> {
            synchronized (s.getContainerLock())
            {
                if (s.isActive())
                {
                    sessionConsumer.accept(s);
                }
            }
        });
    }

    synchronized void forEachActiveSession(BiConsumer<Object, FGContainerSession> sessionConsumer)
    {
        sessionMap_.entrySet().forEach(e -> {
            synchronized (e.getValue().getContainerLock())
            {
                if (e.getValue().isActive())
                {
                    sessionConsumer.accept(e.getKey(), e.getValue());
                }
            }
        });
    }

    synchronized <R> Stream<R> mapSessions(Function<FGContainerSession, R> sessionProcessor)
    {
        return sessionMap_.values().stream().map(sessionProcessor);
    }

    // TODO turn off counter - string pool is broken
    private static long counter_ = 0;
    private static Object getSessionId(IFGTemplate template, InetAddress remoteAddress)
    {
        String name = template.getContainerNamespace() + "_" +
                template.getContainerVarName() + "_" +
                remoteAddress.getHostAddress().toString() + String.valueOf(counter_);
        counter_++;
        for (int i=0; i<SPECIAL_CHARS.length(); i++)
        {
            name = name.replace(SPECIAL_CHARS.charAt(i), '_');
        }
        return name;
    }
}
