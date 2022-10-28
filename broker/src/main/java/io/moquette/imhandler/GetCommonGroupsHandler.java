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

import java.util.Set;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(IMTopic.GetCommonGroupsTopic)
public class GetCommonGroupsHandler extends IMHandler<WFCMessage.IDBuf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.IDBuf request, Qos1PublishHandler.IMCallback callback) {
        WFCMessage.PullUserResult.Builder resultBuilder = WFCMessage.PullUserResult.newBuilder();
        Set<String> strings = m_messagesStore.getCommonGroupIds(fromUser, request.getId());
        WFCMessage.IDListBuf idListBuf = WFCMessage.IDListBuf.newBuilder().addAllId(strings).build();
        byte[] data = idListBuf.toByteArray();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ERROR_CODE_SUCCESS;
    }
}
