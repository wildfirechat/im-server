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
import cn.wildfirechat.pojos.GroupNotificationBinaryContent;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(IMTopic.TransferGroupTopic)
public class TransferGroupHandler extends GroupHandler<WFCMessage.TransferGroupRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.TransferGroupRequest request, Qos1PublishHandler.IMCallback callback) {
        boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;
        if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0 && requestSourceType == ProtoConstants.RequestSourceType.Request_From_User && !m_messagesStore.isAllowClientCustomGroupNotification()) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }
        if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0 && requestSourceType == ProtoConstants.RequestSourceType.Request_From_Robot && !m_messagesStore.isAllowRobotCustomGroupNotification()) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        if(requestSourceType == ProtoConstants.RequestSourceType.Request_From_User) {
            int forbiddenClientOperation = m_messagesStore.getGroupForbiddenClientOperation();
            if((forbiddenClientOperation & ProtoConstants.ForbiddenClientGroupOperationMask.Forbidden_Transfer_Group) > 0) {
                return ErrorCode.ERROR_CODE_NOT_RIGHT;
            }
        }

        ErrorCode errorCode = m_messagesStore.transferGroup(fromUser, request.getGroupId(), request.getNewOwner(), isAdmin);
        if (errorCode == ERROR_CODE_SUCCESS) {
            if (request.hasNotifyContent() && request.getNotifyContent().getType() > 0) {
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), request.getNotifyContent());
            } else {
                WFCMessage.MessageContent content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, null, request.getNewOwner()).getTransferGroupNotifyContent();
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content);
            }
        }
        return errorCode;
    }
}
