/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.model.FriendData;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

import java.util.List;

@Handler(IMTopic.FriendPullTopic)
public class FriendPullHandler extends IMHandler<WFCMessage.Version> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.Version request, Qos1PublishHandler.IMCallback callback) {
        List<FriendData> friendDatas = m_messagesStore.getFriendList(fromUser, request.getVersion());
        WFCMessage.GetFriendsResult.Builder builder = WFCMessage.GetFriendsResult.newBuilder();
        for (FriendData data : friendDatas
            ) {
            builder.addEntry(WFCMessage.Friend.newBuilder().setState(data.getState()).setUid(data.getFriendUid()).setUpdateDt(data.getTimestamp()).setAlias(data.getAlias()).build());
        }
        byte[] data = builder.build().toByteArray();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
