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

@Handler(IMTopic.QuitChatroomTopic)
public class QuitChatroomHandler extends IMHandler<WFCMessage.IDBuf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.IDBuf request, Qos1PublishHandler.IMCallback callback) {
        m_messagesStore.handleQuitChatroom(fromUser, clientID, request.getId());
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
