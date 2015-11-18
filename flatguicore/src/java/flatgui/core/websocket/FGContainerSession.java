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

import java.awt.Font;
import flatgui.core.FGWebContainerWrapper;
import flatgui.core.IFGContainer;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * @author Denis Lebedev
 *
 * // TODO synchronization here?
 *
 */
public class FGContainerSession
{
    private static final long IDLE_MARKER = -1;

    private static final Var strToFont_ = clojure.lang.RT.var("flatgui.awt", "str->font");

    private final Object sessionId_;
    private final FGWebContainerWrapper containerWrapper_;
    private final FGInputEventDecoder parser_;
    private final LongAccumulator lastAccessTime_;
    private final FGWebInteropUtil interopUtil_;

    private volatile FGContainerWebSocket accosiatedWebSocket_;

    public FGContainerSession(IFGContainer container)
    {
        sessionId_ = container.getId();

        containerWrapper_ = new FGWebContainerWrapper(container);
        containerWrapper_.initialize();

        interopUtil_ = (FGWebInteropUtil) container.getInterop();
        containerWrapper_.addFontStrListener(e -> interopUtil_.setReferenceFont(
                e.getNewValue(), (Font) strToFont_.invoke(e.getNewValue())));

        parser_ = new FGInputEventDecoder();
        lastAccessTime_ = new LongAccumulator((r,t) -> t, 0);
    }

    public FGWebContainerWrapper getContainer()
    {
        return containerWrapper_;
    }

    public FGInputEventDecoder getParser()
    {
        return parser_;
    }

    public void markIdle()
    {
        FGAppServer.getFGLogger().info(toString() + " has been marked as idle.");
        lastAccessTime_.accumulate(IDLE_MARKER);
    }

    public void markAccesed()
    {
        if (isMarkedIdle())
        {
            FGAppServer.getFGLogger().info(toString() + " has been restored from idle state due to new activity.");
        }
        lastAccessTime_.accumulate(System.currentTimeMillis());
    }

    public boolean isIdle(long timeout)
    {
        boolean idle = isMarkedIdle() || lastAccessTime_.get() + timeout < System.currentTimeMillis();
        if (idle)
        {
            if (!isMarkedIdle())
            {
                FGAppServer.getFGLogger().info(toString() + " has been detected idle for more than " + timeout + " millis.");
            }
            markIdle();
        }
        return idle;
    }

    public boolean isActive()
    {
        return containerWrapper_.isActive();
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "[id=" + sessionId_ + "]";
    }

    Object getContainerLock()
    {
        return containerWrapper_;
    }

    void setAccosiatedWebSocket(FGContainerWebSocket accosiatedWebSocket)
    {
        accosiatedWebSocket_ = accosiatedWebSocket;
    }

    public FGContainerWebSocket getAccosiatedWebSocket()
    {
        return accosiatedWebSocket_;
    }

    private boolean isMarkedIdle()
    {
        return lastAccessTime_.get() == IDLE_MARKER;
    }
}
