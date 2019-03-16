package com.xiaoleilu.loServer.handler;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HttpFileServerController {

	private static final Logger logger = LoggerFactory.getLogger(HttpFileServerController.class);
	private Map<String, ChannelHandlerContext> contextMap = null; //

	private static class BrokenUploadInfoHandlerHolder {
		private static HttpFileServerController singleton = new HttpFileServerController();
	}

	/**
	 * constructor
	 */
	private HttpFileServerController() {
		contextMap = new ConcurrentHashMap<String, ChannelHandlerContext>();
	}

	/**
	 * getInstance
	 */
	public static HttpFileServerController getInstance() {
		return BrokenUploadInfoHandlerHolder.singleton;
	}

	/**
	 * mapChannelHandlerContext
	 * 
	 * @param requestId
	 * @param ctx
	 */
	public synchronized void mapChannelHandlerContext(String requestId, ChannelHandlerContext ctx) {
		if (contextMap.containsKey(requestId)) {
			logger.warn("contextMap has already contained the key:" + requestId);
		}

		contextMap.put(requestId, ctx);
	}

	/**
	 * getContext
	 * 
	 * @param requestId
	 * @return
	 */
	public ChannelHandlerContext getChannelHandlerContext(String requestId) {
		if (!contextMap.containsKey(requestId)) {
			logger.warn("contextMap not contains the key:" + requestId);
			return null;
		}

		return contextMap.get(requestId);
	}

	/**
	 * removeContext
	 * 
	 * @param requestId
	 */
	public void removeChannelHandlerContext(String requestId) {
		if (!contextMap.containsKey(requestId)) {
			logger.warn("contextMap not contains the key:" + requestId);
			return;
		}

		contextMap.remove(requestId);
		// logger.info("contextMap remove done.(" + requestId + ")");
	}
}
