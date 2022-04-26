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

@Handler(IMTopic.DestroyUserTopic)
public class DestroyUserHandler extends IMHandler<WFCMessage.IDBuf> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.IDBuf request, Qos1PublishHandler.IMCallback callback) {
        boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;
        if (isAdmin) {
            mServer.getImBusinessScheduler().execute(()-> {
                m_sessionsStore.clearUserSession(fromUser);
                m_messagesStore.clearUserMessages(fromUser);
                m_messagesStore.clearUserSettings(fromUser);
                m_messagesStore.clearUserFriend(fromUser);
                m_messagesStore.clearUserGroups(fromUser);
                m_messagesStore.clearUserChannels(fromUser);
                m_messagesStore.destroyUser(fromUser);

                m_messagesStore.destroyRobot(fromUser);
            });
            return ErrorCode.ERROR_CODE_SUCCESS;
        } else {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }
    }
}
