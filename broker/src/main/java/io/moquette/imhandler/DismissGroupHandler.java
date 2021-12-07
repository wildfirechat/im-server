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

import static win.liyufan.im.IMTopic.DismissGroupTopic;

@Handler(value = DismissGroupTopic)
public class DismissGroupHandler extends GroupHandler<WFCMessage.DismissGroupRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.DismissGroupRequest request, Qos1PublishHandler.IMCallback callback) {
            WFCMessage.GroupInfo groupInfo = m_messagesStore.getGroupInfo(request.getGroupId());
            boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;
            ErrorCode errorCode;
            if (groupInfo == null) {
                errorCode = m_messagesStore.dismissGroup(fromUser, request.getGroupId(), isAdmin);

            } else if (isAdmin || (groupInfo.getType() == ProtoConstants.GroupType.GroupType_Normal || groupInfo.getType() == ProtoConstants.GroupType.GroupType_Restricted)
                && groupInfo.getOwner() != null && groupInfo.getOwner().equals(fromUser)) {

                if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0 && requestSourceType == ProtoConstants.RequestSourceType.Request_From_User && !m_messagesStore.isAllowClientCustomGroupNotification()) {
                    return ErrorCode.ERROR_CODE_NOT_RIGHT;
                }

                if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0 && requestSourceType == ProtoConstants.RequestSourceType.Request_From_Robot && !m_messagesStore.isAllowRobotCustomGroupNotification()) {
                    return ErrorCode.ERROR_CODE_NOT_RIGHT;
                }

                //send notify message first, then dismiss group
                if (request.hasNotifyContent() && request.getNotifyContent().getType() > 0) {
                    sendGroupNotification(fromUser, groupInfo.getTargetId(), request.getToLineList(), request.getNotifyContent());
                } else {
                    WFCMessage.MessageContent content = new GroupNotificationBinaryContent(groupInfo.getTargetId(), fromUser, null, "").getDismissGroupNotifyContent();
                    sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content);
                }
                errorCode = m_messagesStore.dismissGroup(fromUser, request.getGroupId(), isAdmin);
            } else {
                errorCode = ErrorCode.ERROR_CODE_NOT_RIGHT;
            }
            return errorCode;
    }
}
