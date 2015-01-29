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

import flatgui.core.FGWebContainerWrapper;
import flatgui.core2.IFGTemplate;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.awt.event.InputEvent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * @author Denis Lebedev
 */
public class FGContainerWebSocket implements WebSocketListener
{
    private final FGContainerSessionHolder sessionHolder_;
    // TODO have some template provider instead
    private final IFGTemplate template_;

    private volatile Session session_;
    private volatile FGWebContainerWrapper container_;
    private volatile FGInputEventDecoder parser_;
    private volatile FGContainerSession fgSession_;

    public FGContainerWebSocket(IFGTemplate template, FGContainerSessionHolder sessionHolder)
    {
        template_ = template;
        sessionHolder_ = sessionHolder;

        FGAppServer.getFGLogger().info("WS Listener created " + System.identityHashCode(this));
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        FGAppServer.getFGLogger().info("WS Connect " + System.identityHashCode(this) +
                " session: " + fgSession_ +
                " remote: " + session_.getRemoteAddress() +
                " reason = " + reason);

        container_.unInitialize();
        fgSession_.markIdle();
        session_ = null;
    }

    @Override
    public void onWebSocketConnect(Session session)
    {
        session_ = session;
        StringBuilder statusMessage = new StringBuilder("Creating session...");
        setTextToRemote(statusMessage.toString());

        fgSession_ = sessionHolder_.getSession(template_, session_.getRemoteAddress().getAddress());

        statusMessage.append("|created session");
        setTextToRemote(statusMessage.toString());

        FGAppServer.getFGLogger().info("WS Connect " + System.identityHashCode(this) +
                " session: " + fgSession_ +
                " remote: " + session.getRemoteAddress());

        container_ = fgSession_.getContainer();

        statusMessage.append("|created app");
        setTextToRemote(statusMessage.toString());

        container_.initialize();

        statusMessage.append("|initialized app");
        setTextToRemote(statusMessage.toString());

        parser_ = fgSession_.getParser();

        statusMessage.append("|retrieving initial state...");
        setTextToRemote(statusMessage.toString());

        container_.resetCache();

        collectAndSendResponse();
    }

    @Override
    public void onWebSocketError(Throwable cause)
    {
        FGAppServer.getFGLogger().error(fgSession_ + " WS error: " + cause.getMessage());
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len)
    {
        //logger_.debug("Received message #" + debugMessageCount_ + ": " + message);
        fgSession_.markAccesed();
        InputEvent e = parser_.getInputEvent(new FGInputEventDecoder.BinaryInput(payload, offset, len));
        processInputEvent(e);
    }

    @Override
    public void onWebSocketText(String message)
    {
        //logger_.debug("Received message #" + debugMessageCount_ + ": " + message);
        fgSession_.markAccesed();
        InputEvent e = parser_.getInputEvent(message);
        processInputEvent(e);
    }

    private void processInputEvent(InputEvent e)
    {
        if (e == null)
        {
            //logger_.debug("Processed message #" + debugMessageCount_ + ": not an input event.");
            //debugMessageCount_++;
            return;
        }

        //
        // Feed input event received from the remote endpoint to the engine
        //

        container_.feedEvent(e);

        // TODO Do not send response if new event is coming?
        collectAndSendResponse();

        //debugMessageCount_++;
    }

    private void collectAndSendResponse()
    {
        Collection<ByteBuffer> response = container_.getResponseForClient();

        if (response.size() > 0)
        {
            response.forEach(this::sendBytesToRemote);
            sendBytesToRemote(ByteBuffer.wrap(new byte[]{FGWebContainerWrapper.REPAINT_CACHED_COMMAND_CODE}));

            //logger_.debug("Finished sending " + response.size() + " responses and repaint cmd for #" + debugMessageCount_);
        }
        else
        {
            //logger_.debug("Processed message #" + debugMessageCount_ + ": empty result.");
        }
    }

    private void sendBytesToRemote(ByteBuffer bytes)
    {
        try
        {
            session_.getRemote().sendBytes(bytes);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void setTextToRemote(String text)
    {
        try
        {
            session_.getRemote().sendString(text);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
