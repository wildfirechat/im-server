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
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

@Handler(value = IMTopic.LoadRemoteMessagesTopic)
public class LoadRemoteMessagesHandler extends IMHandler<WFCMessage.LoadRemoteMessages> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.LoadRemoteMessages request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;

        long beforeUid = request.getBeforeUid();
        if (beforeUid == 0) {
            beforeUid = Long.MAX_VALUE;
        }
        WFCMessage.PullMessageResult result = m_messagesStore.loadRemoteMessages(fromUser, request.getConversation(), beforeUid, request.getCount());
        byte[] data = result.toByteArray();
        LOG.info("User {} load message with count({}), payload size({})", fromUser, result.getMessageCount(), data.length);
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return errorCode;
    }
}
