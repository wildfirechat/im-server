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
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

import static win.liyufan.im.IMTopic.PutUserSettingTopic;
import static win.liyufan.im.UserSettingScope.kUserSettingMyChannels;

@Handler(value = IMTopic.DestroyChannelInfoTopic)
public class DestroyChannelHandler extends GroupHandler<WFCMessage.IDBuf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.IDBuf request, Qos1PublishHandler.IMCallback callback) {
        boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;
        ErrorCode errorCode = m_messagesStore.destroyChannel(fromUser, request.getId(), isAdmin);
        if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
            WFCMessage.ModifyUserSettingReq modifyUserSettingReq = WFCMessage.ModifyUserSettingReq.newBuilder().setScope(kUserSettingMyChannels).setKey(request.getId()).setValue("0").build();
            mServer.onApiMessage(fromUser, null, modifyUserSettingReq.toByteArray(), 0, fromUser, PutUserSettingTopic, requestSourceType);
        }
        return errorCode;
    }
}
