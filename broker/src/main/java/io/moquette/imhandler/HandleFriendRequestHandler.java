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
import io.moquette.persistence.MemorySessionStore;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.I18n;
import win.liyufan.im.IMTopic;
import win.liyufan.im.MessageShardingUtil;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(IMTopic.HandleFriendRequestTopic)
public class HandleFriendRequestHandler extends IMHandler<WFCMessage.HandleFriendRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.HandleFriendRequest request, Qos1PublishHandler.IMCallback callback) {
            WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder();
            builder.setFromUser(request.getTargetUid());
            long[] heads = new long[2];
            ErrorCode errorCode = m_messagesStore.handleFriendRequest(fromUser, request, builder, heads, isAdmin);

            if (errorCode == ERROR_CODE_SUCCESS && !isAdmin && builder.getConversation() != null) {
                long messageId = MessageShardingUtil.generateId();
                long timestamp = System.currentTimeMillis();
                builder.setMessageId(messageId);
                builder.setServerTimestamp(timestamp);
                saveAndPublish(request.getTargetUid(), null, builder.build(), false);

                MemorySessionStore.Session session = m_sessionsStore.getSession(clientID);
                String language = "zh_CN";
                if (session != null && !StringUtil.isNullOrEmpty(session.getLanguage())) {
                    language = session.getLanguage();
                }
                WFCMessage.MessageContent.Builder contentBuilder = WFCMessage.MessageContent.newBuilder().setType(90).setContent(I18n.getString(language, "Above_Greeting_Message"));
                
                builder = WFCMessage.Message.newBuilder();
                builder.setFromUser(request.getTargetUid());
                builder.setConversation(WFCMessage.Conversation.newBuilder().setTarget(fromUser).setLine(0).setType(ProtoConstants.ConversationType.ConversationType_Private).build());
                builder.setContent(contentBuilder);
                timestamp = System.currentTimeMillis();
                builder.setServerTimestamp(timestamp);

                messageId = MessageShardingUtil.generateId();
                builder.setMessageId(messageId);
                saveAndPublish(request.getTargetUid(), null, builder.build(), false);

                contentBuilder.setContent(I18n.getString(language, "Friend_Can_Start_Chat"));
                builder.setContent(contentBuilder);
                messageId = MessageShardingUtil.generateId();
                builder.setMessageId(messageId);
                timestamp = System.currentTimeMillis();
                builder.setServerTimestamp(timestamp);
                saveAndPublish(request.getTargetUid(), null, builder.build(), false);

                publisher.publishNotification(IMTopic.NotifyFriendTopic, request.getTargetUid(), heads[0]);
                publisher.publishNotification(IMTopic.NotifyFriendTopic, fromUser, heads[1]);
            }
            return errorCode;
    }
}
