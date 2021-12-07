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

import java.util.List;

@Handler(IMTopic.FriendRequestPullTopic)
public class FriendRequestPullHandler extends IMHandler<WFCMessage.Version> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.Version request, Qos1PublishHandler.IMCallback callback) {
            List<WFCMessage.FriendRequest> friendDatas = m_messagesStore.getFriendRequestList(fromUser, request.getVersion());
            WFCMessage.GetFriendRequestResult.Builder builder = WFCMessage.GetFriendRequestResult.newBuilder();
            builder.addAllEntry(friendDatas);
            byte[] data = builder.build().toByteArray();
            ackPayload.ensureWritable(data.length).writeBytes(data);
            return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
