/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.model.FriendData;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.IMTopic;

import java.util.List;

@Handler(value = IMTopic.ClearSessionTopic)
public class DisconnectHandler extends IMHandler<Byte> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, Byte request, Qos1PublishHandler.IMCallback callback) {
        if (request == 8) {
            m_sessionsStore.cleanSession(fromUser, clientID);
        } else if(request == 1) {
            m_sessionsStore.disableSession(fromUser, clientID);
        }

        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
