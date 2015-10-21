/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

var referenceFont = '12px Tahoma';
var canvas = document.getElementById("hostCanvas");
var ctx = canvas.getContext("2d");
function initCanvas()
{
    ctx.lineWidth = 1;
    ctx.font = referenceFont;
}
initCanvas();

function adjustCanvasSize()
{
    console.log("Window size: " + window.innerWidth + " -- " + window.innerHeight);
    canvas.width  = window.innerWidth;
    canvas.height = window.innerHeight;
    initCanvas();
}

function handleResize(evt)
{
    adjustCanvasSize();
    sendEventToServer(getEncodedHostResizeEvent());
}

window.onresize = handleResize;

var pendingServerClipboardObject;
var lastExternalClipboardObject;
var userHasNavigatedOut = false;
var userRequestsDataExport = false;

var currentTransform = [[1, 0, 0],
                        [0, 1, 1],
                        [0, 0, 1]];
var currentClip = {x: 0, y: 0, w: Infinity, h: Infinity};
var clipRectStack = [];
var currentFont;


/*
 * Image cache
 */

var uriToImage = {}

function getImage(uri)
{
    var img = uriToImage[uri];
    if (img)
    {
        return img;
    }
    else
    {
        img = new Image;
        img.src = uri;
        img.onload = function(){repaintWholeCache();}
        uriToImage[uri] = img;
        return img;
    }
}

/**
 * Utilities
 */

function componentToHex(c)
{
    var hex = c.toString(16);
    return hex.length == 1 ? "0" + hex : hex;
}

function rgbToHex(codeObj)
{
    return "#" + componentToHex(codeObj.r) + componentToHex(codeObj.g) + componentToHex(codeObj.b);
}

function mxMult(m1, m2)
{
    var result = [];
    for (var i = 0; i < m1.length; i++)
    {
        result[i] = [];
        for (var j = 0; j < m2[0].length; j++)
        {
            var sum = 0;
            for (var k = 0; k < m1[0].length; k++)
            {
                sum += m1[i][k] * m2[k][j];
            }
           result[i][j] = sum;
        }
   }
   return result;
}

function translatonInverse(m)
{
    return [[1, 0, 0, -m[0][3]]
            [0, 1, 0, -m[1][3]]
            [0, 0, 1, -m[2][3]]
            [0, 0, 0,       1]];
}

function lineInt(a1, a2, b1, b2)
{
    if (b1 >= a1 && a2 > b1 && b2 >= a2)
    {
        return {a: b1, b: a2}
    }
    else if (a1 >= b1 && b2 > a1 && a2 >= b2)
    {
        return {a: a1, b: b2}
    }
    else if (b1 <= a1 && a1 <= a2 && a2 <= b2)
    {
        return {a: a1, b: a2}
    }
    else if (a1 <= b1 && b1 <= b2 && b2 <= a2)
    {
        return {a: b1, b: b2}
    }
    else
    {
        return null;
    }
}

function rectInt(a, b)
{
    var x1a = a.x;
    var y1a = a.y;
    var x2a = x1a + a.w;
    var y2a = y1a + a.h;
    var x1b = b.x;
    var y1b = b.y;
    var x2b = x1b + b.w;
    var y2b = y1b + b.h;
    var xinter = lineInt(x1a, x2a, x1b, x2b);
    var yinter = lineInt(y1a, y2a, y1b, y2b);

    if (xinter && yinter)
    {
        var x1 = xinter.a;
        var x2 = xinter.b;
        var y1 = yinter.a;
        var y2 = yinter.b;

        return {x: x1, y: y1, w: (x2-x1), h: (y2-y1)};
    }
    else
    {
        return null;
    }
}

/**
 * Binary decoding/performing
 */

function applyCurrentTransform()
{
    ctx.setTransform(currentTransform[0][0], currentTransform[1][0], currentTransform[0][1], currentTransform[1][1], currentTransform[0][2], currentTransform[1][2]);
}

function applyCurrentFont()
{
    if (currentFont)
    {
        ctx.font = currentFont;
    }
}

function applyCurrentClip()
{
    if (currentClip)
    {
        ctx.restore();
        ctx.save();
        applyCurrentTransform();
        applyCurrentFont();
        currentTx = currentTransform[0][2];
        currentTy = currentTransform[1][2];
        ctx.beginPath();
        ctx.rect(currentClip.x-currentTx, currentClip.y-currentTy, currentClip.w, currentClip.h);
        ctx.clip();
        ctx.closePath();
    }
    else
    {
        ctx.beginPath();
        ctx.rect(0, 0, 0, 0);
        ctx.clip();
        ctx.closePath();
    }
}

function pushCurrentClip()
{
    clipRectStack.push(currentClip);
}

function popCurrentClip()
{
    currentClip = clipRectStack.pop();
    applyCurrentClip();
}

function applyTransform(codeObj)
{
    ctx.transform(1, 0, 0, 1, codeObj.w, codeObj.h);
    currentTransform = mxMult(currentTransform, [[     1,      0, codeObj.w],
                                                 [     0,      1, codeObj.h],
                                                 [     0,      0,         1]]);
}

function applyInverseTransform(codeObj)
{
    ctx.transform(1, 0, 0, 1, -codeObj.w, -codeObj.h);
    currentTransform = mxMult(currentTransform, [[     1,      0, -codeObj.w],
                                                 [     0,      1, -codeObj.h],
                                                 [     0,      0,          1]]);
}

function clipRect(codeObj)
{
    if (currentClip)
    {
        currentTx = currentTransform[0][2];
        currentTy = currentTransform[1][2];
        codeObjT = {x: currentTx, y: currentTy, w: codeObj.w, h: codeObj.h};

        currentClip = rectInt(currentClip, codeObjT);
    }
    applyCurrentClip();
}

function setClip(codeObj)
{
    currentTx = currentTransform[0][2];
    currentTy = currentTransform[1][2];
    currentClip = {x: currentTx, y: currentTy, w: codeObj.w, h: codeObj.h};
    applyCurrentClip();
}

function getTextLineHeight()
{
    return ctx.measureText("M").width;
}

function fillMultilineTextNoWrap(text, x, y)
{
    var lines = text.split("\n");
    var lineHeight = getTextLineHeight();
    for (var i=0; i<lines.length; i++)
    {
        ctx.fillText(lines[i], x, y + i*1.5*lineHeight);
    }
}

function decodeLog(msg)
{
    //console.log(msg);
}

function decodeLookVector(componentIndex, stream, byteLength)
{
    var c = 0;
    while (c < byteLength)
    {
        var codeObj;

        if (stream[c] == 0) // Extended commands
        {
            var opcodeBase = stream[c+1];
            c++;

            switch (opcodeBase)
            {
                case CODE_DRAW_IMAGE_STRPOOL:
                    codeObj = decodeImageURIStrPool(stream, c);
                    var imageUrl;
                    // This is ok, draw image command may arrive before string pool update, just need to check
                    if (stringPools[componentIndex])
                    {
                        imageUrl = stringPools[componentIndex][codeObj.i];
                    }
                    // This is ok, look vector update with new image may arrive before string pool update, just need to check
                    if (imageUrl)
                    {
                        var img = getImage(imageUrl);
                        ctx.drawImage(img, codeObj.x, codeObj.y);
                    }
                    c += codeObj.len;
                    break;
                case CODE_FIT_IMAGE_STRPOOL:
                    codeObj = decodeImageURIStrPool(stream, c);
                    var img = getImage(stringPools[componentIndex][codeObj.i]);
                    ctx.drawImage(img, codeObj.x, codeObj.y, codeObj.w, codeObj.h);
                    c += codeObj.len;
                    break;
                case CODE_FILL_IMAGE_STRPOOL:
                    codeObj = decodeImageURIStrPool(stream, c);
                    var img = getImage(stringPools[componentIndex][codeObj.i]);
                    var w = img.width;
                    var h = img.height;
                    if (codeObj.w <= w && codeObj.h <= h)
                    {
                        ctx.drawImage(img, codeObj.x, codeObj.y);
                    }
                    else if (w > 0 && h >0)
                    {
                        for (var ix=0; ix<codeObj.w; ix+=w)
                        {
                            for (var iy=0; iy<codeObj.h; iy+=h)
                            {
                                ctx.drawImage(img, codeObj.x+ix, codeObj.y+iy);
                            }
                        }
                    }
                    else
                    {
                        decodeLog("Cannot fill image with zero size: w=" + w + " h=" + h);
                    }
                    c += codeObj.len;
                    break;
                case CODE_SET_FONT:
                    codeObj = decodeFontStrPool(stream, c);
                    if (stringPools[componentIndex] && stringPools[componentIndex][codeObj.i])
                    {
                       currentFont = stringPools[componentIndex][codeObj.i];
                       applyCurrentFont();
                    }
                    c += codeObj.len;
                    break;
                default:
                    decodeLog("Unknown extended operation code: " + opcodeBase);
                    throw new Error("Unknown extended operation code: " + opcodeBase);
            }
        }
        else
        {
            var opcodeBase = stream[c] & OP_BASE_MASK;

            switch (opcodeBase)
            {
                case CODE_ZERO_GROUP:
                    if ((stream[c] & MASK_SET_COLOR) == CODE_SET_COLOR)
                    {
                        codeObj = decodeColor(stream, c);
                        decodeLog("setColor " + JSON.stringify(codeObj));
                        var colorStr = rgbToHex(codeObj);
                        ctx.fillStyle=colorStr;
                        ctx.strokeStyle=colorStr;
                        c += codeObj.len;
                    }
                    else if ((stream[c] & MASK_SET_CLIP) == CODE_SET_CLIP)
                    {
                        codeObj = decodeRect(stream, c);
                        decodeLog( "setClip " + JSON.stringify(codeObj));
                        setClip(codeObj);
                        c += codeObj.len;
                    }
                    else if (stream[c] == CODE_PUSH_CLIP)
                    {
                        decodeLog( "pushCurrentClip");
                        pushCurrentClip();
                        c++;
                    }
                    else if (stream[c] == CODE_POP_CLIP)
                    {
                        decodeLog( "popCurrentClip");
                        popCurrentClip();
                        c++;
                    }
                    else
                    {
                        codeObj = decodeString(stream, c);
                        decodeLog( "drawString " + JSON.stringify(codeObj));
                        if (stringPools[componentIndex] && stringPools[componentIndex][codeObj.i])
                        {
                            fillMultilineTextNoWrap(stringPools[componentIndex][codeObj.i], codeObj.x, codeObj.y);
                        }
                        c += codeObj.len;
                    }
                    break;
                case CODE_DRAW_RECT:
                    codeObj = decodeRect(stream, c);
                    decodeLog( "drawRect " + JSON.stringify(codeObj));
                    ctx.strokeRect(codeObj.x+0.5, codeObj.y+0.5, codeObj.w, codeObj.h);
                    c+= codeObj.len;
                    break;
                case CODE_FILL_RECT:
                    codeObj = decodeRect(stream, c);
                    decodeLog( "fillRect " + JSON.stringify(codeObj));
                    ctx.fillRect(codeObj.x, codeObj.y, codeObj.w, codeObj.h);
                    c+= codeObj.len;
                    break;
                case CODE_DRAW_OVAL:
                    codeObj = decodeRect(stream, c);
                    decodeLog( "drawOval " + JSON.stringify(codeObj));
                    var r = codeObj.w/2;
                    ctx.beginPath();
                    ctx.arc(codeObj.x+r+0.5, codeObj.y+r+0.5, codeObj.w/2, 0, 2*Math.PI);
                    ctx.stroke();
                    c+= codeObj.len;
                    break;
                case CODE_FILL_OVAL:
                    codeObj = decodeRect(stream, c);
                    decodeLog( "fillOval " + JSON.stringify(codeObj));
                    var r = codeObj.w/2-0.5;
                    if (r < 0)
                    {
                        r = 0;
                    }
                    ctx.beginPath();
                    ctx.arc(codeObj.x+r+0.5, codeObj.y+r+0.5, r, 0, 2*Math.PI);
                    ctx.fill();
                    c+= codeObj.len;
                    break;
                case CODE_DRAW_LINE:
                    codeObj = decodeRect(stream, c);
                    decodeLog( "drawLine " + JSON.stringify(codeObj));
                    ctx.beginPath();
                    ctx.moveTo(codeObj.x+0.5, codeObj.y+0.5);
                    ctx.lineTo(codeObj.w+0.5, codeObj.h+0.5);
                    ctx.stroke();
                    c+= codeObj.len;
                    break;
                case CODE_TRANSFORM:
                    codeObj = decodeRect(stream, c);
                    decodeLog( "transform " + JSON.stringify(codeObj));
                    applyTransform(codeObj);
                    c+= codeObj.len;
                    break;
                case CODE_CLIP_RECT:
                    codeObj = decodeRect(stream, c);
                    decodeLog( "clipRect " + JSON.stringify(codeObj));
                    clipRect(codeObj);
                    c+= codeObj.len;
                    break;
                default:
                    decodeLog( "Unknown operation code: " + stream[c]);
                    throw new Error("Unknown operation code: " + stream[c]);
            }
        }
    }
}

var POSITION_MATRIX_MAP_COMMAND_CODE = 0;
var VIEWPORT_MATRIX_MAP_COMMAND_CODE = 1;
var CLIP_SIZE_MAP_COMMAND_CODE = 2;
var LOOK_VECTOR_MAP_COMMAND_CODE = 3;
var CHILD_COUNT_MAP_COMMAND_CODE = 4;
var BOOLEAN_STATE_FLAGS_COMMAND_CODE = 5;
var STRING_POOL_MAP_COMMAND_CODE = 7;

var PAINT_ALL_LIST_COMMAND_CODE = 64;
var REPAINT_CACHED_COMMAND_CODE = 65;
var SET_CURSOR_COMMAND_CODE = 66;
var PUSH_TEXT_TO_CLIPBOARD = 67;

var TRANSMISSION_MODE_FIRST = 68;
var TRANSMISSION_MODE_LAST = 71;
var FINISH_PREDICTION_TRANSMISSION = TRANSMISSION_MODE_FIRST;
var MOUSE_LEFT_DOWN_PREDICTION = 69;
var MOUSE_LEFT_UP_PREDICTION = 70;
var MOUSE_LEFT_CLICK_PREDICTION = 71;

var CURSORS_BY_CODE = [
  "alias",
  "all-scroll",
  "auto",
  "cell",
  "context-menu",
  "col-resize",
  "copy",
  "crosshair",
  "default",
  "e-resize",
  "ew-resize",
  "help",
  "move",
  "n-resize",
  "ne-resize",
  "nw-resize",
  "ns-resize",
  "no-drop",
  "none",
  "not-allowed",
  "pointer",
  "progress",
  "row-resize",
  "s-resize",
  "se-resize",
  "sw-resize",
  "text",
  "vertical-text",
  "w-resize",
  "wait",
  "zoom-in",
  "zoom-out"
];

// TODO track removed components

var positions = [];
var viewports = [];
var clipSizes = [];
var lookVectors = [];
var childCounts = [];
var booleanStateFlags = [];
var stringPools = [];

var paintAllSequence;

var absPositions = [];

var STATE_FLAGS_VISIBILITY_MASK = 1;
var STATE_FLAGS_POPUP_MASK = 2;
var STATE_FLAGS_ROLLOVER_DISABLED_MASK = 4;

// 2 bytes
function readShort(stream, c)
{
    var n = stream[c] + (stream[c+1] << 8);
    return n;
}

// 3 bytes
function readRect(stream, c)
{
    var w = stream[c] + ((stream[c+2] & 0x0F) << 8);
    var h = stream[c+1] + ((stream[c+2] & 0xF0) << 4);

    return {w: w, h: h}
}

// 3 bytes
function readRectInv(stream, c)
{
    var w = stream[c] + ((stream[c+2] & 0x0F) << 8);
    var h = stream[c+1] + ((stream[c+2] & 0xF0) << 4);

    return {w: -w, h: -h}
}

// 3 bytes
function readClipRect(stream, c)
{
    var w = stream[c] + ((stream[c+2] & 0x0F) << 8);
    var h = stream[c+1] + ((stream[c+2] & 0xF0) << 4);

    return {x: 0, y: 0, w: w, h: h}
}

function checkFlagForComponent(index, mask)
{
    return ((booleanStateFlags[index] & mask) === mask);
}

function paintComponent(stream, c)
{
    var index = readShort(stream, c);
    c+=2;

    try
    {
        var visible = checkFlagForComponent(index, STATE_FLAGS_VISIBILITY_MASK);

        // Find out child count
        var childCount = childCounts[index];

        //console.log("paintComponent:  index=" + index + " childCount=" + childCount);

        if (visible || (childCount > 0))
        {
            // Push current clip
            pushCurrentClip();

            // Position transformation
            var positionMatrix = positions[index];
            if (positionMatrix)
            {
                applyTransform(positionMatrix);

                absPositions[index] = currentTransform;
            }
            else
            {
                console.log("Position matrix undefined for " + index);
            }

            // Clip rect (or set entirely new clip if this is popup)
            var clipSize = clipSizes[index];
            if (clipSize)
            {
                var popup = checkFlagForComponent(index, STATE_FLAGS_POPUP_MASK);
                if (popup)
                {
                    setClip(clipSize);
                }
                else
                {
                    clipRect(clipSize);
                }
            }
            else
            {
                console.log("Clip undefined for " + index);
            }

            // Viewport transformation
            var viewportMatrix = viewports[index];
            if (viewportMatrix)
            {
                applyTransform(viewportMatrix);
            }
            else
            {
                console.log("Viewport matrix undefined for " + index);
            }

            // Paint component

            if (visible)
            {
                var lookVector = lookVectors[index];
                if (lookVector)
                {
                    if (lookVector.length > 0)
                    {
                        decodeLookVector(index, lookVector, lookVector.length);
                    }
                }
                else
                {
                   console.log("Look undefined for " + index);
                }
            }

            // Paint children
            for (var i=0; i<childCount; i++)
            {
                c = paintComponent(stream, c);
            }

            // Inverse viewport transformation
            if (viewportMatrix)
            {
                applyInverseTransform(viewportMatrix);
            }

            // Inverse position transformation
            if (positionMatrix)
            {
                applyInverseTransform(positionMatrix);
            }

            // Pop current clip
            popCurrentClip();
        }
        return c;
    }
    catch(e)
    {
        console.log("Error painting component with index = " + index + ": " + e.message);
        throw(e);
    }
}

function repaintWholeCache()
{
    paintComponent(paintAllSequence, 1)
}

function decodeCommandVector(stream, byteLength)
{
    if (byteLength == 0)
    {
        return;
    }

    var codeObj;
    var opcodeBase = stream[0];
    var c = 1;

    switch (opcodeBase)
    {
        case POSITION_MATRIX_MAP_COMMAND_CODE:
            while (c < byteLength)
            {
                var index = readShort(stream, c);
                c+=2;
                var position = readRect(stream, c);
                c+=3;
                positions[index] = position;
            }
            break;
        case VIEWPORT_MATRIX_MAP_COMMAND_CODE:
            while (c < byteLength)
            {
                var index = readShort(stream, c);
                c+=2;
                var viewport = readRectInv(stream, c);
                c+=3;
                viewports[index] = viewport;
            }
            break;
        case CLIP_SIZE_MAP_COMMAND_CODE:
            while (c < byteLength)
            {
                var index = readShort(stream, c);
                c+=2;
                var clipSize = readClipRect(stream, c);
                c+=3;
                clipSizes[index] = clipSize;
            }
            break;
        case LOOK_VECTOR_MAP_COMMAND_CODE:
            while (c < byteLength)
            {
                var index = readShort(stream, c);
                c+=2;
                var lookVectorSize = readShort(stream, c);
                c+=2;

                var lookVector = [];
                for (i=0; i<lookVectorSize; i++)
                {
                    lookVector[i] = stream[c+i];
                }
                c += lookVectorSize;
                lookVectors[index] = lookVector;
            }
            break;
        case CHILD_COUNT_MAP_COMMAND_CODE:
            while (c < byteLength)
            {
                var index = readShort(stream, c);
                c+=2;
                var childCount = readShort(stream, c);
                c+=2;
                childCounts[index] = childCount;
            }
            break;
        case BOOLEAN_STATE_FLAGS_COMMAND_CODE:
            while (c < byteLength)
            {
                var index = readShort(stream, c);
                c+=2;
                booleanStateFlags[index] = stream[c];
                c++;
            }
            break;
        case STRING_POOL_MAP_COMMAND_CODE:
            while (c < byteLength)
            {
                var index = readShort(stream, c);
                c+=2;
                var sCount = stream[c];
                c++;
                for (var i=0; i<sCount; i++)
                {
                    var sIndex = stream[c];
                    c++;
                    var sSize = readShort(stream, c);
                    c+=2;
                    var str = "";
                    for (var j=0; j<sSize; j++)
                    {
                        str += String.fromCharCode(stream[c+j]);
                    }
                    c+=sSize;

                    if (!stringPools[index])
                    {
                        stringPools[index] = [];
                    }
                    stringPools[index][sIndex] = str;
                }
            }
            break;
        case PAINT_ALL_LIST_COMMAND_CODE:
            paintAllSequence = stream;
            while (c < byteLength)
            {
                c = paintComponent(stream, c);
            }
            break;
        case REPAINT_CACHED_COMMAND_CODE:
            repaintWholeCache();
            break;
        case SET_CURSOR_COMMAND_CODE:
            var cursorCode = stream[c];
            c++;
            canvas.style.cursor = CURSORS_BY_CODE[cursorCode];
            break;
        case PUSH_TEXT_TO_CLIPBOARD:
            var sSize = readShort(stream, c);
            c+=2;
            var str = "";
            for (var j=0; j<sSize; j++)
            {
                str += String.fromCharCode(stream[c+j]);
            }
            c+=sSize;
            pendingServerClipboardObject = str;
            if (userRequestsDataExport)
            {
                window.prompt("Copy to clipboard: Ctrl+C, Enter", pendingServerClipboardObject);
                userRequestsDataExport = false;
            }
            break;
        default:
           throw new Error("Unknown command code: " + stream[0]);
    }
}

var messages = document.getElementById("messages");

function displayUserTextMessage(msg, x, y)
{
    ctx.beginPath();
    ctx.fillStyle="#FFFFFF";
    ctx.strokeStyle="#FFFFFF";
    ctx.fillText(msg, x, y);
    ctx.closePath();
}

function displayStatus(msg)
{
    //messages.innerHTML = "Connection status: " + msg;
}

/**
 * WebSocket
 */

var webSocket;
var connectionOpen;

var transmissionMode = FINISH_PREDICTION_TRANSMISSION;

var mouseDownPredictionDatas = [];
var mouseDownPredictionDataSizes = [];
var mouseDownPredictionCounter = 0;

var mouseUpPredictionDatas = [];
var mouseUpPredictionDataSizes = [];
var mouseUpPredictionCounter = 0;

var mouseClickPredictionDatas = [];
var mouseClickPredictionDataSizes = [];
var mouseClickPredictionCounter = 0;

function openSocket()
{
    displayUserTextMessage("Establishing connection...", 10, 20);
    displayStatus("establishing...");

    if(webSocket !== undefined && webSocket.readyState !== WebSocket.CLOSED)
    {
        return;
    }

    webSocket = new WebSocket(serverWebSocketUri);

    webSocket.binaryType = "arraybuffer";

    webSocket.onopen = function(event)
    {
        connectionOpen = true;
        displayUserTextMessage("Open.", 10, 30);
        displayStatus("open");

        handleResize(null);
    };

    webSocket.onmessage = function(event)
    {
        if (event.data.byteLength)
        {
            if (connectionOpen)
            {
                //var time = Date.now();
                //console.log("Received responce " + time);

                var dataBuffer = new Uint8Array(event.data);

                if (event.data.byteLength > 0 && dataBuffer[0] >= TRANSMISSION_MODE_FIRST && dataBuffer[0] <= TRANSMISSION_MODE_LAST)
                {
                    transmissionMode = dataBuffer[0];
                    switch (transmissionMode)
                    {
                        case MOUSE_LEFT_DOWN_PREDICTION:
                             mouseDownPredictionCounter = 0;
                             break;
                        case MOUSE_LEFT_UP_PREDICTION:
                             mouseUpPredictionCounter = 0;
                             break;
                        case MOUSE_LEFT_CLICK_PREDICTION:
                             mouseClickPredictionCounter = 0;
                             break;
                    }
                }
                else
                {
                    if (transmissionMode == FINISH_PREDICTION_TRANSMISSION)
                    {
//                        if (event.data.byteLength > 0 && mouseDown)
//                        {
//                            console.log("Drag bytes received: " + event.data.byteLength);
//                        }

                        decodeCommandVector(dataBuffer, event.data.byteLength);
                    }
                    else
                    {
                        switch (transmissionMode)
                        {
                            case MOUSE_LEFT_DOWN_PREDICTION:
                                 mouseDownPredictionDatas[mouseDownPredictionCounter] = dataBuffer;
                                 mouseDownPredictionDataSizes[mouseDownPredictionCounter] = event.data.byteLength;
                                 mouseDownPredictionCounter++;
                                 //console.log("Received predictions for mouse down: " + event.data.byteLength);
                                 break;
                            case MOUSE_LEFT_UP_PREDICTION:
                                 mouseUpPredictionDatas[mouseUpPredictionCounter] = dataBuffer;
                                 mouseUpPredictionDataSizes[mouseUpPredictionCounter] = event.data.byteLength;
                                 mouseUpPredictionCounter++;
                                 //console.log("Received predictions for mouse up: " + event.data.byteLength);
                                 break;
                            case MOUSE_LEFT_CLICK_PREDICTION:
                                 mouseClickPredictionDatas[mouseClickPredictionCounter] = dataBuffer;
                                 mouseClickPredictionDataSizes[mouseClickPredictionCounter] = event.data.byteLength;
                                 mouseClickPredictionCounter++;
                                 //console.log("Received predictions for mouse click: " + event.data.byteLength);
                                 break;
                        }
                    }
                }

                //time = Date.now();
                //console.log("Rendered responce " + time + " cmd =" + dataBuffer[0]);
            }
            else
            {
                displayUserTextMessage("Connection to remote server is closed. Please reload.", 10, 50);
                displayStatus("closed");
            }
        }
        else
        {
            displayUserTextMessage(event.data, 10, 30);
        }
    };

    webSocket.onclose = function(event)
    {
        displayUserTextMessage("Connection to remote server is closed. Please reload.", 10, 50);
        displayStatus("closed");
        connectionOpen = false;
    };

    webSocket.onerror = function(event)
    {
        displayUserTextMessage("Connection error. Please reload.", 10, 70);
        displayStatus("error, closed");
        connectionOpen = false;
    };
}


/**
 * Input events
 */

function sendEventToServer(evt)
{
    //var time = Date.now();
    //console.log("Before sending " + time);

    if (connectionOpen)
    {
        webSocket.send(evt);
    }

    //time = Date.now();
    //console.log("After sending " + time);
}

var mouseDown = false;
var lastMouseX = -1;
var lastMouseY = -1;
var lastMouseDragTime = 0;
var lastUnprocessedMouseDrag;
var lastUnprocessedMouseMove;
var lastIndexUnderMouse = -1;
var lastMousePosWasOnEdge = false;
var MOUSE_INTERVAL_MILLIS = 50;

function getEncodedMouseEvent(x, y, id)
{
    var bytearray = new Uint8Array(4);

    bytearray[0] = id - 400;
    bytearray[1] = x & 0xFF;
    bytearray[2] = y & 0xFF;
    bytearray[3] = ((x >> 4) & 0xF0) | ((y >> 8) & 0x0F);

    return bytearray.buffer;
}

function storeMouseEventAndGetEncoded(evt, id)
{
    var rect = canvas.getBoundingClientRect();
    var x = evt.clientX - rect.left;
    var y = evt.clientY - rect.top;

    lastMouseX = x;
    lastMouseY = y;

    return getEncodedMouseEvent(x, y, id);
}

function sendMouseDownEventToServer(evt)
{
    if (transmissionMode == FINISH_PREDICTION_TRANSMISSION && mouseDownPredictionCounter > 0)
    {
        console.log("mouse down - hit prediction (" + mouseDownPredictionCounter + " predictions)");
        for (var i=0; i<mouseDownPredictionCounter; i++)
        {
            decodeCommandVector(mouseDownPredictionDatas[i], mouseDownPredictionDataSizes[i]);
        }
        mouseDownPredictionDatas = [];
        mouseDownPredictionDataSizes = [];
        mouseDownPredictionCounter = 0;
    }
    else
    {
        console.log("mouse down - missed prediction");
    }
    mouseDown = true;
    sendEventToServer(storeMouseEventAndGetEncoded(evt, 501));
}

function commitLastUnprocessedMouseMove()
{
    if (lastUnprocessedMouseMove)
    {
        sendEventToServer(storeMouseEventAndGetEncoded(lastUnprocessedMouseMove, 503));
        lastUnprocessedMouseMove = null;
    }
}

function commitLastUnprocessedMouseDrag()
{
    if (lastUnprocessedMouseDrag)
    {
        sendEventToServer(storeMouseEventAndGetEncoded(lastUnprocessedMouseDrag, 506));
        lastUnprocessedMouseDrag = null;
    }
}

function commitPendingMouseEvents()
{
    commitLastUnprocessedMouseMove();
    commitLastUnprocessedMouseDrag();
}

function sendMouseUpEventToServer(evt)
{
    if (transmissionMode == FINISH_PREDICTION_TRANSMISSION && mouseUpPredictionCounter > 0)
    {
        console.log("mouse Up - hit prediction (" + mouseUpPredictionCounter + " predictions)");
        for (var i=0; i<mouseUpPredictionCounter; i++)
        {
            decodeCommandVector(mouseUpPredictionDatas[i], mouseUpPredictionDataSizes[i]);
        }
        mouseUpPredictionDatas = [];
        mouseUpPredictionDataSizes = [];
        mouseUpPredictionCounter = 0;
    }
    else
    {
        console.log("mouse Up - missed prediction");
    }
    commitLastUnprocessedMouseDrag();
    mouseDown = false;
    sendEventToServer(storeMouseEventAndGetEncoded(evt, 502));
}

function sendMouseClickEventToServer(evt)
{
    if (transmissionMode == FINISH_PREDICTION_TRANSMISSION && mouseClickPredictionCounter > 0)
    {
        console.log("mouse Click - hit prediction (" + mouseClickPredictionCounter + " predictions)");
        for (var i=0; i<mouseClickPredictionCounter; i++)
        {
            decodeCommandVector(mouseClickPredictionDatas[i], mouseClickPredictionDataSizes[i]);
        }
        mouseClickPredictionDatas = [];
        mouseClickPredictionDataSizes = [];
        mouseClickPredictionCounter = 0;
    }
    else
    {
        console.log("mouse Click - missed prediction");
    }
    sendEventToServer(storeMouseEventAndGetEncoded(evt, 500));
}

function isComponentReadyForMouseRollover(i)
{
    return absPositions[i] && !checkFlagForComponent(i, STATE_FLAGS_ROLLOVER_DISABLED_MASK);
}

function sendMouseMoveEventToServer(evt)
{
    if (evt.preventDefault)
    {
        evt.preventDefault();
    }

    var rect = canvas.getBoundingClientRect();
    var x = evt.clientX - rect.left;
    var y = evt.clientY - rect.top;

    if ( x != lastMouseX || y != lastMouseY)
    {
        // Any click predictions are not valid any more
        mouseDownPredictionDatas = [];
        mouseDownPredictionDataSizes = [];
        mouseDownPredictionCounter = 0;
        mouseUpPredictionDatas = [];
        mouseUpPredictionDataSizes = [];
        mouseUpPredictionCounter = 0;
        mouseClickPredictionDatas = [];
        mouseClickPredictionDataSizes = [];
        mouseClickPredictionCounter = 0;

        if (mouseDown)
        {
            var nowTime = Date.now();
            if (nowTime - lastMouseDragTime > MOUSE_INTERVAL_MILLIS)
            {
                lastUnprocessedMouseDrag = null;
                sendEventToServer(storeMouseEventAndGetEncoded(evt, 506));
            }
            else
            {
                lastUnprocessedMouseDrag = evt;
            }
            lastMouseDragTime = nowTime;
        }
        else
        {
            var indexUnderMouse;
            var onEdge = false;
            var t = function(a,b) {return Math.abs(a-b) < 2;};
            // Iterate from the end to hit tompost children first. Root is always at i=0.
            for (var i=absPositions.length-1; i>=0; i--)
            {
                if (isComponentReadyForMouseRollover(i))
                {
                    var ix = absPositions[i][0][2];
                    var iy = absPositions[i][1][2];

                    if (x >= ix && y >= iy
                        && x < ix+clipSizes[i].w && y < iy+clipSizes[i].h)
                    {
                        indexUnderMouse = i;
                        onEdge = t(x, ix) || t(y, iy) || t(x, ix+clipSizes[i].w-1) || t(y, iy+clipSizes[i].h-1);
                        break;
                    }
                }
            }

            if (!onEdge && lastIndexUnderMouse && isComponentReadyForMouseRollover(lastIndexUnderMouse))
            {
                var i = lastIndexUnderMouse;
                var ix = absPositions[i][0][2];
                var iy = absPositions[i][1][2];
                onEdge = t(x, ix) || t(y, iy) || t(x, ix+clipSizes[i].w-1) || t(y, iy+clipSizes[i].h-1);
            }

            if (indexUnderMouse !== lastIndexUnderMouse || lastMousePosWasOnEdge || onEdge)
            {
                sendEventToServer(getEncodedMouseEvent(lastMouseX, lastMouseY, 503));
                sendEventToServer(storeMouseEventAndGetEncoded(evt, 503));

                lastIndexUnderMouse = indexUnderMouse;
                lastMousePosWasOnEdge = onEdge;
            }
            else
            {
                lastUnprocessedMouseMove = evt;
            }
        }
    }
}

window.setInterval(commitPendingMouseEvents, MOUSE_INTERVAL_MILLIS * 5);

var CLIPBOARD_PASTE_EVENT_CODE = 403;
var CLIPBOARD_COPY_EVENT_CODE = 404;

function handlePaste(evt)
{
    var text;

    var eData = evt.clipboardData.getData('text/plain');

    if (evt && evt.clipboardData && evt.clipboardData.getData)
    {
        if (pendingServerClipboardObject)
        {
            if (userHasNavigatedOut && eData != lastExternalClipboardObject)
            {
                // External clipboard content has changed when user navigated out of the window and then back
                lastExternalClipboardObject = eData;
                text = eData;
                pendingServerClipboardObject = null;
            }
            else
            {
                text = pendingServerClipboardObject;
                lastExternalClipboardObject = eData;
            }
        }
        else
        {
            text = eData;
        }
    }
    else
    {
        text = pendingServerClipboardObject;
    }

    // Just decided which text to paste, so this flag is not needed any more
    userHasNavigatedOut = false;

    if (text && text.length)
    {
        var bytearray = new Uint8Array(3 + text.length);

        bytearray[0] = CLIPBOARD_PASTE_EVENT_CODE - 400;
        bytearray[1] = text.length & 0xFF;
        bytearray[2] = ((text.length & 0xFF00) >> 8);

        for (var i=0; i<text.length; i++)
        {
            bytearray[3+i] = text.charCodeAt(i);
        }

        sendEventToServer(bytearray.buffer);
    }
}

function handleCopyEvent()
{
    // Here we don't know what has to be copied yet. We just sent copy event to server.
    var bytearray = new Uint8Array(1);
    bytearray[0] = CLIPBOARD_COPY_EVENT_CODE - 400;
    sendEventToServer(bytearray.buffer);
}

window.addEventListener("focus", function(e){userHasNavigatedOut=true;}, false);

canvas.addEventListener("mousedown", sendMouseDownEventToServer, false);
canvas.addEventListener("mouseup", sendMouseUpEventToServer, false);
canvas.addEventListener("click", sendMouseClickEventToServer, false);
canvas.addEventListener("mousemove", sendMouseMoveEventToServer, false);

window.addEventListener("paste", handlePaste, false);
window.addEventListener("copy", handleCopyEvent, false);

canvas.ondragstart = function(e)
{
    if (e && e.preventDefault) { e.preventDefault(); }
    if (e && e.stopPropagation) { e.stopPropagation(); }
    return false;
}

canvas.onselectstart = function(e)
{
    if (e && e.preventDefault) { e.preventDefault(); }
    if (e && e.stopPropagation) { e.stopPropagation(); }
    return false;
}

function getEncodedKeyEvent(evt, id)
{
    var bytearray = new Uint8Array(5);

    bytearray[0] = id - 400;
    bytearray[1] = evt.keyCode & 0xFF
    bytearray[2] = ((evt.keyCode & 0xFF00) >> 8)
    bytearray[3] = evt.charCode & 0xFF
    bytearray[4] = ((evt.charCode & 0xFF00) >> 8)

    return bytearray.buffer;
}

function sendKeyDownEventToServer(evt)
{
    sendEventToServer(getEncodedKeyEvent(evt, 401));
    if (evt.preventDefault && (evt.keyCode == 9 || evt.keyCode == 8 || evt.keyCode == 36 || evt.keyCode == 35)) // Do not let browser process TAB, Backspace, Home, End
    {
        evt.preventDefault();
        evt.stopPropagation();
    }
}

function handleKeyDownEvent(evt)
{
    // Special handling for CTRL+ALT+SHIFT+C: give user chance to copy text to system clipboard
    if(evt.shiftKey && evt.altKey && evt.ctrlKey && evt.keyCode == 67)
    {
        userRequestsDataExport = true;
        handleCopyEvent();
    }
    sendKeyDownEventToServer(evt);
}

function sendKeyUpEventToServer(evt)
{
//    if (evt.keyCode == 9 && evt.ctrlKey) // This may be used for emulating Ctrl+Tab
//    {
//        sendEventToServer(getEncodedKeyEvent(evt, 401));
//    }
    sendEventToServer(getEncodedKeyEvent(evt, 402));
    if (evt.preventDefault && (evt.keyCode == 9 || evt.keyCode == 8 || evt.keyCode == 36 || evt.keyCode == 35)) // Do not let browser process TAB, Backspace, Home, End
    {
        evt.preventDefault();
        evt.stopPropagation();
    }
}

function sendKeyPressEventToServer(evt)
{
    sendEventToServer(getEncodedKeyEvent(evt, 400));
}

window.addEventListener("keydown", handleKeyDownEvent, false);
window.addEventListener("keyup", sendKeyUpEventToServer, false);
window.addEventListener("keypress", sendKeyPressEventToServer, false);


// Adjust canvas size for window size, and send host-resize event to server

var HOST_RESIZE_EVENT_CODE = 406;

function getEncodedHostResizeEvent()
{
    var bytearray = new Uint8Array(5);

    bytearray[0] = HOST_RESIZE_EVENT_CODE - 400;
    bytearray[1] = window.innerWidth & 0xFF
    bytearray[2] = ((window.innerWidth & 0xFF00) >> 8)
    bytearray[3] = window.innerHeight & 0xFF
    bytearray[4] = ((window.innerHeight & 0xFF00) >> 8)

    return bytearray.buffer;
}

// Remove scroll bars
document.documentElement.style.overflow = 'hidden';  // firefox, chrome
document.body.scroll = "no"; // ie only

// Start streaming

openSocket();