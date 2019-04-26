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
import static win.liyufan.im.IMTopic.ModifyChannelInfoTopic;
import static win.liyufan.im.IMTopic.ModifyGroupInfoTopic;

@Handler(value = ModifyChannelInfoTopic)
public class ModifyChannelInfoHandler extends GroupHandler<WFCMessage.ModifyChannelInfo> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.ModifyChannelInfo request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode= m_messagesStore.modifyChannelInfo(fromUser, request.getChannelId(), request.getType(), request.getValue());
        return errorCode;
    }
}
