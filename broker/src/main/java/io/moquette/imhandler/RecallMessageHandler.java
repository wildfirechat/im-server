/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

import java.util.LinkedHashSet;
import java.util.Set;

@Handler(value = IMTopic.RecallMessageTopic)
public class RecallMessageHandler extends IMHandler<WFCMessage.INT64Buf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.INT64Buf int64Buf, Qos1PublishHandler.IMCallback callback) {
        boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;
        ErrorCode errorCode = m_messagesStore.recallMessage(int64Buf.getId(), fromUser, clientID, isAdmin);

        if(errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
            return errorCode;
        }

        WFCMessage.Message message = m_messagesStore.getMessage(int64Buf.getId());
        if (message == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        publish(message.getFromUser(), clientID, message, requestSourceType);

        return errorCode;
    }

}
