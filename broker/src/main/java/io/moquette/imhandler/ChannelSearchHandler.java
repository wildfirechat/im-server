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

import java.util.List;

@Handler(IMTopic.ChannelSearchTopic)
public class ChannelSearchHandler extends IMHandler<WFCMessage.SearchUserRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.SearchUserRequest request, Qos1PublishHandler.IMCallback callback) {
        List<WFCMessage.ChannelInfo> users = m_messagesStore.searchChannel(request.getKeyword(), request.getFuzzy() > 0, request.getPage());
        WFCMessage.SearchChannelResult.Builder builder = WFCMessage.SearchChannelResult.newBuilder();
        builder.addAllChannel(users);
        builder.setKeyword(request.getKeyword());
        byte[] data = builder.build().toByteArray();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
