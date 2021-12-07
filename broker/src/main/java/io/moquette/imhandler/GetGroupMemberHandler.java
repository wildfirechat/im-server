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

import java.util.ArrayList;
import java.util.List;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(IMTopic.GetGroupMemberTopic)
public class GetGroupMemberHandler extends IMHandler<WFCMessage.PullGroupMemberRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.PullGroupMemberRequest request, Qos1PublishHandler.IMCallback callback) {
        List<WFCMessage.GroupMember> members = new ArrayList<>();
        ErrorCode errorCode = m_messagesStore.getGroupMembers(fromUser, request.getTarget(), request.getHead(), members);

        if (errorCode == ERROR_CODE_SUCCESS) {
            WFCMessage.PullGroupMemberResult result = WFCMessage.PullGroupMemberResult.newBuilder().addAllMember(members).build();
            byte[] data = result.toByteArray();
            ackPayload.ensureWritable(data.length).writeBytes(data);
        }
        return errorCode;
    }
}
