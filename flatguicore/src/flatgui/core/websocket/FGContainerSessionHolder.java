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

import java.net.InetAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Denis Lebedev
 */
class FGContainerSessionHolder
{
    private static final String SPECIAL_CHARS = ".:/=";
    private static final long IDLE_SESSION_TIMEOUT = 15 * 60 * 1000;

    private Object id_;

    private Map<Object, FGContainerSession> sessionMap_;

    FGContainerSessionHolder(Object id)
    {
        id_ = id;
        sessionMap_ = new ConcurrentHashMap<>();

        Timer repaintTimer = new Timer("FlatGUI web container idle session cleaner", true);
        repaintTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
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
        }, IDLE_SESSION_TIMEOUT, IDLE_SESSION_TIMEOUT);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "[id="+id_+"]";
    }

    FGContainerSession getSession(String applicationName, InetAddress remoteAddress)
    {
        FGContainerSession s = sessionMap_.computeIfAbsent(getSessionId(applicationName, remoteAddress),
                k -> new FGContainerSession(applicationName, k));
        FGAppServer.getFGLogger().debug(toString() + " state:");
        FGAppServer.getFGLogger().debug(sessionMap_.toString());
        FGAppServer.getFGLogger().debug(toString() +
                " returning for app=" + applicationName + " remoteAddress=" + remoteAddress + " session: " + s);
        return s;
    }

    //private static long counter_ = 0;

    private static Object getSessionId(String applicationName, InetAddress remoteAddress)
    {
        String name = applicationName + "_" + remoteAddress.getHostAddress().toString();// + String.valueOf(counter_);
        //counter_++;
        for (int i=0; i<SPECIAL_CHARS.length(); i++)
        {
            name = name.replace(SPECIAL_CHARS.charAt(i), '_');
        }
        return name;
    }
}
