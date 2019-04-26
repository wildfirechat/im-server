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
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

import static win.liyufan.im.ErrorCode.ERROR_CODE_SUCCESS;
import static win.liyufan.im.IMTopic.HandleFriendRequestTopic;

@Handler(IMTopic.AddFriendRequestTopic)
public class AddFriendHandler extends GroupHandler<WFCMessage.AddFriendRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.AddFriendRequest request, Qos1PublishHandler.IMCallback callback) {
            long[] head = new long[1];
            ErrorCode errorCode = m_messagesStore.saveAddFriendRequest(fromUser, request, head);
            if (errorCode == ERROR_CODE_SUCCESS) {
                WFCMessage.User user = m_messagesStore.getUserInfo(request.getTargetUid());
                if (user != null && user.getType() == ProtoConstants.UserType.UserType_Normal) {
                    publisher.publishNotification(IMTopic.NotifyFriendRequestTopic, request.getTargetUid(), head[0]);
                } else if(user != null && user.getType() == ProtoConstants.UserType.UserType_Robot) {
                    WFCMessage.HandleFriendRequest handleFriendRequest = WFCMessage.HandleFriendRequest.newBuilder().setTargetUid(fromUser).setStatus(ProtoConstants.FriendRequestStatus.RequestStatus_Accepted).build();
                    mServer.internalRpcMsg(request.getTargetUid(), null, handleFriendRequest.toByteArray(), 0, fromUser, HandleFriendRequestTopic, false);
                }
            }
            return errorCode;
    }
}
