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

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(IMTopic.DeleteFriendTopic)
public class DeleteFriendHandler extends IMHandler<WFCMessage.IDBuf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.IDBuf request, Qos1PublishHandler.IMCallback callback) {
        long[] head = new long[2];
        ErrorCode errorCode = m_messagesStore.deleteFriend(fromUser, request.getId(), head);
        if (errorCode == ERROR_CODE_SUCCESS) {
            publisher.publishNotification(IMTopic.NotifyFriendTopic, fromUser, head[0]);
            publisher.publishNotification(IMTopic.NotifyFriendTopic, request.getId(), head[1]);
        }
        return errorCode;
    }
}
