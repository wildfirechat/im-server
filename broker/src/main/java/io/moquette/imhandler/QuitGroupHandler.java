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

import static win.liyufan.im.IMTopic.QuitGroupTopic;

@Handler(value = QuitGroupTopic)
public class QuitGroupHandler extends GroupHandler<WFCMessage.QuitGroupRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.QuitGroupRequest request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        WFCMessage.GroupInfo groupInfo = m_messagesStore.getGroupInfo(request.getGroupId());
        if (groupInfo == null) {
            errorCode = m_messagesStore.quitGroup(fromUser, request.getGroupId());
        } else {
            //send notify message first, then quit group
            if (request.hasNotifyContent() && request.getNotifyContent().getType() > 0) {
                sendGroupNotification(fromUser, groupInfo.getTargetId(), request.getToLineList(), request.getNotifyContent());
            } else {
                WFCMessage.MessageContent content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, null, "").getQuitGroupNotifyContent();
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content);
            }
            errorCode = m_messagesStore.quitGroup(fromUser, request.getGroupId());
        }
        return errorCode;
    }
}
