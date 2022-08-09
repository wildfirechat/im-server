/*
 * Copyright (c) 2012-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.moquette.spi.impl;

import cn.wildfirechat.pojos.OutputClient;
import cn.wildfirechat.pojos.OutputMessageData;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import cn.wildfirechat.push.PushServer;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.util.StringUtil;
import cn.wildfirechat.pojos.OutputNotifyChannelSubscribeStatus;
import com.xiaoleilu.loServer.model.FriendData;
import io.moquette.persistence.*;
import io.moquette.persistence.MemorySessionStore.Session;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.Server;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import win.liyufan.im.HttpUtils;
import win.liyufan.im.IMTopic;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.Utility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import static cn.wildfirechat.common.IMExceptionEvent.EventType.EVENT_CALLBACK_Exception;
import static cn.wildfirechat.proto.ProtoConstants.PersistFlag.Transparent;

public class MessagesPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(MessagesPublisher.class);
    private final ConnectionDescriptorStore connectionDescriptors;
    private final ISessionsStore m_sessionsStore;
    private final IMessagesStore m_messagesStore;
    private final PersistentQueueMessageSender messageSender;
    private ConcurrentHashMap<UserClientEntry, Long> chatRoomHeaders = new ConcurrentHashMap<>();
    private ExecutorService chatroomScheduler = Executors.newFixedThreadPool(1);
    private boolean schedulerStarted = false;
    private static ExecutorService executorCallback = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void startChatroomScheduler() {
        schedulerStarted = true;
        chatroomScheduler.execute(() -> {
            while (schedulerStarted) {
                try {
                    if (chatRoomHeaders.size() < 100) {
                        Thread.sleep(500);
                    } else if(chatRoomHeaders.size() < 500) {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                }

                chatRoomHeaders.forEach(100, (s, aLong) -> {
                    chatRoomHeaders.remove(s, aLong);
                    publish2ChatroomReceivers(s.userId, s.clientId, aLong);
                });
            }
        });
    }

    public void stopChatroomScheduler() {
        schedulerStarted = false;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
        chatroomScheduler.shutdown();
    }

    public MessagesPublisher(ConnectionDescriptorStore connectionDescriptors, ISessionsStore sessionsStore,
                             PersistentQueueMessageSender messageSender, HazelcastInstance hz, IMessagesStore messagesStore) {
        this.connectionDescriptors = connectionDescriptors;
        this.m_sessionsStore = sessionsStore;
        this.messageSender = messageSender;
        this.m_messagesStore = messagesStore;
        this.startChatroomScheduler();
    }

    static MqttPublishMessage notRetainedPublish(String topic, MqttQoS qos, ByteBuf message) {
        return notRetainedPublishWithMessageId(topic, qos, message, 0);
    }

    private static MqttPublishMessage notRetainedPublishWithMessageId(String topic, MqttQoS qos, ByteBuf message,
            int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, false, 0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topic, messageId);
        return new MqttPublishMessage(fixedHeader, varHeader, message);
    }

    private void publish2ChatroomReceivers(String user, String clientId, long messageHead) {
        publish2ChatroomReceiversDirectly(user, clientId, messageHead);
    }

    public void publish2ChatroomReceiversDirectly(String user, String clientId, long messageHead) {
        try {
            Session session = m_sessionsStore.getSession(clientId);

            if (session != null) {
                LOG.warn("session for {} not exit", clientId);
                return;
            }
            if(!session.getUsername().equals(user)) {
                LOG.warn("session {} user is not {}", clientId, user);
                return;
            }
            if (!this.connectionDescriptors.isConnected(clientId)) {
                LOG.warn("session {} not connected", clientId);
                return;
            }

            WFCMessage.NotifyMessage notifyMessage = WFCMessage.NotifyMessage
                .newBuilder()
                .setType(ProtoConstants.PullType.Pull_ChatRoom)
                .setHead(messageHead)
                .build();

            ByteBuf payload = Unpooled.buffer();
            byte[] byteData = notifyMessage.toByteArray();
            payload.ensureWritable(byteData.length).writeBytes(byteData);
            MqttPublishMessage publishMsg;
            publishMsg = notRetainedPublish(IMTopic.NotifyMessageTopic, MqttQoS.AT_MOST_ONCE, payload);

            boolean result = !this.messageSender.sendPublish(session.getClientSession(), publishMsg);
            if (!result) {
                LOG.warn("send publish to {} failure", clientId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    private void publish2Receivers(String sender, int conversationType, String target, int line, long messageId, Collection<String> receivers, String pushContent, String pushData, String exceptClientId, int pullType, int messageContentType, long serverTime, int mentionType, List<String> mentionTargets, int persistFlag, boolean directing) {
        if (persistFlag == Transparent) {
            publishTransparentMessage2Receivers(messageId, receivers, pullType, exceptClientId);
            return;
        }

        WFCMessage.Message message = null;
        for (String user : receivers) {
            if (!user.equals(sender)) {
                WFCMessage.User userInfo = m_messagesStore.getUserInfo(user);
                if (userInfo != null && userInfo.getType() == ProtoConstants.UserType.UserType_Robot) {
                    WFCMessage.Robot robot = m_messagesStore.getRobot(user);
                    if (robot != null && !StringUtil.isNullOrEmpty(robot.getCallback())) {
                        if (message == null) {
                            message = m_messagesStore.getMessage(messageId);
                        }
                        OutputClient outputClient = null;
                        if(m_messagesStore.isRobotCallbackWithClientInfo() && !StringUtil.isNullOrEmpty(exceptClientId)) {
                            Session session = m_sessionsStore.getSession(exceptClientId);
                            if(session != null && session.getUsername().equals(sender)) {
                                outputClient = new OutputClient(session.getPlatform(), exceptClientId);
                            }
                        }

                        final WFCMessage.Message finalMsg = message;
                        OutputClient finalOutputClient = outputClient;
                        Server.getServer().getCallbackScheduler().execute(() -> {
                            try {
                                HttpUtils.httpJsonPost(robot.getCallback(), new Gson().toJson(OutputMessageData.fromProtoMessage(finalMsg, finalOutputClient), OutputMessageData.class), HttpUtils.HttpPostType.POST_TYPE_Robot_Message_Callback);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Utility.printExecption(LOG, e, EVENT_CALLBACK_Exception);
                            }
                        });
                        continue;
                    }
                }
            }
            long messageSeq;
            if (pullType != ProtoConstants.PullType.Pull_ChatRoom) {
                messageSeq = m_messagesStore.insertUserMessages(sender, conversationType, target, line, messageContentType, user, messageId, directing);
            } else {
                messageSeq = m_messagesStore.insertChatroomMessages(user, line, messageId);
            }

            Collection<Session> sessions = m_sessionsStore.sessionForUser(user);
            String senderName = null;
            String targetName = null;
            boolean nameLoaded = false;


            Collection<String> targetClients = null;
            if (pullType == ProtoConstants.PullType.Pull_ChatRoom) {
                targetClients = m_messagesStore.getChatroomMemberClient(user);
            }
            boolean isPcOnline = false;

            if (!user.equals(sender)) {
                for (Session targetSession : sessions) {
                    if (targetSession.getPlatform() == ProtoConstants.Platform.Platform_WEB
                        || targetSession.getPlatform() == ProtoConstants.Platform.Platform_Windows
                        || targetSession.getPlatform() == ProtoConstants.Platform.Platform_LINUX
                        || targetSession.getPlatform() == ProtoConstants.Platform.Platform_OSX) {
                        boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
                        if (targetIsActive) {
                            isPcOnline = true;
                            break;
                        }
                    }
                }
            }

            for (Session targetSession : sessions) {
                //超过7天不活跃的用户忽略
                if(System.currentTimeMillis() - targetSession.getLastActiveTime() > 7 * 24 * 60 * 60 * 1000) {
                    continue;
                }

                if (exceptClientId != null && exceptClientId.equals(targetSession.getClientSession().clientID)) {
                    continue;
                }

                if (targetSession.getClientID() == null) {
                    continue;
                }

                if (pullType == ProtoConstants.PullType.Pull_ChatRoom && !targetClients.contains(targetSession.getClientID())) {
                    continue;
                }

                if (pullType == ProtoConstants.PullType.Pull_ChatRoom) {
                    if (exceptClientId != null && exceptClientId.equals(targetSession.getClientID())) {
                        targetSession.refreshLastChatroomActiveTime();
                    }

                    if (!m_messagesStore.checkChatroomParticipantIdelTime(targetSession)) {
                        m_messagesStore.handleQuitChatroom(user, targetSession.getClientID(), target);
                        continue;
                    }
                }

                boolean isSilent;
                boolean isVoipSilent;
                boolean isNoDisturb;
                boolean isConvSilent = false;
                if (pullType == ProtoConstants.PullType.Pull_ChatRoom) {
                    isSilent = true;
                    isConvSilent = true;
                    isVoipSilent = true;
                    isNoDisturb = true;
                } else {
                    isSilent = false;
                    isVoipSilent = false;
                    isNoDisturb = false;

                    if (!user.equals(sender)) {
                        WFCMessage.Conversation conversation;
                        if (conversationType == ProtoConstants.ConversationType.ConversationType_Private) {
                            conversation = WFCMessage.Conversation.newBuilder().setType(conversationType).setLine(line).setTarget(sender).build();
                        } else {
                            conversation = WFCMessage.Conversation.newBuilder().setType(conversationType).setLine(line).setTarget(target).build();
                        }

                        if (m_messagesStore.getUserConversationSilent(user, conversation)) {
                            LOG.info("The conversation {}-{}-{} is slient", conversation.getType(), conversation.getTarget(), conversation.getLine());
                            isConvSilent = true;
                        }

                        if (m_messagesStore.getUserGlobalSilent(user)) {
                            LOG.info("The user {} is global sliented", user);
                            isSilent = true;
                        }

                        if (m_messagesStore.getUserVoipSilent(user)) {
                            LOG.info("The user {} is voip sliented", user);
                            isVoipSilent = true;
                        }

                        if (!isSilent && isPcOnline && m_messagesStore.getSilentWhenPcOnline(user)) {
                            LOG.info("The user {} pc online and silent when pc online", user);
                            isSilent = true;
                        }

                        if (m_messagesStore.isUserNoDisturbing(user)) {
                            LOG.info("The user {} is no disturbing", user);
                            isNoDisturb = true;
                        }
                    }

                    if (!StringUtil.isNullOrEmpty(pushContent) || messageContentType == 400) {
                        if (!isConvSilent && (persistFlag & 0x02) > 0) {
                            targetSession.setUnReceivedMsgs(targetSession.getUnReceivedMsgs() + 1);
                        }
                    }
                }

                boolean needPush = !user.equals(sender);

                boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
                if (targetIsActive) {
                    WFCMessage.NotifyMessage notifyMessage = WFCMessage.NotifyMessage
                        .newBuilder()
                        .setType(pullType)
                        .setHead(messageSeq)
                        .build();

                    ByteBuf payload = Unpooled.buffer();
                    byte[] byteData = notifyMessage.toByteArray();
                    payload.ensureWritable(byteData.length).writeBytes(byteData);
                    MqttPublishMessage publishMsg;
                    publishMsg = notRetainedPublish(IMTopic.NotifyMessageTopic, MqttQoS.AT_MOST_ONCE, payload);

                    boolean sent = this.messageSender.sendPublish(targetSession.getClientSession(), publishMsg);
                    if (sent) {
                        needPush = false;
                    }
                } else {
                    LOG.info("the target {} of user {} is not active", targetSession.getClientID(), targetSession.getUsername());
                }

                if (needPush && pullType != ProtoConstants.PullType.Pull_ChatRoom) {
                    int curMentionType = 0;
                    if (mentionType == 2) {
                        curMentionType = 2;
                        isConvSilent = false;
                    } else if (mentionType == 1){
                        if (mentionTargets != null && mentionTargets.contains(user)) {
                            curMentionType = 1;
                            isConvSilent = false;
                        }
                    }
                    boolean isVoip = messageContentType == 402 || messageContentType == 400 || messageContentType == 401 || messageContentType == 406;

                    if (StringUtil.isNullOrEmpty(pushContent) && StringUtil.isNullOrEmpty(pushData) && !isVoip) {
                        LOG.info("push content is empty and contenttype is {}", messageContentType);
                        continue;
                    }

                    if (StringUtil.isNullOrEmpty(targetSession.getDeviceToken())) {
                        LOG.info("device token not exist");
                        continue;
                    }

                    if (isConvSilent && !isVoip) {
                        LOG.info("Silent of user or conversation");
                        continue;
                    }

                    if (isSilent && !isVoip) {
                        LOG.info("Silent of user");
                        continue;
                    }

                    if(isVoip && isVoipSilent) {
                        LOG.info("voip silent of user");
                        continue;
                    }

                    if(isNoDisturb) {
                        LOG.info("no disturb of user");
                        continue;
                    }

                    boolean isHiddenDetail = m_messagesStore.getUserPushHiddenDetail(user);

                    if(!nameLoaded) {
                        senderName = getUserDisplayName(sender, conversationType == ProtoConstants.ConversationType.ConversationType_Group ? target : null);
                        targetName = getTargetName(target, conversationType);
                        nameLoaded = true;
                    }

                    String name = senderName;
                    if (!sender.equals(user)) {
                        FriendData fd = m_messagesStore.getFriendData(user, sender);
                        if (fd != null && !StringUtil.isNullOrEmpty(fd.getAlias())) {
                            name = fd.getAlias();
                        }
                    }
                    this.messageSender.sendPush(sender, conversationType, target, line, messageId, targetSession.getClientID(), pushContent, pushData, messageContentType, serverTime, name, targetName, targetSession.getUnReceivedMsgs(), curMentionType, isHiddenDetail, targetSession.getLanguage());
                }

            }
        }
    }
    
    public boolean sendOfflineNotify(String clientId) {
        boolean targetIsActive = this.connectionDescriptors.isConnected(clientId);
        if (targetIsActive) {
            ByteBuf payload = Unpooled.buffer();
            payload.ensureWritable(1).writeBytes("1".getBytes());
            MqttPublishMessage publishMsg;
            publishMsg = notRetainedPublish(IMTopic.NotifyOffline, MqttQoS.AT_MOST_ONCE, payload);

            boolean sent = this.messageSender.sendPublish(clientId, publishMsg);
            if (sent) {
                return true;
            }
        } else {
            LOG.info("the target {} is not active", clientId);
        }

        return false;
    }

    private void publishTransparentMessage2Receivers(long messageHead, Collection<String> receivers, int pullType, String exceptClientId) {
        WFCMessage.Message message = m_messagesStore.getMessage(messageHead);

        if (message != null) {
            for (String user : receivers) {
                Collection<Session> sessions = m_sessionsStore.sessionForUser(user);

                for (Session targetSession : sessions) {
                    if(System.currentTimeMillis() - targetSession.getLastActiveTime() > 60 * 60 * 1000) {
                        continue;
                    }

                    if (targetSession.getClientID() == null) {
                        continue;
                    }
                    if (targetSession.getClientID().equals(exceptClientId)) {
                        continue;
                    }

                    boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
                    if (targetIsActive) {
                        ByteBuf payload = Unpooled.buffer();
                        byte[] byteData = message.toByteArray();
                        payload.ensureWritable(byteData.length).writeBytes(byteData);
                        MqttPublishMessage publishMsg;
                        publishMsg = notRetainedPublish(IMTopic.SendMessageTopic, MqttQoS.AT_MOST_ONCE, payload);

                        this.messageSender.sendPublish(targetSession.getClientSession(), publishMsg);
                    } else {
                        LOG.info("the target {} of user {} is not active", targetSession.getClientID(), targetSession.getUsername());
                    }
                }
            }
        }
    }

    String getUserDisplayName(String userId, String groupId) {
        WFCMessage.User user = m_messagesStore.getUserInfo(userId);
        String userName = null;
        if(user != null) {
            userName = user.getDisplayName();
        }
        if (!StringUtil.isNullOrEmpty(groupId)) {
            WFCMessage.GroupMember member = m_messagesStore.getGroupMember(groupId, userId);
            if (member != null && !StringUtil.isNullOrEmpty(member.getAlias())) {
                userName = member.getAlias();
            }
        }
        return userName;
    }

    String getTargetName(String targetId, int cnvType) {
        if (cnvType == ProtoConstants.ConversationType.ConversationType_Private) {
            return getUserDisplayName(targetId, null);
        } else if(cnvType == ProtoConstants.ConversationType.ConversationType_Group) {
            WFCMessage.GroupInfo group = m_messagesStore.getGroupInfo(targetId);
            if(group != null) {
                return group.getName();
            }
        } else if(cnvType == ProtoConstants.ConversationType.ConversationType_Channel) {
            WFCMessage.ChannelInfo channelInfo = m_messagesStore.getChannelInfo(targetId);
            if (channelInfo != null) {
                return channelInfo.getName();
            }
        }
        return null;
    }

    public void publishNotification(String topic, String receiver, long head) {
        publishNotification(topic, receiver, head, null, null);
    }

    public void publishNotification(String topic, String receiver, long head, String exceptClientId) {
        publishNotification(topic, receiver, head, null, null, exceptClientId);
    }

    public void publishNotification(String topic, String receiver, long head, String fromUser, String pushContent) {
        publishNotification(topic, receiver, head, fromUser, pushContent, null);
    }
    public void publishNotification(String topic, String receiver, long head, String fromUser, String pushContent, String exceptClientId) {
        Collection<Session> sessions = m_sessionsStore.sessionForUser(receiver);
        String fromUserName = null;
        for (Session targetSession : sessions) {
            if(targetSession.getClientID().equals(exceptClientId)) {
                continue;
            }

            boolean needPush = !StringUtil.isNullOrEmpty(pushContent);
            boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
            if (targetIsActive) {
                ByteBuf payload = Unpooled.buffer();
                payload.writeLong(head);
                MqttPublishMessage publishMsg;
                publishMsg = notRetainedPublish(topic, MqttQoS.AT_MOST_ONCE, payload);

                boolean result = this.messageSender.sendPublish(targetSession.getClientSession(), publishMsg);
                if (!result) {
                    LOG.warn("Publish friend request failure");
                } else {
                    needPush = false;
                }
            }
            if (needPush) {
                if (fromUserName == null) {
                    WFCMessage.User userInfo = m_messagesStore.getUserInfo(fromUser);
                    if (userInfo == null) {
                        fromUserName = "";
                    } else {
                        fromUserName = userInfo.getDisplayName();
                    }
                }

                if (IMTopic.NotifyFriendRequestTopic.equals(topic)) {
                    messageSender.sendPush(fromUser, receiver, targetSession.getClientID(), pushContent, PushServer.PushMessageType.PUSH_MESSAGE_TYPE_FRIEND_REQUEST, System.currentTimeMillis(), fromUserName, targetSession.getUnReceivedMsgs() + 1, targetSession.getLanguage());
                }
            }
        }
    }

    public void updateChatroomMembersQueue(String chatroomId, int line, long messageId) {
        final long messageSeq = m_messagesStore.insertChatroomMessages(chatroomId, line, messageId);
        Collection<UserClientEntry> members = m_messagesStore.getChatroomMembers(chatroomId);
        for (UserClientEntry member : members
             ) {
            chatRoomHeaders.compute(member, new BiFunction<UserClientEntry, Long, Long>() {
                @Override
                public Long apply(UserClientEntry s, Long aLong) {
                    if (aLong == null)
                        return messageSeq;
                    if (messageSeq > aLong)
                        return messageSeq;
                    return aLong;
                }
            });
        }
    }

    public void publishRecall2ReceiversLocal(long messageUid, String operatorId, Collection<String> receivers, String exceptClientId) {
        for (String user : receivers) {


            Collection<Session> sessions = m_sessionsStore.sessionForUser(user);
            for (Session targetSession : sessions) {
                if (exceptClientId != null && exceptClientId.equals(targetSession.getClientSession().clientID)) {
                    continue;
                }

                if (targetSession.getClientID() == null) {
                    continue;
                }

                boolean targetIsActive = this.connectionDescriptors.isConnected(targetSession.getClientSession().clientID);
                if (targetIsActive) {
                    WFCMessage.NotifyRecallMessage notifyMessage = WFCMessage.NotifyRecallMessage
                        .newBuilder()
                        .setFromUser(operatorId)
                        .setId(messageUid)
                        .build();

                    ByteBuf payload = Unpooled.buffer();
                    byte[] byteData = notifyMessage.toByteArray();
                    payload.ensureWritable(byteData.length).writeBytes(byteData);
                    MqttPublishMessage publishMsg;
                    publishMsg = notRetainedPublish(IMTopic.NotifyRecallMessageTopic, MqttQoS.AT_MOST_ONCE, payload);

                    this.messageSender.sendPublish(targetSession.getClientSession(), publishMsg);
                } else {
                    LOG.info("the target {} of user {} is not active", targetSession.getClientID(), targetSession.getUsername());
                }
            }
        }
    }

    public void publishRecall2Receivers(long messageUid, String operatorId, Set<String> receivers, String exceptClientId) {
        publishRecall2ReceiversLocal(messageUid, operatorId, receivers, exceptClientId);
    }

    public void publish2Receivers(WFCMessage.Message message, Set<String> receivers, String exceptClientId, int pullType) {
        if (message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Channel) {
            WFCMessage.ChannelInfo channelInfo = m_messagesStore.getChannelInfo(message.getConversation().getTarget());
            if (channelInfo != null && !StringUtil.isNullOrEmpty(channelInfo.getCallback()) && channelInfo.getAutomatic() == 1 && !message.getFromUser().equals(channelInfo.getOwner())) {
                OutputClient outputClient = null;
                if(m_messagesStore.isChannelCallbackWithClientInfo() && !StringUtil.isNullOrEmpty(exceptClientId)) {
                    Session session = m_sessionsStore.getSession(exceptClientId);
                    if(session != null && session.getUsername().equals(message.getFromUser())) {
                        outputClient = new OutputClient(session.getPlatform(), exceptClientId);
                    }
                }

                OutputClient finalOutputClient = outputClient;
                Server.getServer().getCallbackScheduler().execute(() -> {
                    try {
                        HttpUtils.httpJsonPost(channelInfo.getCallback() + "/message", new Gson().toJson(OutputMessageData.fromProtoMessage(message, finalOutputClient), OutputMessageData.class), HttpUtils.HttpPostType.POST_TYPE_Channel_Message_Callback);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Utility.printExecption(LOG, e, EVENT_CALLBACK_Exception);
                    }
                });
            } else {
                if (channelInfo == null) {
                    LOG.info("Channel {} not exist", message.getConversation().getTarget());
                } else {
                    LOG.info("Channel {} callback {}", message.getConversation().getTarget(), channelInfo.getCallback());
                }
            }
        }
        long messageId = message.getMessageId();

        String pushContent = message.getContent().getPushContent();
        if (StringUtil.isNullOrEmpty(pushContent)) {
            int type = message.getContent().getType();
            if (type == ProtoConstants.ContentType.Image) {
                pushContent = "[图片]";
            } else if(type == ProtoConstants.ContentType.Location) {
                pushContent = "[位置]";
            } else if(type == ProtoConstants.ContentType.Text) {
                pushContent = message.getContent().getSearchableContent();
            } else if(type == ProtoConstants.ContentType.Voice) {
                pushContent = "[语音]";
            } else if(type == ProtoConstants.ContentType.Video) {
                pushContent = "[视频]";
            } else if(type == ProtoConstants.ContentType.Link) {
                pushContent = "[链接]";
            } else if(type == ProtoConstants.ContentType.File) {
                pushContent = "[文件]";
            } else if(type == ProtoConstants.ContentType.Sticker) {
                pushContent = "[表情]";
            }
        }

        if (message.getContent().getPersistFlag() == Transparent) {
            pushContent = null;
        }

        publish2Receivers(message.getFromUser(),
                    message.getConversation().getType(), message.getConversation().getTarget(), message.getConversation().getLine(),
                    messageId,
                    receivers,
                    pushContent, message.getContent().getPushData(), exceptClientId, pullType, message.getContent().getType(), message.getServerTimestamp(), message.getContent().getMentionedType(), message.getContent().getMentionedTargetList(), message.getContent().getPersistFlag(), message.hasToUser() || !message.getToList().isEmpty());

    }

    public void forwardMessage(final WFCMessage.Message message, String forwardUrl, OutputClient outputClient) {
        Server.getServer().getCallbackScheduler().execute(() -> {
            try {
                HttpUtils.httpJsonPost(forwardUrl, new Gson().toJson(OutputMessageData.fromProtoMessage(message, outputClient), OutputMessageData.class), HttpUtils.HttpPostType.POST_TYPE_Forward_Message_Callback);
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e, EVENT_CALLBACK_Exception);
            }
        });
    }

    public void forwardMessageWithCallback(final WFCMessage.Message message, String forwardUrl, HttpUtils.HttpCallback callback) {
        try {
            HttpUtils.httpJsonPost(forwardUrl, new Gson().toJson(OutputMessageData.fromProtoMessage(message), OutputMessageData.class), new HttpUtils.HttpCallback() {
                @Override
                public void onSuccess(String content) {
                    Server.getServer().getImBusinessScheduler().execute(()->{
                        callback.onSuccess(content);
                    });
                }

                @Override
                public void onFailure(int statusCode, String errorMessage) {
                    Server.getServer().getImBusinessScheduler().execute(()->{
                        callback.onFailure(statusCode, errorMessage);
                    });
                }
            }, HttpUtils.HttpPostType.POST_TYPE_Forward_Message_Callback);
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e, EVENT_CALLBACK_Exception);
        }
    }

    public void notifyChannelListenStatusChanged(WFCMessage.ChannelInfo channelInfo, String user, boolean listen) {
        if (channelInfo == null || StringUtil.isNullOrEmpty(channelInfo.getCallback()) || channelInfo.getAutomatic() == 0) {
            return;
        }

        Server.getServer().getCallbackScheduler().execute(() -> {
            try {
                HttpUtils.httpJsonPost(channelInfo.getCallback() + "/subscribe", new Gson().toJson(new OutputNotifyChannelSubscribeStatus(user, channelInfo.getTargetId(), listen), OutputNotifyChannelSubscribeStatus.class), HttpUtils.HttpPostType.POST_TYPE_Channel_Subscriber_Event_Callback);
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e, EVENT_CALLBACK_Exception);
            }
        });
    }
}
