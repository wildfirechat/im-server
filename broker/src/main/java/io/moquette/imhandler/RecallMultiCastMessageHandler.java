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

@Handler(value = IMTopic.RecallMultiCastMessageTopic)
public class RecallMultiCastMessageHandler extends IMHandler<WFCMessage.RecallMultiCastMessageRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.RecallMultiCastMessageRequest request, Qos1PublishHandler.IMCallback callback) {
        boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;
        if (!isAdmin) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        ErrorCode errorCode = m_messagesStore.recallCastMessage(request.getMessageId(), fromUser);

        if(errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
            return errorCode;
        }

        publishRecallMultiCastMsg(request.getMessageId(), request.getReceiverList());

        return errorCode;
    }

}
