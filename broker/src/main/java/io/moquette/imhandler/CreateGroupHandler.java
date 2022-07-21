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
import com.hazelcast.util.StringUtil;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

@Handler(value = IMTopic.CreateGroupTopic)
public class CreateGroupHandler extends GroupHandler<WFCMessage.CreateGroupRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.CreateGroupRequest request, Qos1PublishHandler.IMCallback callback) {
        if(request.getGroup().getGroupInfo().getType() < 0 || request.getGroup().getGroupInfo().getType() > 2) {
            return ErrorCode.ERROR_CODE_INVALID_DATA;
        }

        if (!StringUtil.isNullOrEmpty(request.getGroup().getGroupInfo().getTargetId())) {
            WFCMessage.GroupInfo existGroupInfo = m_messagesStore.getGroupInfo(request.getGroup().getGroupInfo().getTargetId());
            if (existGroupInfo != null) {
                return ErrorCode.ERROR_CODE_GROUP_ALREADY_EXIST;
            }
        }
        boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;
        if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0 && requestSourceType == ProtoConstants.RequestSourceType.Request_From_User && !m_messagesStore.isAllowClientCustomGroupNotification()) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0 && requestSourceType == ProtoConstants.RequestSourceType.Request_From_Robot && !m_messagesStore.isAllowRobotCustomGroupNotification()) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        WFCMessage.GroupInfo groupInfo = m_messagesStore.createGroup(fromUser, request.getGroup().getGroupInfo(), request.getGroup().getMembersList(), request.getMemberExtra(), isAdmin);
        if (groupInfo != null) {
            if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0) {
                sendGroupNotification(fromUser, groupInfo.getTargetId(), request.getToLineList(), request.getNotifyContent());
            } else {
                WFCMessage.MessageContent content = new GroupNotificationBinaryContent(groupInfo.getTargetId(), fromUser, groupInfo.getName(), "").getCreateGroupNotifyContent();
                sendGroupNotification(fromUser, groupInfo.getTargetId(), request.getToLineList(), content);
            }
        }
        byte[] data = groupInfo.getTargetId().getBytes();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
