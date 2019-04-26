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
import com.hazelcast.util.StringUtil;
import io.moquette.BrokerConstants;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;
import win.liyufan.im.MessageShardingUtil;
import win.liyufan.im.SensitiveFilter;

import java.util.Set;

import static cn.wildfirechat.proto.ProtoConstants.ContentType.Text;

@Handler(value = IMTopic.SendMessageTopic)
public class SendMessageHandler extends IMHandler<WFCMessage.Message> {
    private int mSensitiveType = 0;  //命中敏感词时，0 失败，1 吞掉， 2 敏感词替换成*。
    private String mForwardUrl = null;
    public SendMessageHandler() {
        super();

        String forwardUrl = mServer.getConfig().getProperty(BrokerConstants.MESSAGE_Forward_Url);
        if (!StringUtil.isNullOrEmpty(forwardUrl)) {
            mForwardUrl = forwardUrl;
        }

        try {
            mSensitiveType = Integer.parseInt(mServer.getConfig().getProperty(BrokerConstants.SENSITIVE_Filter_Type));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.Message message, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        if (message != null) {
            if (!isAdmin) {  //admin do not check the right
                int userStatus = m_messagesStore.getUserStatus(fromUser);
                if (userStatus == 1 || userStatus == 2) {
                    return ErrorCode.ERROR_CODE_FORBIDDEN_SEND_MSG;
                }

                if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Private) {
                    if (m_messagesStore.isBlacked(message.getConversation().getTarget(), fromUser)) {
                        return ErrorCode.ERROR_CODE_IN_BLACK_LIST;
                    }
                }


                if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Group && !m_messagesStore.isMemberInGroup(fromUser, message.getConversation().getTarget())) {
                    return ErrorCode.ERROR_CODE_NOT_IN_GROUP;
                } else if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Group && m_messagesStore.isForbiddenInGroup(fromUser, message.getConversation().getTarget())) {
                    return ErrorCode.ERROR_CODE_NOT_RIGHT;
                } else if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_ChatRoom && !m_messagesStore.checkUserClientInChatroom(fromUser, clientID, message.getConversation().getTarget())) {
                    return ErrorCode.ERROR_CODE_NOT_IN_CHATROOM;
                } else if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Channel && !m_messagesStore.checkUserInChannel(fromUser, message.getConversation().getTarget())) {
                    return ErrorCode.ERROR_CODE_NOT_IN_CHANNEL;
                }
            }

            long timestamp = System.currentTimeMillis();
            long messageId = MessageShardingUtil.generateId();
            message = message.toBuilder().setFromUser(fromUser).setMessageId(messageId).setServerTimestamp(timestamp).build();

            if (mForwardUrl != null) {
                publisher.forwardMessage(message, mForwardUrl);
            }

            boolean ignoreMsg = false;
            if (!isAdmin && message.getContent().getType() == Text) {
                Set<String> matched = m_messagesStore.handleSensitiveWord(message.getContent().getSearchableContent());
                if (matched != null && !matched.isEmpty()) {
                    if (mSensitiveType == 0) {
                        errorCode = ErrorCode.ERROR_CODE_SENSITIVE_MATCHED;
                    } else if(mSensitiveType == 1) {
                        ignoreMsg = true;
                    } else {
                        String text = message.getContent().getSearchableContent();
                        for (String word : matched) {
                            text = text.replace(word, "***");
                        }

                        message = message.toBuilder().setContent(message.getContent().toBuilder().setSearchableContent(text).build()).build();
                    }
                }
            }

            if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                if (!ignoreMsg) {
                    saveAndPublish(fromUser, clientID, message);
                }
                ackPayload = ackPayload.capacity(20);
                ackPayload.writeLong(messageId);
                ackPayload.writeLong(timestamp);
            }
        } else {
            errorCode = ErrorCode.ERROR_CODE_INVALID_MESSAGE;
        }
        return errorCode;
    }

}
