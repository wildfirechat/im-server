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

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(IMTopic.RriendRequestUnreadSyncTopic)
public class SyncFriendRequestUnreadHandler extends GroupHandler<WFCMessage.Version> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.Version request, Qos1PublishHandler.IMCallback callback) {
            long[] head = new long[1];
            ErrorCode errorCode = m_messagesStore.SyncFriendRequestUnread(fromUser, request.getVersion(), head);
            if (errorCode == ERROR_CODE_SUCCESS) {
                publisher.publishNotification(IMTopic.NotifyFriendRequestTopic, fromUser, head[0]);
            }
            return errorCode;
    }
}
