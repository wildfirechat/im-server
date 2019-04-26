/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

import static win.liyufan.im.IMTopic.PutUserSettingTopic;
import static win.liyufan.im.UserSettingScope.kUserSettingMyChannels;

@Handler(value = IMTopic.DestoryChannelInfoTopic)
public class DistoryChannelHandler extends GroupHandler<WFCMessage.IDBuf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.IDBuf request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = m_messagesStore.distoryChannel(fromUser, request.getId());
        if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
            WFCMessage.ModifyUserSettingReq modifyUserSettingReq = WFCMessage.ModifyUserSettingReq.newBuilder().setScope(kUserSettingMyChannels).setKey(request.getId()).setValue("0").build();
            mServer.internalRpcMsg(fromUser, null, modifyUserSettingReq.toByteArray(), 0, fromUser, PutUserSettingTopic, false);
        }
        return errorCode;
    }
}
