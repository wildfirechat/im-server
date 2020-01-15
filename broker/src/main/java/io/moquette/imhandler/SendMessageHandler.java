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
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;
import win.liyufan.im.MessageShardingUtil;
import win.liyufan.im.Utility;

import java.util.Set;

import static cn.wildfirechat.proto.ProtoConstants.ContentType.Text;

@Handler(value = IMTopic.SendMessageTopic)
public class SendMessageHandler extends IMHandler<WFCMessage.Message> {
    private int mSensitiveType = 0;  //命中敏感词时，0 失败，1 吞掉， 2 敏感词替换成*。
    private String mForwardUrl = null;
    private int mBlacklistStrategy = 0; //黑名单中时，0失败，1吞掉。
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
            Utility.printExecption(LOG, e);
        }

        try {
            mBlacklistStrategy = Integer.parseInt(mServer.getConfig().getProperty(BrokerConstants.MESSAGE_Blacklist_Strategy));
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.Message message, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        if (message != null) {
            boolean ignoreMsg = false;
            if (!isAdmin) {  //admin do not check the right
                // 不能在端上直接发送撤回和群通知
                if (message.getContent().getType() == 80 || (message.getContent().getType() >= 100 && message.getContent().getType() < 200)) {
                    return ErrorCode.INVALID_PARAMETER;
                }
                int userStatus = m_messagesStore.getUserStatus(fromUser);
                if (userStatus == ProtoConstants.UserStatus.Muted || userStatus == ProtoConstants.UserStatus.Forbidden) {
                    return ErrorCode.ERROR_CODE_FORBIDDEN_SEND_MSG;
                }

                if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Private) {
                    errorCode = m_messagesStore.isAllowUserMessage(message.getConversation().getTarget(), fromUser);
                    if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                        if (errorCode == ErrorCode.ERROR_CODE_IN_BLACK_LIST && mBlacklistStrategy != ProtoConstants.BlacklistStrategy.Message_Reject) {
                            ignoreMsg = true;
                            errorCode = ErrorCode.ERROR_CODE_SUCCESS;
                        } else {
                            return errorCode;
                        }
                    }

                    userStatus = m_messagesStore.getUserStatus(message.getConversation().getTarget());
                    if (userStatus == ProtoConstants.UserStatus.Forbidden) {
                        return ErrorCode.ERROR_CODE_USER_FORBIDDEN;
                    }
                }

                if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Group ) {
                    errorCode = m_messagesStore.canSendMessageInGroup(fromUser, message.getConversation().getTarget());
                    if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                        return errorCode;
                    }
                } else if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_ChatRoom) {
                    if(!m_messagesStore.checkUserClientInChatroom(fromUser, clientID, message.getConversation().getTarget())) {
                        return ErrorCode.ERROR_CODE_NOT_IN_CHATROOM;
                    }
                } else if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Channel) {
                    if(!m_messagesStore.checkUserInChannel(fromUser, message.getConversation().getTarget())) {
                        return ErrorCode.ERROR_CODE_NOT_IN_CHANNEL;
                    }
                }
            }

            long timestamp = System.currentTimeMillis();
            long messageId = MessageShardingUtil.generateId();
            message = message.toBuilder().setFromUser(fromUser).setMessageId(messageId).setServerTimestamp(timestamp).build();

            if (mForwardUrl != null) {
                publisher.forwardMessage(message, mForwardUrl);
            }


            if (!isAdmin && message.getContent().getType() == Text) {
                Set<String> matched = m_messagesStore.handleSensitiveWord(message.getContent().getSearchableContent());
                if (matched != null && !matched.isEmpty()) {
                    m_messagesStore.storeSensitiveMessage(message);
                    if (mSensitiveType == 0) {
                        errorCode = ErrorCode.ERROR_CODE_SENSITIVE_MATCHED;
                    } else if(mSensitiveType == 1) {
                        ignoreMsg = true;
                    } else if(mSensitiveType == 2) {
                        String text = message.getContent().getSearchableContent();
                        for (String word : matched) {
                            text = text.replace(word, "***");
                        }

                        message = message.toBuilder().setContent(message.getContent().toBuilder().setSearchableContent(text).build()).build();
                    } else if(mSensitiveType == 3) {

                    }
                }
            }

            if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                saveAndPublish(fromUser, clientID, message, ignoreMsg);
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
