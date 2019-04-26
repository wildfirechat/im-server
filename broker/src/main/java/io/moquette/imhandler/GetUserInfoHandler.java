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

import static win.liyufan.im.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(IMTopic.GetUserInfoTopic)
public class GetUserInfoHandler extends IMHandler<WFCMessage.PullUserRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.PullUserRequest request, Qos1PublishHandler.IMCallback callback) {
        WFCMessage.PullUserResult.Builder resultBuilder = WFCMessage.PullUserResult.newBuilder();

        ErrorCode errorCode = m_messagesStore.getUserInfo(request.getRequestList(), resultBuilder);
        if (errorCode == ERROR_CODE_SUCCESS) {
            byte[] data = resultBuilder.build().toByteArray();
            ackPayload.ensureWritable(data.length).writeBytes(data);
        }
        return errorCode;
    }
}
