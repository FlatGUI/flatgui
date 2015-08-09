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

import flatgui.core.FGContainer;
import flatgui.core.IFGContainerHost;
import flatgui.core.IFGTemplate;

import java.net.InetAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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

    FGContainerSession getSession(IFGTemplate template, InetAddress remoteAddress)
    {
        Object sessionId = getSessionId(template, remoteAddress);

        FGContainerSession s = sessionMap_.computeIfAbsent(
                getSessionId(template, remoteAddress),
                k -> sessionHost_.hostContainer(new FGContainer(template, sessionId.toString())));

        FGAppServer.getFGLogger().debug(toString() + " state:");
        FGAppServer.getFGLogger().debug(sessionMap_.toString());
        FGAppServer.getFGLogger().debug(toString() +
            " returning for remoteAddress=" + remoteAddress + " session: " + s);

        return s;
    }

    synchronized void forEachSession(Consumer<FGContainerSession> sessionConsumer)
    {
        sessionMap_.values().forEach(sessionConsumer);
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
