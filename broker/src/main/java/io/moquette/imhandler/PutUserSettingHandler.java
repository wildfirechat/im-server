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

@Handler(IMTopic.PutUserSettingTopic)
public class PutUserSettingHandler extends IMHandler<WFCMessage.ModifyUserSettingReq> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.ModifyUserSettingReq request, Qos1PublishHandler.IMCallback callback) {
            m_messagesStore.updateUserSettings(fromUser, request, clientID);
            return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
