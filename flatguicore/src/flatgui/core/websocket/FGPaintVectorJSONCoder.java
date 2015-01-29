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

import flatgui.core.IFGContainer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class FGPaintVectorJSONCoder
{
    // TODO get rid of this
    private static int UNIT_SIZE_PX = IFGContainer.UNIT_SIZE_PX;

    private Map<String, ICommandCoder> cmdNameToCoder_;

    public FGPaintVectorJSONCoder()
    {
        cmdNameToCoder_ = new HashMap<>();

        registerCoder("setColor", new SetColorCoder());
        registerCoder("drawRect", new DrawRectCoder());
        registerCoder("fillRect", new FillRectCoder());
        registerCoder("drawRoundRect", new DrawRoundRectCoder());
        registerCoder("drawOval", new DrawOvalCoder());
        registerCoder("fillOval", new FillOvalCoder());
        registerCoder("drawString", new DrawStringCoder());
        registerCoder("drawLine", new DrawLineCoder());
        registerCoder("transform", new TransformCoder());
        registerCoder("clipRect", new ClipRectCoder());
        registerCoder("setClip", new SetClipCoder());
        registerCoder("pushCurrentClip", new PushCurrentClipCoder());
        registerCoder("popCurrentClip", new PopCurrentClipCoder());
    }

    public JSONArray codeCommandVector(List<Object> commandVector)
    {
        JSONArray arr = new JSONArray();
        for (int i=0; i<commandVector.size(); i++)
        {
            List singleCommand = (List)commandVector.get(i);
            if (singleCommand.get(0) instanceof String) {
                String command = (String) singleCommand.get(0);
                ICommandCoder coder = cmdNameToCoder_.get(command);
                if (coder == null) {
                    throw new IllegalArgumentException("No coder for command: " + command);
                }
                try {
                    JSONObject cmdJson = coder.codeCommand(singleCommand);
                    arr.put(i, cmdJson);
                    //System.out.println("-DLTEMP- FGPaintVectorJSONCoder.codeCommandVector coded " + command + ": " + cmdJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                System.out.println("-DLTEMP- FGPaintVectorJSONCoder.codeCommandVector ERROR: " + singleCommand);
            }
        }
        return arr;
    }

    public void registerCoder(String commandName, ICommandCoder coder)
    {
        cmdNameToCoder_.put(commandName, coder);
    }

    // Coders

    public static interface ICommandCoder
    {
        public JSONObject codeCommand(List command);
    }

    public abstract static class AbstractCoder implements ICommandCoder
    {
        @Override
        public JSONObject codeCommand(List command)
        {
            JSONObject json = new JSONObject();
            try
            {
                fillJSON(json, command);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            return json;
        }

        protected abstract void fillJSON(JSONObject json, List command) throws JSONException;
    }

    public static class SetColorCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 1);
            json.put("r", ((Color)command.get(1)).getRed());
            json.put("g", ((Color)command.get(1)).getGreen());
            json.put("b", ((Color)command.get(1)).getBlue());
        }
    }

    private static int getCoord(List command, int index)
    {
        return (int) (((Number)command.get(index)).doubleValue() * UNIT_SIZE_PX);
    }

    public static class DrawRectCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 2);
            json.put("x", getCoord(command, 1));
            json.put("y", getCoord(command, 2));
            json.put("w", getCoord(command, 3));
            json.put("h", getCoord(command, 4));
        }
    }

    public static class FillRectCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 3);
            json.put("x", getCoord(command, 1));
            json.put("y", getCoord(command, 2));
            json.put("w", getCoord(command, 3));
            json.put("h", getCoord(command, 4));
        }
    }

    public static class DrawRoundRectCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 4);
            json.put("x", getCoord(command, 1));
            json.put("y", getCoord(command, 2));
            json.put("w", getCoord(command, 3));
            json.put("h", getCoord(command, 4));
            json.put("k", getCoord(command, 5));
            json.put("l", getCoord(command, 6));
        }
    }

    public static class DrawOvalCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 5);
            json.put("x", getCoord(command, 1));
            json.put("y", getCoord(command, 2));
            json.put("w", getCoord(command, 3));
            json.put("h", getCoord(command, 4));
        }
    }

    public static class FillOvalCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 6);
            json.put("x", getCoord(command, 1));
            json.put("y", getCoord(command, 2));
            json.put("w", getCoord(command, 3));
            json.put("h", getCoord(command, 4));
        }
    }

    public static class DrawStringCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 7);
            json.put("t", command.get(1));
            json.put("x", getCoord(command, 2));
            json.put("y", getCoord(command, 3));
        }
    }

    public static class DrawLineCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 8);
            json.put("k", getCoord(command, 1));
            json.put("l", getCoord(command, 2));
            json.put("m", getCoord(command, 3));
            json.put("n", getCoord(command, 4));
        }
    }

    public static class TransformCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 9);
            AffineTransform m = (AffineTransform) command.get(1);
            //json.put("k", m.getScaleX());
           // json.put("l", m.getShearX());
            //json.put("m", m.getShearY());
            //json.put("n", m.getScaleY());
            json.put("o", (int)Math.round(m.getTranslateX()));
            json.put("p", (int)Math.round(m.getTranslateY()));

            //System.out.println("-DLTEMP- TransformCoder.fillJSON " + json);
        }
    }

    public static class ClipRectCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 10);
            json.put("x", getCoord(command, 1));
            json.put("y", getCoord(command, 2));
            json.put("w", getCoord(command, 3));
            json.put("h", getCoord(command, 4));
        }
    }

    public static class SetClipCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 11);
            json.put("x", getCoord(command, 1));
            json.put("y", getCoord(command, 2));
            json.put("w", getCoord(command, 3));
            json.put("h", getCoord(command, 4));
        }
    }

    public static class PushCurrentClipCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 12);
        }
    }

    public static class PopCurrentClipCoder extends AbstractCoder
    {
        @Override
        protected void fillJSON(JSONObject json, List command) throws JSONException
        {
            json.put("c", 13);
        }
    }
}
