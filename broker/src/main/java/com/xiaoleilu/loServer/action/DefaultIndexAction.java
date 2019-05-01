package com.xiaoleilu.loServer.action;

import com.hazelcast.core.HazelcastInstance;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.spi.IMessagesStore;

/**
 * 默认的主页Action，当访问主页且没有定义主页Action时，调用此Action
 * @author Looly
 *
 */
public class DefaultIndexAction extends Action{
    @Override
    public boolean action(Request request, Response response) {
        response.setContent("Welcome to LoServer.");
        return true;
    }
}
