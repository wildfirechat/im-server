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

package io.moquette.persistence;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.protobuf.ByteString;
import com.hazelcast.core.*;
import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.model.FriendData;
import com.xiaoleilu.loServer.pojos.InputOutputUserBlockStatus;
import io.moquette.server.Server;
import io.moquette.spi.IMatchingCondition;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.impl.security.TokenAuthenticator;
import io.moquette.spi.security.Tokenor;
import io.moquette.spi.impl.subscriptions.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;
import win.liyufan.im.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;


import static cn.wildfirechat.proto.ProtoConstants.ChannelStatus.Channel_Status_Destoryed;
import static cn.wildfirechat.proto.ProtoConstants.ModifyChannelInfoType.*;
import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_Extra;
import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_Name;
import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_Portrait;
import static cn.wildfirechat.proto.ProtoConstants.PersistFlag.Transparent;
import static io.moquette.BrokerConstants.*;
import static io.moquette.server.Constants.MAX_CHATROOM_MESSAGE_QUEUE;
import static io.moquette.server.Constants.MAX_MESSAGE_QUEUE;
import static win.liyufan.im.MyInfoType.*;

public class MemoryMessagesStore implements IMessagesStore {
    private static final String MESSAGES_MAP = "messages_map";
    private static final String GROUPS_MAP = "groups_map";
    private static final String GROUP_ID_COUNTER = "group_id_counter";
    static int dumy = 0;
    static final String GROUP_MEMBERS = "group_members";

    private static final String CHATROOM_MEMBER_IDS = "chatroom_members";
    private static final String USER_CHATROOM = "user_chatroom";

    static final String USER_FRIENDS = "user_friends";
    static final String USER_FRIENDS_REQUEST = "user_friends_request";

    static final String USERS = "users";

    static final String USER_STATUS = "user_status";

    private static final String CHATROOMS = "chatrooms";

    private static final String USER_SETTING = "user_setting";

    private static final String CHANNELS = "channels_map";

    private static final String CHANNEL_LISTENERS = "channel_listeners";

    static final String USER_ROBOTS = "user_robots";
    static final String USER_THINGS = "user_things";

    static final String ROBOTS = "robots";
    static final String THINGS = "things";

    private static final Logger LOG = LoggerFactory.getLogger(MemoryMessagesStore.class);

    private Map<Topic, StoredMessage> m_retainedStore = new HashMap<>();

    private final Server m_Server;

    private Map<String, TreeMap<Long, Long>> userMessages = new HashMap<>();

    private Map<String, TreeMap<Long, Long>> chatroomMessages = new HashMap<>();

    private ReadWriteLock mLock = new ReentrantReadWriteLock();
    private Lock mReadLock = mLock.readLock();
    private Lock mWriteLock = mLock.writeLock();

    private final DatabaseStore databaseStore;
    private ConcurrentHashMap<String, Long> userMaxPullSeq = new ConcurrentHashMap<>();

    private SensitiveFilter mSensitiveFilter;
    private volatile long lastUpdateSensitiveTime = 0;

    private ConcurrentHashMap<String, Boolean> userGlobalSlientMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Boolean> userPushHiddenDetail = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Boolean> userConvSlientMap = new ConcurrentHashMap<>();

    MemoryMessagesStore(Server server, DatabaseStore databaseStore) {
        m_Server = server;
        this.databaseStore = databaseStore;
    }

    @Override
    public void initStore() {
        //TODO reload data from mysql
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(MemoryMessagesStore.GROUP_MEMBERS);
        if (groupMembers.size() == 0) {
            databaseStore.reloadGroupMemberFromDB(hzInstance);
            databaseStore.reloadFriendsFromDB(hzInstance);
            databaseStore.reloadFriendRequestsFromDB(hzInstance);
        }

        updateSensitiveWord();
    }

    private void updateSensitiveWord() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateSensitiveTime > 2 * 60 * 60 * 1000) {
            lastUpdateSensitiveTime = now;
            Set<String> sensitiveWords = databaseStore.getSensitiveWord();
            mSensitiveFilter = new SensitiveFilter(sensitiveWords);
        }
    }

    @Override
    public DatabaseStore getDatabaseStore() {
        return databaseStore;
    }

    @Override
    public WFCMessage.Message storeMessage(String fromUser, String fromClientId, WFCMessage.Message message) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<Long, MessageBundle> mIMap = hzInstance.getMap(MESSAGES_MAP);


        MessageBundle messageBundle = new MessageBundle(message.getMessageId(), fromUser, fromClientId, message);
        if (message.getContent().getPersistFlag() ==  Transparent) {
            mIMap.put(message.getMessageId(), messageBundle, 10, TimeUnit.SECONDS);
        } else {
            mIMap.put(message.getMessageId(), messageBundle, 7, TimeUnit.DAYS);
        }

        return message;
    }

    @Override
    public int getNotifyReceivers(String fromUser, WFCMessage.Message message, Set<String> notifyReceivers) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        int type = message.getConversation().getType();

        int pullType = 0;
        if (type == ProtoConstants.ConversationType.ConversationType_Private) {
            notifyReceivers.add(fromUser);
            notifyReceivers.add(message.getConversation().getTarget());
            pullType = ProtoConstants.PullType.Pull_Normal;
        } else if (type == ProtoConstants.ConversationType.ConversationType_Group) {
            notifyReceivers.add(fromUser);

            if (!StringUtil.isNullOrEmpty(message.getToUser())) {
                notifyReceivers.add(message.getToUser());
            } else if(!message.getToList().isEmpty()) {
                notifyReceivers.addAll(message.getToList());
            } else {
                MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);
                Collection<WFCMessage.GroupMember> members = groupMembers.get(message.getConversation().getTarget());
                for (WFCMessage.GroupMember member : members) {
                    if (member.getType() != ProtoConstants.GroupMemberType.GroupMemberType_Removed) {
                        notifyReceivers.add(member.getMemberId());
                    }
                }
            }

            //如果是群助手的消息，返回pull type group，否则返回normal
            //群助手还没有实现
            pullType = ProtoConstants.PullType.Pull_Normal;
        } else if (type == ProtoConstants.ConversationType.ConversationType_ChatRoom) {
            boolean isDirect = !StringUtil.isNullOrEmpty(message.getToUser());
            Collection<UserClientEntry> entries = getChatroomMembers(message.getConversation().getTarget());
            for (UserClientEntry entry : entries) {
                if (isDirect) {
                    if (entry.userId.equals(message.getToUser())) {
                        notifyReceivers.add(message.getToUser());
                        break;
                    }
                } else {
                    notifyReceivers.add(entry.userId);
                }
            }

            pullType = ProtoConstants.PullType.Pull_ChatRoom;
        } else if(type == ProtoConstants.ConversationType.ConversationType_Channel) {
            WFCMessage.ChannelInfo channelInfo = getChannelInfo(message.getConversation().getTarget());
            if (channelInfo != null) {
                notifyReceivers.add(fromUser);
                if (channelInfo.getOwner().equals(fromUser)) {
                    if (StringUtil.isNullOrEmpty(message.getToUser())) {
                        MultiMap<String, String> listeners = hzInstance.getMultiMap(CHANNEL_LISTENERS);
                        notifyReceivers.addAll(listeners.get(message.getConversation().getTarget()));
                    } else {
                        MultiMap<String, String> listeners = hzInstance.getMultiMap(CHANNEL_LISTENERS);
                        if (listeners.values().contains(message.getToUser())) {
                            notifyReceivers.add(message.getToUser());
                        }
                    }
                } else {
                    if (StringUtil.isNullOrEmpty(channelInfo.getCallback()) || channelInfo.getAutomatic() == 0) {
                        notifyReceivers.add(channelInfo.getOwner());
                    }
                }
            } else {
                LOG.error("Channel not exist");
            }
        }

        if (message.getContent().getPersistFlag() == Transparent) {
            notifyReceivers.remove(fromUser);
        }

        return pullType;
    }

    @Override
    public WFCMessage.PullMessageResult fetchMessage(String user, String exceptClientId, long fromMessageId, int pullType) {
        WFCMessage.PullMessageResult.Builder builder = WFCMessage.PullMessageResult.newBuilder();

        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<Long, MessageBundle> mIMap = hzInstance.getMap(MESSAGES_MAP);

        MemorySessionStore.Session session = m_Server.getStore().sessionsStore().getSession(exceptClientId);
        session.refreshLastActiveTime();
        if (pullType != ProtoConstants.PullType.Pull_ChatRoom) {
            session.setUnReceivedMsgs(0);
        } else {
            session.refreshLastChatroomActiveTime();
        }

        String chatroomId = null;
        if (pullType == ProtoConstants.PullType.Pull_ChatRoom) {
            chatroomId = (String)m_Server.getHazelcastInstance().getMap(USER_CHATROOM).get(user);
        }


        long head = fromMessageId;
        long current = fromMessageId;

        TreeMap<Long, Long> maps;

        if (pullType != ProtoConstants.PullType.Pull_ChatRoom) {
            maps = userMessages.get(user);
            if (maps == null) {
                loadUserMessages(user);
            }
            maps = userMessages.get(user);
        } else {
            maps = chatroomMessages.get(user);
            if (maps == null) {
                mWriteLock.lock();
                try {
                    maps = chatroomMessages.get(user);
                    if (maps == null) {
                        maps = new TreeMap<>();
                        chatroomMessages.put(user, maps);
                    }
                } finally {
                    mWriteLock.unlock();
                }
            }
        }

        mReadLock.lock();
        int size = 0;
        try {
            if (pullType != ProtoConstants.PullType.Pull_ChatRoom) {
                userMaxPullSeq.compute(user, (s, aLong) -> {
                    if (aLong == null || aLong < fromMessageId) {
                        return fromMessageId;
                    } else {
                        return aLong;
                    }
                });
            }

            while (true) {
                Map.Entry<Long, Long> entry = maps.higherEntry(current);
                if (entry == null) {
                    break;
                }
                current = entry.getKey();
                long targetMessageId = entry.getValue();

                MessageBundle bundle = mIMap.get(targetMessageId);
                if (bundle == null) {
                    bundle = databaseStore.getMessage(targetMessageId);
                }

                if (bundle != null) {
                    if (exceptClientId == null || !exceptClientId.equals(bundle.getFromClientId()) || !user.equals(bundle.getFromUser())) {

                        if (pullType == ProtoConstants.PullType.Pull_ChatRoom) {
                            if (!bundle.getMessage().getConversation().getTarget().equals(chatroomId)) {
                                continue;
                            }
                        }

                        size += bundle.getMessage().getSerializedSize();
                        if (size >= 1 * 1024 * 1024) { //3M
                            break;
                        }
                        builder.addMessage(bundle.getMessage());
                    }
                }
            }

            Map.Entry<Long, Long> lastEntry = maps.lastEntry();
            if (lastEntry != null) {
                head = lastEntry.getKey();
            }
        } finally {
            mReadLock.unlock();
        }

        builder.setCurrent(current);
        builder.setHead(head);
        return builder.build();
    }

    @Override
    public WFCMessage.PullMessageResult loadRemoteMessages(String user, WFCMessage.Conversation conversation, long beforeUid, int count) {
        WFCMessage.PullMessageResult.Builder builder = WFCMessage.PullMessageResult.newBuilder();
        List<WFCMessage.Message> messages = databaseStore.loadRemoteMessages(user, conversation, beforeUid, count);
        builder.setCurrent(0).setHead(0);
        if(messages != null) {
            builder.addAllMessage(messages);
        }
        return builder.build();
    }

    @Override
    public Collection<UserClientEntry> getChatroomMembers(String chatroomId) {
        MultiMap<String, UserClientEntry> chatroomMembers = m_Server.getHazelcastInstance().getMultiMap(CHATROOM_MEMBER_IDS);
        Collection<UserClientEntry> members = chatroomMembers.get(chatroomId);
        return members;
    }
    //Todo chatroom
    @Override
    public WFCMessage.PullMessageResult fetchChatroomMessage(String fromUser, String chatroomId, String exceptClientId, long fromMessageId) {
        WFCMessage.PullMessageResult.Builder builder = WFCMessage.PullMessageResult.newBuilder();

        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<Long, MessageBundle> mIMap = hzInstance.getMap(MESSAGES_MAP);


        long head = fromMessageId;
        long current = fromMessageId;

        TreeMap<Long, Long> maps = chatroomMessages.get(chatroomId);
        if (maps == null) {
            mWriteLock.lock();
            try {
                maps = chatroomMessages.get(chatroomId);
                if (maps == null) {
                    maps = new TreeMap<>();
                    chatroomMessages.put(chatroomId, maps);
                }
            } finally {
                mWriteLock.unlock();
            }
        }

        mReadLock.lock();
        int size = 0;
        try {
            maps = chatroomMessages.get(chatroomId);

            while (true) {
                Map.Entry<Long, Long> entry = maps.higherEntry(current);
                if (entry == null) {
                    break;
                }
                current = entry.getKey();
                long targetMessageId = entry.getValue();

                MessageBundle bundle = mIMap.get(targetMessageId);
                if (bundle == null) {
                    bundle = databaseStore.getMessage(targetMessageId);
                }

                if (bundle != null) {
                    if (exceptClientId == null || !exceptClientId.equals(bundle.getFromClientId()) || !fromUser.equals(bundle.getFromUser())) {
                        size += bundle.getMessage().getSerializedSize();
                        if (size >= 3 * 1024 * 1024) { //3M
                            break;
                        }
                        builder.addMessage(bundle.getMessage());
                    }
                }
            }

            Map.Entry<Long, Long> lastEntry = maps.lastEntry();
            if (lastEntry != null) {
                head = lastEntry.getKey();
            }
        } finally {
            mReadLock.unlock();
        }

        builder.setCurrent(current);
        builder.setHead(head);
        return builder.build();
    }

    @Override
    public long insertUserMessages(String sender, int conversationType, String target, int line, int messageContentType, String user, long messageId) {
        // messageId是全局的，messageSeq是跟个人相关的，理论上messageId的增长数度远远大于seq。
        // 考虑到一种情况，当服务器发生变化，用户发生迁移后，messageSeq还需要保持有序。 要么把Seq持久化，要么在迁移后Seq取一个肯定比以前更大的数字（这个数字就是messageId）
        // 这里选择使用后面一种情况
        long messageSeq = 0;

        if (conversationType == ProtoConstants.ConversationType.ConversationType_ChatRoom) {
            return insertChatroomMessages(target, line, messageId);
        }


        mWriteLock.lock();
        try {
            TreeMap<Long, Long> maps = userMessages.get(user);
            if (maps == null) {
                maps = databaseStore.reloadUserMessageMaps(user);
                userMessages.put(user, maps);
            }

            Map.Entry<Long, Long> lastEntry = maps.lastEntry();
            if (lastEntry != null) {
                messageSeq = (lastEntry.getKey() + 1);
            }
            Long maxPullSeq = userMaxPullSeq.get(user);
            if (maxPullSeq != null && maxPullSeq > messageSeq) {
                messageSeq = maxPullSeq + 1;
            }

            if (messageSeq == 0) {
                messageSeq = messageId;
            }

            maps.put(messageSeq, messageId);
            if (maps.size() > MAX_MESSAGE_QUEUE) {
                maps.remove(maps.firstKey());
            }
        } finally {
            mWriteLock.unlock();
        }

        databaseStore.persistUserMessage(user, messageId, messageSeq);
        return messageSeq;
    }

    @Override
    public Collection<String> getChatroomMemberClient(String userId) {
        String chatroomId = (String)m_Server.getHazelcastInstance().getMap(USER_CHATROOM).get(userId);
        if (StringUtil.isNullOrEmpty(chatroomId)) {
            return new HashSet<>();
        }
        MultiMap<String, UserClientEntry> chatroomMembers = m_Server.getHazelcastInstance().getMultiMap(CHATROOM_MEMBER_IDS);
        if (chatroomMembers == null) {
            return new HashSet<>();
        }

        Collection<UserClientEntry> entries = chatroomMembers.get(chatroomId);

        HashSet<String> clientIds = new HashSet<>();
        if (entries == null) {
            return clientIds;
        }
        for (UserClientEntry entry : entries
             ) {
            clientIds.add(entry.clientId);
        }
        return clientIds;
    }

    @Override
    public boolean checkUserClientInChatroom(String user, String clientId, String chatroomId) {
        MemorySessionStore.Session session = m_Server.getStore().sessionsStore().getSession(clientId);
        if (session == null || System.currentTimeMillis() - session.getLastChatroomActiveTime() > 5 * 60 * 1000) {
            handleQuitChatroom(user, clientId, chatroomId);
            return false;
        }

        String existChatroomId = (String)m_Server.getHazelcastInstance().getMap(USER_CHATROOM).get(user);
        if (StringUtil.isNullOrEmpty(existChatroomId) && (chatroomId == null || !existChatroomId.equals(chatroomId))) {
            return false;
        }

        MultiMap<String, UserClientEntry> chatroomMembers = m_Server.getHazelcastInstance().getMultiMap(CHATROOM_MEMBER_IDS);
        if (chatroomMembers == null) {
            return false;
        }

        Collection<UserClientEntry> entries = chatroomMembers.get(existChatroomId);
        if (entries == null) {
            return false;
        }

        for (UserClientEntry entry : entries
            ) {
            if (entry.clientId.equals(clientId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long insertChatroomMessages(String target, int line, long messageId) {
        // messageId是全局的，messageSeq是跟个人相关的，理论上messageId的增长数度远远大于seq。
        // 考虑到一种情况，当服务器发生变化，用户发生迁移后，messageSeq还需要保持有序。 要么把Seq持久化，要么在迁移后Seq取一个肯定比以前更大的数字（这个数字就是messageId）
        // 这里选择使用后面一种情况
        long messageSeq = 0;

        mWriteLock.lock();
        try {
            TreeMap<Long, Long> maps = chatroomMessages.get(target);
            if (maps == null) {
                maps = new TreeMap<>();
                chatroomMessages.put(target, maps);
            }

            Map.Entry<Long, Long> lastEntry = maps.lastEntry();
            if (lastEntry != null) {
                messageSeq = (lastEntry.getKey() + 1);
            }


            if (messageSeq == 0) {
                messageSeq = messageId;
            }

            maps.put(messageSeq, messageId);
            if (maps.size() > MAX_CHATROOM_MESSAGE_QUEUE) {
                maps.remove(maps.firstKey());
            }
        } finally {
            mWriteLock.unlock();
        }

        return messageSeq;
    }

    @Override
    public WFCMessage.GroupInfo createGroup(String fromUser, WFCMessage.GroupInfo groupInfo, List<WFCMessage.GroupMember> memberList) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);

        String groupId = null;
        long dt = System.currentTimeMillis();
        if (StringUtil.isNullOrEmpty(groupInfo.getTargetId())) {
            groupId = getShortUUID();
        }
        groupInfo = groupInfo.toBuilder()
            .setTargetId(groupId)
            .setName(groupInfo.getName())
            .setPortrait(groupInfo.getPortrait())
            .setType(groupInfo.getType())
            .setExtra(groupInfo.getExtra())
            .setUpdateDt(dt)
            .setMemberUpdateDt(dt)
            .setMemberCount(memberList.size())
            .setOwner(StringUtil.isNullOrEmpty(groupInfo.getOwner()) ? fromUser : groupInfo.getOwner())
            .build();

        mIMap.put(groupId, groupInfo);
        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);

        for (WFCMessage.GroupMember member : memberList) {
            member = member.toBuilder().setUpdateDt(dt).build();
            groupMembers.put(groupId, member);
        }

        databaseStore.persistGroupMember(groupId, memberList);

        return groupInfo;
    }


    @Override
    public ErrorCode addGroupMembers(String operator, String groupId, List<WFCMessage.GroupMember> memberList) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);

        WFCMessage.GroupInfo groupInfo = mIMap.get(groupId);
        if (groupInfo == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }
        if (groupInfo.getType() == ProtoConstants.GroupType.GroupType_Restricted && (groupInfo.getOwner() == null || !groupInfo.getOwner().equals(operator))) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);

        long updateDt = System.currentTimeMillis();

        List<WFCMessage.GroupMember> tmp = new ArrayList<>();
        ArrayList<String> newInviteUsers = new ArrayList<>();
        for (WFCMessage.GroupMember member : memberList) {
            member = member.toBuilder().setType(ProtoConstants.GroupMemberType.GroupMemberType_Normal).setUpdateDt(updateDt).setAlias("").build();
            tmp.add(member);
            newInviteUsers.add(member.getMemberId());
        }
        memberList = tmp;

        for (WFCMessage.GroupMember member : groupMembers.get(groupId)) {
            if (newInviteUsers.contains(member.getMemberId())) {
                groupMembers.remove(groupId, member);
            }
        }

        for (WFCMessage.GroupMember member : memberList) {
            groupMembers.put(groupId, member);
        }

        int count = 0;
        for (WFCMessage.GroupMember member : groupMembers.get(groupId)) {
            if (member.getType() != ProtoConstants.GroupMemberType.GroupMemberType_Removed) {
                count++;
            }
        }

        mIMap.put(groupId, groupInfo.toBuilder().setMemberUpdateDt(updateDt).setUpdateDt(updateDt).setMemberCount(count).build());
        databaseStore.persistGroupMember(groupId, memberList);
        databaseStore.updateGroupMemberCountDt(groupId, count, updateDt);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode kickoffGroupMembers(String operator, String groupId, List<String> memberList) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);

        WFCMessage.GroupInfo groupInfo = mIMap.get(groupId);
        if (groupInfo == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }
        if ((groupInfo.getType() == ProtoConstants.GroupType.GroupType_Restricted || groupInfo.getType() == ProtoConstants.GroupType.GroupType_Normal)
            && (groupInfo.getOwner() == null || !groupInfo.getOwner().equals(operator))) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);

        int removeCount = 0;
        long updateDt = System.currentTimeMillis();
        ArrayList<WFCMessage.GroupMember> list = new ArrayList<>();
        List<WFCMessage.GroupMember> allMembers = new ArrayList<>(groupMembers.get(groupId));
        for (WFCMessage.GroupMember member : allMembers) {
            if (memberList.contains(member.getMemberId())) {
                boolean removed = groupMembers.remove(groupId, member);
                if (removed) {
                    removeCount++;
                    member = member.toBuilder().setType(ProtoConstants.GroupMemberType.GroupMemberType_Removed).setUpdateDt(updateDt).build();
                    groupMembers.put(groupId, member);
                    list.add(member);
                }
            }
        }

        if (removeCount > 0) {
            databaseStore.persistGroupMember(groupId, list);
            databaseStore.updateGroupMemberCountDt(groupId, groupInfo.getMemberCount()-removeCount, updateDt);
            mIMap.put(groupId, groupInfo.toBuilder().setMemberUpdateDt(updateDt).setUpdateDt(updateDt).setMemberCount(groupInfo.getMemberCount() - removeCount).build());
        }

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode quitGroup(String operator, String groupId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);

        WFCMessage.GroupInfo groupInfo = mIMap.get(groupId);
        if (groupInfo == null) {
            MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);
            for (WFCMessage.GroupMember member : groupMembers.get(groupId)
                ) {
                if (member.getMemberId().equals(operator)) {
                    groupMembers.remove(groupId, member);
                    break;
                }
            }
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        if (groupInfo.getType() != ProtoConstants.GroupType.GroupType_Free && groupInfo.getOwner() != null && groupInfo.getOwner().equals(operator)) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }
        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);

        long updateDt = System.currentTimeMillis();
        boolean removed = false;
        for (WFCMessage.GroupMember member : groupMembers.get(groupId)
            ) {
            if (member.getMemberId().equals(operator)) {
                removed = groupMembers.remove(groupId, member);
                if (removed) {
                    ArrayList<WFCMessage.GroupMember> list = new ArrayList<>();
                    list.add(member);
                    databaseStore.persistGroupMember(groupId, list);

                    databaseStore.updateGroupMemberCountDt(groupId, groupInfo.getMemberCount()-1, updateDt);
                    member = member.toBuilder().setType(ProtoConstants.GroupMemberType.GroupMemberType_Removed).setUpdateDt(updateDt).build();
                    groupMembers.put(groupId, member);
                }
                break;
            }
        }

        if (removed) {
            mIMap.put(groupId, groupInfo.toBuilder().setMemberUpdateDt(updateDt).setUpdateDt(updateDt).setMemberCount(groupInfo.getMemberCount() - 1).build());
        }
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode dismissGroup(String operator, String groupId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);


        WFCMessage.GroupInfo groupInfo = mIMap.get(groupId);
        if (groupInfo == null) {
            MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);
            groupMembers.remove(groupId);

            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        if (groupInfo.getType() == ProtoConstants.GroupType.GroupType_Free ||
            (groupInfo.getType() == ProtoConstants.GroupType.GroupType_Restricted || groupInfo.getType() == ProtoConstants.GroupType.GroupType_Normal)
                && (groupInfo.getOwner() == null || !groupInfo.getOwner().equals(operator))) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);

        groupMembers.remove(groupId);
        mIMap.remove(groupId);

        databaseStore.removeGroupMemberFromDB(groupId);

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode modifyGroupInfo(String operator, String groupId, int modifyType, String value) {

        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);

        WFCMessage.GroupInfo oldInfo = mIMap.get(groupId);

        if (oldInfo == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        if ((oldInfo.getType() == ProtoConstants.GroupType.GroupType_Restricted)
            && (oldInfo.getOwner() == null || !oldInfo.getOwner().equals(operator))) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        if (oldInfo.getType() == ProtoConstants.GroupType.GroupType_Normal) {
            if (oldInfo.getOwner() == null) {
                return ErrorCode.ERROR_CODE_NOT_RIGHT;
            }
            if (!oldInfo.getOwner().equals(operator)) {
                if (modifyType == Modify_Group_Extra) {
                    return ErrorCode.ERROR_CODE_NOT_RIGHT;
                }
            }
        }

        WFCMessage.GroupInfo.Builder newInfoBuilder = oldInfo.toBuilder();

        if (modifyType == Modify_Group_Name)
            newInfoBuilder.setName(value);
        else if(modifyType == Modify_Group_Portrait)
            newInfoBuilder.setPortrait(value);
        else if(modifyType == Modify_Group_Extra)
            newInfoBuilder.setExtra(value);


        newInfoBuilder.setUpdateDt(System.currentTimeMillis());
        mIMap.put(groupId, newInfoBuilder.build());
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode modifyGroupAlias(String operator, String groupId, String alias) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();

        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);
        WFCMessage.GroupInfo groupInfo = mIMap.get(groupId);
        if (groupInfo == null) {
            groupInfo = databaseStore.getPersistGroupInfo(groupId);
            if (groupInfo != null) {
                mIMap.set(groupId, groupInfo);
            } else {
                return ErrorCode.ERROR_CODE_NOT_EXIST;
            }
        }

        long updateDt = System.currentTimeMillis();
        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);
        Collection<WFCMessage.GroupMember> members = groupMembers.get(groupId);
        for (WFCMessage.GroupMember member : members
            ) {
            if (member.getMemberId().equals(operator)) {
                groupMembers.remove(groupId, member);
                member = member.toBuilder().setAlias(alias).setUpdateDt(updateDt).build();
                databaseStore.persistGroupMember(groupId, Arrays.asList(member));
                databaseStore.updateGroupMemberCountDt(groupId, -1, updateDt);
                groupMembers.put(groupId, member);

                mIMap.put(groupId, groupInfo.toBuilder().setUpdateDt(updateDt).setMemberUpdateDt(updateDt).build());
                break;
            }
        }

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public List<WFCMessage.GroupInfo> getGroupInfos(List<WFCMessage.UserRequest> requests) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);
        ArrayList<WFCMessage.GroupInfo> out = new ArrayList<>();
        for (WFCMessage.UserRequest request : requests) {
            WFCMessage.GroupInfo groupInfo = mIMap.get(request.getUid());
            if (groupInfo == null) {
                groupInfo = databaseStore.getPersistGroupInfo(request.getUid());
                if (groupInfo != null) {
                    mIMap.set(request.getUid(), groupInfo);
                }
            }

            if (groupInfo != null && groupInfo.getUpdateDt() > request.getUpdateDt()) {
                out.add(groupInfo);
            }
        }

        return out;
    }

    @Override
    public WFCMessage.GroupInfo getGroupInfo(String groupId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);

        WFCMessage.GroupInfo groupInfo = mIMap.get(groupId);
        if (groupInfo == null) {
            groupInfo = databaseStore.getPersistGroupInfo(groupId);
            if (groupInfo != null) {
                mIMap.put(groupId, groupInfo);
            }
        }
        return groupInfo;
    }

    @Override
    public ErrorCode getGroupMembers(String groupId, long maxDt, List<WFCMessage.GroupMember> members) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);

        WFCMessage.GroupInfo groupInfo = mIMap.get(groupId);
        if (groupInfo == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        } else if (groupInfo.getMemberUpdateDt() <= maxDt) {
            return ErrorCode.ERROR_CODE_NOT_MODIFIED;
        }

        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);

        members.addAll(groupMembers.get(groupId));
        return ErrorCode.ERROR_CODE_SUCCESS;
    }


    @Override
    public ErrorCode transferGroup(String operator, String groupId, String newOwner) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.GroupInfo> mIMap = hzInstance.getMap(GROUPS_MAP);


        WFCMessage.GroupInfo groupInfo = mIMap.get(groupId);

        if (groupInfo == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        if ((groupInfo.getType() == ProtoConstants.GroupType.GroupType_Restricted || groupInfo.getType() == ProtoConstants.GroupType.GroupType_Normal)
            && (groupInfo.getOwner() == null || !groupInfo.getOwner().equals(operator))) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        //check the new owner is in member list? is that necessary?

        groupInfo = groupInfo.toBuilder().setOwner(newOwner).build();

        mIMap.set(groupId, groupInfo);

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public boolean isMemberInGroup(String memberId, String groupId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);

        Collection<WFCMessage.GroupMember> members = groupMembers.get(groupId);

        for (WFCMessage.GroupMember member : members
            ) {
            if (member.getMemberId().equals(memberId) && member.getType() != ProtoConstants.GroupMemberType.GroupMemberType_Removed)
                return true;
        }

        return false;
    }

    @Override
    public boolean isForbiddenInGroup(String memberId, String groupId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);

        Collection<WFCMessage.GroupMember> members = groupMembers.get(groupId);

        for (WFCMessage.GroupMember member : members
            ) {
            if (member.getMemberId().equals(memberId) && member.getType() == ProtoConstants.GroupMemberType.GroupMemberType_Silent)
                return true;
        }

        return false;
    }

    @Override
    public ErrorCode recallMessage(long messageUid, String operatorId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<Long, MessageBundle> mIMap = hzInstance.getMap(MESSAGES_MAP);

        MessageBundle messageBundle = mIMap.get(messageUid);
        if (messageBundle != null) {
            WFCMessage.Message message = messageBundle.getMessage();
            boolean canRecall = false;
            if (message.getFromUser().equals(operatorId)) {
                canRecall = true;
            }

            if (!canRecall && message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_Group) {
                IMap<String, WFCMessage.GroupInfo> groupMap = hzInstance.getMap(GROUPS_MAP);

                WFCMessage.GroupInfo groupInfo = groupMap.get(message.getConversation().getTarget());
                if (groupInfo == null) {
                    return ErrorCode.ERROR_CODE_NOT_EXIST;
                }
                if (operatorId.equals(groupInfo.getOwner())) {
                    canRecall = true;
                }

                MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(GROUP_MEMBERS);
                Collection<WFCMessage.GroupMember> members = groupMembers.get(message.getConversation().getTarget());
                for (WFCMessage.GroupMember member : members) {
                    if (member.getMemberId().equals(operatorId)) {
                        if (member.getType() == ProtoConstants.GroupMemberType.GroupMemberType_Manager || member.getType() == ProtoConstants.GroupMemberType.GroupMemberType_Owner) {
                            canRecall = true;
                        }
                        break;
                    }
                }
            }

            if (!canRecall && message.getConversation().getType() == ProtoConstants.ConversationType.ConversationType_ChatRoom) {
                IMap<String, WFCMessage.ChatroomInfo> chatroomInfoMap = hzInstance.getMap(CHATROOMS);
                WFCMessage.ChatroomInfo room = chatroomInfoMap.get(message.getConversation().getTarget());
                //todo check is manager
            }

            if (!canRecall) {
                return ErrorCode.ERROR_CODE_NOT_RIGHT;
            }

            message = message.toBuilder().setContent(message.getContent().toBuilder().setContent(operatorId).setType(80).clearSearchableContent().setData(ByteString.copyFrom(new StringBuffer().append(messageUid).toString().getBytes())).build()).build();
            messageBundle.setMessage(message);

            mIMap.put(messageUid, messageBundle, 7, TimeUnit.DAYS);
            return ErrorCode.ERROR_CODE_SUCCESS;
        } else {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }
    }

    @Override
    public WFCMessage.Robot getRobot(String robotId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.Robot> mRobotMap = hzInstance.getMap(ROBOTS);
        return mRobotMap.get(robotId);
    }

    @Override
    public void addRobot(WFCMessage.Robot robot) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.Robot> mUserMap = hzInstance.getMap(ROBOTS);
        mUserMap.put(robot.getUid(), robot);
    }


    @Override
    public ErrorCode getUserInfo(List<WFCMessage.UserRequest> requestList, WFCMessage.PullUserResult.Builder builder) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.User> mUserMap = hzInstance.getMap(USERS);

        for (WFCMessage.UserRequest request : requestList
            ) {
            WFCMessage.User user = mUserMap.get(request.getUid());
            if (user == null) {
                user = databaseStore.getPersistUser(request.getUid());
                if (user != null) {
                    mUserMap.set(request.getUid(), user);
                }
            }
            WFCMessage.UserResult.Builder resultBuilder = WFCMessage.UserResult.newBuilder();
            if (user == null) {
                LOG.warn("Get user info, user {} not exist", request.getUid());
                user = WFCMessage.User.newBuilder().setUid(request.getUid()).build();
                resultBuilder.setUser(user);
                resultBuilder.setCode(ProtoConstants.UserResultCode.NotFound);
            } else {
                LOG.debug("Get user info, user {}  userDt {} : request {}",request.getUid(), user.getUpdateDt(), request.getUpdateDt());
                if (user.getUpdateDt() > request.getUpdateDt()) {
                    resultBuilder.setUser(user);
                    resultBuilder.setCode(ProtoConstants.UserResultCode.Success);
                } else {
                    user = WFCMessage.User.newBuilder().setUid(request.getUid()).build();
                    resultBuilder.setUser(user);
                    resultBuilder.setCode(ProtoConstants.UserResultCode.NotModified);
                }
            }
            builder.addResult(resultBuilder.build());

        }

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode modifyUserInfo(String userId, WFCMessage.ModifyMyInfoRequest request) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.User> mUserMap = hzInstance.getMap(USERS);


        WFCMessage.User user = mUserMap.get(userId);
        if (user == null) {
            user = databaseStore.getPersistUser(userId);
            if (user != null) {
                mUserMap.set(userId, user);
            } else {
                return ErrorCode.ERROR_CODE_NOT_EXIST;
            }
        }

        WFCMessage.User.Builder builder = user.toBuilder();
        boolean modified = false;
        for (WFCMessage.InfoEntry entry : request.getEntryList()
            ) {
            switch (entry.getType()) {
                case Modify_DisplayName:
                    builder.setDisplayName(entry.getValue());
                    modified = true;
                    break;
                case Modify_Gender:
                    builder.setGender(Integer.parseInt(entry.getValue()));
                    modified = true;
                    break;
                case Modify_Portrait:
                    builder.setPortrait(entry.getValue());
                    modified = true;
                    break;
                case Modify_Mobile:
                    builder.setMobile(entry.getValue());
                    modified = true;
                    break;
                case Modify_Email:
                    builder.setEmail(entry.getValue());
                    modified = true;
                    break;
                case Modify_Address:
                    builder.setAddress(entry.getValue());
                    modified = true;
                    break;
                case Modify_Company:
                    builder.setCompany(entry.getValue());
                    modified = true;
                    break;
                case Modify_Social:

                    break;
                case Modify_Extra:
                    builder.setExtra(entry.getValue());
                    modified = true;
                    break;
                default:
                    break;
            }
        }

        if (modified) {
            builder.setUpdateDt(System.currentTimeMillis());
            user = builder.build();
            mUserMap.set(userId, user);
//            databaseStore.updateUser(user);
            return ErrorCode.ERROR_CODE_SUCCESS;
        } else {
            return ErrorCode.ERROR_CODE_NOT_MODIFIED;
        }

    }

    @Override
    public ErrorCode modifyUserStatus(String userId, int status) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, Integer> mUserMap = hzInstance.getMap(USER_STATUS);
        if (status == 0) {
            mUserMap.delete(userId);
        } else {
            mUserMap.put(userId, status);
        }
        databaseStore.updateUserStatus(userId, status);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public int getUserStatus(String userId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, Integer> mUserMap = hzInstance.getMap(USER_STATUS);
        Integer status = mUserMap.get(userId);

        if (status != null) {
            return status;
        }

        return 0;
    }

    @Override
    public List<InputOutputUserBlockStatus> getUserStatusList() {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, Integer> mUserMap = hzInstance.getMap(USER_STATUS);
        ArrayList<InputOutputUserBlockStatus> out = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : mUserMap.entrySet()) {
            out.add(new InputOutputUserBlockStatus(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    @Override
    public void addUserInfo(WFCMessage.User user, String password) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.User> mUserMap = hzInstance.getMap(USERS);
        mUserMap.put(user.getUid(), user);
        databaseStore.updateUserPassword(user.getUid(), password);
    }

    @Override
    public WFCMessage.User getUserInfo(String userId) {
        if (StringUtil.isNullOrEmpty(userId)) {
            return null;
        }

        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.User> mUserMap = hzInstance.getMap(USERS);
        WFCMessage.User user = mUserMap.get(userId);
        return user;
    }

    @Override
    public WFCMessage.User getUserInfoByName(String name) {
        String userId = databaseStore.getUserIdByName(name);
        return getUserInfo(userId);
    }

    @Override
    public WFCMessage.User getUserInfoByMobile(String mobile) {
        String userId = databaseStore.getUserIdByMobile(mobile);
        return getUserInfo(userId);
    }
    @Override
    public List<WFCMessage.User> searchUser(String keyword, boolean buzzy, int page) {
        return databaseStore.searchUserFromDB(keyword, buzzy, page);
    }

    @Override
    public void createChatroom(String chatroomId, WFCMessage.ChatroomInfo chatroomInfo) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.ChatroomInfo> chatroomInfoMap = hzInstance.getMap(CHATROOMS);
        WFCMessage.ChatroomInfo preInfo = chatroomInfoMap.get(chatroomId);
        if (preInfo != null) {
            WFCMessage.ChatroomInfo.Builder builder = preInfo.toBuilder();
            if (!StringUtil.isNullOrEmpty(chatroomInfo.getTitle())) {
                builder.setTitle(chatroomInfo.getTitle());
            }

            if (!StringUtil.isNullOrEmpty(chatroomInfo.getDesc())) {
                builder.setDesc(chatroomInfo.getDesc());
            }

            if (!StringUtil.isNullOrEmpty(chatroomInfo.getPortrait())) {
                builder.setPortrait(chatroomInfo.getPortrait());
            }

            if (!StringUtil.isNullOrEmpty(chatroomInfo.getExtra())) {
                builder.setExtra(chatroomInfo.getExtra());
            }

            builder.setState(chatroomInfo.getState());

            chatroomInfo = builder.build();
        }
        chatroomInfoMap.put(chatroomId, chatroomInfo);
    }

    @Override
    public void destoryChatroom(String chatroomId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.ChatroomInfo> chatroomInfoMap = hzInstance.getMap(CHATROOMS);
        WFCMessage.ChatroomInfo room = chatroomInfoMap.get(chatroomId);
        if (room != null) {
            room = room.toBuilder().setUpdateDt(System.currentTimeMillis()).setState(ProtoConstants.ChatroomState.Chatroom_State_End).build();
            chatroomInfoMap.put(chatroomId, room);
        }
    }

    @Override
    public WFCMessage.ChatroomInfo getChatroomInfo(String chatroomId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.ChatroomInfo> chatroomInfoMap = hzInstance.getMap(CHATROOMS);
        return chatroomInfoMap.get(chatroomId);
    }

    @Override
    public WFCMessage.ChatroomMemberInfo getChatroomMemberInfo(String chatroomId, final int maxMemberCount) {
        MultiMap<String, UserClientEntry> chatroomMembers = m_Server.getHazelcastInstance().getMultiMap(CHATROOM_MEMBER_IDS);
        Collection<UserClientEntry> members = chatroomMembers.get(chatroomId);

        if (members != null) {
            ArrayList<String> memberIds = new ArrayList<>();
            if (members.size() <= maxMemberCount) {
                    members.stream().forEach(new Consumer<UserClientEntry>() {
                        @Override
                        public void accept(UserClientEntry userClientEntry) {
                            memberIds.add(userClientEntry.userId);
                        }
                    });
            } else {
                Spliterator<UserClientEntry> p = members.spliterator();
                while (p.estimateSize() > 2*maxMemberCount) {
                    p = p.trySplit();
                }

                p.forEachRemaining(new Consumer<UserClientEntry>() {
                    public void accept(UserClientEntry s) {
                        if (memberIds.size() < maxMemberCount) {
                            memberIds.add(s.userId);
                        }
                    }
                });
            }
            return WFCMessage.ChatroomMemberInfo.newBuilder().setMemberCount(members.size()).addAllMembers(memberIds).build();
        }

        return WFCMessage.ChatroomMemberInfo.newBuilder().setMemberCount(0).build();

    }
    @Override
    public int getChatroomMemberCount(String chatroomId) {
        return m_Server.getHazelcastInstance().getMultiMap(CHATROOM_MEMBER_IDS).valueCount(chatroomId);
    }

    @Override
    public ErrorCode verifyToken(String userId, String token, List<String> serverIPs, List<Integer> ports) {
        String tokenUserId = Tokenor.getUserId(token.getBytes());

        if (tokenUserId != null) {
            if (tokenUserId.equals(userId)) {
                Member member = m_Server.getHazelcastInstance().getCluster().getLocalMember();
                String serverIp = member.getStringAttribute(HZ_Cluster_Node_External_IP);
                String longPort = member.getStringAttribute(HZ_Cluster_Node_External_Long_Port);
                String shortPort = member.getStringAttribute(HZ_Cluster_Node_External_Short_Port);
                if (!StringUtil.isNullOrEmpty(serverIp)) {
                    serverIPs.add(serverIp);
                }
                ports.add(Integer.parseInt(longPort));
                ports.add(Integer.parseInt(shortPort));
            } else {
                return ErrorCode.ERROR_CODE_TOKEN_ERROR;
            }
        } else {
            return ErrorCode.ERROR_CODE_TOKEN_ERROR;
        }
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public boolean isBlacked(String fromUser, String userId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, FriendData> friendsMap = hzInstance.getMultiMap(USER_FRIENDS);

        Collection<FriendData> friendDatas = friendsMap.get(fromUser);

        if (friendDatas == null) {
            return false;
        }

        for (FriendData friendData : friendDatas) {
            if (friendData.getUserId().equals(userId)) {
                if (friendData.getState() == 2) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    @Override
    public ErrorCode login(String name, String password, List<String> userIdRet) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_uid`, `_passwd_md5` from t_user where `_name` = ?";

            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, name);

            rs = statement.executeQuery();
            if (rs.next()) {
                String uid = rs.getString(1);

                int status = getUserStatus(uid);
                if (status == 1) {
                    return ErrorCode.ERROR_CODE_USER_FORBIDDEN;
                }
                String pwd_md5 = rs.getString(2);
                try {
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    BASE64Encoder base64en = new BASE64Encoder();
                    String passwdMd5 = base64en.encode(md5.digest(password.getBytes("utf-8")));
                    if (passwdMd5.equals(pwd_md5)) {
                        LOG.info("login success userName={}, userId={}", name, uid);
                        userIdRet.add(uid);
                        return ErrorCode.ERROR_CODE_SUCCESS;
                    } else {
                        LOG.info("login failure incorrect password, userName={}, userId={}", name, uid);
                        return ErrorCode.ERROR_CODE_PASSWORD_INCORRECT;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    LOG.info("login failure execption password, userName={}", name);
                    return ErrorCode.ERROR_CODE_PASSWORD_INCORRECT;
                }
            } else {
                LOG.info("login failure user not exist, userName={}", name);
                return ErrorCode.ERROR_CODE_NOT_EXIST;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
            LOG.info("login failure db execption, userName={}", name);
            return ErrorCode.ERROR_CODE_SERVER_ERROR;
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
    }

    @Override
    public List<FriendData> getFriendList(String userId, long version) {
        List<FriendData> out = new ArrayList<FriendData>();

        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, FriendData> friendsMap = hzInstance.getMultiMap(USER_FRIENDS);
        Collection<FriendData> friends = friendsMap.get(userId);
        if (friends == null || friends.size() == 0) {
            friends = databaseStore.getPersistFriends(userId);
            if (friends != null) {
                for (FriendData friend : friends) {
                    friendsMap.put(userId, friend);
                }
            } else {
                friends = new ArrayList<>();
            }
        }

        for (FriendData friend : friends) {
            if (friend.getTimestamp() > version) {
                out.add(friend);
            }
        }

        return out;
    }

    @Override
    public List<WFCMessage.FriendRequest> getFriendRequestList(String userId, long version) {
        List<WFCMessage.FriendRequest> out = new ArrayList<>();

        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.FriendRequest> requestMap = hzInstance.getMultiMap(USER_FRIENDS_REQUEST);
        Collection<WFCMessage.FriendRequest> requests = requestMap.get(userId);
        if (requests == null || requests.size() == 0) {
            requests = databaseStore.getPersistFriendRequests(userId);
            if (requests != null) {
                for (WFCMessage.FriendRequest request : requests) {
                    if (request.getUpdateDt() > version)
                        requestMap.put(userId, request);
                }
            } else {
                requests = new ArrayList<>();
            }
        }

        for (WFCMessage.FriendRequest request : requests) {
            if (request.getUpdateDt() > version) {
                out.add(request);
            }
        }

        return out;
    }

    @Override
    public ErrorCode saveAddFriendRequest(String userId, WFCMessage.AddFriendRequest request, long[] head) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.FriendRequest> requestMap = hzInstance.getMultiMap(USER_FRIENDS_REQUEST);
        Collection<WFCMessage.FriendRequest> requests = requestMap.get(userId);
        if (requests == null || requests.size() == 0) {
            requests = databaseStore.getPersistFriendRequests(userId);
            if (requests != null) {
                for (WFCMessage.FriendRequest r : requests
                    ) {
                    requestMap.put(userId, r);
                }
            } else {
                requests = new ArrayList<>();
            }
        }

        WFCMessage.FriendRequest existRequest = null;

        for (WFCMessage.FriendRequest tmpRequest : requests) {
            if (tmpRequest.getToUid().equals(request.getTargetUid())) {
                existRequest = tmpRequest;
                break;
            }
        }

        if (existRequest != null && existRequest.getStatus() != ProtoConstants.FriendRequestStatus.RequestStatus_Accepted) {
            if (System.currentTimeMillis() - existRequest.getUpdateDt() > 7 * 24 * 60 * 60 * 1000) {
                if (existRequest.getStatus() == ProtoConstants.FriendRequestStatus.RequestStatus_Rejected
                    && System.currentTimeMillis() - existRequest.getUpdateDt() < 30 * 24 * 60 * 60 * 1000) {
                    return ErrorCode.ERROR_CODE_FRIEND_REQUEST_BLOCKED;
                }
                requestMap.remove(userId, existRequest);
            } else {
                return ErrorCode.ERROR_CODE_FRIEND_ALREADY_REQUEST;
            }
        }
        WFCMessage.FriendRequest newRequest = WFCMessage.FriendRequest
            .newBuilder()
            .setFromUid(userId)
            .setToUid(request.getTargetUid())
            .setReason(request.getReason())
            .setStatus(ProtoConstants.FriendRequestStatus.RequestStatus_Sent)
            .setToReadStatus(false)
            .setUpdateDt(System.currentTimeMillis())
            .build();

        requestMap.put(userId, newRequest);
        requestMap.put(request.getTargetUid(), newRequest);
        databaseStore.persistOrUpdateFriendRequest(newRequest);
        head[0] = newRequest.getUpdateDt();
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode SyncFriendRequestUnread(String userId, long unreadDt, long[] head) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.FriendRequest> requestMap = hzInstance.getMultiMap(USER_FRIENDS_REQUEST);

        long curTS = System.currentTimeMillis();
        head[0] = curTS;

        databaseStore.persistFriendRequestUnreadStatus(userId, unreadDt, curTS);
        requestMap.remove(userId);

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode handleFriendRequest(String userId, WFCMessage.HandleFriendRequest request, WFCMessage.Message.Builder msgBuilder, long[] heads) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.FriendRequest> requestMap = hzInstance.getMultiMap(USER_FRIENDS_REQUEST);
        Collection<WFCMessage.FriendRequest> requests = requestMap.get(userId);
        if (requests == null || requests.size() == 0) {
            requests = databaseStore.getPersistFriendRequests(userId);
            if (requests != null) {
                for (WFCMessage.FriendRequest r : requests
                    ) {
                    requestMap.put(userId, r);
                }
            }
        }

        WFCMessage.FriendRequest existRequest = null;
        for (WFCMessage.FriendRequest tmpRequest : requests) {
            if (tmpRequest.getFromUid().equals(request.getTargetUid())) {
                existRequest = tmpRequest;
                break;
            }
        }

        if (existRequest != null) {
            if (System.currentTimeMillis() - existRequest.getUpdateDt() > 7 * 24 * 60 * 60 * 1000) {
                return ErrorCode.ERROR_CODE_FRIEND_REQUEST_OVERTIME;
            } else {
                requestMap.remove(userId, existRequest);
                existRequest = existRequest.toBuilder().setStatus(ProtoConstants.FriendRequestStatus.RequestStatus_Accepted).setUpdateDt(System.currentTimeMillis()).build();
                databaseStore.persistOrUpdateFriendRequest(existRequest);
                requestMap.put(userId, existRequest);


                MultiMap<String, FriendData> friendsMap = hzInstance.getMultiMap(USER_FRIENDS);

                FriendData friendData1 = new FriendData(userId, request.getTargetUid(), "", 0, System.currentTimeMillis());
                friendsMap.put(userId, friendData1);
                databaseStore.persistOrUpdateFriendData(friendData1);

                FriendData friendData2 = new FriendData(request.getTargetUid(), userId, "", 0, friendData1.getTimestamp());
                friendsMap.put(request.getTargetUid(), friendData2);
                databaseStore.persistOrUpdateFriendData(friendData2);

                heads[0] = friendData2.getTimestamp();
                heads[1] = friendData1.getTimestamp();

                msgBuilder.setConversation(WFCMessage.Conversation.newBuilder().setTarget(userId).setLine(0).setType(ProtoConstants.ConversationType.ConversationType_Private).build());
                msgBuilder.setContent(WFCMessage.MessageContent.newBuilder().setType(1).setSearchableContent(existRequest.getReason()).build());
                return ErrorCode.ERROR_CODE_SUCCESS;
            }
        } else {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }
    }

    @Override
    public ErrorCode blackUserRequest(String fromUser, String targetUserId, int state, long[] heads) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, FriendData> friendsMap = hzInstance.getMultiMap(USER_FRIENDS);

        FriendData friendData = null;
        Collection<FriendData> friends = friendsMap.get(fromUser);
        for (FriendData fd:friends) {
            if (fd.getFriendUid().equals(targetUserId)) {
                friendData = fd;
                break;
            }
        }

        if (friendData == null) {
            friendData = new FriendData(fromUser, targetUserId, "", state, System.currentTimeMillis());
        }
        friendData.setState(state);
        friendData.setTimestamp(System.currentTimeMillis());

        friendsMap.put(fromUser, friendData);
        databaseStore.persistOrUpdateFriendData(friendData);

        heads[0] = friendData.getTimestamp();

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode setFriendAliasRequest(String fromUser, String targetUserId, String alias, long[] heads){
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, FriendData> friendsMap = hzInstance.getMultiMap(USER_FRIENDS);

        FriendData friendData = null;
        Collection<FriendData> friends = friendsMap.get(fromUser);
        for (FriendData fd:friends) {
            if (fd.getFriendUid().equals(targetUserId)) {
                friendData = fd;
                break;
            }
        }

        if (friendData == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        friendData.setAlias(alias);
        friendData.setTimestamp(System.currentTimeMillis());

        friendsMap.put(fromUser, friendData);
        databaseStore.persistOrUpdateFriendData(friendData);

        heads[0] = friendData.getTimestamp();

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode handleJoinChatroom(String userId, String clientId, String chatroomId) {
        IMap<String, WFCMessage.ChatroomInfo> chatroomInfoMap = m_Server.getHazelcastInstance().getMap(CHATROOMS);
        if (chatroomInfoMap == null || chatroomInfoMap.get(chatroomId) == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        m_Server.getStore().sessionsStore().getSession(clientId).refreshLastChatroomActiveTime();
        m_Server.getHazelcastInstance().getMap(USER_CHATROOM).put(userId, chatroomId);
        m_Server.getHazelcastInstance().getMultiMap(CHATROOM_MEMBER_IDS).put(chatroomId, new UserClientEntry(userId, clientId));

        mWriteLock.lock();
        chatroomMessages.put(userId, new TreeMap<>());
        mWriteLock.unlock();


        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode handleQuitChatroom(String userId, String clientId, String chatroomId) {
        m_Server.getHazelcastInstance().getMap(USER_CHATROOM).remove(userId);
        m_Server.getHazelcastInstance().getMultiMap(CHATROOM_MEMBER_IDS).remove(chatroomId, new UserClientEntry(userId, clientId));

        mWriteLock.lock();
        chatroomMessages.remove(userId);
        mWriteLock.unlock();
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode deleteFriend(String userId, String friendUid) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, FriendData> friendsMap = hzInstance.getMultiMap(USER_FRIENDS);
        Collection<FriendData> user1Friends = friendsMap.get(userId);
        for (FriendData data :
            user1Friends) {
            if (data.getFriendUid().equals(friendUid)) {
                friendsMap.remove(userId, data);
                data.setState(1);
                data.setTimestamp(System.currentTimeMillis());
                friendsMap.put(userId, data);
                databaseStore.persistOrUpdateFriendData(data);
                break;
            }
        }

        Collection<FriendData> user2Friends = friendsMap.get(friendUid);
        for (FriendData data :
            user2Friends) {
            if (data.getFriendUid().equals(userId)) {
                friendsMap.remove(friendUid, data);
                data.setState(1);
                data.setTimestamp(System.currentTimeMillis());
                friendsMap.put(friendUid, data);
                databaseStore.persistOrUpdateFriendData(data);
                break;
            }
        }

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode getUserSettings(String userId, long version, WFCMessage.GetUserSettingResult.Builder builder) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.UserSettingEntry> userSettingMap = hzInstance.getMultiMap(USER_SETTING);

        Collection<WFCMessage.UserSettingEntry> entries = userSettingMap.get(userId);
        if (entries == null || entries.size() == 0) {
            entries = loadPersistedUserSettings(userId, userSettingMap);
        }

        ErrorCode ec = ErrorCode.ERROR_CODE_NOT_MODIFIED;
        if (entries != null) {
            for (WFCMessage.UserSettingEntry entry : entries
            ) {
                if (entry.getUpdateDt() > version) {
                    ec = ErrorCode.ERROR_CODE_SUCCESS;
                    builder.addEntry(entry);
                }
            }
        }

        return ec;
    }


    private List<WFCMessage.UserSettingEntry> loadPersistedUserSettings(String userId, MultiMap<String, WFCMessage.UserSettingEntry> userSettingMap) {
        List<WFCMessage.UserSettingEntry> datas = databaseStore.getPersistUserSetting(userId);
        for (WFCMessage.UserSettingEntry data : datas
             ) {
            userSettingMap.put(userId, data);
        }
        return datas;
    }


    @Override
    public WFCMessage.UserSettingEntry getUserSetting(String userId, int scope, String key) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.UserSettingEntry> userSettingMap = hzInstance.getMultiMap(USER_SETTING);

        Collection<WFCMessage.UserSettingEntry> entries = userSettingMap.get(userId);
        if (entries == null || entries.size() == 0) {
            entries = loadPersistedUserSettings(userId, userSettingMap);
        }

        if (entries != null) {
            for (WFCMessage.UserSettingEntry entry : entries) {
                if (entry.getScope() == scope && (key== null || key.equals(entry.getKey()))) {
                    return entry;
                }
            }
        }

        return null;
    }

    @Override
    public List<WFCMessage.UserSettingEntry> getUserSetting(String userId, int scope) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.UserSettingEntry> userSettingMap = hzInstance.getMultiMap(USER_SETTING);

        Collection<WFCMessage.UserSettingEntry> entries = userSettingMap.get(userId);
        if (entries == null || entries.size() == 0) {
            entries = loadPersistedUserSettings(userId, userSettingMap);
        }

        List<WFCMessage.UserSettingEntry> result = new ArrayList<>();
        if (entries != null) {
            for (WFCMessage.UserSettingEntry entry : entries
            ) {
                if (entry.getScope() == scope) {
                    result.add(entry);
                }
            }
        }

        return result;
    }

    @Override
    public long updateUserSettings(String userId, WFCMessage.ModifyUserSettingReq request) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.UserSettingEntry> userSettingMap = hzInstance.getMultiMap(USER_SETTING);

        Collection<WFCMessage.UserSettingEntry> entries = userSettingMap.get(userId);
        if (entries == null || entries.size() == 0) {
            entries = loadPersistedUserSettings(userId, userSettingMap);
            if (entries == null) {
                entries = new ArrayList<>();
            }
        }

        long updateDt = System.currentTimeMillis();
        WFCMessage.UserSettingEntry settingEntry = WFCMessage.UserSettingEntry.newBuilder().setScope(request.getScope()).setKey(request.getKey()).setValue(request.getValue()).setUpdateDt(updateDt).build();
        databaseStore.persistUserSetting(userId, settingEntry);

        for (WFCMessage.UserSettingEntry entry : entries
            ) {
            if (entry.getScope() == request.getScope() && entry.getKey().equals(request.getKey())) {
                userSettingMap.remove(userId, entry);
                userSettingMap.put(userId, settingEntry);
                return updateDt;
            }
        }

        userSettingMap.put(userId, settingEntry);
        userGlobalSlientMap.remove(userId);
        userConvSlientMap.remove(userId);
        userPushHiddenDetail.remove(userId);
        return updateDt;
    }

    @Override
    public boolean getUserGlobalSlient(String userId) {
        Boolean slient = userGlobalSlientMap.get(userId);
        if (slient == null) {
            WFCMessage.UserSettingEntry entry = getUserSetting(userId, UserSettingScope.kUserSettingGlobalSilent, null);
            if (entry == null || !entry.getValue().equals("1")) {
                slient = false;
            } else {
                slient = true;
            }
            userGlobalSlientMap.put(userId, slient);
        }
        return slient;
    }

    @Override
    public boolean getUserPushHiddenDetail(String userId) {
        Boolean hidden = userPushHiddenDetail.get(userId);
        if (hidden == null) {
            WFCMessage.UserSettingEntry entry = getUserSetting(userId, UserSettingScope.kUserSettingGlobalSilent, null);
            if (entry == null || !entry.getValue().equals("1")) {
                hidden = false;
            } else {
                hidden = true;
            }
            userPushHiddenDetail.put(userId, hidden);
        }
        return hidden;
    }

    @Override
    public boolean getUserConversationSlient(String userId, WFCMessage.Conversation conversation) {
        String key = userId + "|" + conversation.getType() + "|" + conversation.getTarget() + "|" + conversation.getLine();
        Boolean slient = userConvSlientMap.get(key);
        if (slient == null) {
            String convSlientKey = conversation.getType() + "-" + conversation.getLine() + "-" + conversation.getTarget();
            WFCMessage.UserSettingEntry entry = getUserSetting(userId, UserSettingScope.kUserSettingConversationSilent, convSlientKey);
            if (entry == null || !entry.getValue().equals("1")) {
                slient = false;
            } else {
                slient = true;
            }
            userConvSlientMap.put(userId, slient);
        }
        return slient;
    }

    @Override
    public ErrorCode createChannel(String operator, WFCMessage.ChannelInfo channelInfo) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.ChannelInfo> mIMap = hzInstance.getMap(CHANNELS);

        mIMap.put(channelInfo.getTargetId(), channelInfo);

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode modifyChannelInfo(String operator, String channelId, int modifyType, String value) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.ChannelInfo> mIMap = hzInstance.getMap(CHANNELS);

        WFCMessage.ChannelInfo oldInfo = mIMap.get(channelId);

        if (oldInfo == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        if (oldInfo.getOwner() == null || !oldInfo.getOwner().equals(operator)) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }



        WFCMessage.ChannelInfo.Builder newInfoBuilder = oldInfo.toBuilder();

        if (modifyType == Modify_Channel_Name)
            newInfoBuilder.setName(value);
        else if(modifyType == Modify_Channel_Portrait)
            newInfoBuilder.setPortrait(value);
        else if(modifyType == Modify_Channel_Desc)
            newInfoBuilder.setDesc(value);
        else if(modifyType == Modify_Channel_Extra)
            newInfoBuilder.setExtra(value);
        else if(modifyType == Modify_Channel_Secret)
            newInfoBuilder.setSecret(value);
        else if(modifyType == Modify_Channel_Callback)
            newInfoBuilder.setCallback(value);
        else if(modifyType == Modify_Channel_OnlyCallback) {
            try {
                newInfoBuilder.setAutomatic(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }


        newInfoBuilder.setUpdateDt(System.currentTimeMillis());
        mIMap.put(channelId, newInfoBuilder.build());
        return ErrorCode.ERROR_CODE_SUCCESS;
    }


    @Override
    public ErrorCode transferChannel(String operator, String channelId, String newOwner) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.ChannelInfo> mIMap = hzInstance.getMap(CHANNELS);

        WFCMessage.ChannelInfo oldInfo = mIMap.get(channelId);

        if (oldInfo == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        if (oldInfo.getOwner() == null || !oldInfo.getOwner().equals(operator)) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }



        WFCMessage.ChannelInfo.Builder newInfoBuilder = oldInfo.toBuilder();

        newInfoBuilder.setOwner(newOwner);
        newInfoBuilder.setUpdateDt(System.currentTimeMillis());
        mIMap.put(channelId, newInfoBuilder.build());
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ErrorCode distoryChannel(String operator, String channelId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.ChannelInfo> mIMap = hzInstance.getMap(CHANNELS);

        WFCMessage.ChannelInfo oldInfo = mIMap.get(channelId);

        if (oldInfo == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        if (oldInfo.getOwner() == null || !oldInfo.getOwner().equals(operator)) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }


        WFCMessage.ChannelInfo.Builder newInfoBuilder = oldInfo.toBuilder();

        newInfoBuilder.setStatus(Channel_Status_Destoryed);
        newInfoBuilder.setUpdateDt(System.currentTimeMillis());
        mIMap.put(channelId, newInfoBuilder.build());
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public List<WFCMessage.ChannelInfo> searchChannel(String keyword, boolean buzzy, int page) {
        return databaseStore.searchChannelFromDB(keyword, buzzy, page);
    }

    @Override
    public ErrorCode listenChannel(String operator, String channelId, boolean listen) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, String> listeners = hzInstance.getMultiMap(CHANNEL_LISTENERS);

        if (listen) {
            listeners.put(channelId, operator);
        } else {
            listeners.remove(channelId, operator);
        }
        databaseStore.updateChannelListener(channelId, operator, listen);

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public WFCMessage.ChannelInfo getChannelInfo(String channelId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<String, WFCMessage.ChannelInfo> mIMap = hzInstance.getMap(CHANNELS);
        return mIMap.get(channelId);
    }

    @Override
    public boolean checkUserInChannel(String user, String channelId) {
        MultiMap<String, String> chatroomMembers = m_Server.getHazelcastInstance().getMultiMap(CHANNEL_LISTENERS);
        if (chatroomMembers == null) {
            return false;
        }

        if(!chatroomMembers.containsEntry(channelId, user)) {
            IMap<String, WFCMessage.ChannelInfo> mIMap = m_Server.getHazelcastInstance().getMap(CHANNELS);
            WFCMessage.ChannelInfo info = mIMap.get(channelId);
            if (info == null || !info.getOwner().equals(user)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<String> handleSensitiveWord(String message) {
        updateSensitiveWord();
        return mSensitiveFilter.getSensitiveWords(message, SensitiveFilter.MatchType.MAX_MATCH);
    }

    @Override
    public boolean addSensitiveWords(List<String> words) {
        for (String word :
            words) {
            databaseStore.persistSensitiveWord(word);
        }
        return true;
    }

    @Override
    public boolean removeSensitiveWords(List<String> words) {
        for (String word :
            words) {
            databaseStore.deleteSensitiveWord(word);
        }
        return true;
    }

    @Override
    public List<String> getAllSensitiveWords() {
        return new ArrayList<>(databaseStore.getSensitiveWord());
    }

    @Override
    public WFCMessage.Message getMessage(long messageId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        IMap<Long, MessageBundle> mIMap = hzInstance.getMap(MESSAGES_MAP);
        MessageBundle bundle = mIMap.get(messageId);
        if (bundle != null) {
            return bundle.getMessage();
        } else {
            bundle = databaseStore.getMessage(messageId);
            if (bundle != null)
                return bundle.getMessage();
        }
        return null;
    }

    @Override
    public long getMessageHead(String user) {
        TreeMap<Long, Long> maps = userMessages.get(user);
        if (maps == null) {
            loadUserMessages(user);
        }

        mReadLock.lock();
        try {
            maps = userMessages.get(user);
            Map.Entry<Long, Long> lastEntry = maps.lastEntry();
            if (lastEntry != null) {
                return lastEntry.getKey();
            }
        } finally {
            mReadLock.unlock();
        }
        return 0;
    }

    private void loadUserMessages(String user) {
        mWriteLock.lock();
        try {
            TreeMap<Long, Long> maps = userMessages.get(user);
            if (maps == null) {
                maps = databaseStore.reloadUserMessageMaps(user);
                userMessages.put(user, maps);
            }
        } finally {
            mWriteLock.unlock();
        }
    }

    @Override
    public long getFriendHead(String userId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, FriendData> friendsMap = hzInstance.getMultiMap(USER_FRIENDS);
        Collection<FriendData> friends = friendsMap.get(userId);
        long max = 0;
        if (friends == null || friends.size() == 0) {
            friends = databaseStore.getPersistFriends(userId);
            for (FriendData friend :
                friends) {
                friendsMap.put(userId, friend);
            }
        }

        if (friends != null && friends.size() > 0) {
            for (FriendData friend :
                friends) {
                if (friend.getTimestamp() > max)
                    max = friend.getTimestamp();
            }
        }

        return max;
    }

    @Override
    public long getFriendRqHead(String userId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.FriendRequest> friendsReqMap = hzInstance.getMultiMap(USER_FRIENDS_REQUEST);
        Collection<WFCMessage.FriendRequest> friendsReq = friendsReqMap.get(userId);
        long max = 0;
        if (friendsReq == null || friendsReq.size() == 0) {
            friendsReq = databaseStore.getPersistFriendRequests(userId);
            for (WFCMessage.FriendRequest req : friendsReq
                 ) {
                friendsReqMap.put(userId, req);
            }
        }

        if (friendsReq != null && friendsReq.size() > 0) {
            for (WFCMessage.FriendRequest request :
                friendsReq) {
                if (request.getUpdateDt() > max)
                    max = request.getUpdateDt();
            }
        }

        return max;
    }

    @Override
    public long getSettingHead(String userId) {
        HazelcastInstance hzInstance = m_Server.getHazelcastInstance();
        MultiMap<String, WFCMessage.UserSettingEntry> userSettingMap = hzInstance.getMultiMap(USER_SETTING);

        Collection<WFCMessage.UserSettingEntry> entries = userSettingMap.get(userId);
        if (entries == null || entries.size() == 0) {
            return 0L;
        }

        long max = 0;
        for (WFCMessage.UserSettingEntry entry : entries
            ) {
            if (entry.getUpdateDt() > max) {
                max = entry.getUpdateDt();
            }
        }

        return max;
    }


    @Override
    public String getShortUUID(){
        int id = databaseStore.getGeneratedId();
        if (id > 0) {
            return IDUtils.toUid(id);
        }
        return UUIDGenerator.getUUID();
    }
    
    @Override
    public void storeRetained(Topic topic, StoredMessage storedMessage) {
        LOG.debug("Store retained message for topic={}, CId={}", topic, storedMessage.getClientID());
        if (storedMessage.getClientID() == null) {
            throw new IllegalArgumentException("Message to be persisted must have a not null client ID");
        }
        m_retainedStore.put(topic, storedMessage);
    }

    @Override
    public Collection<StoredMessage> searchMatching(IMatchingCondition condition) {
        LOG.debug("searchMatching scanning all retained messages, presents are {}", m_retainedStore.size());

        List<StoredMessage> results = new ArrayList<>();

        for (Map.Entry<Topic, StoredMessage> entry : m_retainedStore.entrySet()) {
            StoredMessage storedMsg = entry.getValue();
            if (condition.match(entry.getKey())) {
                results.add(storedMsg);
            }
        }

        return results;
    }

    @Override
    public void cleanRetained(Topic topic) {
        m_retainedStore.remove(topic);
    }


    public static String getName(int[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            char ch = (char)(bytes[i]+17);
            sb.append(ch);
        }
        return sb.toString();
    }
}
