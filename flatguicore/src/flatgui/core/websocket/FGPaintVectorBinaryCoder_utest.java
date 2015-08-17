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

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Denis Lebedev
 */
public class FGPaintVectorBinaryCoder_utest extends Assert
{
    private FGPaintVectorBinaryCoder.RectAreaCoder rectAreaCoder_;
    private Invocable decoder_;

    @Before
    public void setUp() throws Exception
    {
        rectAreaCoder_ = new FGPaintVectorBinaryCoder.RectAreaCoder(true, (l,i)->(Integer)(l.get(i)))
        {
            @Override
            protected byte getOpCode()
            {
                return 0;
            }
        };

        URL javaScriptCodeURL = ClassLoader.getSystemResource("flatgui/core/websocket/fgdecoder.js");
        String script = Files.readAllLines(Paths.get(javaScriptCodeURL.toURI())).stream().map(l -> l+"\n").collect(Collectors.joining());

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        engine.eval(script);
        decoder_ = (Invocable)engine;
    }

    @Test
    public void testRectWWWH0() // WWWH0  | [WWWW|HHHH] | Up to 127/31 vector which is good for table cells, ticks, arrows, etc.
            throws Exception
    {
        byte[] stream = new byte[6];
        int writtenBytes = rectAreaCoder_.writeCommand(stream, 0, cmd(null, 0, 0, 0, 0));
        assertEquals(2, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 2, cmd(null, 0, 0, 112, 29));
        assertEquals(2, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 4, cmd(null, 0, 0, 127, 31));
        assertEquals(2, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 0);
        assertEquals(0, decoded0.get("w"));
        assertEquals(0, decoded0.get("h"));
        ScriptObjectMirror decoded2 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 2);
        assertEquals(112, decoded2.get("w"));
        assertEquals(29, decoded2.get("h"));
        ScriptObjectMirror decoded4 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 4);
        assertEquals(127, decoded4.get("w"));
        assertEquals(31, decoded4.get("h"));
    }

    @Test
    public void testRectWH001() // WH001  | [WWWW|WWWW][HHHH|HHHH] | Up to 511/511
            throws Exception
    {
        byte[] stream = new byte[9];
        int writtenBytes = new FGPaintVectorBinaryCoder.SubCoderPoint511x511().writeBodyWithSubCodeHeader(stream, 0, (byte) 0, 0, 0);
        assertEquals(3, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 3, cmd(null, 0, 0, 432, 312));
        assertEquals(3, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 6, cmd(null, 0, 0, 511, 511));
        assertEquals(3, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 0);
        assertEquals(0, decoded0.get("w"));
        assertEquals(0, decoded0.get("h"));
        ScriptObjectMirror decoded3 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 3);
        assertEquals(432, decoded3.get("w"));
        assertEquals(312, decoded3.get("h"));
        ScriptObjectMirror decoded6 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 6);
        assertEquals(511, decoded6.get("w"));
        assertEquals(511, decoded6.get("h"));
    }

    @Test
    public void testRectWW011() // WW011  | [WWWW|WWWW][HHHH|HHHH] | Up to 1023/255
            throws Exception
    {
        byte[] stream = new byte[9];
        int writtenBytes = new FGPaintVectorBinaryCoder.SubCoderPoint1023x255().writeBodyWithSubCodeHeader(stream, 0, (byte)0, 0, 0);
        assertEquals(3, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 3, cmd(null, 0, 0, 679, 202));
        assertEquals(3, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 6, cmd(null, 0, 0, 1023, 255));
        assertEquals(3, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 0);
        assertEquals(0, decoded0.get("w"));
        assertEquals(0, decoded0.get("h"));
        ScriptObjectMirror decoded3 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 3);
        assertEquals(679, decoded3.get("w"));
        assertEquals(202, decoded3.get("h"));
        ScriptObjectMirror decoded6 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 6);
        assertEquals(1023, decoded6.get("w"));
        assertEquals(255, decoded6.get("h"));
    }

    @Test
    public void testRectHH101() // HH101  | [WWWW|WWWW][HHHH|HHHH] | Up to 255/1023
            throws Exception
    {
        byte[] stream = new byte[9];
        int writtenBytes = new FGPaintVectorBinaryCoder.SubCoderPoint255x1023().writeBodyWithSubCodeHeader(stream, 0, (byte)0, 0, 0);
        assertEquals(3, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 3, cmd(null, 0, 0, 117, 995));
        assertEquals(3, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 6, cmd(null, 0, 0, 255, 1023));
        assertEquals(3, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 0);
        assertEquals(0, decoded0.get("w"));
        assertEquals(0, decoded0.get("h"));
        ScriptObjectMirror decoded3 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 3);
        assertEquals(117, decoded3.get("w"));
        assertEquals(995, decoded3.get("h"));
        ScriptObjectMirror decoded6 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 6);
        assertEquals(255, decoded6.get("w"));
        assertEquals(1023, decoded6.get("h"));
    }

    @Test
    public void testRect00111() // 00111  | [XXXX|YYYY][WWWW|HHHH][XXYY|WWHH]  | Up to 63,63/63,63
            throws Exception
    {
        byte[] stream = new byte[16];
        int writtenBytes = new FGPaintVectorBinaryCoder.SubCoderRect63x63().writeBodyWithSubCodeHeader(stream, 0, (byte) 0, 0, 0, 0, 0);
        assertEquals(4, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 4, cmd(null, 33, 0, 21, 61));
        assertEquals(4, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 8, cmd(null, 55, 42, 13, 17));
        assertEquals(4, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 12, cmd(null, 63, 63, 63, 63));
        assertEquals(4, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 0);
        assertEquals(0, decoded0.get("x"));
        assertEquals(0, decoded0.get("y"));
        assertEquals(0, decoded0.get("w"));
        assertEquals(0, decoded0.get("h"));
        ScriptObjectMirror decoded4 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 4);
        assertEquals(33, decoded4.get("x"));
        assertEquals(0, decoded4.get("y"));
        assertEquals(21, decoded4.get("w"));
        assertEquals(61, decoded4.get("h"));
        ScriptObjectMirror decoded8 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 8);
        assertEquals(55, decoded8.get("x"));
        assertEquals(42, decoded8.get("y"));
        assertEquals(13, decoded8.get("w"));
        assertEquals(17, decoded8.get("h"));
        ScriptObjectMirror decoded12 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 12);
        assertEquals(63, decoded12.get("x"));
        assertEquals(63, decoded12.get("y"));
        assertEquals(63, decoded12.get("w"));
        assertEquals(63, decoded12.get("h"));
    }

    @Test
    public void testRect01111() // 01111  | [XXXX|XXXX][YYYY|YYYY][WWWW|WWWW][HHHH|HHHH]    | Up to 255,255/255,255
            throws Exception
    {
        byte[] stream = new byte[20];
        int writtenBytes = new FGPaintVectorBinaryCoder.SubCoderRect255x255().writeBodyWithSubCodeHeader(stream, 0, (byte) 0, 0, 0, 0, 0);
        assertEquals(5, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 5, cmd(null, 178, 200, 201, 255));
        assertEquals(5, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 10, cmd(null, 55, 255, 13, 17));
        assertEquals(5, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 15, cmd(null, 255, 255, 255, 255));
        assertEquals(5, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 0);
        assertEquals(0, decoded0.get("x"));
        assertEquals(0, decoded0.get("y"));
        assertEquals(0, decoded0.get("w"));
        assertEquals(0, decoded0.get("h"));
        ScriptObjectMirror decoded5 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 5);
        assertEquals(178, decoded5.get("x"));
        assertEquals(200, decoded5.get("y"));
        assertEquals(201, decoded5.get("w"));
        assertEquals(255, decoded5.get("h"));
        ScriptObjectMirror decoded10 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 10);
        assertEquals(55, decoded10.get("x"));
        assertEquals(255, decoded10.get("y"));
        assertEquals(13, decoded10.get("w"));
        assertEquals(17, decoded10.get("h"));
        ScriptObjectMirror decoded15 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 15);
        assertEquals(255, decoded15.get("x"));
        assertEquals(255, decoded15.get("y"));
        assertEquals(255, decoded15.get("w"));
        assertEquals(255, decoded15.get("h"));
    }

    @Test
    public void testRect10111() // 10111  | [XXXX|XXXX][YYYY|YYYY][WWWW|WWWW][HHHH|HHHH][HHWW|YYXX]    | Up to 1023,1023/1023,1023
            throws Exception
    {
        byte[] stream = new byte[24];
        int writtenBytes = new FGPaintVectorBinaryCoder.SubCoderRect1023x1023().writeBodyWithSubCodeHeader(stream, 0, (byte) 0, 0, 0, 0, 0);
        assertEquals(6, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 6, cmd(null, 998, 240, 1001, 255));
        assertEquals(6, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 12, cmd(null, 3, 255, 13, 1017));
        assertEquals(6, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 18, cmd(null, 1023, 1023, 1023, 1023));
        assertEquals(6, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 0);
        assertEquals(0, decoded0.get("x"));
        assertEquals(0, decoded0.get("y"));
        assertEquals(0, decoded0.get("w"));
        assertEquals(0, decoded0.get("h"));
        ScriptObjectMirror decoded6 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 6);
        assertEquals(998, decoded6.get("x"));
        assertEquals(240, decoded6.get("y"));
        assertEquals(1001, decoded6.get("w"));
        assertEquals(255, decoded6.get("h"));
        ScriptObjectMirror decoded12 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 12);
        assertEquals(3, decoded12.get("x"));
        assertEquals(255, decoded12.get("y"));
        assertEquals(13, decoded12.get("w"));
        assertEquals(1017, decoded12.get("h"));
        ScriptObjectMirror decoded18 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 18);
        assertEquals(1023, decoded18.get("x"));
        assertEquals(1023, decoded18.get("y"));
        assertEquals(1023, decoded18.get("w"));
        assertEquals(1023, decoded18.get("h"));
    }

    @Test
    public void testRect10111_1() // 10111  | [XXXX|XXXX][YYYY|YYYY][WWWW|WWWW][HHHH|HHHH][HHWW|YYXX]    | Up to 1023,1023/1023,1023
            throws Exception
    {
        byte[] stream = new byte[24];
        AffineTransform t = new AffineTransform();
        t.setToTranslation(448.0, 448.0);
        int writtenBytes = new FGPaintVectorBinaryCoder.TransformCoder().writeCommand(stream, 0, cmd(null, t));
        assertEquals(3, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 0);
        assertEquals(0, decoded0.get("x"));
        assertEquals(0, decoded0.get("y"));
        assertEquals(448, decoded0.get("w"));
        assertEquals(448, decoded0.get("h"));
    }

    @Test
    public void testRect11111() // 11111  | [XXXX|XXXX][XXXX|XXXX][YYYY|YYYY][YYYY|YYYY][WWWW|WWWW][WWWW|WWWW][HHHH|HHHH][HHHH|HHHH] | Up to 65535,65535/65535,65535
            throws Exception
    {
        byte[] stream = new byte[36];
        int writtenBytes = new FGPaintVectorBinaryCoder.SubCoderRect65535x65535().writeBodyWithSubCodeHeader(stream, 0, (byte) 0, 0, 0, 0, 0);
        assertEquals(9, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 9, cmd(null, 2048, 1536, 1001, 255));
        assertEquals(9, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 18, cmd(null, 3434, 5454, 6311, 1437));
        assertEquals(9, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 27, cmd(null, 32767, 32767, 32767, 32767));
        assertEquals(9, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 0);
        assertEquals(0, decoded0.get("x"));
        assertEquals(0, decoded0.get("y"));
        assertEquals(0, decoded0.get("w"));
        assertEquals(0, decoded0.get("h"));
        ScriptObjectMirror decoded9 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 9);
        assertEquals(2048, decoded9.get("x"));
        assertEquals(1536, decoded9.get("y"));
        assertEquals(1001, decoded9.get("w"));
        assertEquals(255, decoded9.get("h"));
        ScriptObjectMirror decoded18 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 18);
        assertEquals(3434, decoded18.get("x"));
        assertEquals(5454, decoded18.get("y"));
        assertEquals(6311, decoded18.get("w"));
        assertEquals(1437, decoded18.get("h"));
        ScriptObjectMirror decoded27 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 27);
        assertEquals(32767, decoded27.get("x"));
        assertEquals(32767, decoded27.get("y"));
        assertEquals(32767, decoded27.get("w"));
        assertEquals(32767, decoded27.get("h"));
    }

    @Test
    public void testRect11111_1() // 11111  | [XXXX|XXXX][XXXX|XXXX][YYYY|YYYY][YYYY|YYYY][WWWW|WWWW][WWWW|WWWW][HHHH|HHHH][HHHH|HHHH] | Up to 65535,65535/65535,65535
            throws Exception
    {
        byte[] stream = new byte[27];
        int writtenBytes = new FGPaintVectorBinaryCoder.SubCoderRect65535x65535().writeBodyWithSubCodeHeader(stream, 0, (byte) 0, 0, 0, 0, 0);
        assertEquals(9, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 9, cmd(null, 0, -32768, -23, -141));
        assertEquals(9, writtenBytes);
        writtenBytes = rectAreaCoder_.writeCommand(stream, 18, cmd(null, -3453, -12395, -11, -9));
        assertEquals(9, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 0);
        assertEquals(0, decoded0.get("x"));
        assertEquals(0, decoded0.get("y"));
        assertEquals(0, decoded0.get("w"));
        assertEquals(0, decoded0.get("h"));
        ScriptObjectMirror decoded9 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 9);
        assertEquals(0, decoded9.get("x"));
        assertEquals(-32768, decoded9.get("y"));
        assertEquals(-23, decoded9.get("w"));
        assertEquals(-141, decoded9.get("h"));
        ScriptObjectMirror decoded18 = (ScriptObjectMirror)decoder_.invokeFunction("decodeRect", stream, 18);
        assertEquals(-3453, decoded18.get("x"));
        assertEquals(-12395, decoded18.get("y"));
        assertEquals(-11, decoded18.get("w"));
        assertEquals(-9, decoded18.get("h"));
    }

    @Test
    public void testColor()
            throws Exception
    {
        // [00000|000]  | [RRRR|RRRR][GGGG|GGGG][BBBB|BBBB]  | 8 bits for each R G B
        // [01000|000]  | [CCCC|CCCC]                        | 8 bits for equal R G B (gray colors)

        FGPaintVectorBinaryCoder.SetColorCoder colorCoder = new FGPaintVectorBinaryCoder.SetColorCoder();
        byte[] stream = new byte[8];
        int writtenBytes = colorCoder.writeCommand(stream, 0, cmd(null, new Color(0, 0, 0)));
        assertEquals(2, writtenBytes);
        writtenBytes = colorCoder.writeCommand(stream, 2, cmd(null, new Color(11, 245, 43)));
        assertEquals(4, writtenBytes);
        writtenBytes = colorCoder.writeCommand(stream, 6, cmd(null, new Color(111, 111, 111)));
        assertEquals(2, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeColor", stream, 0);
        assertEquals(0, decoded0.get("r"));
        assertEquals(0, decoded0.get("g"));
        assertEquals(0, decoded0.get("b"));
        ScriptObjectMirror decoded2 = (ScriptObjectMirror)decoder_.invokeFunction("decodeColor", stream, 2);
        assertEquals(11, decoded2.get("r"));
        assertEquals(245, decoded2.get("g"));
        assertEquals(43, decoded2.get("b"));
        ScriptObjectMirror decoded6 = (ScriptObjectMirror)decoder_.invokeFunction("decodeColor", stream, 6);
        assertEquals(111, decoded6.get("r"));
        assertEquals(111, decoded6.get("g"));
        assertEquals(111, decoded6.get("b"));
    }

// This is a test for regular drawString code (that transmits actual stirng) which is not used by default
//    @Test
//    public void testString()
//        throws Exception
//    {
//        // [N1010|000]  | [LLLL|LLLL][XXXX|XXXX][YYYY|YYYY][YYYY|XXXX][..S..]   | Up to 255 str len; up to 4095 X; up 4095 Y; N=1 means 2 bytes per char, 0 means 1 byte
//        // [N1110|000]  | [XXXX|LLLL][XXYY|YYYY][..S..]                         | Up to 15 str len; up to 63 X; up to 63 Y; N=1 means 2 bytes per char, 0 means 1 byte
//
//        String s1 = "Hello, world!";
//        String s2 = "1234?";
//        FGPaintVectorBinaryCoder.DrawStringCoder stringCoder = new FGPaintVectorBinaryCoder.DrawStringCoder((l,i)->(Integer)(l.get(i)));
//        byte[] stream = new byte[26];
//        int writtenBytes = stringCoder.writeCommand(stream, 0, cmd(null, s1, 2, 0));
//        assertEquals(3+s1.length(), writtenBytes);
//        writtenBytes = stringCoder.writeCommand(stream, 3+s1.length(), cmd(null, s2, 2, 64));
//        assertEquals(5+s2.length(), writtenBytes);
//        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeString", stream, 0);
//        assertEquals(2, decoded0.get("x"));
//        assertEquals(0, decoded0.get("y"));
//        assertEquals(s1, decoded0.get("s"));
//        ScriptObjectMirror decoded16 = (ScriptObjectMirror)decoder_.invokeFunction("decodeString", stream, 3+s1.length());
//        assertEquals(2, decoded16.get("x"));
//        assertEquals(64, decoded16.get("y"));
//        assertEquals(s2, decoded16.get("s"));
//    }

    @Test
    public void testString()
            throws Exception
    {
        FGPaintVectorBinaryCoder.DrawStringCoder stringCoder =
                new FGPaintVectorBinaryCoder.DrawStringCoder((s, uid)->((Integer)uid), (l,i)->(Integer)(l.get(i)));
        byte[] stream = new byte[8];
        stringCoder.setCodedComponentUid(Integer.valueOf(1), -1);
        int writtenBytes = stringCoder.writeCommand(stream, 0, cmd(null, "?", 2, 0));
        assertEquals(3, writtenBytes);
        stringCoder.setCodedComponentUid(Integer.valueOf(2), -1);
        writtenBytes = stringCoder.writeCommand(stream, 3, cmd(null, "?", 2, 64));
        assertEquals(5, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeString", stream, 0);
        assertEquals(2, decoded0.get("x"));
        assertEquals(0, decoded0.get("y"));
        assertEquals(1, ((Number)decoded0.get("i")).intValue());
        ScriptObjectMirror decoded3 = (ScriptObjectMirror)decoder_.invokeFunction("decodeString", stream, 3);
        assertEquals(2, decoded3.get("x"));
        assertEquals(64, decoded3.get("y"));
        assertEquals(2, ((Number)decoded3.get("i")).intValue());
    }

// Not used by default
//    @Test
//    public void testDrawImage()
//            throws Exception
//    {
//        String s1 = "Hello, world!";
//        String s2 = "1234?";
//        FGPaintVectorBinaryCoder.DrawImageRegularCoder stringCoder = new FGPaintVectorBinaryCoder.DrawImageRegularCoder((l,i)->(Integer)(l.get(i)));
//        byte[] stream = new byte[30];
//        int writtenBytes = stringCoder.writeCommand(stream, 0, cmd(null, s1, 2, 0));
//        assertEquals(6+s1.length(), writtenBytes);
//        writtenBytes = stringCoder.writeCommand(stream, 6+s1.length(), cmd(null, s2, 2, 64));
//        assertEquals(6+s2.length(), writtenBytes);
//        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeImageURIRegular", stream, 0);
//        assertEquals(2, decoded0.get("x"));
//        assertEquals(0, decoded0.get("y"));
//        assertEquals(s1, decoded0.get("s"));
//        ScriptObjectMirror decoded6 = (ScriptObjectMirror)decoder_.invokeFunction("decodeImageURIRegular", stream, 6+s1.length());
//        assertEquals(2, decoded6.get("x"));
//        assertEquals(64, decoded6.get("y"));
//        assertEquals(s2, decoded6.get("s"));
//    }
//
//    @Test
//    public void testFitImage()
//            throws Exception
//    {
//        String s1 = "Hello, world!";
//        String s2 = "1234?";
//        FGPaintVectorBinaryCoder.FitImageRegularCoder stringCoder = new FGPaintVectorBinaryCoder.FitImageRegularCoder((l,i)->(Integer)(l.get(i)));
//        byte[] stream = new byte[36];
//        int writtenBytes = stringCoder.writeCommand(stream, 0, cmd(null, s1, 2, 0, 10, 20));
//        assertEquals(9+s1.length(), writtenBytes);
//        writtenBytes = stringCoder.writeCommand(stream, 9+s1.length(), cmd(null, s2, 1000, 2000, 3000, 4000));
//        assertEquals(9+s2.length(), writtenBytes);
//        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeImageURIRegular", stream, 0);
//        assertEquals(2, decoded0.get("x"));
//        assertEquals(0, decoded0.get("y"));
//        assertEquals(10, decoded0.get("w"));
//        assertEquals(20, decoded0.get("h"));
//        assertEquals(s1, decoded0.get("s"));
//        ScriptObjectMirror decoded9 = (ScriptObjectMirror)decoder_.invokeFunction("decodeImageURIRegular", stream, 9+s1.length());
//        assertEquals(1000, decoded9.get("x"));
//        assertEquals(2000, decoded9.get("y"));
//        assertEquals(3000, decoded9.get("w"));
//        assertEquals(4000, decoded9.get("h"));
//        assertEquals(s2, decoded9.get("s"));
//    }

    @Test
    public void testDrawPoolImage()
            throws Exception
    {
        FGPaintVectorBinaryCoder.DrawImageStrPoolCoder stringCoder =
                new FGPaintVectorBinaryCoder.DrawImageStrPoolCoder((s, uid)->((Integer)uid), (l,i)->(Integer)(l.get(i)));
        byte[] stream = new byte[10];
        stringCoder.setCodedComponentUid(Integer.valueOf(11), -1);
        int writtenBytes = stringCoder.writeCommand(stream, 0, cmd(null, "?", 2, 0));
        assertEquals(5, writtenBytes);
        stringCoder.setCodedComponentUid(Integer.valueOf(22), -1);
        writtenBytes = stringCoder.writeCommand(stream, 5, cmd(null, "?", 2, 64));
        assertEquals(5, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeImageURIStrPool", stream, 0);
        assertEquals(2, decoded0.get("x"));
        assertEquals(0, decoded0.get("y"));
        assertEquals(11, decoded0.get("i"));
        ScriptObjectMirror decoded5 = (ScriptObjectMirror)decoder_.invokeFunction("decodeImageURIStrPool", stream, 5);
        assertEquals(2, decoded5.get("x"));
        assertEquals(64, decoded5.get("y"));
        assertEquals(22, decoded5.get("i"));
    }

    @Test
    public void testFitPoolImage()
            throws Exception
    {
        FGPaintVectorBinaryCoder.FitImageStrPoolCoder stringCoder =
                new FGPaintVectorBinaryCoder.FitImageStrPoolCoder((s, uid)->((Integer)uid),  (l,i)->(Integer)(l.get(i)));
        byte[] stream = new byte[16];
        stringCoder.setCodedComponentUid(Integer.valueOf(33), -1);
        int writtenBytes = stringCoder.writeCommand(stream, 0, cmd(null, "?", 2, 0, 10, 20));
        assertEquals(8, writtenBytes);
        stringCoder.setCodedComponentUid(Integer.valueOf(44), -1);
        writtenBytes = stringCoder.writeCommand(stream, 8, cmd(null, "?", 1000, 2000, 3000, 4000));
        assertEquals(8, writtenBytes);
        ScriptObjectMirror decoded0 = (ScriptObjectMirror)decoder_.invokeFunction("decodeImageURIStrPool", stream, 0);
        assertEquals(2, decoded0.get("x"));
        assertEquals(0, decoded0.get("y"));
        assertEquals(10, decoded0.get("w"));
        assertEquals(20, decoded0.get("h"));
        assertEquals(33, decoded0.get("i"));
        ScriptObjectMirror decoded8 = (ScriptObjectMirror)decoder_.invokeFunction("decodeImageURIStrPool", stream, 8);
        assertEquals(1000, decoded8.get("x"));
        assertEquals(2000, decoded8.get("y"));
        assertEquals(3000, decoded8.get("w"));
        assertEquals(4000, decoded8.get("h"));
        assertEquals(44, decoded8.get("i"));
    }

    private List cmd(Object... objects)
    {
        List l = new ArrayList<>(objects.length);
        for (Object o : objects)
        {
            l.add(o);
        }
        return l;
    }
}
