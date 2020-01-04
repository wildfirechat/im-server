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
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

import java.util.LinkedHashSet;
import java.util.Set;

@Handler(value = IMTopic.RecallMessageTopic)
public class RecallMessageHandler extends IMHandler<WFCMessage.INT64Buf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.INT64Buf int64Buf, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = m_messagesStore.recallMessage(int64Buf.getId(), fromUser, isAdmin);

        if(errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
            return errorCode;
        }

        Set<String> notifyReceivers = new LinkedHashSet<>();
        WFCMessage.Message message = m_messagesStore.getMessage(int64Buf.getId());
        if (message == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        publish(fromUser, clientID, message);


        //等待客户端实现根据撤回消息更新内容，之后可以删掉这段代码
        m_messagesStore.getNotifyReceivers(fromUser, message.toBuilder(), notifyReceivers, false);
        this.publisher.publishRecall2Receivers(int64Buf.getId(), fromUser, notifyReceivers, clientID);

        return errorCode;
    }

}
