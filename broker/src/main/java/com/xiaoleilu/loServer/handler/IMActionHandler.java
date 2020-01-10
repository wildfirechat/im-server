/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.handler;

import com.xiaoleilu.hutool.lang.Singleton;
import com.xiaoleilu.loServer.ServerSetting;
import com.xiaoleilu.loServer.action.Action;
import com.xiaoleilu.loServer.action.FileAction;
import com.xiaoleilu.loServer.action.UnknownErrorAction;
import com.xiaoleilu.loServer.filter.Filter;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action处理单元
 * 
 * @author Looly
 */
public class IMActionHandler extends ActionHandler {
    private static final Logger Logger = LoggerFactory.getLogger(IMActionHandler.class);

    public IMActionHandler(IMessagesStore messagesStore, ISessionsStore sessionsStore) {
        super(messagesStore, sessionsStore);
    }

    @Override
    boolean isValidePath(String path) {
        if (!StringUtil.isNullOrEmpty(path) && !path.startsWith("/admin")) {
            return true;
        }
        return false;
    }
}
