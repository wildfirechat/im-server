package com.xiaoleilu.loServer.action;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.hazelcast.core.HazelcastInstance;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.StaticLog;
import com.xiaoleilu.hutool.util.StrUtil;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;

import io.moquette.spi.IMessagesStore;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.LoggerFactory;

/**
 * 错误堆栈Action类
 * @author Looly
 *
 */
public class UnknownErrorAction extends Action{
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(UnknownErrorAction.class);
	
	public final static String ERROR_PARAM_NAME = "_e";
	
	private final static String TEMPLATE_ERROR = "<!DOCTYPE html><html><head><title>LoServer - Error report</title><style>h1,h3 {color:white; background-color: gray;}</style></head><body><h1>HTTP Status {} - {}</h1><hr size=\"1\" noshade=\"noshade\" /><p>{}</p><hr size=\"1\" noshade=\"noshade\" /><h3>LoServer</h3></body></html>";

    @Override
    public boolean action(Request request, Response response) {
        response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        return true;
    }
}
