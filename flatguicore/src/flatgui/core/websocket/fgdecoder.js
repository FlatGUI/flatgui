
function b(n)
{
    return parseInt(n, 2);
}

var MASK_HBIT = b('10000000');
var MASK_7BIT = b('01111111');

var MASK_HB = b('11110000');
var MASK_LB = b('00001111');

var MASK_Q0 = b('00000011');
var MASK_Q1 = b('00001100');
var MASK_Q2 = b('00110000');
var MASK_Q3 = b('11000000');

var MASK_Q123 = b('00111111');

var OP_BASE_MASK = b('00000111');

var CODE_ZERO_GROUP = 0;
var CODE_DRAW_RECT = b('00000001');
var CODE_FILL_RECT = b('00000010');
var CODE_DRAW_OVAL = b('00000011');
var CODE_FILL_OVAL = b('00000100');
var CODE_DRAW_LINE = b('00000101');
var CODE_TRANSFORM = b('00000110');
var CODE_CLIP_RECT = b('00000111');

var MASK_SET_COLOR = b('00011111');
var CODE_SET_COLOR = 0;

var MASK_SET_CLIP = b('00001000');
var CODE_SET_CLIP = b('00001000');

var CODE_PUSH_CLIP = b('00010000');
var CODE_POP_CLIP = b('00110000');

var MASK_RECT_1 = b('00001000');
var MASK_RECT_3 = b('00111000');
var MASK_RECT_5 = b('11111000');

var MASK_W_1 = b('11100000');
var MASK_H_1 = b('00010000');

var MASK_W_3 = b('10000000');
var MASK_H_3 = b('01000000');

var MASK_V_3 = b('11000000');

var RECT_WWWH0 = 0;             // WWWH0  | [WWWW|HHHH]                                                                              | Up to 127/31 vector which is good for table cells, ticks, arrows, etc.
var RECT_WH001 = b('00001000'); // WH001  | [WWWW|WWWW][HHHH|HHHH]                                                                   | Up to 511/511
var RECT_WW011 = b('00011000'); // WW011  | [WWWW|WWWW][HHHH|HHHH]                                                                   | Up to 1023/255
var RECT_HH101 = b('00101000'); // HH101  | [WWWW|WWWW][HHHH|HHHH]                                                                   | Up to 255/1023
var RECT_00111 = b('00111000'); // 00111  | [YYYY|XXXX][HHHH|WWWW][HHWW|YYXX]                                                        | Up to 63,63/63,63
var RECT_01111 = b('01111000'); // 01111  | [XXXX|XXXX][YYYY|YYYY][WWWW|WWWW][HHHH|HHHH]                                             | Up to 255,255/255,255
var RECT_10111 = b('10111000'); // 10111  | [XXXX|XXXX][YYYY|YYYY][WWWW|WWWW][HHHH|HHHH][HHWW|YYXX]                                  | Up to 1023,1023/1023,1023
var RECT_11111 = b('11111000'); // 11111  | [XXXX|XXXX][XXXX|XXXX][YYYY|YYYY][YYYY|YYYY][WWWW|WWWW][WWWW|WWWW][HHHH|HHHH][HHHH|HHHH] | Up to 65535,65535/65535,65535

var COLOR_REGULAR_OP = b('10000000') ;  // [10000|000]  | [RRRR|RRRR][GGGG|GGGG][BBBB|BBBB]       | 8 bits for each R G B
var COLOR_GRAY_OP = b('01000000');      // [01000|000]  | [CCCC|CCCC]                             | 8 bits for equal R G B (gray colors)

var STR_REG_1BYTE_OP = b('01010000');      // [N1010|000]  | [LLLL|LLLL][XXXX|XXXX][YYYY|YYYY][YYYY|XXXX][..S..]   | Up to 255 str len; up to 4095 X; up 4095 Y; N=1 means 2 bytes per char, 0 means 1 byte
var STR_SHORT_1BYTE_OP = b('01110000');    // [N1110|000]  | [XXXX|LLLL][XXYY|YYYY][..S..]                         | Up to 15 str len; up to 63 X; up to 63 Y; N=1 means 2 bytes per char, 0 means 1 byte

// Returns object {x: <x> y: <y> w: <w> h: <h> :len <actual length of command in bytes>}
function decodeRect(stream, c)
{
    var x = 0;
    var y = 0;
    var w;
    var h;
    var len;

    if ((stream[c] & MASK_RECT_1) == RECT_WWWH0)
    {
        var uw = stream[c] & MASK_W_1;
        var uh = stream[c] & MASK_H_1;
        w = (uw >> 1) | ((stream[c+1] & MASK_HB) >> 4);
        h = uh | (stream[c+1] & MASK_LB);
        len = 2;
    }
    else if ((stream[c] & MASK_RECT_3) == RECT_WH001)
    {
        var uw = stream[c] & MASK_W_3;
        var uh = stream[c] & MASK_H_3;

        var c1_hbit = (stream[c+1] & MASK_HBIT) >> 7;
        var c1_7bit = stream[c+1] & MASK_7BIT;
        var c1 = c1_7bit + c1_hbit*128;
        w = (uw << 1) | c1;

        var c2_hbit = (stream[c+2] & MASK_HBIT) >> 7;
        var c2_7bit = stream[c+2] & MASK_7BIT;
        var c2 = c2_7bit + c2_hbit*128;
        h = (uh << 2) | c2;

        len = 3;
    }
    else if ((stream[c] & MASK_RECT_3) == RECT_WW011)
    {
        var uw = stream[c] & MASK_V_3;

        var c1_hbit = (stream[c+1] & MASK_HBIT) >> 7;
        var c1_7bit = stream[c+1] & MASK_7BIT;
        var c1 = c1_7bit + c1_hbit*128;
        w = (uw << 2) | c1;

        var c2_hbit = (stream[c+2] & MASK_HBIT) >> 7;
        var c2_7bit = stream[c+2] & MASK_7BIT;
        var c2 = 0x0000 | (c2_7bit + c2_hbit*128);  // Without 0x0000 h becomes Double type
        h = c2;

        len = 3;
    }
    else if ((stream[c] & MASK_RECT_3) == RECT_HH101)
    {
        var uh = stream[c] & MASK_V_3;

        var c1_hbit = (stream[c+1] & MASK_HBIT) >> 7;
        var c1_7bit = stream[c+1] & MASK_7BIT;
        var c1 = 0x0000 | (c1_7bit + c1_hbit*128);  // Without 0x0000 w becomes Double type
        w = c1;

        var c2_hbit = (stream[c+2] & MASK_HBIT) >> 7;
        var c2_7bit = stream[c+2] & MASK_7BIT;
        var c2 = c2_7bit + c2_hbit*128;
        h = (uh << 2) | c2;

        len = 3;
    }
    else if ((stream[c] & MASK_RECT_5) == RECT_00111)
    {
        // Without 0x0000 these become Double type
        x = 0x0000 | ((stream[c+1] & MASK_LB) + ((stream[c+3] & MASK_Q0) << 4));
        y = 0x0000 | (((stream[c+1] & MASK_HB) >> 4) + ((stream[c+3] & MASK_Q1) << 2));
        w = 0x0000 | ((stream[c+2] & MASK_LB) + (stream[c+3] & MASK_Q2));
        h = 0x0000 | (((stream[c+2] & MASK_HB) >> 4) + ((stream[c+3] & MASK_Q3) >> 2));

        len = 4;
    }
    else if ((stream[c] & MASK_RECT_5) == RECT_01111)
    {
        var c1_hbit = (stream[c+1] & MASK_HBIT) >> 7;
        var c1_7bit = stream[c+1] & MASK_7BIT;
        x = 0x0000 | (c1_7bit + c1_hbit*128);  // Without 0x0000 it becomes Double type

        var c2_hbit = (stream[c+2] & MASK_HBIT) >> 7;
        var c2_7bit = stream[c+2] & MASK_7BIT;
        y = 0x0000 | (c2_7bit + c2_hbit*128);  // Without 0x0000 it becomes Double type
        
        var c3_hbit = (stream[c+3] & MASK_HBIT) >> 7;
        var c3_7bit = stream[c+3] & MASK_7BIT;
        w = 0x0000 | (c3_7bit + c3_hbit*128);  // Without 0x0000 it becomes Double type
        
        var c4_hbit = (stream[c+4] & MASK_HBIT) >> 7;
        var c4_7bit = stream[c+4] & MASK_7BIT;
        h = 0x0000 | (c4_7bit + c4_hbit*128);  // Without 0x0000 it becomes Double type

        len = 5;
    }
    else if ((stream[c] & MASK_RECT_5) == RECT_10111)
    {
        var c1_hbit = (stream[c+1] & MASK_HBIT) >> 7;
        var c1_7bit = stream[c+1] & MASK_7BIT;
        x = 0x0000 | (c1_7bit + c1_hbit*128);  // Without 0x0000 it becomes Double type

        var c2_hbit = (stream[c+2] & MASK_HBIT) >> 7;
        var c2_7bit = stream[c+2] & MASK_7BIT;
        y = 0x0000 | (c2_7bit + c2_hbit*128);  // Without 0x0000 it becomes Double type

        var c3_hbit = (stream[c+3] & MASK_HBIT) >> 7;
        var c3_7bit = stream[c+3] & MASK_7BIT;
        w = 0x0000 | (c3_7bit + c3_hbit*128);  // Without 0x0000 it becomes Double type

        var c4_hbit = (stream[c+4] & MASK_HBIT) >> 7;
        var c4_7bit = stream[c+4] & MASK_7BIT;
        h = 0x0000 | (c4_7bit + c4_hbit*128);  // Without 0x0000 it becomes Double type

        x = 0x0000 | (x + ((stream[c+5] & MASK_Q0) << 8));
        y = 0x0000 | (y + ((stream[c+5] & MASK_Q1) << 6));
        w = 0x0000 | (w + ((stream[c+5] & MASK_Q2) << 4));
        h = 0x0000 | (h + ((stream[c+5] & MASK_Q3) << 2));

        len=6;
    }
    else if ((stream[c] & MASK_RECT_5) == RECT_11111)
    {
        var c1_hbit = (stream[c+1] & MASK_HBIT) >> 7;
        var c1_7bit = stream[c+1] & MASK_7BIT;
        x = c1_7bit + c1_hbit*128;  
        var c2_hbit = (stream[c+2] & MASK_HBIT);
        var c2_7bit = stream[c+2] & MASK_7BIT;
        // Without 0x0000 it becomes Double type
        var c12abs = (x + c2_7bit*256);
        x = 0x0000 | (c2_hbit == 0 ? c12abs : c12abs - 32768);

        var c3_hbit = (stream[c+3] & MASK_HBIT) >> 7;
        var c3_7bit = stream[c+3] & MASK_7BIT;
        y = c3_7bit + c3_hbit*128;  
        var c4_hbit = (stream[c+4] & MASK_HBIT);
        var c4_7bit = stream[c+4] & MASK_7BIT;
        // Without 0x0000 it becomes Double type
        var c34abs = (y + c4_7bit*256);
        y = 0x0000 | (c4_hbit == 0 ? c34abs : c34abs - 32768);

        var c5_hbit = (stream[c+5] & MASK_HBIT) >> 7;
        var c5_7bit = stream[c+5] & MASK_7BIT;
        w = c5_7bit + c5_hbit*128;
        var c6_hbit = (stream[c+6] & MASK_HBIT);
        var c6_7bit = stream[c+6] & MASK_7BIT;
        // Without 0x0000 it becomes Double type
        var c56abs = (w + c6_7bit*256);
        w = 0x0000 | (c6_hbit == 0 ? c56abs : c56abs - 32768);

        var c7_hbit = (stream[c+7] & MASK_HBIT) >> 7;
        var c7_7bit = stream[c+7] & MASK_7BIT;
        h = c7_7bit + c7_hbit*128;
        var c8_hbit = (stream[c+8] & MASK_HBIT);
        var c8_7bit = stream[c+8] & MASK_7BIT;
        // Without 0x0000 it becomes Double type
        var c78abs = (h + c8_7bit*256);
        h = 0x0000 | (c8_hbit == 0 ? c78abs : c78abs - 32768);

        len=9;        
    }
    
    return {x: x, y: y, w: w, h: h, len: len}
}

// Returns object {r: <r> g: <g> b: <b> :len <actual length of command in bytes>}
function decodeColor(stream, c)
{
    var r;
    var g;
    var b;
    var len;

    var c1_hbit = (stream[c+1] & MASK_HBIT) >> 7;
    var c1_7bit = stream[c+1] & MASK_7BIT;
    r = 0x0000 | (c1_7bit + c1_hbit*128);  // Without 0x0000 it becomes Double type

    if (stream[c] & COLOR_REGULAR_OP)
    {
        var c2_hbit = (stream[c+2] & MASK_HBIT) >> 7;
        var c2_7bit = stream[c+2] & MASK_7BIT;
        g = 0x0000 | (c2_7bit + c2_hbit*128);  // Without 0x0000 it becomes Double type

        var c3_hbit = (stream[c+3] & MASK_HBIT) >> 7;
        var c3_7bit = stream[c+3] & MASK_7BIT;
        b = 0x0000 | (c3_7bit + c3_hbit*128);  // Without 0x0000 it becomes Double type

        len = 4;
    }
    else if (stream[c] & COLOR_GRAY_OP)
    {
        g = r;
        b = r;

        len = 2;
    }

    return {r: r, g: g, b: b, len: len};
}

//var STR_REG_1BYTE_OP = b('01010000');      // [N1010|000]  | [LLLL|LLLL][XXXX|XXXX][YYYY|YYYY][YYYY|XXXX][..S..]   | Up to 255 str len; up to 4095 X; up 4095 Y; N=1 means 2 bytes per char, 0 means 1 byte
//var STR_SHORT_1BYTE_OP = b('01110000');    // [N1110|000]  | [XXXX|LLLL][XXYY|YYYY][..S..]                         | Up to 15 str len; up to 63 X; up to 63 Y; N=1 means 2 bytes per char, 0 means 1 byte

// Returns object {x: <x> y: <y> s: <string> :len <actual length of command in bytes>}
function decodeString(stream, c)
{
    var x;
    var y;
    var s;
    var len;

    var strlen;
    var sstart;
    var sto = sstart+strlen;
    s = "";

    print(' stream[c] = ' + stream[c]);

    if (stream[c] == STR_REG_1BYTE_OP)
    {
        print('Regular str');

        strlen = stream[c+1]
        x = stream[c+2];
        y = stream[c+3];
        x = 0x0000 | (x + ((stream[c+4] & MASK_LB) << 8));
        y = 0x0000 | (y + ((stream[c+4] & MASK_HB) << 4));

        sstart = c+5;

        len = 5 + strlen;
    }
    else if (stream[c] == STR_SHORT_1BYTE_OP)
    {
        print('Short str');

        strlen = (stream[c+1] & MASK_LB);
        x = 0x0000 | ((stream[c+1] & MASK_HB) >> 4) + ((stream[c+2] & MASK_Q3) >> 2);
        y = 0x0000 | (stream[c+2] & MASK_Q123);

        sstart = c+3;

        len = 3 + strlen;
    }

    var sto = sstart+strlen;
    for (var i = sstart; i<sto; i++)
    {
        s += String.fromCharCode(stream[i]);
    }
    print(' s= ' + s);

    return {x: x, y: y, s: s, len: len};
}

function decodeStream(stream, byteLength)
{
    var c = 0;
    while (c < byteLength)
    {
        var codeObj;
        var opcodeBase = stream[c] & OP_BASE_MASK;
        switch (varName)
        {
            case CODE_ZERO_GROUP:
                if ((stream[c] & MASK_SET_COLOR) == CODE_SET_COLOR)
                {
                    codeObj = decodeColor(stream, c);

                    c += codeObj.len;
                }
                else if ((stream[c] & MASK_SET_CLIP) == CODE_SET_CLIP)
                {
                    codeObj = decodeRect(stream, c);

                    c += codeObj.len;
                }
                else if (stream[c] == CODE_PUSH_CLIP)
                {

                    c++;
                }
                else if (stream[c] == CODE_POP_CLIP)
                {

                    c++;
                }
                else
                {
                    codeObj = decodeString(stream, c);

                    c+= codeObj.len;
                }
            case CODE_DRAW_RECT:
                codeObj = decodeRect(stream, c);

                c+= codeObj.len;
                break;
            case CODE_FILL_RECT:
                codeObj = decodeRect(stream, c);

                c+= codeObj.len;
                break;
            case CODE_DRAW_OVAL:
                codeObj = decodeRect(stream, c);

                c+= codeObj.len;
                break;
            case CODE_FILL_OVAL:
                codeObj = decodeRect(stream, c);

                c+= codeObj.len;
                break;
            case CODE_DRAW_LINE:
                codeObj = decodeRect(stream, c);

                c+= codeObj.len;
                break;
            case CODE_DRAW_CODE_TRANSFORM:
                codeObj = decodeRect(stream, c);

                c+= codeObj.len;
                break;
            case CODE_CLIP_RECT:
                codeObj = decodeRect(stream, c);

                c+= codeObj.len;
                break;
            default:
               throw new Error("Unknown operation code: " + stream[c]);
        }
    }
}
