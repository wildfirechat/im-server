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
import com.google.protobuf.InvalidProtocolBufferException;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

@Handler(value = IMTopic.PullMessageTopic)
public class PullMessageHandler extends IMHandler<WFCMessage.PullMessageRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.PullMessageRequest request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;

        if (request.getType() == ProtoConstants.PullType.Pull_ChatRoom && !m_messagesStore.checkUserClientInChatroom(fromUser, clientID, null)) {
            errorCode = ErrorCode.ERROR_CODE_NOT_IN_CHATROOM;
        } else {
            WFCMessage.PullMessageResult result = m_messagesStore.fetchMessage(fromUser, clientID, request.getId(), request.getType());
            byte[] data = result.toByteArray();
            LOG.info("User {} pull message with count({}), payload size({})", fromUser, result.getMessageCount(), data.length);
            ackPayload.ensureWritable(data.length).writeBytes(data);
        }
        return errorCode;
    }
}
