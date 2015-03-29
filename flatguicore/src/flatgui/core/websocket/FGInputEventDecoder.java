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

import flatgui.core.FGClipboardEvent;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Denis Lebedev
 */
public class FGInputEventDecoder
{
    public static final Component dummySourceComponent_ = new Container();

    private Collection<IParser<BinaryInput, ?>> binaryParsers_;

    public FGInputEventDecoder()
    {
        binaryParsers_ = new ArrayList<>();
        addBinaryParser(new MouseBinaryParser());
        addBinaryParser(new ClipboardBinaryParser());
    }

    public final void addBinaryParser(IParser<BinaryInput, ?> parser)
    {
        binaryParsers_.add(parser);
    }

    public <E> E getInputEvent(BinaryInput binaryData)
    {
        return getInputEvent(binaryData, binaryParsers_);
    }

    private static <S, E> E getInputEvent(S input, Collection<IParser<S, ?>> parsers)
    {
        try
        {
            for (IParser p : parsers)
            {
                Object e = p.getInputEvent(input);
                if (e != null)
                {
                    return (E)e;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    // Inner classes

    public static class BinaryInput
    {
        private byte[] payload_;
        private int offset_;
        private int len_;

        public BinaryInput(byte[] payload, int offset, int len)
        {
            payload_ = payload;
            offset_ = offset;
            len_ = len;
        }

        public byte[] getPayload()
        {
            return payload_;
        }

        public int getOffset()
        {
            return offset_;
        }

        public int getLen()
        {
            return len_;
        }
    }

    public interface IParser<S, E>
    {
        public E getInputEvent(S jsonObj) throws Exception;
    }


//    public static class KeyJSONParser extends AbstractJSONParser<KeyEvent>
//    {
//        public static final String C = "c";
//        public static final String S = "s";
//
//        @Override
//        protected KeyEvent parseImpl(JSONObject jsonObj, int id) throws JSONException
//        {
//            if (id >= KeyEvent.KEY_FIRST && id <= KeyEvent.KEY_LAST)
//            {
//                int code = jsonObj.getInt(C);
//                char c = (char)jsonObj.getInt(S);
//
//                KeyEvent e = new KeyEvent(dummySourceComponent_, id, 0, 0,
//                        id == KeyEvent.KEY_TYPED ? KeyEvent.VK_UNDEFINED : code,
//                        c);
//                return e;
//            }
//            else
//            {
//                return null;
//            }
//        }
//    }

    public static abstract class AbstractBinaryParser<E> implements IParser<BinaryInput, E>
    {
        public E getInputEvent(BinaryInput binaryData) throws Exception
        {
            byte[] array = binaryData.getPayload();
            int ofs = binaryData.getOffset();
            int id = array[ofs]+400;
            return parseImpl(binaryData, id);
        }

        protected abstract E parseImpl(BinaryInput binaryData, int id) throws Exception;
    }

    public static class MouseBinaryParser extends AbstractBinaryParser<MouseEvent>
    {
        @Override
        protected MouseEvent parseImpl(BinaryInput binaryData, int id) throws Exception
        {
            if (id >= MouseEvent.MOUSE_FIRST && id <= MouseEvent.MOUSE_LAST)
            {
                byte[] array = binaryData.getPayload();
                int ofs = binaryData.getOffset();

                int x = array[ofs + 1] & 0x7F;
                if ((array[ofs + 1] & 0x80) == 0x80)
                {
                    x += 0x80;
                }
                x += ((array[ofs + 3] & 0x70) << 4);
                if ((array[ofs + 3] & 0x80) == 0x80)
                {
                    x += 0x80 << 4;
                }

                int y = array[ofs + 2] & 0x7F;
                if ((array[ofs + 2] & 0x80) == 0x80)
                {
                    y += 0x80;
                }
                y += ((array[ofs + 3] & 0x0F) << 8);

                return getMouseEvent(id, x, y);
            }
            else
            {
                return null;
            }
        }
    }

    public static class ClipboardBinaryParser extends AbstractBinaryParser<FGClipboardEvent>
    {

        @Override
        protected FGClipboardEvent parseImpl(BinaryInput binaryData, int id) throws Exception
        {
            if (id >= FGClipboardEvent.CLIPBOARD_FIRST && id <= FGClipboardEvent.CLIPBOARD_LAST)
            {
                if (id == FGClipboardEvent.CLIPBOARD_PASTE)
                {
                    byte[] array = binaryData.getPayload();
                    int ofs = binaryData.getOffset();

                    int lenLo = array[ofs + 1] & 0x7F;
                    if ((array[ofs + 1] & 0x80) == 0x80)
                    {
                        lenLo += 0x80;
                    }
                    int lenHi = array[ofs + 2] & 0x7F;
                    if ((array[ofs + 2] & 0x80) == 0x80)
                    {
                        lenHi += 0x80;
                    }
                    int len = lenHi * 256 + lenLo;

                    char[] charArray = new char[len];
                    for (int i = 0; i < len; i++)
                    {
                        charArray[i] = (char) array[ofs + 3 + i];
                    }
                    Object data = String.valueOf(charArray);

                    System.out.println("-DLTEMP- ClipboardBinaryParser.parseImpl received data = " + data  +
                        " lenLo = " + lenLo + " lenHi = " + lenHi + " len = " + len);

                    return FGClipboardEvent.createPasteEvent(data);
                }
                else
                {
                    return null;
                }
            }
            else
            {
                return null;
            }
        }
    }

    private static MouseEvent getMouseEvent(int id, int x, int y)
    {
        boolean buttonEvent = id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_RELEASED ||
                id == MouseEvent.MOUSE_CLICKED || id == MouseEvent.MOUSE_DRAGGED;
        int clickCount = buttonEvent && (id != MouseEvent.MOUSE_DRAGGED) ? 1 : 0;
        int button = buttonEvent ? MouseEvent.BUTTON1 : 0;
        MouseEvent e = new MouseEvent(dummySourceComponent_, id, 0,
                buttonEvent && id == MouseEvent.MOUSE_DRAGGED ? InputEvent.getMaskForButton(button) : 0,
                x, y, x, y,
                clickCount, false, button);
        return e;
    }

}
