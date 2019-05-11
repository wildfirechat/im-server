/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.xiaoleilu.loServer.pojos.GroupNotificationBinaryContent;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

import static io.moquette.BrokerConstants.MESSAGE_CONTENT_TYPE_CREATE_GROUP;

@Handler(value = IMTopic.CreateGroupTopic)
public class CreateGroupHandler extends GroupHandler<WFCMessage.CreateGroupRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.CreateGroupRequest request, Qos1PublishHandler.IMCallback callback) {
        WFCMessage.GroupInfo groupInfo = m_messagesStore.createGroup(fromUser, request.getGroup().getGroupInfo(), request.getGroup().getMembersList());
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
