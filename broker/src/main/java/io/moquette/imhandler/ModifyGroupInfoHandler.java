/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.pojos.GroupNotificationBinaryContent;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.ErrorCode;

import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_Name;
import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_Portrait;
import static win.liyufan.im.ErrorCode.ERROR_CODE_SUCCESS;
import static win.liyufan.im.IMTopic.ModifyGroupInfoTopic;

@Handler(value = ModifyGroupInfoTopic)
public class ModifyGroupInfoHandler extends GroupHandler<WFCMessage.ModifyGroupInfoRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.ModifyGroupInfoRequest request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode= m_messagesStore.modifyGroupInfo(fromUser, request.getGroupId(), request.getType(), request.getValue());
        if (errorCode == ERROR_CODE_SUCCESS) {
            if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0) {
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), request.getNotifyContent());
            } else {
                WFCMessage.MessageContent content = null;
                if (request.getType() == Modify_Group_Name) {
                    content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, request.getValue(), "").getChangeGroupNameNotifyContent();
                } else if(request.getType() == Modify_Group_Portrait) {
                    content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, null, "").getChangeGroupPortraitNotifyContent();
                }

                if (content != null) {
                    sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content);
                }
            }
        }
        return errorCode;
    }
}
