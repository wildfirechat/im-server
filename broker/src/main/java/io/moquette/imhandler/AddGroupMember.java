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

@Handler(value = IMTopic.AddGroupMemberTopic)
public class AddGroupMember extends GroupHandler<WFCMessage.AddGroupMemberRequest> {

    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.AddGroupMemberRequest request, Qos1PublishHandler.IMCallback callback) {
        boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;

        if(requestSourceType == ProtoConstants.RequestSourceType.Request_From_User) {
            int forbiddenClientOperation = m_messagesStore.getGroupForbiddenClientOperation();
            if(request.getAddedMemberList().size() == 1 && request.getAddedMember(0).getMemberId().equals(fromUser)) {
                if ((forbiddenClientOperation & ProtoConstants.ForbiddenClientGroupOperationMask.Forbidden_Join_Group) > 0) {
                    return ErrorCode.ERROR_CODE_NOT_RIGHT;
                }
            } else {
                if ((forbiddenClientOperation & ProtoConstants.ForbiddenClientGroupOperationMask.Forbidden_Invite_Group_Member) > 0) {
                    return ErrorCode.ERROR_CODE_NOT_RIGHT;
                }
            }
        }

        ErrorCode errorCode = m_messagesStore.addGroupMembers(fromUser, isAdmin, request.getGroupId(), request.getAddedMemberList(), request.getExtra());
        if (errorCode == ERROR_CODE_SUCCESS) {
            if (request.hasNotifyContent() && request.getNotifyContent().getType() > 0) {
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), request.getNotifyContent());
            } else {
                WFCMessage.MessageContent content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, null, getMemberIdList(request.getAddedMemberList())).getAddGroupNotifyContent();
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content);
            }
        }

        return errorCode;
    }
}
