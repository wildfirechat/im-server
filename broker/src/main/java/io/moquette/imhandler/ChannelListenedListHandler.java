package io.moquette.imhandler;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.IMTopic;

import java.util.List;

@Handler(IMTopic.ListenedChannelListTopic)
public class ChannelListenedListHandler extends IMHandler<Void> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, Void request, Qos1PublishHandler.IMCallback callback) {
        List<String> channelInfoList = m_messagesStore.getListenedChannels(fromUser);
        WFCMessage.IDListBuf.Builder builder = WFCMessage.IDListBuf.newBuilder();
        builder.addAllId(channelInfoList);
        byte[] data = builder.build().toByteArray();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
