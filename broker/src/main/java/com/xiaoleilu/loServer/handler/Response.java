package com.xiaoleilu.loServer.handler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import com.xiaoleilu.hutool.http.HttpUtil;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.StaticLog;
import com.xiaoleilu.hutool.util.CharsetUtil;
import com.xiaoleilu.hutool.util.DateUtil;
import com.xiaoleilu.hutool.util.StrUtil;
import com.xiaoleilu.loServer.ServerSetting;
import com.xiaoleilu.loServer.listener.FileProgressiveFutureListener;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.slf4j.LoggerFactory;

/**
 * 响应对象
 * 
 * @author Looly
 *
 */
public class Response {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(Response.class);

	/** 返回内容类型：普通文本 */
	public final static String CONTENT_TYPE_TEXT = "text/plain";
	/** 返回内容类型：HTML */
	public final static String CONTENT_TYPE_HTML = "text/html";
	/** 返回内容类型：XML */
	public final static String CONTENT_TYPE_XML = "text/xml";
	/** 返回内容类型：JAVASCRIPT */
	public final static String CONTENT_TYPE_JAVASCRIPT = "application/javascript";
	/** 返回内容类型：JSON */
	public final static String CONTENT_TYPE_JSON = "application/json";
	public final static String CONTENT_TYPE_JSON_IE = "text/json";

	private ChannelHandlerContext ctx;
	private Request request;

	private HttpVersion httpVersion = HttpVersion.HTTP_1_1;
	private HttpResponseStatus status = HttpResponseStatus.OK;
	private String contentType = CONTENT_TYPE_HTML;
	private String charset = ServerSetting.getCharset();
	private HttpHeaders headers = new DefaultHttpHeaders();
	private Set<Cookie> cookies = new HashSet<Cookie>();
	private Object content = Unpooled.EMPTY_BUFFER;
	//发送完成标记
	private boolean isSent;

	public Response(ChannelHandlerContext ctx, Request request) {
		this.ctx = ctx;
		this.request = request;
	}

	/**
	 * 设置响应的Http版本号
	 * 
	 * @param httpVersion http版本号对象
	 * @return 自己
	 */
	public Response setHttpVersion(HttpVersion httpVersion) {
		this.httpVersion = httpVersion;
		return this;
	}

	/**
	 * 响应状态码<br>
	 * 使用io.netty.handler.codec.http.HttpResponseStatus对象
	 * 
	 * @param status 状态码
	 * @return 自己
	 */
	public Response setStatus(HttpResponseStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * 响应状态码
	 * 
	 * @param status 状态码
	 * @return 自己
	 */
	public Response setStatus(int status) {
		return setStatus(HttpResponseStatus.valueOf(status));
	}

	/**
	 * 设置Content-Type
	 * 
	 * @param contentType Content-Type
	 * @return 自己
	 */
	public Response setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	/**
	 * 设置返回内容的字符集编码
	 * 
	 * @param charset 编码
	 * @return 自己
	 */
	public Response setCharset(String charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * 增加响应的Header<br>
	 * 重复的Header将被叠加
	 * 
	 * @param name 名
	 * @param value 值，可以是String，Date， int
	 * @return 自己
	 */
	public Response addHeader(String name, Object value) {
		headers.add(name, value);
		return this;
	}

	/**
	 * 设置响应的Header<br>
	 * 重复的Header将被替换
	 * 
	 * @param name 名
	 * @param value 值，可以是String，Date， int
	 * @return 自己
	 */
	public Response setHeader(String name, Object value) {
		headers.set(name, value);
		return this;
	}

	/**
	 * 设置响应体长度
	 * 
	 * @param contentLength 响应体长度
	 * @return 自己
	 */
	public Response setContentLength(long contentLength) {
		setHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), contentLength);
		return this;
	}

	/**
	 * 设置是否长连接
	 * 
	 * @return 自己
	 */
	public Response setKeepAlive() {
		setHeader(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString());
		return this;
	}

	// --------------------------------------------------------- Cookie start
	/**
	 * 设定返回给客户端的Cookie
	 * 
	 * @param cookie
	 * @return 自己
	 */
	public Response addCookie(Cookie cookie) {
		cookies.add(cookie);
		return this;
	}

	/**
	 * 设定返回给客户端的Cookie
	 * 
	 * @param name Cookie名
	 * @param value Cookie值
	 * @return 自己
	 */
	public Response addCookie(String name, String value) {
		return addCookie(new DefaultCookie(name, value));
	}

	/**
	 * 设定返回给客户端的Cookie
	 * 
	 * @param name cookie名
	 * @param value cookie值
	 * @param maxAgeInSeconds -1: 关闭浏览器清除Cookie. 0: 立即清除Cookie. n>0 : Cookie存在的秒数.
	 * @param path Cookie的有效路径
	 * @param domain the Cookie可见的域，依据 RFC 2109 标准
	 * @return 自己
	 */
	public Response addCookie(String name, String value, int maxAgeInSeconds, String path, String domain) {
		Cookie cookie = new DefaultCookie(name, value);
		if (domain != null) {
			cookie.setDomain(domain);
		}
		cookie.setMaxAge(maxAgeInSeconds);
		cookie.setPath(path);
		return addCookie(cookie);
	}

	/**
	 * 设定返回给客户端的Cookie<br>
	 * Path: "/"<br>
	 * No Domain
	 * 
	 * @param name cookie名
	 * @param value cookie值
	 * @param maxAgeInSeconds -1: 关闭浏览器清除Cookie. 0: 立即清除Cookie. n>0 : Cookie存在的秒数.
	 * @return 自己
	 */
	public Response addCookie(String name, String value, int maxAgeInSeconds) {
		return addCookie(name, value, maxAgeInSeconds, "/", null);
	}
	// --------------------------------------------------------- Cookie end

	/**
	 * 设置响应HTML文本内容
	 * 
	 * @param contentText 响应的文本
	 * @return 自己
	 */
	public Response setContent(String contentText) {
		this.content = Unpooled.copiedBuffer(contentText, Charset.forName(charset));
		return this;
	}
	
	/**
	 * 设置响应文本内容
	 * 
	 * @param contentText 响应的文本
	 * @return 自己
	 */
	public Response setTextContent(String contentText) {
		setContentType(CONTENT_TYPE_TEXT);
		return setContent(contentText);
	}
	
	/**
	 * 设置响应JSON文本内容
	 * 
	 * @param contentText 响应的JSON文本
	 * @return 自己
	 */
	public Response setJsonContent(String contentText) {
		setContentType(request.isIE() ? CONTENT_TYPE_JSON : CONTENT_TYPE_JSON);
		return setContent(contentText);
	}
	
	/**
	 * 设置响应XML文本内容
	 * 
	 * @param contentText 响应的XML文本
	 * @return 自己
	 */
	public Response setXmlContent(String contentText) {
		setContentType(CONTENT_TYPE_XML);
		return setContent(contentText);
	}

	/**
	 * 设置响应文本内容
	 * 
	 * @param contentBytes 响应的字节
	 * @return 自己
	 */
	public Response setContent(byte[] contentBytes) {
		return setContent(Unpooled.copiedBuffer(contentBytes));
	}

	/**
	 * 设置响应文本内容
	 * 
	 * @param byteBuf 响应的字节
	 * @return 自己
	 */
	public Response setContent(ByteBuf byteBuf) {
		this.content = byteBuf;
		return this;
	}
	
	/**
	 * 设置响应到客户端的文件
	 * 
	 * @param file 文件
	 * @return 自己
	 */
	public Response setContent(File file) {
		this.content = file;
		return this;
	}

	/**
	 * Sets the Date and Cache headers for the HTTP Response
	 *
	 * @param response HTTP response
	 * @param fileToCache file to extract content type
	 */
	/**
	 * 设置日期和过期时间
	 * 
	 * @param lastModify 上一次修改时间
	 * @param httpCacheSeconds 缓存时间，单位秒
	 */
	public void setDateAndCache(long lastModify, int httpCacheSeconds) {
		SimpleDateFormat formatter = new SimpleDateFormat(DateUtil.HTTP_DATETIME_PATTERN, Locale.US);
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

		// Date header
		Calendar time = new GregorianCalendar();
		setHeader(HttpHeaderNames.DATE.toString(), formatter.format(time.getTime()));

		// Add cache headers
		time.add(Calendar.SECOND, httpCacheSeconds);

		setHeader(HttpHeaderNames.EXPIRES.toString(), formatter.format(time.getTime()));
		setHeader(HttpHeaderNames.CACHE_CONTROL.toString(), "private, max-age=" + httpCacheSeconds);
		setHeader(HttpHeaderNames.LAST_MODIFIED.toString(), formatter.format(DateUtil.date(lastModify)));
	}

	// -------------------------------------------------------------------------------------- build HttpResponse start
	/**
	 * 转换为Netty所用Response<br>
	 * 不包括content，一般用于返回文件类型的响应
	 * 
	 * @return DefaultHttpResponse
	 */
	private DefaultHttpResponse toDefaultHttpResponse() {
		final DefaultHttpResponse defaultHttpResponse = new DefaultHttpResponse(httpVersion, status);

		// headers
		HttpHeaders httpHeaders = defaultHttpResponse.headers().add(headers);
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
		// Cookies
		for (Cookie cookie : cookies) {
			httpHeaders.add(HttpHeaderNames.SET_COOKIE.toString(), ServerCookieEncoder.LAX.encode(cookie));
		}

		return defaultHttpResponse;
	}

	/**
	 * 转换为Netty所用Response<br>
	 * 用于返回一般类型响应（文本）
	 * 
	 * @return FullHttpResponse
	 */
	private FullHttpResponse toFullHttpResponse() {
		final ByteBuf byteBuf = (ByteBuf)content;
		final FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion, status, byteBuf);

		// headers
		final HttpHeaders httpHeaders = fullHttpResponse.headers().add(headers);
		if ("application/octet-stream".equals(contentType)) {
            httpHeaders.set(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
        } else {
            httpHeaders.set(HttpHeaderNames.CONTENT_TYPE.toString(), StrUtil.format("{};charset={}", contentType, charset));
            httpHeaders.set(HttpHeaderNames.CONTENT_ENCODING.toString(), charset);
        }

		httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH.toString(), byteBuf.readableBytes());

		// Cookies
		for (Cookie cookie : cookies) {
			httpHeaders.add(HttpHeaderNames.SET_COOKIE.toString(), ServerCookieEncoder.LAX.encode(cookie));
		}

		return fullHttpResponse;
	}
	// -------------------------------------------------------------------------------------- build HttpResponse end

	// -------------------------------------------------------------------------------------- send start
	/**
	 * 发送响应到客户端<br>
	 * 
	 * @return ChannelFuture
	 * @throws IOException 
	 */
	public ChannelFuture send() {
		ChannelFuture channelFuture;
		if(content instanceof File){
			//文件
			File file = (File)content;
			try {
				channelFuture = sendFile(file);
			} catch (IOException e) {
                Logger.error(StrUtil.format("Send {} error!", file), e.toString());
				channelFuture = sendError(HttpResponseStatus.FORBIDDEN, "");
			}
		}else{
			//普通文本
			channelFuture = sendFull();
		}
		
		this.isSent = true;
		return channelFuture;
	}
	
	/**
	 * @return 是否已经出发发送请求，内部使用<br>
	 */
	protected boolean isSent(){
		return this.isSent;
	}
	
	/**
	 * 发送响应到客户端
	 * 
	 * @return ChannelFuture
	 */
	private ChannelFuture sendFull() {
		if (request != null && request.isKeepAlive()) {
			setKeepAlive();
			return ctx.writeAndFlush(this.toFullHttpResponse());
		} else {
			return sendAndCloseFull();
		}
	}
	
	/**
	 * 发送给到客户端并关闭ChannelHandlerContext
	 * 
	 * @return ChannelFuture
	 */
	private ChannelFuture sendAndCloseFull() {
		return ctx.writeAndFlush(this.toFullHttpResponse()).addListener(ChannelFutureListener.CLOSE);
	}
	
	/**
	 * 发送文件
	 * 
	 * @param file 文件
	 * @return ChannelFuture
	 * @throws IOException
	 */
	private ChannelFuture sendFile(File file) throws IOException {
		final RandomAccessFile raf = new RandomAccessFile(file, "r");
		
		// 内容长度
		long fileLength = raf.length();
		this.setContentLength(fileLength);
		
		//文件类型
		String contentType = HttpUtil.getMimeType(file.getName());
		if(StrUtil.isBlank(contentType)){
			//无法识别默认使用数据流
			contentType = "application/octet-stream";
		}
		this.setContentType(contentType);
		
		ctx.write(this.toDefaultHttpResponse());
		ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise())
			.addListener(FileProgressiveFutureListener.build(raf));
		
		return sendEmptyLast();
	}
	
	/**
	 * 发送结尾标记，表示发送结束
	 * @return ChannelFuture
	 */
	private ChannelFuture sendEmptyLast(){
		final ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		if (false == request.isKeepAlive()) {
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}
		
		return lastContentFuture;
	}
	// -------------------------------------------------------------------------------------- send end

	// ---------------------------------------------------------------------------- special response start

	/**
	 * 302 重定向
	 * 
	 * @param uri 重定向到的URI
	 * @return ChannelFuture
	 */
	public ChannelFuture sendRedirect(String uri) {
		return this.setStatus(HttpResponseStatus.FOUND).setHeader(HttpHeaderNames.LOCATION.toString(), uri).send();
	}

	/**
	 * 304 文件未修改
	 * 
	 * @return ChannelFuture
	 */
	public ChannelFuture sendNotModified() {
		return this.setStatus(HttpResponseStatus.NOT_MODIFIED).setHeader(HttpHeaderNames.DATE.toString(), DateUtil.formatHttpDate(DateUtil.date())).send();
	}

	/**
	 * 发送错误消息
	 * 
	 * @param status 错误状态码
	 * @param msg 消息内容
	 * @return ChannelFuture
	 */
	public ChannelFuture sendError(HttpResponseStatus status, String msg) {
		if (ctx.channel().isActive()) {
			return this.setStatus(status).setContent(msg).send();
		}
		return null;
	}

	/**
	 * 发送404 Not Found
	 * 
	 * @param msg 消息内容
	 * @return ChannelFuture
	 */
	public ChannelFuture sendNotFound(String msg) {
		return sendError(HttpResponseStatus.NOT_FOUND, msg);
	}

	/**
	 * 发送500 Internal Server Error
	 * 
	 * @param msg 消息内容
	 * @return ChannelFuture
	 */
	public ChannelFuture sendServerError(String msg) {
		return sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, msg);
	}

	// ---------------------------------------------------------------------------- special response end

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("headers:\r\n ");
		for ( Entry<String, String> entry : headers.entries()) {
			sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
		}
		sb.append("content: ").append(StrUtil.str(content, CharsetUtil.UTF_8));

		return sb.toString();
	}

	// ---------------------------------------------------------------------------- static method start
	/**
	 * 构建Response对象
	 * 
	 * @param ctx ChannelHandlerContext
	 * @param request 请求对象
	 * @return Response对象
	 */
	protected static Response build(ChannelHandlerContext ctx, Request request) {
		return new Response(ctx, request);
	}

	/**
	 * 构建Response对象，Request对象为空，将无法获得某些信息<br>
	 * 1. 无法使用长连接
	 * 
	 * @param ctx ChannelHandlerContext
	 * @return Response对象
	 */
	protected static Response build(ChannelHandlerContext ctx) {
		return new Response(ctx, null);
	}
	// ---------------------------------------------------------------------------- static method end
}
