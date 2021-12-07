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

import java.util.Set;

import static win.liyufan.im.IMTopic.KickoffGroupMemberTopic;

@Handler(value = KickoffGroupMemberTopic)
public class KickoffGroupMember extends GroupHandler<WFCMessage.RemoveGroupMemberRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.RemoveGroupMemberRequest request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode;
        WFCMessage.GroupInfo groupInfo = m_messagesStore.getGroupInfo(request.getGroupId());
        if (groupInfo == null) {
            errorCode = ErrorCode.ERROR_CODE_NOT_EXIST;
        } else {
            boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;
            boolean isAllow = isAdmin;
            if (!isAllow) {
                if (groupInfo.getOwner() != null) {
                    if (request.getRemovedMemberList().contains(groupInfo.getOwner())) {
                        return ErrorCode.ERROR_CODE_NOT_RIGHT;
                    }
                }

                if (groupInfo.getOwner() != null && groupInfo.getOwner().equals(fromUser)) {
                    isAllow = true;
                }
            }

            if (!isAllow) {
                WFCMessage.GroupMember gm = m_messagesStore.getGroupMember(request.getGroupId(), fromUser);
                if (gm != null && gm.getType() == ProtoConstants.GroupMemberType.GroupMemberType_Manager) {
                    isAllow = true;
                }
            }
            if (!isAllow && (groupInfo.getType() == ProtoConstants.GroupType.GroupType_Normal || groupInfo.getType() == ProtoConstants.GroupType.GroupType_Restricted)) {
                errorCode = ErrorCode.ERROR_CODE_NOT_RIGHT;
            } else {
                if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0 && requestSourceType == ProtoConstants.RequestSourceType.Request_From_User && !m_messagesStore.isAllowClientCustomGroupNotification()) {
                    return ErrorCode.ERROR_CODE_NOT_RIGHT;
                }
                if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0 && requestSourceType == ProtoConstants.RequestSourceType.Request_From_Robot && !m_messagesStore.isAllowRobotCustomGroupNotification()) {
                    return ErrorCode.ERROR_CODE_NOT_RIGHT;
                }

                //send notify message first, then kickoff the member
                if (request.hasNotifyContent() && request.getNotifyContent().getType() > 0) {
                    sendGroupNotification(fromUser, groupInfo.getTargetId(), request.getToLineList(), request.getNotifyContent());
                } else {
                    WFCMessage.MessageContent content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, null, request.getRemovedMemberList()).getKickokfMemberGroupNotifyContent();
                    sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content);
                }

                if((m_messagesStore.getVisibleQuitKickoffNotification() & 0x02) > 0) {
                    Set<String> toUsers = m_messagesStore.getGroupManagers(request.getGroupId(), true);
                    toUsers.addAll(request.getRemovedMemberList());
                    WFCMessage.MessageContent content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, null, request.getRemovedMemberList()).getKickokfMemberVisibleGroupNotifyContent();
                    sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content, toUsers);
                }
                errorCode = m_messagesStore.kickoffGroupMembers(fromUser, isAdmin, request.getGroupId(), request.getRemovedMemberList());
            }
        }
        return errorCode;
    }
}
