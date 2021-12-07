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
import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.model.FriendData;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

import java.util.List;

@Handler(IMTopic.FriendPullTopic)
public class FriendPullHandler extends IMHandler<WFCMessage.Version> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.Version request, Qos1PublishHandler.IMCallback callback) {
        List<FriendData> friendDatas = m_messagesStore.getFriendList(fromUser, clientID, request.getVersion());
        WFCMessage.GetFriendsResult.Builder builder = WFCMessage.GetFriendsResult.newBuilder();
        for (FriendData data : friendDatas
            ) {
            WFCMessage.Friend.Builder builder1 = WFCMessage.Friend.newBuilder().setState(data.getState()).setBlacked(data.getBlacked()).setUid(data.getFriendUid()).setUpdateDt(data.getTimestamp());
            if (!StringUtil.isNullOrEmpty(data.getAlias())) {
                builder1.setAlias(data.getAlias());
            }
            if (!StringUtil.isNullOrEmpty(data.getExtra())) {
                builder1.setExtra(data.getExtra());
            }
            builder.addEntry(builder1.build());
        }
        byte[] data = builder.build().toByteArray();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
