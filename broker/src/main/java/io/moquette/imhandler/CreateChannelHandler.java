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
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

import static win.liyufan.im.IMTopic.PutUserSettingTopic;
import static win.liyufan.im.UserSettingScope.kUserSettingMyChannels;

@Handler(value = IMTopic.CreateChannelTopic)
public class CreateChannelHandler extends GroupHandler<WFCMessage.ChannelInfo> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.ChannelInfo request, Qos1PublishHandler.IMCallback callback) {
        WFCMessage.ChannelInfo.Builder builder = request.toBuilder();
        if (StringUtil.isNullOrEmpty(request.getTargetId())) {
            builder.setTargetId(m_messagesStore.getShortUUID());
        }
        if (StringUtil.isNullOrEmpty(request.getOwner())) {
            builder.setOwner(fromUser);
        }
        if (StringUtil.isNullOrEmpty(request.getSecret())) {
            builder.setSecret(m_messagesStore.getShortUUID());
        }

        long update = System.currentTimeMillis();
        request = builder.setUpdateDt(update).build();

        ErrorCode errorCode = m_messagesStore.createChannel(fromUser, request);

        if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
            WFCMessage.ModifyUserSettingReq modifyUserSettingReq = WFCMessage.ModifyUserSettingReq.newBuilder().setScope(kUserSettingMyChannels).setKey(request.getTargetId()).setValue("1").build();
            mServer.onApiMessage(fromUser, null, modifyUserSettingReq.toByteArray(), 0, fromUser, PutUserSettingTopic, requestSourceType);
            byte[] data = request.getTargetId().getBytes();
            ackPayload.ensureWritable(data.length).writeBytes(data);
            return ErrorCode.ERROR_CODE_SUCCESS;
        } else {
            return errorCode;
        }
    }
}
