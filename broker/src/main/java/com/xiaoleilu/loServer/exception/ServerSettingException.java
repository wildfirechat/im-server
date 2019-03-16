package com.xiaoleilu.loServer.exception;

import com.xiaoleilu.hutool.util.StrUtil;

/**
 * 设置异常
 * @author xiaoleilu
 */
public class ServerSettingException extends RuntimeException{
	private static final long serialVersionUID = -4134588858314744501L;

	public ServerSettingException(Throwable e) {
		super(e);
	}
	
	public ServerSettingException(String message) {
		super(message);
	}
	
	public ServerSettingException(String messageTemplate, Object... params) {
		super(StrUtil.format(messageTemplate, params));
	}
	
	public ServerSettingException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
	public ServerSettingException(Throwable throwable, String messageTemplate, Object... params) {
		super(StrUtil.format(messageTemplate, params), throwable);
	}
}
