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

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_ALREADY_FRIENDS;
import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(IMTopic.HandleFriendRequestTopic)
public class HandleFriendRequestHandler extends IMHandler<WFCMessage.HandleFriendRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.HandleFriendRequest request, Qos1PublishHandler.IMCallback callback) {
            WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder();
            builder.setFromUser(request.getTargetUid());
            long[] heads = new long[4];
            boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;
            ErrorCode errorCode = m_messagesStore.handleFriendRequest(fromUser, request, builder, heads, isAdmin);

            if (errorCode == ERROR_CODE_SUCCESS) {
                if (!isAdmin && builder.getConversation() != null && request.getStatus() == ProtoConstants.FriendRequestStatus.RequestStatus_Accepted) {
                    try {
                        long messageId = MessageShardingUtil.generateId();
                        long timestamp = System.currentTimeMillis();
                        builder.setMessageId(messageId);
                        builder.setServerTimestamp(timestamp);
                        if(!StringUtil.isNullOrEmpty(builder.getContent().getSearchableContent())) {
                            saveAndPublish(request.getTargetUid(), null, builder.build(), requestSourceType);
                        }

                        MemorySessionStore.Session session = m_sessionsStore.getSession(clientID);
                        String language = "zh_CN";
                        if (session != null && !StringUtil.isNullOrEmpty(session.getLanguage())) {
                            language = session.getLanguage();
                        }
                        WFCMessage.MessageContent.Builder contentBuilder = WFCMessage.MessageContent.newBuilder();
                        if (m_messagesStore.isNewFriendWelcomeMessage()) {
                            contentBuilder.setType(92);
                        } else {
                            contentBuilder.setType(90).setContent(I18n.getString(language, "Above_Greeting_Message"));
                        }

                        builder = WFCMessage.Message.newBuilder();
                        builder.setFromUser(request.getTargetUid());
                        builder.setConversation(WFCMessage.Conversation.newBuilder().setTarget(fromUser).setLine(0).setType(ProtoConstants.ConversationType.ConversationType_Private).build());
                        builder.setContent(contentBuilder);
                        builder.setServerTimestamp(++timestamp);

                        messageId = MessageShardingUtil.generateId();
                        builder.setMessageId(messageId);
                        saveAndPublish(request.getTargetUid(), null, builder.build(), requestSourceType);

                        if (m_messagesStore.isNewFriendWelcomeMessage()) {
                            contentBuilder.setType(93);
                        } else {
                            contentBuilder.setContent(I18n.getString(language, "Friend_Can_Start_Chat"));
                        }

                        builder.setContent(contentBuilder);
                        messageId = MessageShardingUtil.generateId();
                        builder.setMessageId(messageId);
                        builder.setServerTimestamp(++timestamp);
                        saveAndPublish(request.getTargetUid(), null, builder.build(), requestSourceType);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (request.getStatus() == ProtoConstants.FriendRequestStatus.RequestStatus_Accepted || isAdmin) {
                    publisher.publishNotification(IMTopic.NotifyFriendTopic, request.getTargetUid(), heads[0]);
                    publisher.publishNotification(IMTopic.NotifyFriendTopic, fromUser, heads[1]);
                }
                if(!isAdmin) {
                    if(heads[2] > 0) {
                        publisher.publishNotification(IMTopic.NotifyFriendRequestTopic, request.getTargetUid(), heads[2], fromUser, null);
                    }
                    if(heads[3] > 0) {
                        publisher.publishNotification(IMTopic.NotifyFriendRequestTopic, fromUser, heads[3], fromUser, null);
                    }
                }
            }
            if(errorCode == ERROR_CODE_ALREADY_FRIENDS) {
                errorCode = ERROR_CODE_SUCCESS;
            }

            return errorCode;
    }
}
