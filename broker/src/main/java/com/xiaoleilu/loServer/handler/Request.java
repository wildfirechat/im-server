package com.xiaoleilu.loServer.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.xiaoleilu.hutool.http.HttpUtil;
import com.xiaoleilu.hutool.lang.Conver;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.StaticLog;
import com.xiaoleilu.hutool.util.CharsetUtil;
import com.xiaoleilu.hutool.util.DateUtil;
import com.xiaoleilu.hutool.util.StrUtil;
import com.xiaoleilu.hutool.util.URLUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import org.slf4j.LoggerFactory;

/**
 * Http请求对象
 * 
 * @author Looly
 *
 */
public class Request {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(Request.class);

	public static final String METHOD_DELETE = HttpMethod.DELETE.name();
	public static final String METHOD_HEAD = HttpMethod.HEAD.name();
	public static final String METHOD_GET = HttpMethod.GET.name();
	public static final String METHOD_OPTIONS = HttpMethod.OPTIONS.name();
	public static final String METHOD_POST = HttpMethod.POST.name();
	public static final String METHOD_PUT = HttpMethod.PUT.name();
	public static final String METHOD_TRACE = HttpMethod.TRACE.name();

	private static final HttpDataFactory HTTP_DATA_FACTORY = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

	private FullHttpRequest nettyRequest;

	private String path;
	private String ip;
	private Map<String, String> headers = new HashMap<String, String>();
	private Map<String, Object> params = new HashMap<String, Object>();
	private Map<String, Cookie> cookies = new HashMap<String, Cookie>();

	/**
	 * 构造
	 * 
	 * @param ctx ChannelHandlerContext
	 * @param nettyRequest HttpRequest
	 */
	private Request(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) {
		this.nettyRequest = nettyRequest;
		final String uri = nettyRequest.uri();
		this.path = URLUtil.getPath(getUri());

		this.putHeadersAndCookies(nettyRequest.headers());

		// request URI parameters
		this.putParams(new QueryStringDecoder(uri));
		if(nettyRequest.method() != HttpMethod.GET){
			HttpPostRequestDecoder decoder = null;
			try {
				decoder = new HttpPostRequestDecoder(HTTP_DATA_FACTORY, nettyRequest);
				this.putParams(decoder);
			} finally {
				if(null != decoder){
					decoder.destroy();
					decoder = null;
				}
			}
		}

		// IP
		this.putIp(ctx);
	}

	/**
	 * @return Netty的HttpRequest
	 */
	public HttpRequest getNettyRequest() {
		return this.nettyRequest;
	}

	/**
	 * 获得版本信息
	 * 
	 * @return 版本
	 */
	public String getProtocolVersion() {
		return nettyRequest.protocolVersion().text();
	}

	/**
	 * 获得URI（带参数的路径）
	 * 
	 * @return URI
	 */
	public String getUri() {
		return nettyRequest.uri();
	}

	/**
	 * @return 获得path（不带参数的路径）
	 */
	public String getPath() {
		return path;
	}

	/**
	 * 获得Http方法
	 * 
	 * @return Http method
	 */
	public String getMethod() {
		return nettyRequest.method().name();
	}

	/**
	 * 获得IP地址
	 * 
	 * @return IP地址
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * 获得所有头信息
	 * 
	 * @return 头信息Map
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * 使用ISO8859_1字符集获得Header内容<br>
	 * 由于Header中很少有中文，故一般情况下无需转码
	 * 
	 * @param headerKey 头信息的KEY
	 * @return 值
	 */
	public String getHeader(String headerKey) {
		return headers.get(headerKey);
	}

	/**
	 * @return 是否为普通表单（application/x-www-form-urlencoded）
	 */
	public boolean isXWwwFormUrlencoded() {
		return "application/x-www-form-urlencoded".equals(getHeader("Content-Type"));
	}

	/**
	 * 获得指定的Cookie
	 * 
	 * @param name cookie名
	 * @return Cookie对象
	 */
	public Cookie getCookie(String name) {
		return cookies.get(name);
	}

	/**
	 * @return 获得所有Cookie信息
	 */
	public Map<String, Cookie> getCookies() {
		return this.cookies;
	}

	/**
	 * @return 客户浏览器是否为IE
	 */
	public boolean isIE() {
		String userAgent = getHeader("User-Agent");
		if (StrUtil.isNotBlank(userAgent)) {
			userAgent = userAgent.toUpperCase();
			if (userAgent.contains("MSIE") || userAgent.contains("TRIDENT")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param name 参数名
	 * @return 获得请求参数
	 */
	public String getParam(String name) {
		final Object value = params.get(name);
		if(null == value){
			return null;
		}
		
		if(value instanceof String){
			return (String)value;
		}
		return value.toString();
	}
	
	/**
	 * @param name 参数名
	 * @return 获得请求参数
	 */
	public Object getObjParam(String name) {
		return params.get(name);
	}

	/**
	 * 获得GET请求参数<br>
	 * 会根据浏览器类型自动识别GET请求的编码方式从而解码<br>
	 * charsetOfServlet为null则默认的ISO_8859_1
	 * 
	 * @param name 参数名
	 * @param charset 字符集
	 * @return 获得请求参数
	 */
	public String getParam(String name, Charset charset) {
		if (null == charset) {
			charset = Charset.forName(CharsetUtil.ISO_8859_1);
		}

		String destCharset = CharsetUtil.UTF_8;
		if (isIE()) {
			// IE浏览器GET请求使用GBK编码
			destCharset = CharsetUtil.GBK;
		}

		String value = getParam(name);
		if (METHOD_GET.equalsIgnoreCase(getMethod())) {
			value = CharsetUtil.convert(value, charset.toString(), destCharset);
		}
		return value;
	}

	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得请求参数
	 */
	public String getParam(String name, String defaultValue) {
		String param = getParam(name);
		return StrUtil.isBlank(param) ? defaultValue : param;
	}

	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Integer类型请求参数
	 */
	public Integer getIntParam(String name, Integer defaultValue) {
		return Conver.toInt(getParam(name), defaultValue);
	}

	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得long类型请求参数
	 */
	public Long getLongParam(String name, Long defaultValue) {
		return Conver.toLong(getParam(name), defaultValue);
	}

	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Double类型请求参数
	 */
	public Double getDoubleParam(String name, Double defaultValue) {
		return Conver.toDouble(getParam(name), defaultValue);
	}

	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Float类型请求参数
	 */
	public Float getFloatParam(String name, Float defaultValue) {
		return Conver.toFloat(getParam(name), defaultValue);
	}

	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Boolean类型请求参数
	 */
	public Boolean getBoolParam(String name, Boolean defaultValue) {
		return Conver.toBool(getParam(name), defaultValue);
	}

	/**
	 * 格式：<br>
	 * 1、yyyy-MM-dd HH:mm:ss <br>
	 * 2、yyyy-MM-dd <br>
	 * 3、HH:mm:ss <br>
	 * 
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Date类型请求参数，默认格式：
	 */
	public Date getDateParam(String name, Date defaultValue) {
		String param = getParam(name);
		return StrUtil.isBlank(param) ? defaultValue : DateUtil.parse(param);
	}

	/**
	 * @param name 参数名
	 * @param format 格式
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Date类型请求参数
	 */
	public Date getDateParam(String name, String format, Date defaultValue) {
		String param = getParam(name);
		return StrUtil.isBlank(param) ? defaultValue : DateUtil.parse(param, format);
	}

	/**
	 * 获得请求参数<br>
	 * 列表类型值，常用于表单中的多选框
	 * 
	 * @param name 参数名
	 * @return 数组
	 */
	@SuppressWarnings("unchecked")
	public List<String> getArrayParam(String name) {
		Object value = params.get(name);
		if(null == value){
			return null;
		}
		
		if(value instanceof List){
			return (List<String>) value;
		}else if(value instanceof String){
			return StrUtil.split((String)value, ',');
		}else{
			throw new RuntimeException("Value is not a List type!");
		}
	}

	/**
	 * 获得所有请求参数
	 * 
	 * @return Map
	 */
	public Map<String, Object> getParams() {
		return params;
	}

	/**
	 * @return 是否为长连接
	 */
	public boolean isKeepAlive() {
		final String connectionHeader = getHeader(HttpHeaderNames.CONNECTION.toString());
		// 无论任何版本Connection为close时都关闭连接
		if (HttpHeaderValues.CLOSE.toString().equalsIgnoreCase(connectionHeader)) {
			return false;
		}

		// HTTP/1.0只有Connection为Keep-Alive时才会保持连接
		if (HttpVersion.HTTP_1_0.text().equals(getProtocolVersion())) {
			if (false == HttpHeaderValues.KEEP_ALIVE.toString().equalsIgnoreCase(connectionHeader)) {
				return false;
			}
		}
		// HTTP/1.1默认打开Keep-Alive
		return true;
	}

	// --------------------------------------------------------- Protected method start
	/**
	 * 填充参数（GET请求的参数）
	 * 
	 * @param decoder QueryStringDecoder
	 */
	protected void putParams(QueryStringDecoder decoder) {
		if (null != decoder) {
			List<String> valueList;
			for (Entry<String, List<String>> entry : decoder.parameters().entrySet()) {
				valueList = entry.getValue();
				if(null != valueList){
					this.putParam(entry.getKey(), 1 == valueList.size() ? valueList.get(0) : valueList);
				}
			}
		}
	}

	/**
	 * 填充参数（POST请求的参数）
	 * 
	 * @param decoder QueryStringDecoder
	 */
	protected void putParams(HttpPostRequestDecoder decoder) {
		if (null == decoder) {
			return;
		}
		
		for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
			putParam(data);
		}
	}
	
	/**
	 * 填充参数
	 * 
	 * @param data InterfaceHttpData
	 */
	protected void putParam(InterfaceHttpData data) {
		final HttpDataType dataType = data.getHttpDataType();
		if (dataType == HttpDataType.Attribute) {
			//普通参数
			Attribute attribute = (Attribute) data;
			try {
				this.putParam(attribute.getName(), attribute.getValue());
			} catch (IOException e) {
                Logger.error(e.toString());
			}
		}else if(dataType == HttpDataType.FileUpload){
			//文件
			FileUpload fileUpload = (FileUpload) data;
			if(fileUpload.isCompleted()){
				try {
					this.putParam(data.getName(), fileUpload.getFile());
				} catch (IOException e) {
                    Logger.error(e.toString(), "Get file param [{}] error!", data.getName());
				}
			}
		}
	}

	/**
	 * 填充参数
	 * 
	 * @param key 参数名
	 * @param value 参数值
	 */
	protected void putParam(String key, Object value) {
		this.params.put(key, value);
	}

	/**
	 * 填充头部信息和Cookie信息
	 * 
	 * @param headers HttpHeaders
	 */
	protected void putHeadersAndCookies(HttpHeaders headers) {
		for (Entry<String, String> entry : headers) {
			this.headers.put(entry.getKey(), entry.getValue());
		}

		// Cookie
		final String cookieString = this.headers.get(HttpHeaderNames.COOKIE);
		if (StrUtil.isNotBlank(cookieString)) {
			final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
			for (Cookie cookie : cookies) {
				this.cookies.put(cookie.name(), cookie);
			}
		}
	}

	/**
	 * 设置客户端IP
	 * 
	 * @param ctx ChannelHandlerContext
	 */
	protected void putIp(ChannelHandlerContext ctx) {
		String ip = getHeader("X-Forwarded-For");
		if (StrUtil.isNotBlank(ip)) {
			ip = HttpUtil.getMultistageReverseProxyIp(ip);
		} else {
			final InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
			ip = insocket.getAddress().getHostAddress();
		}
		this.ip = ip;
	}
	// --------------------------------------------------------- Protected method end

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("\r\nprotocolVersion: ").append(getProtocolVersion()).append("\r\n");
		sb.append("uri: ").append(getUri()).append("\r\n");
		sb.append("path: ").append(path).append("\r\n");
		sb.append("method: ").append(getMethod()).append("\r\n");
		sb.append("ip: ").append(ip).append("\r\n");
		sb.append("headers:\r\n ");
		for (Entry<String, String> entry : headers.entrySet()) {
			sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
		}
		sb.append("params: \r\n");
		for (Entry<String, Object> entry : params.entrySet()) {
			sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
		}

		return sb.toString();
	}

	/**
	 * 构建Request对象
	 * 
	 * @param ctx ChannelHandlerContext
	 * @param nettyRequest Netty的HttpRequest
	 * @return Request
	 */
	protected final static Request build(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) {
		return new Request(ctx, nettyRequest);
	}
}
