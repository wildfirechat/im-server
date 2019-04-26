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

@Handler(IMTopic.ChannelPullTopic)
public class GetChannelInfoHandler extends IMHandler<WFCMessage.PullChannelInfo> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.PullChannelInfo request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        WFCMessage.ChannelInfo info = m_messagesStore.getChannelInfo(request.getChannelId());
        if (info == null) {
            errorCode = ErrorCode.ERROR_CODE_NOT_EXIST;
        } else if(info.getUpdateDt() <= request.getHead()) {
            errorCode = ErrorCode.ERROR_CODE_NOT_MODIFIED;
        } else {
            if (!info.getOwner().equals(fromUser)) {
                info = info.toBuilder().clearCallback().clearSecret().build();
            }

            byte[] data = info.toByteArray();
            ackPayload.ensureWritable(data.length).writeBytes(data);
        }
        return errorCode;
    }
}
