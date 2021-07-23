package com.xiaoleilu.loServer.action;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import com.hazelcast.core.HazelcastInstance;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.StaticLog;
import com.xiaoleilu.hutool.util.DateUtil;
import com.xiaoleilu.hutool.util.FileUtil;
import com.xiaoleilu.hutool.util.ReUtil;
import com.xiaoleilu.hutool.util.StrUtil;
import com.xiaoleilu.loServer.ServerSetting;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;

import io.moquette.spi.IMessagesStore;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.LoggerFactory;

/**
 * 默认的主页Action，当访问主页且没有定义主页Action时，调用此Action
 * 
 * @author Looly
 *
 */
public class FileAction extends Action {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(FileAction.class);

    @Override
    public boolean action(Request request, Response response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        if (false == Request.METHOD_GET.equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpResponseStatus.METHOD_NOT_ALLOWED, "Please use GET method to request file!");
            return true;
        }

        if(ServerSetting.isRootAvailable() == false){
            response.sendError(HttpResponseStatus.NOT_FOUND, "404 Root dir not avaliable!");
            return true;
        }

        File file = null;
        try {
            file = getFileByPath(request.getPath());
        } catch (Exception e) {
            response.sendError(HttpResponseStatus.NOT_FOUND, "404 File not found!");
            return true;
        }

        // 隐藏文件或不存在，跳过
        if (file == null || file.isHidden() || !file.exists()) {
            response.sendError(HttpResponseStatus.NOT_FOUND, "404 File not found!");
            return true;
        }

        // 非文件，跳过
        if (!file.isFile()) {
            response.sendError(HttpResponseStatus.FORBIDDEN, "403 Forbidden!");
            return true;
        }

        Logger.debug("Client [{}] get file [{}]", request.getIp(), file.getPath());
        
        // Cache Validation
        String ifModifiedSince = request.getHeader(HttpHeaderNames.IF_MODIFIED_SINCE.toString());
        if (StrUtil.isNotBlank(ifModifiedSince)) {
            Date ifModifiedSinceDate = null;
            try {
                ifModifiedSinceDate = DateUtil.parse(ifModifiedSince, HTTP_DATE_FORMATER);
            } catch (Exception e) {
                Logger.warn("If-Modified-Since header parse error: {}", e.getMessage());
            }
            if(ifModifiedSinceDate != null) {
                // 只对比到秒一级别
                long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
                long fileLastModifiedSeconds = file.lastModified() / 1000;
                if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                    Logger.debug("File {} not modified.", file.getPath());
                    response.sendNotModified();
                    return true;
                }
            }
        }

        response.setContent(file);
        return true;
    }

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
	private static final SimpleDateFormat HTTP_DATE_FORMATER = new SimpleDateFormat(DateUtil.HTTP_DATETIME_PATTERN, Locale.US);

	
	/**
	 * 通过URL中的path获得文件的绝对路径
	 * 
	 * @param httpPath Http请求的Path
	 * @return 文件绝对路径
	 */
	public static File getFileByPath(String httpPath) {
		// Decode the path.
		try {
			httpPath = URLDecoder.decode(httpPath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new Error(e);
		}

		if (httpPath.isEmpty() || httpPath.charAt(0) != '/') {
			return null;
		}

		// 路径安全检查
        String path = httpPath.substring(0, httpPath.lastIndexOf("/"));
		if (path.contains("/.") || path.contains("./") || ReUtil.isMatch(INSECURE_URI, path)) {
			return null;
		}

		// 转换为绝对路径
		return FileUtil.file(ServerSetting.getRoot(), httpPath);
	}
}
