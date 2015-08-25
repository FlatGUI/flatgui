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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Denis Lebedev
 */
public class FGPaintVectorBinaryCoder
{
    // TODO get rid of this
    private static int UNIT_SIZE_PX = IFGContainer.UNIT_SIZE_PX;

    private Map<String, ICommandCoder> cmdNameToCoder_;
    private Collection<ICommandCoder> uidAwareCoders_;

    public FGPaintVectorBinaryCoder(StringPoolIdSupplier stringPoolIdSupplier)
    {
        cmdNameToCoder_ = new HashMap<>();

        registerCoder("setColor", new SetColorCoder());
        registerCoder("drawRect", new DrawRectCoder());
        registerCoder("fillRect", new FillRectCoder());
        registerCoder("drawRoundRect", new DrawRoundRectCoder());
        registerCoder("drawOval", new DrawOvalCoder());
        registerCoder("fillOval", new FillOvalCoder());

        ICommandCoder drawStringCoder = new DrawStringCoder(stringPoolIdSupplier);
        registerCoder("drawString", drawStringCoder);

        registerCoder("drawLine", new DrawLineCoder());
        registerCoder("transform", new TransformCoder());
        registerCoder("clipRect", new ClipRectCoder());
        registerCoder("setClip", new SetClipCoder());
        registerCoder("pushCurrentClip", new PushCurrentClipCoder());
        registerCoder("popCurrentClip", new PopCurrentClipCoder());

        ICommandCoder drawImageCoder = new DrawImageStrPoolCoder(stringPoolIdSupplier);
        ICommandCoder fitImageCoder = new FitImageStrPoolCoder(stringPoolIdSupplier);
        ICommandCoder fillImageCoder = new FillImageStrPoolCoder(stringPoolIdSupplier);
        registerCoder("drawImage", new ExtendedCommandCoder(drawImageCoder));
        registerCoder("fitImage", new ExtendedCommandCoder(fitImageCoder));
        registerCoder("fillImage", new ExtendedCommandCoder(fillImageCoder));

        ICommandCoder setFontCoder = new SetFontStrPoolCoder(stringPoolIdSupplier);
        registerCoder("setFont", new ExtendedCommandCoder(setFontCoder));

        uidAwareCoders_ = Arrays.asList(drawStringCoder, drawImageCoder, fitImageCoder, fillImageCoder, setFontCoder);
    }

    public void setCodedComponentUid(Object componentId, int uid)
    {
        uidAwareCoders_.forEach(c -> c.setCodedComponentUid(componentId, uid));
    }

    public ByteBuffer codeCommandVector(List<Object> commandVector)
    {
        int totalBytes = 0;
        byte[] stream = new byte[131072];

        //List<String> log = new ArrayList<>();

        totalBytes += writeCoded(stream, totalBytes, commandVector);

//        try {
//            Files.write(Paths.get("D:\\encode.txt"), log, new OpenOption[]{StandardOpenOption.CREATE});
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try
        {
//            long t = System.currentTimeMillis();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            //DeflaterOutputStream go = new DeflaterOutputStream(bo);
            DeflaterOutputStream go = new GZIPOutputStream(bo);
            go.write(stream, 0, totalBytes);
            go.close();
            byte[] result = bo.toByteArray();
//            System.out.println("-DLTEMP- FGPaintVectorBinaryCoder.codeCommandVector GZIP in " +
//                    (System.currentTimeMillis() - t) + " millis. Src size = " + totalBytes + " compressed = " + result.length);

            return ByteBuffer.wrap(result);
        }
        catch (IOException e)
        {
            e.printStackTrace();

        }

        return ByteBuffer.wrap(stream, 0, totalBytes);
    }

    // TODO use ByteArrayOutputStream
    public int writeCoded(byte[] stream, int n, List<Object> commandVector)
    {
        int totalBytes = n;
        for (int i=0; i<commandVector.size(); i++)
        {
            List singleCommand = (List)commandVector.get(i);
            if (singleCommand.get(0) instanceof String) {
                String command = (String) singleCommand.get(0);
                ICommandCoder coder = cmdNameToCoder_.get(command);
                if (coder == null) {
                    throw new IllegalArgumentException("No coder for command: " + command);
                }

                int bytesWritten = coder.writeCommand(stream, totalBytes, singleCommand);
                totalBytes += bytesWritten;

                //log.add(cmdNameToLogger_.get(singleCommand.get(0)).apply(singleCommand, bytesWritten));
            }
            else
            {
                System.out.println("-DLTEMP- FGPaintVectorBinaryCoder.codeCommandVector ERROR: " + singleCommand);
            }
        }
        return totalBytes - n;
    }

    public void registerCoder(String commandName, ICommandCoder coder)
    {
        cmdNameToCoder_.put(commandName, coder);
    }

    // Coders

    public static interface ICommandCoder
    {
        int writeCommand(byte[] stream, int n, List command);

        default void setCodedComponentUid(Object componentId, int uid)
        {}
    }


    public static class SetColorCoder implements ICommandCoder
    {
        // Header
        // [xxx00|000]
        //
        // Bodys
        // -----
        // Sub          | Body                                    | Comment
        // [10000|000]  | [RRRR|RRRR][GGGG|GGGG][BBBB|BBBB]       | 8 bits for each R G B
        // [01000|000]  | [CCCC|CCCC]                             | 8 bits for equal R G B (gray colors)

        @Override
        public int writeCommand(byte[] stream, int n, List command)
        {
            int ir = ((Color)command.get(1)).getRed();
            int ig = ((Color)command.get(1)).getGreen();
            int ib = ((Color)command.get(1)).getBlue();

            byte r = (byte)ir;
            byte g = (byte)ig;
            byte b = (byte)ib;

            if (ir == ig && ig == ib)
            {
                stream[n] = (byte)0b01000000;
                stream[n+1] = r;
                return 2;
            }
            else
            {
                return writeRegularFormat(stream, n, r, g, b);
            }
        }

        private int writeRegularFormat(byte[] stream, int n, byte r, byte g, byte b)
        {
            stream[n] = (byte)0b10000000;
            stream[n+1] = r;
            stream[n+2] = g;
            stream[n+3] = b;
            return 4;
        }
    }

    public static int getCoord(List command, int index)
    {
        return (int) (((Number)command.get(index)).doubleValue() * UNIT_SIZE_PX);
    }

    @FunctionalInterface
    public interface IPointPredicate
    {
        public boolean test(int w, int h);
    }

    @FunctionalInterface
    public interface IRectPredicate
    {
        public boolean test(int x, int y, int w, int h);
    }

    public interface IPointSubCommandBodyCoder
    {
        public IPointPredicate getPointTester();

        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int w, int h);
    }

    public interface IRectSubCommandBodyCoder
    {
        public IRectPredicate getRectTester();

        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int x, int y, int w, int h);
    }

    public static class SubCoderPoint127x31 implements IPointSubCommandBodyCoder
    {
        @Override
        public IPointPredicate getPointTester() {
            return (w,h) -> w >= 0 && h>= 0 && w <= 127 && h <=31;
        }
        @Override
        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int w, int h) {
            // WWWH0000  | [WWWW|HHHH]

            byte headerw = (byte)((w & 0b01110000) << 1);
            byte headerh = (byte)(h & 0b00010000);
            stream[n] = (byte)(opcode | headerw | headerh);

            byte bodyw = (byte)((w & 0b00001111) << 4);
            byte bodyh = (byte)(h & 0b00001111);
            stream[n+1] = (byte)(bodyw | bodyh);

            return 2;
        }
    }

    public static class SubCoderPoint511x511 implements IPointSubCommandBodyCoder
    {
        @Override
        public IPointPredicate getPointTester() {
            return (w,h) -> w >= 0 && h>= 0 && w <= 511 && h <= 511;
        }
        @Override
        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int w, int h) {
            // WH001000  | [WWWW|WWWW][HHHH|HHHH]

            byte headerw = (byte)((w & 0b100000000) >> 1);
            byte headerh = (byte)((h & 0b100000000) >> 2);
            stream[n] = (byte)(headerw | headerh | 0b00001000 | opcode);
            stream[n+1] = (byte)w;
            stream[n+2] = (byte)h;

            return 3;
        }
    }

    public static class SubCoderPoint1023x255 implements IPointSubCommandBodyCoder
    {
        @Override
        public IPointPredicate getPointTester() {
            return (w,h) -> w >= 0 && h>= 0 && w <= 1023 && h <= 255;
        }
        @Override
        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int w, int h) {
            // WW011000  | [WWWW|WWWW][HHHH|HHHH]                                                                   | Up to 1023/255

            byte headerw = (byte)((w & 0b1100000000) >> 2);
            stream[n] = (byte)(headerw | 0b00011000 | opcode);
            stream[n+1] = (byte)w;
            stream[n+2] = (byte)h;

            return 3;
        }
    }

    public static class SubCoderPoint255x1023 implements IPointSubCommandBodyCoder
    {
        @Override
        public IPointPredicate getPointTester() {
            return (w,h) -> w >= 0 && h >= 0 && w <= 255 && h <= 1023;
        }
        @Override
        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int w, int h) {
            // HH101000  | [WWWW|WWWW][HHHH|HHHH]                                                                   | Up to 255/1023

            byte headerh = (byte)((h & 0b1100000000) >> 2);
            stream[n] = (byte)(headerh | 0b00101000 | opcode);
            stream[n+1] = (byte)w;
            stream[n+2] = (byte)h;

            return 3;
        }
    }

    public static class PointSubCoder
    {
        private List<IPointSubCommandBodyCoder> subCommandBodyCoders_;

        private PointSubCoder(boolean includeMinimalRect)
        {
            subCommandBodyCoders_ = new ArrayList<>();

            if (includeMinimalRect)
            {
                subCommandBodyCoders_.add(new SubCoderPoint127x31());
            }
            subCommandBodyCoders_.add(new SubCoderPoint511x511());
            subCommandBodyCoders_.add(new SubCoderPoint1023x255());
            subCommandBodyCoders_.add(new SubCoderPoint255x1023());
        }

        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int w, int h)
        {
            for (IPointSubCommandBodyCoder c : subCommandBodyCoders_)
            {
                if (c.getPointTester().test(w, h))
                {
                    return c.writeBodyWithSubCodeHeader(stream, n, opcode, w, h);
                }
            }
            return -1;
        }
    }

    public static class SubCoderRect63x63 implements IRectSubCommandBodyCoder
    {
        @Override
        public IRectPredicate getRectTester() {
            return (x,y,w,h) -> x >= 0 && y >= 0 && w >= 0 && h >= 0 && x <= 63 && y <= 63 && w <= 63 && h <= 63;
        }

        @Override
        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int x, int y, int w, int h) {
            // 00111000  | [YYYY|XXXX][HHHH|WWWW][HHWW|YYXX]          | Up to 63,63/63,63

            stream[n] = (byte)(0b00111000 | opcode);
            stream[n+1] = (byte)((x & 0b00001111) | ((y & 0b00001111) << 4));
            stream[n+2] = (byte)((w & 0b00001111) | ((h & 0b00001111) << 4));
            stream[n+3] = (byte)(((x & 0b00110000) >> 4) |
                                 ((y & 0b00110000) >> 2) |
                                  (w & 0b00110000) |
                                 ((h & 0b00110000) << 2));
            return 4;
        }
    }

    public static class SubCoderRect255x255 implements IRectSubCommandBodyCoder
    {
        @Override
        public IRectPredicate getRectTester() {
            return (x,y,w,h) -> x >= 0 && y >= 0 && w >= 0 && h >= 0 && x <= 255 && y <= 255 && w <= 255 && h <= 255;
        }

        @Override
        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int x, int y, int w, int h) {
            // 01111000  | [XXXX|XXXX][YYYY|YYYY][WWWW|WWWW][HHHH|HHHH]   | Up to 255,255/255,255

            stream[n] = (byte)(0b01111000 | opcode);
            stream[n+1] = (byte)x;
            stream[n+2] = (byte)y;
            stream[n+3] = (byte)w;
            stream[n+4] = (byte)h;
            return 5;
        }
    }

    public static class SubCoderRect1023x1023 implements IRectSubCommandBodyCoder
    {
        @Override
        public IRectPredicate getRectTester() {
            return (x,y,w,h) -> x >= 0 && y >= 0 && w >= 0 && h >= 0 && x <= 1023 && y <= 1023 && w <= 1023 && h <= 1023;
        }

        @Override
        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int x, int y, int w, int h) {
            // 10111  | [XXXX|XXXX][YYYY|YYYY][WWWW|WWWW][HHHH|HHHH][HHWW|YYXX] | Up to 1023,1023/1023,1023

            stream[n] = (byte)(0b10111000 | opcode);
            stream[n+1] = (byte)x;
            stream[n+2] = (byte)y;
            stream[n+3] = (byte)w;
            stream[n+4] = (byte)h;
            stream[n+5] = (byte)(((x & 0b1100000000) >> 8) |
                                 ((y & 0b1100000000) >> 6) |
                                 ((w & 0b1100000000) >> 4) |
                                 ((h & 0b1100000000) >> 2));
            return 6;
        }
    }

    public static class SubCoderRect65535x65535 implements IRectSubCommandBodyCoder
    {
        @Override
        public IRectPredicate getRectTester() {
            return (x,y,w,h) -> x >= -32768 && y >= -32768 && w >= -32768 && h >= -32768 &&
                    x <= 32767 && y <= 32767 && w <= 32767 && h <= 32767;
        }

        @Override
        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int x, int y, int w, int h) {
            // 11111  | [XXXX|XXXX][XXXX|XXXX][YYYY|YYYY][YYYY|YYYY][WWWW|WWWW][WWWW|WWWW][HHHH|HHHH][HHHH|HHHH] | Up to 32767,32767/32767,32767

            stream[n] = (byte)(0b11111000 | opcode);
            stream[n+1] = (byte)x;
            stream[n+2] = (byte)(x >> 8);
            stream[n+3] = (byte)y;
            stream[n+4] = (byte)(y >> 8);
            stream[n+5] = (byte)w;
            stream[n+6] = (byte)(w >> 8);
            stream[n+7] = (byte)h;
            stream[n+8] = (byte)(h >> 8);
            return 9;
        }
    }

    public static class RectSubCoder
    {
        private List<IRectSubCommandBodyCoder> subCommandBodyCoders_;

        private RectSubCoder()
        {
            subCommandBodyCoders_ = new ArrayList<>();

            subCommandBodyCoders_.add(new SubCoderRect63x63());
            subCommandBodyCoders_.add(new SubCoderRect255x255());
            subCommandBodyCoders_.add(new SubCoderRect1023x1023());
            subCommandBodyCoders_.add(new SubCoderRect65535x65535());
        }

        public int writeBodyWithSubCodeHeader(byte[] stream, int n, byte opcode, int x, int y, int w, int h)
        {
            for (IRectSubCommandBodyCoder c : subCommandBodyCoders_)
            {
                if (c.getRectTester().test(x, y, w, h))
                {
                    return c.writeBodyWithSubCodeHeader(stream, n, opcode, x, y, w, h);
                }
            }
            throw new IllegalStateException("Could not encode rect ["+x+";"+y+";"+w+";"+h+"], opcode = " + opcode);
        }
    }

    public static abstract class RectAreaCoder implements ICommandCoder
    {
        private PointSubCoder pointSubCoder_;
        private RectSubCoder rectSubCoder_;
        private BiFunction<List, Integer, Integer> coordProvider_;

        public RectAreaCoder()
        {
            this(true);
        }

        public RectAreaCoder(boolean includeMinimalRect)
        {
            this(includeMinimalRect, FGPaintVectorBinaryCoder::getCoord);
        }

        public RectAreaCoder(boolean includeMinimalRect, BiFunction<List, Integer, Integer> coordProvider)
        {
            pointSubCoder_ = new PointSubCoder(includeMinimalRect);
            rectSubCoder_ = new RectSubCoder();
            coordProvider_ = coordProvider;
        }

        @Override
        public int writeCommand(byte[] stream, int n, List command)
        {
            // Header
            // [SSSSS|OOO]
            //
            // Bodys
            // -----
            // Sub    | Body                                                                                     | Comment
            // WWWH0  | [WWWW|HHHH]                                                                              | Up to 127/31 vector which is good for table cells, ticks, arrows, etc.
            // WH001  | [WWWW|WWWW][HHHH|HHHH]                                                                   | Up to 511/511
            // WW011  | [WWWW|WWWW][HHHH|HHHH]                                                                   | Up to 1023/255
            // HH101  | [WWWW|WWWW][HHHH|HHHH]                                                                   | Up to 255/1023
            // 00111  | [YYYY|XXXX][HHHH|WWWW][HHWW|YYXX]                                                        | Up to 63,63/63,63
            // 01111  | [XXXX|XXXX][YYYY|YYYY][WWWW|WWWW][HHHH|HHHH]                                             | Up to 255,255/255,255
            // 10111  | [XXXX|XXXX][YYYY|YYYY][WWWW|WWWW][HHHH|HHHH][HHWW|YYXX]                                  | Up to 1023,1023/1023,1023
            // 11111  | [XXXX|XXXX][XXXX|XXXX][YYYY|YYYY][YYYY|YYYY][WWWW|WWWW][WWWW|WWWW][HHHH|HHHH][HHHH|HHHH] | Up to 65535,65535/65535,65535

            int x = coordProvider_.apply(command, 1);
            int y = coordProvider_.apply(command, 2);
            int w = coordProvider_.apply(command, 3);
            int h = coordProvider_.apply(command, 4);
            if (x == 0 && y == 0)
            {
                int c = pointSubCoder_.writeBodyWithSubCodeHeader(stream, n, getOpCode(), w, h);
                if (c >= 0)
                {
                    return c;
                }
                else
                {
                    return rectSubCoder_.writeBodyWithSubCodeHeader(stream, n, getOpCode(), x, y, w, h);
                }
            }
            else
            {
                return rectSubCoder_.writeBodyWithSubCodeHeader(stream, n, getOpCode(), x, y, w, h);
            }
        }

        protected abstract byte getOpCode();
    }


    public static class DrawRectCoder extends RectAreaCoder
    {
        @Override
        protected byte getOpCode()
        {
            return 0b001;
        }
    }

    public static class FillRectCoder extends RectAreaCoder
    {
        @Override
        protected byte getOpCode()
        {
            return 0b010;
        }
    }

    public static class DrawOvalCoder extends RectAreaCoder
    {
        @Override
        protected byte getOpCode()
        {
            return 0b011;
        }
    }

    public static class FillOvalCoder extends RectAreaCoder
    {
        @Override
        protected byte getOpCode()
        {
            return 0b100;
        }
    }

// This is a regular drawString coder that transmits actual string. It is not used by default
//
//    public static class DrawStringCoder implements ICommandCoder
//    {
//        private BiFunction<List, Integer, Integer> coordProvider_;
//
//        public DrawStringCoder()
//        {
//            this(FGPaintVectorBinaryCoder::getCoord);
//        }
//
//        public DrawStringCoder(BiFunction<List, Integer, Integer> coordProvider)
//        {
//            coordProvider_ = coordProvider;
//        }
//
//        // Header
//        // [SSSSS|OOO]
//        //
//        // Body
//        // -----
//        // Sub          | Body                                                  | Comment
//        // [N1010|000]  | [LLLL|LLLL][XXXX|XXXX][YYYY|YYYY][YYYY|XXXX][..S..]   | Up to 255 str len; up to 4095 X; up 4095 Y; N=1 means 2 bytes per char, 0 means 1 byte
//        // [N1110|000]  | [XXXX|LLLL][XXYY|YYYY][..S..]                         | Up to 15 str len; up to 63 X; up to 63 Y; N=1 means 2 bytes per char, 0 means 1 byte
//
//        @Override
//        public int writeCommand(byte[] stream, int n, List command)
//        {
//            // TODO detect whether it needs 2 bytes per char, or 1 byte is enough
//            // TODO use java.nio.charset.CharsetEncoder then
//            byte charsetIndicator = (byte)0b00000000;
//
//            String s = (String)command.get(1);
//            byte[] sbytes = s.getBytes();
//            int x = coordProvider_.apply(command, 2);
//            int y = coordProvider_.apply(command, 3);
//
//            if (sbytes.length <= 15 && x <= 63 && y <= 63)
//            {
//                stream[n] = (byte)(charsetIndicator | 0b01110000);
//                stream[n+1] = (byte)(((x & 0b00001111) << 4) | sbytes.length);
//                stream[n+2] = (byte)(((x & 0b00110000) << 2) | y);
//                for (int i=0;i<sbytes.length;i++) stream[n+3+i] = sbytes[i];
//                return 3 + sbytes.length;
//            }
//            else if(sbytes.length <= 255 && x <= 4095 && y <= 4095)
//            {
//                stream[n] = (byte)(charsetIndicator | 0b01010000);
//                stream[n+1] = (byte)sbytes.length;
//                stream[n+2] = (byte)(x & 0b11111111);
//                stream[n+3] = (byte)(y & 0b11111111);
//                stream[n+4] = (byte)(((x & 0b111100000000) >> 8) | ((y & 0b111100000000) >> 4));
//                for (int i=0;i<sbytes.length;i++) stream[n+5+i] = sbytes[i];
//                return 5+sbytes.length;
//            }
//            else
//            {
//                throw new IllegalStateException("Cannot encode drawString using regular commands. String: " + s
//                        + " len=" + sbytes.length + " x=" + x + " y=" + y);
//            }
//        }
//    }

    /*
     * Transmits id of string in the pool instead of whole string
     */
    public static class DrawStringCoder implements ICommandCoder
    {
        private BiFunction<List, Integer, Integer> coordProvider_;
        private StringPoolIdSupplier stringPoolIdSupplier_;
        private Object componentId_;

        public DrawStringCoder(StringPoolIdSupplier stringPoolIdSupplier)
        {
            this(stringPoolIdSupplier, FGPaintVectorBinaryCoder::getCoord);
        }

        public DrawStringCoder(StringPoolIdSupplier stringPoolIdSupplier, BiFunction<List, Integer, Integer> coordProvider)
        {
            stringPoolIdSupplier_ = stringPoolIdSupplier;
            coordProvider_ = coordProvider;
        }

        @Override
        public void setCodedComponentUid(Object componentId, int uid)
        {
            componentId_ = componentId;
        }

        // Header
        // [SSSSS|OOO]
        //
        // Body
        // -----
        // Sub          | Body                                                  | Comment
        // [N1010|000]  | [IIII|IIII][XXXX|XXXX][YYYY|YYYY][YYYY|XXXX][..S..]   | [0-255] str id; up to 4095 X; up 4095 Y; N=1 means 2 bytes per char, 0 means 1 byte
        // [N1110|000]  | [XXXX|IIII][XXYY|YYYY][..S..]                         | [0-15] str id; up to 63 X; up to 63 Y; N=1 means 2 bytes per char, 0 means 1 byte

        @Override
        public int writeCommand(byte[] stream, int n, List command)
        {
            // TODO detect whether it needs 2 bytes per char, or 1 byte is enough
            // TODO use java.nio.charset.CharsetEncoder then
            byte charsetIndicator = (byte)0b00000000;

            String s = (String)command.get(1);

            if (s.isEmpty())
            {
                return 0;
            }

            Integer sId = stringPoolIdSupplier_.getStringPoolId(s, componentId_);
            if (sId == null)
            {
                // TODO transmit actual string contents?
                return 0;
            }

            int x = coordProvider_.apply(command, 2);
            int y = coordProvider_.apply(command, 3);

            if (sId <= 15 && x <= 63 && y <= 63)
            {
                stream[n] = (byte)(charsetIndicator | 0b01110000);
                stream[n+1] = (byte)(((x & 0b00001111) << 4) | sId.byteValue());
                stream[n+2] = (byte)(((x & 0b00110000) << 2) | y);
                return 3;
            }
            else if(sId <= 255 && x <= 4095 && y <= 4095)
            {
                stream[n] = (byte)(charsetIndicator | 0b01010000);
                stream[n+1] = sId.byteValue();
                stream[n+2] = (byte)(x & 0b11111111);
                stream[n+3] = (byte)(y & 0b11111111);
                stream[n+4] = (byte)(((x & 0b111100000000) >> 8) | ((y & 0b111100000000) >> 4));
                return 5;
            }
            else
            {
                throw new IllegalStateException("Cannot encode drawString using regular commands. String: " + s
                        + " sId=" + sId + " x=" + x + " y=" + y);
            }
        }
    }

    public static class DrawLineCoder extends RectAreaCoder
    {
        @Override
        protected byte getOpCode()
        {
            return 0b101;
        }
    }

    public static class TransformCoder extends RectAreaCoder
    {
        public TransformCoder()
        {
            super(true, (l,i) -> {
                AffineTransform at = (AffineTransform) l.get(1);
                return i==3
                        ? (int)Math.round(at.getTranslateX()) // TODO why is it scaled for unit size already?
                        : i==4
                            ? (int)Math.round(at.getTranslateY())
                            : 0;});
        }

        @Override
        public int writeCommand(byte[] stream, int n, List command)
        {
            return super.writeCommand(stream, n, command);
        }

        @Override
        protected byte getOpCode()
        {
            return 0b110;
        }
    }

    public static class ClipRectCoder extends RectAreaCoder
    {
        @Override
        protected byte getOpCode()
        {
            return 0b111;
        }
    }

    public static class SetClipCoder extends RectAreaCoder
    {
        public SetClipCoder()
        {
            super(false);
        }

        @Override
        protected byte getOpCode()
        {
            return 0b000;
        }
    }

    public static class PushCurrentClipCoder implements ICommandCoder
    {
        @Override
        public int writeCommand(byte[] stream, int n, List command)
        {
            stream[n] = 0b00010000;
            return 1;
        }
    }

    public static class PopCurrentClipCoder implements ICommandCoder
    {
        @Override
        public int writeCommand(byte[] stream, int n, List command)
        {
            stream[n] = 0b00110000;
            return 1;
        }
    }

    @Deprecated
    public static class DrawRoundRectCoder implements ICommandCoder
    {
        @Override
        public int writeCommand(byte[] stream, int n, List command)
        {
            return 0;
        }
    }

    public static class ExtendedCommandCoder implements ICommandCoder
    {
        private final ICommandCoder nestedCoder_;

        ExtendedCommandCoder(ICommandCoder nestedCoder)
        {
            nestedCoder_ = nestedCoder;
        }

        @Override
        public int writeCommand(byte[] stream, int n, List command)
        {
            stream[n] = 0; // Extended command indicator
            int nestedWritten = nestedCoder_.writeCommand(stream, n+1, command);
            return nestedWritten+1;
        }
    }

// Not used by default
//    public static abstract class ImageRegularCoder implements ICommandCoder
//    {
//        private BiFunction<List, Integer, Integer> coordProvider_;
//
//        public ImageRegularCoder()
//        {
//            this(FGPaintVectorBinaryCoder::getCoord);
//        }
//
//        public ImageRegularCoder(BiFunction<List, Integer, Integer> coordProvider)
//        {
//            coordProvider_ = coordProvider;
//        }
//
//        // Up to 255 str len; up to 4095 X and Y; 1 byte per char
//        // [image op] | [LLLL|LLLL][LLLL|LLLL][XXXX|XXXX][YYYY|YYYY][YYYY|XXXX][..S..]
//        //
//        // Up to 255 str len; up to 4095 X,Y,W,H; 1 byte per char
//        // [image op] | [LLLL|LLLL][LLLL|LLLL][XXXX|XXXX][YYYY|YYYY][YYYY|XXXX][WWWW|WWWW][HHHH|HHHH][HHHH|WWWW][..S..]
//
//        @Override
//        public int writeCommand(byte[] stream, int n, List command)
//        {
//            int csize = command.size();
//
//            if (csize != 4 && csize != 6)
//            {
//                throw new IllegalStateException(
//                        "Cannot encode image operation: command length should be 4 or 6. Command " + command);
//            }
//
//            String s = (String)command.get(1);
//            byte[] sbytes = s.getBytes();
//            int x = coordProvider_.apply(command, 2);
//            int y = coordProvider_.apply(command, 3);
//
//            if(sbytes.length <= 65535 && x <= 4095 && y <= 4095)
//            {
//                stream[n] = getImageCommandCode();
//                stream[n+1] = (byte)(sbytes.length & 0xFF);
//                stream[n+2] = (byte)((sbytes.length & 0xFF00) >> 8);
//                stream[n+3] = (byte)(x & 0b11111111);
//                stream[n+4] = (byte)(y & 0b11111111);
//                stream[n+5] = (byte)(((x & 0b111100000000) >> 8) | ((y & 0b111100000000) >> 4));
//
//                int header = 6;
//
//                if (csize == 6)
//                {
//                    int w = coordProvider_.apply(command, 4);
//                    int h = coordProvider_.apply(command, 5);
//
//                    stream[n+6] = (byte)(w & 0b11111111);
//                    stream[n+7] = (byte)(h & 0b11111111);
//                    stream[n+8] = (byte)(((w & 0b111100000000) >> 8) | ((h & 0b111100000000) >> 4));
//
//                    header += 3;
//                }
//
//                for (int i=0;i<sbytes.length;i++) stream[n+header+i] = sbytes[i];
//                return header+sbytes.length;
//            }
//            else
//            {
//                throw new IllegalStateException("Cannot encode image operation: params out of range. URI: " + s
//                        + " len=" + sbytes.length + " x=" + x + " y=" + y);
//            }
//        }
//
//        protected abstract byte getImageCommandCode();
//    }
//
//    public static class DrawImageRegularCoder extends ImageRegularCoder
//    {
//        public DrawImageRegularCoder()
//        {
//            super();
//        }
//
//        public DrawImageRegularCoder(BiFunction<List, Integer, Integer> coordProvider)
//        {
//            super(coordProvider);
//        }
//
//        @Override
//        protected byte getImageCommandCode()
//        {
//            return 1;
//        }
//    }
//
//    public static class FitImageRegularCoder extends ImageRegularCoder
//    {
//        public FitImageRegularCoder()
//        {
//            super();
//        }
//
//        public FitImageRegularCoder(BiFunction<List, Integer, Integer> coordProvider)
//        {
//            super(coordProvider);
//        }
//
//        @Override
//        protected byte getImageCommandCode()
//        {
//            return 3;
//        }
//    }
//
//    public static class FillImageRegularCoder extends ImageRegularCoder
//    {
//        @Override
//        protected byte getImageCommandCode()
//        {
//            return 5;
//        }
//    }

    public static abstract class ImageStrPoolCoder implements ICommandCoder
    {
        private BiFunction<List, Integer, Integer> coordProvider_;
        private StringPoolIdSupplier stringPoolIdSupplier_;
        private Object componentId_;

        public ImageStrPoolCoder(StringPoolIdSupplier stringPoolIdSupplier)
        {
            this(stringPoolIdSupplier, FGPaintVectorBinaryCoder::getCoord);
        }

        public ImageStrPoolCoder(StringPoolIdSupplier stringPoolIdSupplier, BiFunction<List, Integer, Integer> coordProvider)
        {
            stringPoolIdSupplier_ = stringPoolIdSupplier;
            coordProvider_ = coordProvider;
        }

        // Up to 255 str len; up to 4095 X and Y; 1 byte per char
        // [image op] | [IIII|IIII][YYYY|YYYY][YYYY|XXXX]
        //
        // Up to 255 str len; up to 4095 X,Y,W,H; 1 byte per char
        // [image op] | [IIII|IIII][YYYY|YYYY][YYYY|XXXX][WWWW|WWWW][HHHH|HHHH][HHHH|WWWW]

        @Override
        public void setCodedComponentUid(Object componentId, int uid)
        {
            componentId_ = componentId;
        }

        @Override
        public int writeCommand(byte[] stream, int n, List command)
        {
            int csize = command.size();

            if (csize != 4 && csize != 6)
            {
                throw new IllegalStateException(
                        "Cannot encode image operation: command length should be 4 or 6. Command " + command);
            }

            String s = (String)command.get(1);
            Integer sId = stringPoolIdSupplier_.getStringPoolId(s, componentId_);
            if (sId == null)
            {
                // TODO transmit actual string contents?
                return 0;
            }

            int x = coordProvider_.apply(command, 2);
            int y = coordProvider_.apply(command, 3);

            if(x <= 4095 && y <= 4095)
            {
                stream[n] = getImageCommandCode();
                stream[n+1] = sId.byteValue();
                stream[n+2] = (byte)(x & 0b11111111);
                stream[n+3] = (byte)(y & 0b11111111);
                stream[n+4] = (byte)(((x & 0b111100000000) >> 8) | ((y & 0b111100000000) >> 4));

                int header = 5;

                if (csize == 6)
                {
                    int w = coordProvider_.apply(command, 4);
                    int h = coordProvider_.apply(command, 5);

                    stream[n+5] = (byte)(w & 0b11111111);
                    stream[n+6] = (byte)(h & 0b11111111);
                    stream[n+7] = (byte)(((w & 0b111100000000) >> 8) | ((h & 0b111100000000) >> 4));

                    header += 3;
                }
                return header;
            }
            else
            {
                throw new IllegalStateException("Cannot encode image operation: params out of range. URI: " + s
                        + " x=" + x + " y=" + y);
            }
        }

        protected abstract byte getImageCommandCode();
    }

    public static class DrawImageStrPoolCoder extends ImageStrPoolCoder
    {
        public DrawImageStrPoolCoder(StringPoolIdSupplier stringPoolIdSupplier)
        {
            super(stringPoolIdSupplier);
        }

        public DrawImageStrPoolCoder(StringPoolIdSupplier stringPoolIdSupplier, BiFunction<List, Integer, Integer> coordProvider)
        {
            super(stringPoolIdSupplier, coordProvider);
        }

        @Override
        protected byte getImageCommandCode()
        {
            return 2;
        }
    }

    public static class FitImageStrPoolCoder extends ImageStrPoolCoder
    {
        public FitImageStrPoolCoder(StringPoolIdSupplier stringPoolIdSupplier)
        {
            super(stringPoolIdSupplier);
        }

        public FitImageStrPoolCoder(StringPoolIdSupplier stringPoolIdSupplier, BiFunction<List, Integer, Integer> coordProvider)
        {
            super(stringPoolIdSupplier, coordProvider);
        }

        @Override
        protected byte getImageCommandCode()
        {
            return 4;
        }
    }

    public static class FillImageStrPoolCoder extends ImageStrPoolCoder
    {
        public FillImageStrPoolCoder(StringPoolIdSupplier stringPoolIdSupplier)
        {
            super(stringPoolIdSupplier);
        }

        @Override
        protected byte getImageCommandCode()
        {
            return 6;
        }
    }

    public static class SetFontStrPoolCoder implements ICommandCoder
    {
        private StringPoolIdSupplier stringPoolIdSupplier_;
        private Object componentId_;

        public SetFontStrPoolCoder(StringPoolIdSupplier stringPoolIdSupplier)
        {
            stringPoolIdSupplier_ = stringPoolIdSupplier;
        }

        @Override
        public void setCodedComponentUid(Object componentId, int uid)
        {
            componentId_ = componentId;
        }

        @Override
        public int writeCommand(byte[] stream, int n, List command)
        {
            int csize = command.size();

            if (csize != 2)
            {
                throw new IllegalStateException(
                    "Cannot encode setFont operation: command length should be 2. Command " + command);
            }

            String s = (String)command.get(1);
            Integer sId = stringPoolIdSupplier_.getStringPoolId(s, componentId_);
            if (sId == null)
            {
                // TODO transmit actual string contents?
                return 0;
            }

            stream[n] = 7; // setFont command code
            stream[n+1] = sId.byteValue();
            return 2;
        }
    }

    public interface StringPoolIdSupplier
    {
        Integer getStringPoolId(String s, Object componentId);
    }
}
