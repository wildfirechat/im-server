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

package io.moquette.spi;

import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.model.FriendData;
import com.xiaoleilu.loServer.pojos.InputOutputUserBlockStatus;
import io.moquette.persistence.DatabaseStore;
import io.moquette.persistence.MemoryMessagesStore;
import io.moquette.persistence.UserClientEntry;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import win.liyufan.im.ErrorCode;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Defines the SPI to be implemented by a StorageService that handle persistence of messages
 */
public interface IMessagesStore {

    class StoredMessage {

        final MqttQoS m_qos;
        final byte[] m_payload;
        final String m_topic;
        private boolean m_retained;
        private String m_clientID;
        private MessageGUID m_guid;

        public StoredMessage(byte[] message, MqttQoS qos, String topic) {
            m_qos = qos;
            m_payload = message;
            m_topic = topic;
        }

        public MqttQoS getQos() {
            return m_qos;
        }

        public String getTopic() {
            return m_topic;
        }

        public void setGuid(MessageGUID guid) {
            this.m_guid = guid;
        }

        public MessageGUID getGuid() {
            return m_guid;
        }

        public String getClientID() {
            return m_clientID;
        }

        public void setClientID(String m_clientID) {
            this.m_clientID = m_clientID;
        }

        public ByteBuf getPayload() {
            return Unpooled.copiedBuffer(m_payload);
        }

        public void setRetained(boolean retained) {
            this.m_retained = retained;
        }

        public boolean isRetained() {
            return m_retained;
        }

        @Override
        public String toString() {
            return "PublishEvent{clientID='" + m_clientID + '\'' + ", m_retain="
                    + m_retained + ", m_qos=" + m_qos + ", m_topic='" + m_topic + '\'' + '}';
        }
    }

    DatabaseStore getDatabaseStore();
    WFCMessage.Message storeMessage(String fromUser, String fromClientId, WFCMessage.Message message);
	int getNotifyReceivers(String fromUser, WFCMessage.Message message, Set<String> notifyReceivers);
    WFCMessage.PullMessageResult fetchMessage(String user, String exceptClientId, long fromMessageId, int pullType);
    WFCMessage.PullMessageResult loadRemoteMessages(String user, WFCMessage.Conversation conversation, long beforeUid, int count);
    long insertUserMessages(String sender, int conversationType, String target, int line, int messageContentType, String userId, long messageId);
    WFCMessage.GroupInfo createGroup(String operator, WFCMessage.GroupInfo groupInfo, List<WFCMessage.GroupMember> memberList);
    ErrorCode addGroupMembers(String operator, String groupId, List<WFCMessage.GroupMember> memberList);
    ErrorCode kickoffGroupMembers(String operator, String groupId, List<String> memberList);
    ErrorCode quitGroup(String operator, String groupId);
    ErrorCode dismissGroup(String operator, String groupId);
    ErrorCode modifyGroupInfo(String operator, String groupId, int modifyType, String value);
    ErrorCode modifyGroupAlias(String operator, String groupId, String alias);
    List<WFCMessage.GroupInfo> getGroupInfos(List<WFCMessage.UserRequest> requests);
    WFCMessage.GroupInfo getGroupInfo(String groupId);
    ErrorCode getGroupMembers(String groupId, long maxDt, List<WFCMessage.GroupMember> members);
    ErrorCode transferGroup(String operator, String groupId, String newOwner);
    boolean isMemberInGroup(String member, String groupId);
    boolean isForbiddenInGroup(String member, String groupId);

    ErrorCode recallMessage(long messageUid, String operatorId);

    WFCMessage.Robot getRobot(String robotId);
    void addRobot(WFCMessage.Robot robot);
    ErrorCode getUserInfo(List<WFCMessage.UserRequest> requestList, WFCMessage.PullUserResult.Builder builder);
    ErrorCode modifyUserInfo(String userId, WFCMessage.ModifyMyInfoRequest request);

    ErrorCode modifyUserStatus(String userId, int status);
    int getUserStatus(String userId);
    List<InputOutputUserBlockStatus> getUserStatusList();

    void addUserInfo(WFCMessage.User user, String password);
    WFCMessage.User getUserInfo(String userId);
    WFCMessage.User getUserInfoByName(String name);
    WFCMessage.User getUserInfoByMobile(String mobile);
    List<WFCMessage.User> searchUser(String keyword, boolean buzzy, int page);

    void createChatroom(String chatroomId, WFCMessage.ChatroomInfo chatroomInfo);
    void destoryChatroom(String chatroomId);
    WFCMessage.ChatroomInfo getChatroomInfo(String chatroomId);
    WFCMessage.ChatroomMemberInfo getChatroomMemberInfo(String chatroomId, final int maxMemberCount);
    int getChatroomMemberCount(String chatroomId);
    Collection<String> getChatroomMemberClient(String userId);
    boolean checkUserClientInChatroom(String user, String clientId, String chatroomId);

    long insertChatroomMessages(String target, int line, long messageId);
    Collection<UserClientEntry> getChatroomMembers(String chatroomId);
    WFCMessage.PullMessageResult fetchChatroomMessage(String fromUser, String chatroomId, String exceptClientId, long fromMessageId);

    ErrorCode verifyToken(String userId, String token, List<String> serverIPs, List<Integer> ports);
    ErrorCode login(String name, String password, List<String> userIdRet);

    List<FriendData> getFriendList(String userId, long version);
    List<WFCMessage.FriendRequest> getFriendRequestList(String userId, long version);

    ErrorCode saveAddFriendRequest(String userId, WFCMessage.AddFriendRequest request, long[] head);
    ErrorCode handleFriendRequest(String userId, WFCMessage.HandleFriendRequest request, WFCMessage.Message.Builder msgBuilder, long[] heads);
    ErrorCode deleteFriend(String userId, String friendUid);
    ErrorCode blackUserRequest(String fromUser, String targetUserId, int status, long[] head);
    ErrorCode SyncFriendRequestUnread(String userId, long unreadDt, long[] head);
    boolean isBlacked(String fromUser, String userId);
    ErrorCode setFriendAliasRequest(String fromUser, String targetUserId, String alias, long[] head);

    ErrorCode handleJoinChatroom(String userId, String clientId, String chatroomId);
    ErrorCode handleQuitChatroom(String userId, String clientId, String chatroomId);

    ErrorCode getUserSettings(String userId, long version, WFCMessage.GetUserSettingResult.Builder builder);
    WFCMessage.UserSettingEntry getUserSetting(String userId, int scope, String key);
    List<WFCMessage.UserSettingEntry> getUserSetting(String userId, int scope);
    long updateUserSettings(String userId, WFCMessage.ModifyUserSettingReq request);

    boolean getUserGlobalSlient(String userId);
    boolean getUserPushHiddenDetail(String userId);
    boolean getUserConversationSlient(String userId, WFCMessage.Conversation conversation);

    ErrorCode createChannel(String operator, WFCMessage.ChannelInfo channelInfo);
    ErrorCode modifyChannelInfo(String operator, String channelId, int modifyType, String value);
    ErrorCode transferChannel(String operator, String channelId, String newOwner);
    ErrorCode distoryChannel(String operator, String channelId);
    List<WFCMessage.ChannelInfo> searchChannel(String keyword, boolean buzzy, int page);
    ErrorCode listenChannel(String operator, String channelId, boolean listen);
    WFCMessage.ChannelInfo getChannelInfo(String channelId);
    boolean checkUserInChannel(String user, String channelId);

    Set<String> handleSensitiveWord(String message);
    boolean addSensitiveWords(List<String> words);
    boolean removeSensitiveWords(List<String> words);
    List<String> getAllSensitiveWords();

    WFCMessage.Message getMessage(long messageId);
    long getMessageHead(String user);
    long getFriendHead(String user);
    long getFriendRqHead(String user);
    long getSettingHead(String user);

    //使用了数据库，会比较慢，仅能用户用户/群组等id的生成
    String getShortUUID();
    /**
     * Used to initialize all persistent store structures
     */
    void initStore();

    /**
     * Return a list of retained messages that satisfy the condition.
     *
     * @param condition
     *            the condition to match during the search.
     * @return the collection of matching messages.
     */
    Collection<StoredMessage> searchMatching(IMatchingCondition condition);

    void cleanRetained(Topic topic);

    void storeRetained(Topic topic, StoredMessage storedMessage);
}
