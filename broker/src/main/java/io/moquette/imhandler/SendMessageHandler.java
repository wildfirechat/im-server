/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.pojos.MessagePayload;
import cn.wildfirechat.pojos.OutputClient;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.hazelcast.util.StringUtil;
import io.moquette.BrokerConstants;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import io.netty.buffer.Unpooled;
import win.liyufan.im.HttpUtils;
import win.liyufan.im.IMTopic;
import win.liyufan.im.MessageShardingUtil;
import win.liyufan.im.Utility;

import java.util.HashSet;
import java.util.Set;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;

@Handler(value = IMTopic.SendMessageTopic)
public class SendMessageHandler extends IMHandler<WFCMessage.Message> {
    private int mSensitiveType = 0;  //命中敏感词时，0 失败，1 吞掉， 2 敏感词替换成*。
    private String mForwardUrl = null;
    private String mSensitiveMessageForwardUrl = null;
    private Set<Integer> mForwardMessageTypes = new HashSet<>();
    private String mMentionForwardUrl = null;
    private int mBlacklistStrategy = 0; //黑名单中时，0失败，1吞掉。
    private boolean mNoForwardAdminMessage = false;

    private String mRemoteSensitiveServerUrl = null;
    private Set<Integer> mRemoteSensitiveMessageTypes;
    private boolean mWaitRemoteSensitiveServerResponse = false;

    public SendMessageHandler() {
        super();

        mForwardUrl = mServer.getConfig().getProperty(BrokerConstants.MESSAGE_Forward_Url);
        if (!StringUtil.isNullOrEmpty(mForwardUrl)) {
            String forwardTypes = mServer.getConfig().getProperty(BrokerConstants.MESSAGE_Forward_Types);
            if (!StringUtil.isNullOrEmpty(forwardTypes)) {
                String[] tss = forwardTypes.split(",");
                for (String ts:tss) {
                    mForwardMessageTypes.add(Integer.parseInt(ts.trim()));
                }
            }
        }
        mSensitiveMessageForwardUrl = mServer.getConfig().getProperty(BrokerConstants.MESSAGE_Sensitive_Forward_Url);
        mMentionForwardUrl = mServer.getConfig().getProperty(BrokerConstants.MESSAGE_MentionMsg_Forward_Url);

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
        try {
            mRemoteSensitiveServerUrl = mServer.getConfig().getProperty(BrokerConstants.SENSITIVE_Remote_Server_URL);
            if(!StringUtil.isNullOrEmpty(mRemoteSensitiveServerUrl)) {
                mWaitRemoteSensitiveServerResponse = Boolean.parseBoolean(mServer.getConfig().getProperty(BrokerConstants.SENSITIVE_Remote_Fail_When_Matched, "false"));
                String types = mServer.getConfig().getProperty(BrokerConstants.SENSITIVE_Remote_Message_Type, "");
                mRemoteSensitiveMessageTypes = new HashSet<>();
                if(!StringUtil.isNullOrEmpty(types)) {
                    String[] ts = types.split(",");
                    for (String t:ts) {
                        mRemoteSensitiveMessageTypes.add(Integer.parseInt(t.trim()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }

        try {
            mNoForwardAdminMessage = Boolean.parseBoolean(mServer.getConfig().getProperty(BrokerConstants.MESSAGE_NO_Forward_Admin_Message, "false"));
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }

    }

    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.Message message, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        boolean isAdmin = requestSourceType == ProtoConstants.RequestSourceType.Request_From_Admin;
        if (message != null) {
            boolean ignoreMsg = false;
            if (!isAdmin) {  //admin do not check the right
                // 不能在端上直接发送撤回和群通知
                if (message.getContent().getType() == 80 || message.getContent().getType() == 81 || (message.getContent().getType() >= 100 && message.getContent().getType() < 200)) {
                    return ErrorCode.INVALID_PARAMETER;
                }

                if(m_messagesStore.getClientForbiddenSendTypes().contains(message.getContent().getType())) {
                    return ErrorCode.ERROR_CODE_NOT_RIGHT;
                }

                int userStatus = m_messagesStore.getUserStatus(fromUser);
                if (userStatus == ProtoConstants.UserStatus.Muted || userStatus == ProtoConstants.UserStatus.Forbidden) {
                    if(!m_messagesStore.getGlobalMuteExceptionTypes().contains(message.getContent().getType())) {
                        return ErrorCode.ERROR_CODE_FORBIDDEN_SEND_MSG;
                    }
                }

                if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Private) {
                    if(!m_messagesStore.getBlackListExceptionTypes().contains(message.getContent().getType())) {
                        errorCode = m_messagesStore.isAllowUserMessage(message.getConversation().getTarget(), fromUser);
                        if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                            if (errorCode == ErrorCode.ERROR_CODE_IN_BLACK_LIST && mBlacklistStrategy != ProtoConstants.BlacklistStrategy.Message_Reject) {
                                ignoreMsg = true;
                                errorCode = ErrorCode.ERROR_CODE_SUCCESS;
                            } else {
                                return errorCode;
                            }
                        }
                    }

                    userStatus = m_messagesStore.getUserStatus(message.getConversation().getTarget());
                    if (userStatus == ProtoConstants.UserStatus.Forbidden) {
                        return ErrorCode.ERROR_CODE_USER_FORBIDDEN;
                    }
                }

                if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Group ) {
                    if(!m_messagesStore.getGroupMuteExceptionTypes().contains(message.getContent().getType())) {
                        errorCode = m_messagesStore.canSendMessageInGroup(fromUser, message.getConversation().getTarget());
                        if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                            return errorCode;
                        }
                    }
                } else if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_ChatRoom) {
                    if(!m_messagesStore.getGroupMuteExceptionTypes().contains(message.getContent().getType())) {
                        if (!m_messagesStore.checkUserClientInChatroom(fromUser, clientID, message.getConversation().getTarget())) {
                            return ErrorCode.ERROR_CODE_NOT_IN_CHATROOM;
                        }
                    }
                } else if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Channel) {
                    if(!m_messagesStore.getGroupMuteExceptionTypes().contains(message.getContent().getType())) {
                        if (!m_messagesStore.canSendMessageInChannel(fromUser, message.getConversation().getTarget())) {
                            return ErrorCode.ERROR_CODE_NOT_IN_CHANNEL;
                        }
                    }
                }
            }

            long timestamp = System.currentTimeMillis();
            final long messageId;
            try {
                messageId = MessageShardingUtil.generateId();
            } catch (Exception e) {
                e.printStackTrace();
                return ErrorCode.ERROR_CODE_SERVER_ERROR;
            }
            message = message.toBuilder().setFromUser(fromUser).setMessageId(messageId).setServerTimestamp(timestamp).build();

            OutputClient outputClient = null;
            if(m_messagesStore.isForwardMessageWithClientInfo() && requestSourceType == ProtoConstants.RequestSourceType.Request_From_User && !StringUtil.isNullOrEmpty(clientID)) {
                MemorySessionStore.Session session = m_sessionsStore.getSession(clientID);
                if(session != null && session.getUsername().equals(fromUser)) {
                    outputClient = new OutputClient(session.getPlatform(), clientID);
                }
            }

            if (!StringUtil.isNullOrEmpty(mForwardUrl) && (mForwardMessageTypes.isEmpty() || mForwardMessageTypes.contains(message.getContent().getType())) && !(isAdmin && mNoForwardAdminMessage)) {
                publisher.forwardMessage(message, mForwardUrl, outputClient);
            }

            if(!StringUtil.isNullOrEmpty(mMentionForwardUrl) && message.hasContent() && message.getContent().getMentionedType() != 0 && !(isAdmin && mNoForwardAdminMessage)) {
                publisher.forwardMessage(message, mMentionForwardUrl, outputClient);
            }

            if (!isAdmin) {
                if(StringUtil.isNullOrEmpty(mRemoteSensitiveServerUrl)) {
                    Set<String> matched = m_messagesStore.handleSensitiveWord(message.getContent().getSearchableContent());
                    if (matched != null && !matched.isEmpty()) {
                        m_messagesStore.storeSensitiveMessage(message);
                        if (!StringUtil.isNullOrEmpty(mSensitiveMessageForwardUrl)) {
                            publisher.forwardMessage(message, mSensitiveMessageForwardUrl, outputClient);
                        }
                        if (mSensitiveType == 0) {
                            errorCode = ErrorCode.ERROR_CODE_SENSITIVE_MATCHED;
                        } else if (mSensitiveType == 1) {
                            ignoreMsg = true;
                        } else if (mSensitiveType == 2) {
                            String text = message.getContent().getSearchableContent();
                            for (String word : matched) {
                                text = text.replace(word, "***");
                            }
                            message = message.toBuilder().setContent(message.getContent().toBuilder().setSearchableContent(text).build()).build();
                        }
                    }
                } else {
                    if(mRemoteSensitiveMessageTypes.contains(message.getContent().getType())) {
                        final WFCMessage.Message finalMsg = message;
                        publisher.forwardMessageWithCallback(message, mRemoteSensitiveServerUrl, new HttpUtils.HttpCallback() {
                            @Override
                            public void onSuccess(String content) {
                                if(StringUtil.isNullOrEmpty(content)) {
                                    saveAndPublish(fromUser, clientID, finalMsg, requestSourceType);
                                } else {
                                    MessagePayload payload = new Gson().fromJson(content, MessagePayload.class);
                                    if (payload != null && payload.getType() > 0) {
                                        WFCMessage.Message newMsg = finalMsg.toBuilder().setContent(payload.toProtoMessageContent()).build();
                                        saveAndPublish(fromUser, clientID, newMsg, requestSourceType);
                                    } else {
                                        LOG.error("Response content {} from censor is invalid payload, ignore response and send original message", content);
                                        saveAndPublish(fromUser, clientID, finalMsg, requestSourceType);
                                    }
                                }
                                if(mWaitRemoteSensitiveServerResponse) {
                                    ByteBuf ackPayload = Unpooled.buffer(21);
                                    ErrorCode errorCode = ERROR_CODE_SUCCESS;
                                    ackPayload.writeByte(errorCode.getCode());
                                    ackPayload.writeLong(messageId);
                                    ackPayload.writeLong(timestamp);
                                    callback.onIMHandled(ErrorCode.ERROR_CODE_SUCCESS, ackPayload);
                                }
                            }

                            @Override
                            public void onFailure(int statusCode, String errorMessage) {
                                ByteBuf ackPayload = null;
                                ErrorCode errorCode = ERROR_CODE_SUCCESS;
                                if(statusCode == 403) {
                                    if(mWaitRemoteSensitiveServerResponse) {
                                        errorCode = ErrorCode.ERROR_CODE_SENSITIVE_MATCHED;
                                        ackPayload = Unpooled.buffer(1);
                                        ackPayload.writeByte(errorCode.getCode());
                                    }
                                } else {
                                    LOG.warn("Failed to censor message with status {}, errorMessage {}. Send the original message", statusCode, errorMessage);
                                    saveAndPublish(fromUser, clientID, finalMsg, requestSourceType);

                                    if(mWaitRemoteSensitiveServerResponse) {
                                        errorCode = ERROR_CODE_SUCCESS;
                                        ackPayload = Unpooled.buffer(21);
                                        ackPayload.writeByte(errorCode.getCode());
                                        ackPayload.writeLong(messageId);
                                        ackPayload.writeLong(timestamp);
                                    }
                                }

                                if(mWaitRemoteSensitiveServerResponse) {
                                    callback.onIMHandled(errorCode, ackPayload);
                                }
                            }
                        });

                        if(mWaitRemoteSensitiveServerResponse) {
                            ackPayload.clear();
                            return ErrorCode.INVALID_ASYNC_HANDLING;
                        } else {
                            ackPayload = ackPayload.capacity(20);
                            ackPayload.writeLong(messageId);
                            ackPayload.writeLong(timestamp);
                            return ErrorCode.ERROR_CODE_SUCCESS;
                        }
                    }
                }
            }

            if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                if(!ignoreMsg) {
                    saveAndPublish(fromUser, clientID, message, requestSourceType);
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
