/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.GroupNotificationBinaryContent;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.IMTopic;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(IMTopic.ModifyGroupMemberExtraTopic)
public class ModifyGroupMemberExtraHandler extends GroupHandler<WFCMessage.ModifyGroupMemberExtra> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.ModifyGroupMemberExtra request, Qos1PublishHandler.IMCallback callback) {
        if(request.hasNotifyContent() && request.getNotifyContent().getType() > 0 && !isAdmin && !m_messagesStore.isAllowClientCustomGroupNotification()) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        ErrorCode errorCode = m_messagesStore.modifyGroupMemberExtra(fromUser, request.getGroupId(), request.getExtra(), request.getMemberId(), isAdmin);
        if (errorCode == ERROR_CODE_SUCCESS) {
            if (request.hasNotifyContent()&& request.getNotifyContent().getType() > 0 && (isAdmin || m_messagesStore.isAllowClientCustomGroupNotification())) {
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), request.getNotifyContent());
            } else {
                WFCMessage.MessageContent content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, request.getExtra(), request.getMemberId()).getModifyGroupMemberExtraNotifyContent();
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content);
            }
        }
        return errorCode;
    }
}
