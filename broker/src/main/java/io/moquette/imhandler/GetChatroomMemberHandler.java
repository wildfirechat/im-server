/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

@Handler(IMTopic.GetChatroomMemberTopic)
public class GetChatroomMemberHandler extends IMHandler<WFCMessage.GetChatroomMemberInfoRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.GetChatroomMemberInfoRequest request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
            WFCMessage.ChatroomMemberInfo info = m_messagesStore.getChatroomMemberInfo(request.getChatroomId(), request.getMaxCount());
            if (info != null) {
                byte[] data = info.toByteArray();
                ackPayload.ensureWritable(data.length).writeBytes(data);
            } else {
                errorCode = ErrorCode.ERROR_CODE_NOT_EXIST;
            }
            return errorCode;
    }
}
