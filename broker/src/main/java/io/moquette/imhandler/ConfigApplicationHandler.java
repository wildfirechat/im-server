/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.IMTopic;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(IMTopic.ApplicationConfigRequestTopic)
public class ConfigApplicationHandler extends IMHandler<WFCMessage.ApplicationConfigRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.ApplicationConfigRequest request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = m_messagesStore.configApplication(request.getAppId(), request.getAppType(), request.getTimestamp(), request.getNonce(), request.getSignature());
        return errorCode;
    }
}

