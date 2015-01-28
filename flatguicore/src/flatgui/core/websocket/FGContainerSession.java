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

import java.util.concurrent.atomic.LongAccumulator;

/**
 * @author Denis Lebedev
 */
public class FGContainerSession
{
    private static final long IDLE_MARKER = -1;

    private final String containerName_;
    private final FGWebContainer container_;
    private final FGInputEventDecoder parser_;
    private final FGPaintVectorBinaryCoder binaryCoder_;
    private final LongAccumulator lastAccessTime_;

    public FGContainerSession(String applicationName, Object sessionId)
    {
        containerName_ = sessionId.toString();

        IFGModule fgModule = new FGModule(containerName_);
        container_ = new FGWebContainer(fgModule);
        FGAppServer.instantiateContainer(applicationName, containerName_);
        FGContainerBase.registerContainer(containerName_, container_.getContainer());
        FGContainerBase.initializeContainer(containerName_);
        parser_ = new FGInputEventDecoder();
        binaryCoder_ = new FGPaintVectorBinaryCoder();
        lastAccessTime_ = new LongAccumulator((r,t) -> t, 0);
    }

    public FGWebContainer getContainer()
    {
        return container_;
    }

    public FGInputEventDecoder getParser()
    {
        return parser_;
    }

    public FGPaintVectorBinaryCoder getBinaryCoder()
    {
        return binaryCoder_;
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

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "[id=" + containerName_ + "]";
    }

    private boolean isMarkedIdle()
    {
        return lastAccessTime_.get() == IDLE_MARKER;
    }
}
