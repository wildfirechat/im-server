package com.xiaoleilu.loServer.handler;

import io.netty.util.internal.StringUtil;
import com.xiaoleilu.hutool.util.FileUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_RANGE;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


public class HttpResponseHelper {

private static final Logger logger = LoggerFactory.getLogger(HttpResponseHelper.class);

/**
 * sendResponse
 *
 * @param ctx
 * @param status
 */
public static void sendResponse(String requestId, ChannelHandlerContext ctx, HttpResponseStatus status) {
    sendResponse(requestId, ctx, status, "status: " + status.toString() + "\r\n");
    }

/**
 * sendResponse
 *
 * @param ctx
 * @param status
 * @param msg
 */
public static void sendResponse(String requestId, ChannelHandlerContext ctx, HttpResponseStatus status, String msg) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    removeChannelHandlerContext(requestId, ctx);
    }

/**
 * sendResponse（成功应答需要注意isKeepAlive和isCloseChannel的值）
 *
 * @param requestId
 * @param ctx
 * @param status
 * @param contentType
 * @param respBodyString
 * @param isKeepAlive
 * @param isCloseChannel
 */
public static void sendResponse(String requestId, ChannelHandlerContext ctx, HttpResponseStatus status, String contentType, String respBodyString, boolean isKeepAlive, boolean isCloseChannel) {
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(respBodyString, CharsetUtil.UTF_8));
    if (!StringUtil.isNullOrEmpty(contentType)) {
    response.headers().set(CONTENT_TYPE, contentType);
    } else {
    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
    }

    if (isKeepAlive) {
    response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");

    ChannelFuture future = ctx.channel().writeAndFlush(response);
    if (!isKeepAlive) {
    future.addListener(ChannelFutureListener.CLOSE);
    }

    removeChannelHandlerContext(requestId, ctx, isCloseChannel);
    }

/**
 * sendResponse
 *
 * @param ctx
 * @param statusCode
 * @param resultDesc
 */
public static void sendResponse(String requestId, ChannelHandlerContext ctx, int statusCode, String resultDesc) {
    HttpResponseStatus httpResponseStatus = new HttpResponseStatus(statusCode, resultDesc);
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, httpResponseStatus, Unpooled.copiedBuffer(resultDesc, CharsetUtil.UTF_8));
    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    removeChannelHandlerContext(requestId, ctx);
    }

/**
 * 断点上传204应答
 *
 * @param ctx
 * @param contentRange
 */
public static void sendBrokenUpload204Resonse(String requestId, ChannelHandlerContext ctx, String contentRange, boolean isKeepAlive, boolean isCloseChannel) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NO_CONTENT, Unpooled.copiedBuffer("", CharsetUtil.UTF_8));
    response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    response.headers().set(CONTENT_RANGE, contentRange);

    ChannelFuture future = ctx.channel().writeAndFlush(response);
    if (!isKeepAlive) {
    future.addListener(ChannelFutureListener.CLOSE);
    }

    removeChannelHandlerContext(requestId, ctx, isCloseChannel);
    }


public static String getMultipartUploadResponseBody(String remoteFileName, String fileUrl) throws Exception {

    return "{\"rc_url\":{\"path\":\"" + fileUrl + "\",\"type\":0}}";
}

/**
 * getContentType
 *
 * @param fileName
 * @return
 */

public static String getFileExt(String fileName) {
    int index = fileName.lastIndexOf(".");
    if (index == -1) {
        return "";
    }

    return fileName.substring(index + 1).toLowerCase();
}

public static String getContentType(String fileName) {
    String fileExtName = getFileExt(fileName);
    if (fileExtName.equals("png")) { // MimetypesFileTypeMap目前缺少png类型
    return "image/png";
    } else {
    return "application/octet-stream";
    }
    }

/**
 * removeChannelHandlerContext
 *
 * @param requestId
 * @param ctx
 */
private static void removeChannelHandlerContext(String requestId, ChannelHandlerContext ctx) {
    removeChannelHandlerContext(requestId, ctx, true);
    }

private static void removeChannelHandlerContext(String requestId, ChannelHandlerContext ctx, boolean isCloseChannel) {
    try {
    if (!StringUtil.isNullOrEmpty(requestId)) {
//    HttpFileServerController.getInstance().removeChannelHandlerContext(requestId);
    }

    if (isCloseChannel)
    ctx.channel().close();
    } catch (Exception e) {
    logger.error("close error!", e);
    }
    }
    }

