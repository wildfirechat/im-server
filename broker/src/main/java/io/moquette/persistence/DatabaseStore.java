/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.persistence;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import cn.wildfirechat.server.ThreadPoolExecutorWrapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;
import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.model.FriendData;
import io.moquette.spi.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;
import win.liyufan.im.DBUtil;
import win.liyufan.im.MessageBundle;
import win.liyufan.im.MessageShardingUtil;
import win.liyufan.im.Utility;

import java.io.IOException;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.function.Function;

import static cn.wildfirechat.proto.ProtoConstants.PersistFlag.Transparent;
import static io.moquette.persistence.MemoryMessagesStore.USER_STATUS;
import static io.moquette.server.Constants.MAX_MESSAGE_QUEUE;

public class DatabaseStore {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseStore.class);
    private final ThreadPoolExecutorWrapper mScheduler;

    public DatabaseStore(ThreadPoolExecutorWrapper scheduler) {
        this.mScheduler = scheduler;
    }

    TreeMap<Long, Long> reloadUserMessageMaps(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        TreeMap<Long, Long> out = new TreeMap<>();
        try {
            connection = DBUtil.getConnection();

            String sql = "select `_seq`, `_mid` from " + getUserMessageTable(userId) + " where `_uid` = ? order by `_seq` DESC limit " + MAX_MESSAGE_QUEUE;

            statement = connection.prepareStatement(sql);

            statement.setString(1, userId);

            statement.executeQuery();

            rs = statement.executeQuery();
            while (rs.next()) {
                int index = 1;
                long msgSeq = rs.getLong(index++);
                long msgId = rs.getLong(index);
                out.put(msgSeq, msgId);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }

        return out;
    }

    List<WFCMessage.User> searchUserFromDB(String keyword, boolean buzzy, int page) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        ArrayList<WFCMessage.User> out = new ArrayList<>();

        try {
            connection = DBUtil.getConnection();
            String sql = "select `_uid`, `_name`" +
                ", `_display_name`" +
                ", `_portrait`" +
                ", `_mobile`" +
                ", `_gender`" +
                ", `_email`" +
                ", `_address`" +
                ", `_company`" +
                ", `_social`" +
                ", `_extra`" +
                ", `_dt` from t_user";
            if (buzzy) {
                sql += " where `_display_name` like ?";
            } else {
                sql += " where `_display_name` = ?";
            }

            sql += " and _type <> 2"; //can search normal user(0) and robot(1), can not search things

            sql += " limit 20";

            if (page > 0) {
                sql += "offset = '" + page * 20 + "'";
            }


            statement = connection.prepareStatement(sql);
            int index = 1;
            if (buzzy) {
                statement.setString(index++, "%" + keyword + "%");
            } else {
                statement.setString(index++, keyword);
            }

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.User.Builder builder = WFCMessage.User.newBuilder();
                index = 1;

                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setUid(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setName(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setDisplayName(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setPortrait(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setMobile(value);

                int gender = rs.getInt(index++);
                builder.setGender(gender);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setEmail(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setAddress(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setCompany(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setSocial(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setExtra(value);

                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);

                WFCMessage.User user = builder.build();

                out.add(user);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
    }

    Integer getUserStatus(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_status` from t_user_status where `_uid` = ?";
            statement = connection.prepareStatement(sql);

            statement.setString(1, userId);
            rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return 0;
    }

    void reloadGroupMemberFromDB(HazelcastInstance hzInstance) {
        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(MemoryMessagesStore.GROUP_MEMBERS);

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_gid`" +
                ", `_mid`" +
                ", `_alias`" +
                ", `_type`" +
                ", `_dt` from t_group_member";
            statement = connection.prepareStatement(sql);

            int index;

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.GroupMember.Builder builder = WFCMessage.GroupMember.newBuilder();
                index = 1;

                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                String groupId = value;

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setMemberId(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setAlias(value);

                int intvalue = rs.getInt(index++);
                builder.setType(intvalue);


                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);

                WFCMessage.GroupMember member = builder.build();
                groupMembers.put(groupId, member);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
    }

    void reloadFriendsFromDB(HazelcastInstance hzInstance) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        MultiMap<String, FriendData> friendsMap = hzInstance.getMultiMap(MemoryMessagesStore.USER_FRIENDS);
        if (friendsMap.size() > 0) {
            return;
        }

        try {
            connection = DBUtil.getConnection();
            String sql = "select `_uid`, `_friend_uid`, `_alias`, `_state`, `_dt` from t_friend";
            statement = connection.prepareStatement(sql);

            int index;

            rs = statement.executeQuery();
            while (rs.next()) {
                FriendData builder = new FriendData();
                index = 1;

                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setUserId(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setFriendUid(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setAlias(value);

                int intvalue = rs.getInt(index++);
                builder.setState(intvalue);

                long longvalue = rs.getLong(index++);
                builder.setTimestamp(longvalue);

                friendsMap.put(builder.getUserId(), builder);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
    }

    void reloadFriendRequestsFromDB(HazelcastInstance hzInstance) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        MultiMap<String, WFCMessage.FriendRequest> requestMap = hzInstance.getMultiMap(MemoryMessagesStore.USER_FRIENDS_REQUEST);
        if (requestMap.size() > 0) {
            return;
        }

        try {
            connection = DBUtil.getConnection();
            String sql = "select `_uid`, `_friend_uid`, `_reason`, `_status`, `_dt`, `_from_read_status`, `_to_read_status` from t_friend_request";
            statement = connection.prepareStatement(sql);

            int index;

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.FriendRequest.Builder builder = WFCMessage.FriendRequest.newBuilder();
                index = 1;

                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setFromUid(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setToUid(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setReason(value);

                int intvalue = rs.getInt(index++);
                builder.setStatus(intvalue);

                long longvalue = rs.getLong(index++);
                builder.setUpdateDt(longvalue);

                intvalue = rs.getInt(index++);
                builder.setFromReadStatus(intvalue > 0);

                intvalue = rs.getInt(index++);
                builder.setToReadStatus(intvalue > 0);


                WFCMessage.FriendRequest request = builder.build();
                requestMap.put(request.getFromUid(), request);
                requestMap.put(request.getToUid(), request);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
    }


    void persistMessage(final WFCMessage.Message message, boolean update) {
        if(message.getContent().getPersistFlag() == Transparent) {
            return;
        }

        mScheduler.execute(()-> {
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into " + MessageShardingUtil.getMessageTable(message.getMessageId()) +
                    " (`_mid`, `_from`, `_type`, `_target`, `_line`, `_data`, `_searchable_key`, `_dt`) values(?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_data` = ?," +
                    "`_searchable_key` = ?," +
                    "`_dt` = ?";

                String searchableContent = message.getContent().getSearchableContent() == null ? "" : message.getContent().getSearchableContent();

                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setLong(index++, message.getMessageId());
                statement.setString(index++, message.getFromUser());
                statement.setInt(index++, message.getConversation().getType());
                statement.setString(index++, message.getConversation().getTarget());
                statement.setInt(index++, message.getConversation().getLine());
                Blob blob = connection.createBlob();
                blob.setBytes(1, message.getContent().toByteArray());
                statement.setBlob(index++, blob);
                statement.setString(index++, searchableContent);
                statement.setTimestamp(index++, new Timestamp(message.getServerTimestamp()));

                statement.setBlob(index++, blob);
                statement.setString(index++, searchableContent);
                statement.setTimestamp(index++, new Timestamp(message.getServerTimestamp()));

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    Map<Long, MessageBundle> getMessages(Collection<Long> keys) {
        Map<String, List<Long>> messageTableMap = new HashMap<>();
        for (Long key : keys) {
            String messageTableId = MessageShardingUtil.getMessageTable(key);

            messageTableMap.computeIfAbsent(messageTableId, new Function<String, List<Long>>() {
                @Override
                public List<Long> apply(String s) {
                    return new ArrayList<>();
                }
            });
            messageTableMap.get(messageTableId).add(key);
        }

        Map<Long, MessageBundle> out = null;
        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            out = new HashMap<>();
            for (Map.Entry<String, List<Long>> entry : messageTableMap.entrySet()) {
                String sql = "select `_mid`, `_from`, `_type`, `_target`, `_line`, `_data`, `_dt` from " + entry.getKey() +" where _mid in (";
                for (int i = 0; i < entry.getValue().size(); i++) {
                    sql += entry.getValue().get(i);
                    if (i != entry.getValue().size() - 1) {
                        sql += ",";
                    }
                }
                sql += ")";

                ResultSet resultSet = null;
                try {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    resultSet = statement.executeQuery();

                    while (resultSet.next()) {
                        WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder();
                        int index = 1;
                        builder.setMessageId(resultSet.getLong(index++));
                        builder.setFromUser(resultSet.getString(index++));
                        WFCMessage.Conversation.Builder cb = WFCMessage.Conversation.newBuilder();
                        cb.setType(resultSet.getInt(index++));
                        cb.setTarget(resultSet.getString(index++));
                        cb.setLine(resultSet.getInt(index++));
                        builder.setConversation(cb.build());
                        Blob blob = resultSet.getBlob(index++);

                        WFCMessage.MessageContent messageContent = WFCMessage.MessageContent.parseFrom(blob.getBinaryStream());
                        builder.setContent(messageContent);
                        builder.setServerTimestamp(resultSet.getTimestamp(index++).getTime());
                        WFCMessage.Message message = builder.build();
                        out.put(message.getMessageId(),new MessageBundle(message.getMessageId(), message.getFromUser(), null, message));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                } catch (IOException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                } finally {
                    try {
                        if (resultSet!=null) {
                            resultSet.close();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Utility.printExecption(LOG, e);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, null);
        }

        return out;
    }

    MessageBundle getMessage(long messageId) {
        String sql = "select  `_from`, `_type`, `_target`, `_line`, `_data`, `_dt` from " + MessageShardingUtil.getMessageTable(messageId) +" where _mid = ? limit 1";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setLong(1, messageId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder();
                builder.setMessageId(messageId);
                int index = 1;
                builder.setFromUser(resultSet.getString(index++));
                WFCMessage.Conversation.Builder cb = WFCMessage.Conversation.newBuilder();
                cb.setType(resultSet.getInt(index++));
                cb.setTarget(resultSet.getString(index++));
                cb.setLine(resultSet.getInt(index++));
                builder.setConversation(cb.build());
                Blob blob = resultSet.getBlob(index++);

                WFCMessage.MessageContent messageContent = WFCMessage.MessageContent.parseFrom(blob.getBinaryStream());
                builder.setContent(messageContent);
                builder.setServerTimestamp(resultSet.getTimestamp(index++).getTime());
                WFCMessage.Message message = builder.build();
                return new MessageBundle(messageId, message.getFromUser(), null, message);
            }
            resultSet.close();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
        return null;
    }

    List<WFCMessage.Message> loadRemoteMessages(String user, WFCMessage.Conversation conversation, long beforeUid, int count) {
        List<WFCMessage.Message> messages = loadRemoteMessagesFromTable(user, conversation, beforeUid, count, MessageShardingUtil.getMessageTable(beforeUid));
        if (messages != null && messages.size() < count) {
            String nexTable = MessageShardingUtil.getPreviousMessageTable(beforeUid);
            if (!StringUtil.isNullOrEmpty(nexTable)) {
                List<WFCMessage.Message> nextMessages = loadRemoteMessagesFromTable(user, conversation, beforeUid, count - messages.size(), nexTable);
                if (nextMessages != null) {
                    messages.addAll(nextMessages);
                }
            }
        }
        return messages;
    }

    List<WFCMessage.Message> loadRemoteMessagesFromTable(String user, WFCMessage.Conversation conversation, long beforeUid, int count, String table) {
        String sql = "select `_mid`, `_from`, `_type`, `_target`, `_line`, `_data`, `_dt` from " + table +" where";
        if (conversation.getType() == ProtoConstants.ConversationType.ConversationType_Private) {
            sql += " _type = ? and _line = ? and _mid < ? and ((_target = ?  and _from = ?) or (_target = ?  and _from = ?))";
        } else {
            sql += " _type = ? and _line = ? and _mid < ? and _target = ?";
        }

        sql += "order by `_mid` DESC limit ?";

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<WFCMessage.Message> out = new ArrayList<>();
        try {
            connection = DBUtil.getConnection();
            statement = connection.prepareStatement(sql);
            int index = 1;
            statement.setInt(index++, conversation.getType());
            statement.setInt(index++, conversation.getLine());
            statement.setLong(index++, beforeUid);
            statement.setString(index++, conversation.getTarget());
            if (conversation.getType() == ProtoConstants.ConversationType.ConversationType_Private) {
                statement.setString(index++, user);
                statement.setString(index++, user);
                statement.setString(index++, conversation.getTarget());
            }
            statement.setInt(index++, count);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder();
                index = 1;
                builder.setMessageId(resultSet.getLong(index++));
                builder.setFromUser(resultSet.getString(index++));
                WFCMessage.Conversation.Builder cb = WFCMessage.Conversation.newBuilder();
                cb.setType(resultSet.getInt(index++));
                cb.setTarget(resultSet.getString(index++));
                cb.setLine(resultSet.getInt(index++));
                builder.setConversation(cb.build());
                Blob blob = resultSet.getBlob(index++);

                WFCMessage.MessageContent messageContent = WFCMessage.MessageContent.parseFrom(blob.getBinaryStream());
                builder.setContent(messageContent);
                builder.setServerTimestamp(resultSet.getTimestamp(index++).getTime());
                WFCMessage.Message message = builder.build();
                out.add(message);
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, resultSet);
        }
        return out;
    }

    void persistUserMessage(final String userId, final long messageId, final long messageSeq) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();

                String sql = "insert into " + getUserMessageTable(userId) + " (`_mid`, `_uid`, `_seq`) values(?, ?, ?)";

                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setLong(index++, messageId);
                statement.setString(index++, userId);
                statement.setLong(index++, messageSeq);
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void persistUserSetting(final String userId, WFCMessage.UserSettingEntry entry) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();

                String sql = "insert into t_user_setting (`_uid`" +
                    ", `_scope`" +
                    ", `_key`" +
                    ", `_value`" +
                    ", `_dt`) values(?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_value` = ?," +
                    "`_dt` = ?";

                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setString(index++, userId);
                statement.setInt(index++, entry.getScope());
                statement.setString(index++, entry.getKey());
                statement.setString(index++, entry.getValue());
                statement.setLong(index++, entry.getUpdateDt());
                statement.setString(index++, entry.getValue());
                statement.setLong(index++, entry.getUpdateDt());

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    List<WFCMessage.UserSettingEntry> getPersistUserSetting(final String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select " +
                "`_scope`" +
                ", `_key`" +
                ", `_value`" +
                ", `_dt`" +
                " from t_user_setting where `_uid` = ?";
            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, userId);


            rs = statement.executeQuery();
            List<WFCMessage.UserSettingEntry> out = new ArrayList<>();
            while (rs.next()) {
                WFCMessage.UserSettingEntry.Builder builder = WFCMessage.UserSettingEntry.newBuilder();

                index = 1;
                int intvalue = rs.getInt(index++);
                builder.setScope(intvalue);

                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setKey(value);


                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setValue(value);

                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);

                out.add(builder.build());
            }
            return out;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }


    void persistGroupInfo(final WFCMessage.GroupInfo groupInfo) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into t_group (`_gid`" +
                    ", `_name`" +
                    ", `_portrait`" +
                    ", `_owner`" +
                    ", `_type`" +
                    ", `_extra`" +
                    ", `_dt`" +
                    ", `_member_count`" +
                    ", `_member_dt`) values(?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_name` = ?," +
                    "`_portrait` = ?," +
                    "`_owner` = ?," +
                    "`_type` = ?," +
                    "`_extra` = ?," +
                    "`_dt` = ?," +
                    "`_member_count` = ?," +
                    "`_member_dt` = ?";


                statement = connection.prepareStatement(sql);

                int index = 1;
                statement.setString(index++, groupInfo.getTargetId());
                statement.setString(index++, groupInfo.getName());
                statement.setString(index++, groupInfo.getPortrait());
                statement.setString(index++, groupInfo.getOwner());
                statement.setInt(index++, groupInfo.getType());
                statement.setString(index++, groupInfo.getExtra());
                statement.setLong(index++, groupInfo.getUpdateDt() == 0 ? System.currentTimeMillis() : groupInfo.getUpdateDt());
                statement.setInt(index++, groupInfo.getMemberCount());
                statement.setLong(index++, groupInfo.getMemberUpdateDt() == 0 ? System.currentTimeMillis() : groupInfo.getUpdateDt());

                statement.setString(index++, groupInfo.getName());
                statement.setString(index++, groupInfo.getPortrait());
                statement.setString(index++, groupInfo.getOwner());
                statement.setInt(index++, groupInfo.getType());
                statement.setString(index++, groupInfo.getExtra());
                statement.setLong(index++, groupInfo.getUpdateDt() == 0 ? System.currentTimeMillis() : groupInfo.getUpdateDt());
                statement.setInt(index++, groupInfo.getMemberCount());
                statement.setLong(index++, groupInfo.getMemberUpdateDt() == 0 ? System.currentTimeMillis() : groupInfo.getUpdateDt());
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    MemorySessionStore.Session getSession(String uid, String clientId, ClientSession clientSession) {
        String sql = "select  `_package_name`,`_token`,`_voip_token`,`_secret`,`_db_secret`,`_platform`,`_push_type`,`_device_name`,`_device_version`,`_phone_name`,`_language`,`_carrier_name`, `_dt` from t_user_session where `_uid` = ? and `_cid` = ? limit 1";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, uid);
            statement.setString(2, clientId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                MemorySessionStore.Session session = new MemorySessionStore.Session(uid, clientId, clientSession);

                int index = 1;
                session.setAppName(resultSet.getString(index++));
                session.setDeviceToken(resultSet.getString(index++));
                session.setVoipDeviceToken(resultSet.getString(index++));
                session.setSecret(resultSet.getString(index++));
                session.setDbSecret(resultSet.getString(index++));
                session.setPlatform(resultSet.getInt(index++));
                session.setPushType(resultSet.getInt(index++));

                session.setDeviceName(resultSet.getString(index++));
                session.setDeviceVersion(resultSet.getString(index++));
                session.setPhoneName(resultSet.getString(index++));
                session.setLanguage(resultSet.getString(index++));
                session.setCarrierName(resultSet.getString(index++));
                session.setUpdateDt(resultSet.getLong(index));
                return session;
            }
            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
        return null;
    }

    void updateSessionToken(String uid, String cid, String token, boolean voipToken) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();

                String sql ;

                if (voipToken) {
                    sql = "update t_user_session set `_voip_token` = ?, `_dt` = ? where `_uid` = ? and `_cid` = ?";
                } else {
                    sql = "update t_user_session set `_token` = ?, `_dt` = ? where `_uid` = ? and `_cid` = ?";
                }
                statement = connection.prepareStatement(sql);

                int index = 1;
                statement.setString(index++, token);
                statement.setLong(index++, System.currentTimeMillis());
                statement.setString(index++, uid);
                statement.setString(index++, cid);

                int c = statement.executeUpdate();
                LOG.info("Update rows {}", c);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    private boolean strEqual(String left, String right) {
        if (left == right)
            return true;
        if (left == null)
            return false;
        return left.equals(right);
    }

    void updateSession(String uid, String cid, MemorySessionStore.Session session, WFCMessage.RouteRequest request) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();

                String sql = "update t_user_session set";


                if (!strEqual(session.getAppName(), request.getApp())) {
                    sql += " `_package_name` = ?,";
                }
                if (session.getPlatform() != request.getPlatform()) {
                    sql += " `_platform` = ?,";
                }
                if (session.getPushType() != request.getPushType()) {
                    sql += " `_push_type` = ?,";
                }
                if (!strEqual(session.getDeviceName(), request.getDeviceName())) {
                    sql += " `_device_name` = ?,";
                }
                if (!strEqual(session.getDeviceVersion(), request.getDeviceVersion())) {
                    sql += " `_device_version` = ?,";
                }
                if (!strEqual(session.getPhoneName(), request.getPhoneName())) {
                    sql += " `_phone_name` = ?,";
                }
                if (!strEqual(session.getLanguage(), request.getLanguage())) {
                    sql += " `_language` = ?,";
                }
                if (!strEqual(session.getCarrierName(), request.getCarrierName())) {
                    sql += " `_carrier_name` = ?,";
                }

                sql += " `_dt` = ?";

                sql += " where `_uid` = ? and `_cid` = ?";

                statement = connection.prepareStatement(sql);

                int index = 1;

                if (!strEqual(session.getAppName(), request.getApp())) {
                    statement.setString(index++, request.getApp());
                }
                if (session.getPlatform() != request.getPlatform()) {
                    statement.setInt(index++, request.getPlatform());
                }
                if (session.getPushType() != request.getPushType()) {
                    statement.setInt(index++, request.getPushType());
                }
                if (!strEqual(session.getDeviceName(), request.getDeviceName())) {
                    statement.setString(index++, request.getDeviceName());
                }
                if (!strEqual(session.getDeviceVersion(), request.getDeviceVersion())) {
                    statement.setString(index++, request.getDeviceVersion());
                }
                if (!strEqual(session.getPhoneName(), request.getPhoneName())) {
                    statement.setString(index++, request.getPhoneName());
                }
                if (!strEqual(session.getLanguage(), request.getLanguage())) {
                    statement.setString(index++, request.getLanguage());
                }
                if (!strEqual(session.getCarrierName(), request.getCarrierName())) {
                    statement.setString(index++, request.getCarrierName());
                }

                statement.setLong(index++, System.currentTimeMillis());
                statement.setString(index++, uid);
                statement.setString(index++, cid);

                int c = statement.executeUpdate();
                LOG.info("Update rows {}", c);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    MemorySessionStore.Session createSession(String uid, String clientId, ClientSession clientSession) {
        Connection connection = null;
        PreparedStatement statement = null;
        LOG.info("Database create session {},{}", uid, clientId);
        try {
            connection = DBUtil.getConnection();
            String sql = "insert into t_user_session  (`_uid`,`_cid`,`_secret`,`_db_secret`, `_dt`) values (?,?,?,?,?)";

            statement = connection.prepareStatement(sql);

            int index = 1;

            MemorySessionStore.Session session = new MemorySessionStore.Session(uid, clientId, clientSession);


            statement.setString(index++, uid);
            statement.setString(index++, clientId);


            session.setSecret(UUID.randomUUID().toString());
            statement.setString(index++, session.getSecret());

            session.setDbSecret(UUID.randomUUID().toString());
            statement.setString(index++, session.getDbSecret());

            statement.setLong(index++, System.currentTimeMillis());

            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);

            return session;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
        return null;
    }

    void updateGroupMemberCountDt(final String groupId, final int count, final long dt) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();

                String sql ;

                if (count >= 0) {
                    sql = "update t_group set `_member_count` = ?, `_member_dt` = ? , `_dt` = ? where `_gid` = ?";
                } else {
                    sql = "update t_group set `_member_dt` = ?, `_dt` = ? where `_gid` = ?";
                }
                statement = connection.prepareStatement(sql);

                int index = 1;
                if (count >=0) {
                    statement.setInt(index++, count);
                }
                statement.setLong(index++, dt);
                statement.setLong(index++, dt);
                statement.setString(index++, groupId);

                int c = statement.executeUpdate();
                LOG.info("Update rows {}", c);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void persistGroupMember(final String groupId, final List<WFCMessage.GroupMember> memberList) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                connection.setAutoCommit(false);

                String sql = "insert into t_group_member (`_gid`" +
                    ", `_mid`" +
                    ", `_alias`" +
                    ", `_type`" +
                    ", `_dt`) values(?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_alias` = ?," +
                    "`_type` = ?," +
                    "`_dt` = ?";

                statement = connection.prepareStatement(sql);

                for (WFCMessage.GroupMember member : memberList
                    ) {
                    int index = 1;
                    long dt = System.currentTimeMillis();
                    if (member.getUpdateDt() > 0) {
                        dt = member.getUpdateDt();
                    }

                    statement.setString(index++, groupId);
                    statement.setString(index++, member.getMemberId());
                    statement.setString(index++, member.getAlias());
                    statement.setInt(index++, member.getType());
                    statement.setLong(index++, dt);
                    statement.setString(index++, member.getAlias());
                    statement.setInt(index++, member.getType());
                    statement.setLong(index++, dt);
                    int count = statement.executeUpdate();
                    LOG.info("Update rows {}", count);
                }

                connection.commit();
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                if (connection != null) {
                    try {
                        connection.commit();
                        connection.setAutoCommit(true);
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    WFCMessage.GroupInfo getPersistGroupInfo(String groupId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_name`" +
                ", `_portrait`" +
                ", `_owner`" +
                ", `_type`" +
                ", `_extra`" +
                ", `_dt`" +
                ", `_member_count`" +
                ", `_member_dt`" +
                " from t_group  where `_gid` = ?";

            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, groupId);

            rs = statement.executeQuery();
            if (rs.next()) {
                String strValue;
                int intValue;
                WFCMessage.GroupInfo.Builder builder = WFCMessage.GroupInfo.newBuilder();
                index = 1;

                builder.setTargetId(groupId);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setName(strValue);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setPortrait(strValue);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setOwner(strValue);

                intValue = rs.getInt(index++);
                builder.setType(intValue);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setExtra(strValue);



                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);


                intValue = rs.getInt(index++);
                builder.setMemberCount(intValue);

                longValue = rs.getLong(index++);
                builder.setMemberUpdateDt(longValue);

                return builder.build();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    void updateChatroomInfo(String chatroomId, WFCMessage.ChatroomInfo chatroomInfo) {
        LOG.info("Database update chatroom info {}", chatroomId);
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into t_chatroom (`_cid`" +
                    ", `_title`" +
                    ", `_portrait`" +
                    ", `_state`" +
                    ", `_desc`" +
                    ", `_extra`" +
                    ", `_dt`) values(?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE `_title`=?" +
                    ", `_portrait`=?" +
                    ", `_state`=?" +
                    ", `_desc`=?" +
                    ", `_extra`=?" +
                    ", `_dt`=?";

                statement = connection.prepareStatement(sql);

                int index = 1;

                statement.setString(index++, chatroomId);
                statement.setString(index++, chatroomInfo.getTitle());
                statement.setString(index++, chatroomInfo.getPortrait());
                statement.setInt(index++, chatroomInfo.getState());
                statement.setString(index++, chatroomInfo.getDesc());
                statement.setString(index++, chatroomInfo.getExtra());
                statement.setLong(index++, chatroomInfo.getUpdateDt() == 0 ? System.currentTimeMillis() : chatroomInfo.getUpdateDt());

                statement.setString(index++, chatroomInfo.getTitle());
                statement.setString(index++, chatroomInfo.getPortrait());
                statement.setInt(index++, chatroomInfo.getState());
                statement.setString(index++, chatroomInfo.getDesc());
                statement.setString(index++, chatroomInfo.getExtra());
                statement.setLong(index++, chatroomInfo.getUpdateDt() == 0 ? System.currentTimeMillis() : chatroomInfo.getUpdateDt());

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void removeChatroomInfo(String chatroomId) {
        LOG.info("Database remove chatroom {}", chatroomId);
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "delete from t_chatroom where `_cid`=?";
                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setString(index++, chatroomId);

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    WFCMessage.ChatroomInfo getPersistChatroomInfo(String chatroomId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_title`" +
                ", `_portrait`" +
                ", `_state`" +
                ", `_desc`" +
                ", `_extra`" +
                ", `_dt`" +
                " from t_chatroom  where `_cid` = ?";

            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, chatroomId);

            rs = statement.executeQuery();
            if (rs.next()) {
                String strValue;
                int intValue;
                WFCMessage.ChatroomInfo.Builder builder = WFCMessage.ChatroomInfo.newBuilder();
                index = 1;

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setTitle(strValue);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setPortrait(strValue);

                intValue = rs.getInt(index++);
                builder.setState(intValue);


                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setDesc(strValue);



                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setExtra(strValue);



                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);
                builder.setCreateDt(longValue);

                return builder.build();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    void updateUserPassword(final String userId, final String password) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "update t_user set `_passwd_md5` = ? where `_uid` = ?";

                statement = connection.prepareStatement(sql);

                int index = 1;

                try {
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    BASE64Encoder base64en = new BASE64Encoder();
                    String passwdMd5 = base64en.encode(md5.digest(password.getBytes("utf-8")));
                    statement.setString(index, passwdMd5);
                } catch (Exception e) {
                    statement.setString(index, "");
                }
                index++;

                statement.setString(index++, userId);
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void deleteUser(final String userId) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "delete from t_user where `_uid`=?";
                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setString(index++, userId);

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void deleteRobot(String robotId) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "delete from t_robot where `_uid`=?";
                statement = connection.prepareStatement(sql);
                statement.setString(1, robotId);

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }
    WFCMessage.Robot getRobot(String robotId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_owner`" +
                ", `_secret`" +
                ", `_callback`" +
                ", `_state`" +
                ", `_extra`" +
                ", `_dt` from t_robot where `_uid` = ?";
            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(1, robotId);


            rs = statement.executeQuery();
            if (rs.next()) {
                WFCMessage.Robot.Builder builder = WFCMessage.Robot.newBuilder();
                index = 1;
                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setUid(robotId);
                builder.setOwner(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setSecret(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setCallback(value);


                int state = rs.getInt(index++);
                builder.setState(state);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setExtra(value);

                long longValue = rs.getLong(index++);

                return builder.build();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    void updateRobot(final WFCMessage.Robot robot) {
        LOG.info("Database update user info {}", robot.getUid());
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into t_robot (`_uid`" +
                    ", `_owner`" +
                    ", `_secret`" +
                    ", `_callback`" +
                    ", `_state`" +
                    ", `_extra`" +
                    ", `_dt`) values(?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE `_owner`=?" +
                    ", `_secret`=?" +
                    ", `_callback`=?" +
                    ", `_state`=?" +
                    ", `_extra`=?" +
                    ", `_dt`=?";

                statement = connection.prepareStatement(sql);

                int index = 1;

                statement.setString(index++, robot.getUid());
                statement.setString(index++, robot.getOwner());
                statement.setString(index++, robot.getSecret());
                statement.setString(index++, robot.getCallback());
                statement.setInt(index++, robot.getState());
                statement.setString(index++, robot.getExtra());
                statement.setLong(index++, System.currentTimeMillis());

                statement.setString(index++, robot.getOwner());
                statement.setString(index++, robot.getSecret());
                statement.setString(index++, robot.getCallback());
                statement.setInt(index++, robot.getState());
                statement.setString(index++, robot.getExtra());
                statement.setLong(index++, System.currentTimeMillis());

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void updateUser(final WFCMessage.User user) {
        LOG.info("Database update user info {} {}", user.getUid(), user.getUpdateDt());
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            LOG.info("Database update user info {}", user.getDisplayName());
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into t_user (`_uid`" +
                    ", `_name`" +
                    ", `_display_name`" +
                    ", `_portrait`" +
                    ", `_mobile`" +
                    ", `_gender`" +
                    ", `_email`" +
                    ", `_address`" +
                    ", `_company`" +
                    ", `_social`" +
                    ", `_extra`" +
                    ", `_type`" +
                    ", `_dt`) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE `_name`=?" +
                    ", `_display_name`=?" +
                    ", `_portrait`=?" +
                    ", `_mobile`=?" +
                    ", `_gender`=?" +
                    ", `_email`=?" +
                    ", `_address`=?" +
                    ", `_company`=?" +
                    ", `_social`=?" +
                    ", `_extra`=?" +
                    ", `_type`=?" +
                    ", `_dt`=?";

                statement = connection.prepareStatement(sql);

                int index = 1;

                statement.setString(index++, user.getUid());
                statement.setString(index++, user.getName());
                statement.setString(index++, user.getDisplayName());
                statement.setString(index++, user.getPortrait());
                statement.setString(index++, user.getMobile());
                statement.setInt(index++, user.getGender());
                statement.setString(index++, user.getEmail());
                statement.setString(index++, user.getAddress());
                statement.setString(index++, user.getCompany());
                statement.setString(index++, user.getSocial());

                statement.setString(index++, user.getExtra());
                statement.setInt(index++, user.getType());
                statement.setLong(index++, user.getUpdateDt() == 0 ? System.currentTimeMillis() : user.getUpdateDt());

                statement.setString(index++, user.getName());
                statement.setString(index++, user.getDisplayName());
                statement.setString(index++, user.getPortrait());
                statement.setString(index++, user.getMobile());
                statement.setInt(index++, user.getGender());
                statement.setString(index++, user.getEmail());
                statement.setString(index++, user.getAddress());
                statement.setString(index++, user.getCompany());
                statement.setString(index++, user.getSocial());

                statement.setString(index++, user.getExtra());
                statement.setInt(index++, user.getType());
                statement.setLong(index++, user.getUpdateDt() == 0 ? System.currentTimeMillis() : user.getUpdateDt());

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void updateUserStatus(String userId, int status) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                if (status == 0) {
                    String sql = "delete from t_user_status where _uid = ?";
                    statement = connection.prepareStatement(sql);
                    statement.setString(1, userId);
                    int count = statement.executeUpdate();
                    LOG.info("Update rows {}", count);
                } else {
                    String sql = "insert into t_user_status (`_uid`, `_status`, `_dt`) values(?,?,?) ON DUPLICATE KEY UPDATE `_status` = ?";

                    statement = connection.prepareStatement(sql);
                    int index = 1;
                    statement.setString(index++, userId);
                    statement.setInt(index++, status);
                    statement.setLong(index++,  System.currentTimeMillis());
                    statement.setInt(index++, status);

                    int count = statement.executeUpdate();
                    LOG.info("Update rows {}", count);
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    int getGeneratedId() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "INSERT INTO `t_id_generator` (`id`) VALUES (NULL);";

            statement = connection.prepareStatement(sql);
            if(statement.executeUpdate()> 0) {
                sql = "SELECT LAST_INSERT_ID()";

                try {
                    if (statement!=null) {
                        statement.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                }

                statement = connection.prepareStatement(sql);

                rs = statement.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return -1;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
            return -1;
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
    }

    WFCMessage.User getPersistUser(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_name`" +
                ", `_display_name`" +
                ", `_portrait`" +
                ", `_mobile`" +
                ", `_gender`" +
                ", `_email`" +
                ", `_address`" +
                ", `_company`" +
                ", `_social`" +
                ", `_extra`" +
                ", `_type`" +
                ", `_dt` from t_user where `_uid` = ?";
            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(1, userId);


            rs = statement.executeQuery();
            if (rs.next()) {
                WFCMessage.User.Builder builder = WFCMessage.User.newBuilder();
                index = 1;
                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setUid(userId);
                builder.setName(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setDisplayName(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setPortrait(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setMobile(value);

                int gender = rs.getInt(index++);
                builder.setGender(gender);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setEmail(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setAddress(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setCompany(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setSocial(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setExtra(value);

                int type = rs.getInt(index++);
                builder.setType(type);

                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);

                return builder.build();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    String getUserIdByName(String name) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_uid` from t_user where `_name` = ? limit 1";
            statement = connection.prepareStatement(sql);
            statement.setString(1, name);
            rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    String getUserIdByMobile(String mobile) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_uid` from t_user where `_mobile` = ? limit 1";
            statement = connection.prepareStatement(sql);
            statement.setString(1, mobile);
            rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    List<FriendData> getPersistFriends(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_friend_uid`, `_alias`, `_state`, `_dt` from t_friend where `_uid` = ?";
            statement = connection.prepareStatement(sql);


            int index = 1;
            statement.setString(index++, userId);


            rs = statement.executeQuery();
            List<FriendData> out = new ArrayList<>();
            while (rs.next()) {
                String uid = rs.getString(1);
                String alias = rs.getString(2);
                int state = rs.getInt(3);
                long timestamp = rs.getLong(4);

                FriendData data = new FriendData(userId, uid, alias, state, timestamp);
                out.add(data);
            }
            return out;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    List<WFCMessage.FriendRequest> getPersistFriendRequests(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select   `_uid`" +
                ", `_friend_uid`" +
                ", `_reason`" +
                ", `_status`" +
                ", `_dt`" +
                ", `_from_read_status`" +
                ", `_to_read_status` from t_friend_request where `_uid` = ? or `_friend_uid` = ?";
            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, userId);
            statement.setString(index++, userId);


            rs = statement.executeQuery();
            List<WFCMessage.FriendRequest> out = new ArrayList<>();
            while (rs.next()) {
                WFCMessage.FriendRequest.Builder builder = WFCMessage.FriendRequest.newBuilder();
                index = 1;
                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setFromUid(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setToUid(value);


                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setReason(value);

                int intValue = rs.getInt(index++);
                builder.setStatus(intValue);

                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);

                boolean b = rs.getBoolean(index++);
                builder.setFromReadStatus(b);

                b = rs.getBoolean(index++);
                builder.setToReadStatus(b);

                out.add(builder.build());
            }
            return out;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    void persistFriendRequestUnreadStatus(String userId, long readDt, long updateDt) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "update t_friend_request set `_dt`=? , `_to_read_status`=?" +
                    " where " +
                    "`_friend_uid` = ? and" +
                    "`_dt` <= ? and" +
                    "`_to_read_status` = ?";

                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setLong(index++, updateDt);
                statement.setBoolean(index++, true);
                statement.setString(index++, userId);
                statement.setLong(index++, readDt);
                statement.setBoolean(index++, false);
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }
    //
    void persistOrUpdateFriendRequest(final WFCMessage.FriendRequest request) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into t_friend_request (`_uid`, `_friend_uid`, `_reason`, `_status`, `_dt`, `_from_read_status`, `_to_read_status`) values(?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_reason` = ?," +
                    "`_status` = ?," +
                    "`_dt` = ?," +
                    "`_from_read_status` = ?," +
                    "`_to_read_status` = ?";

                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setString(index++, request.getFromUid());
                statement.setString(index++, request.getToUid());
                statement.setString(index++, request.getReason());
                statement.setInt(index++, request.getStatus());
                statement.setLong(index++, request.getUpdateDt());
                statement.setInt(index++, request.getFromReadStatus() ? 1 : 0);
                statement.setInt(index++, request.getToReadStatus() ? 1 : 0);

                statement.setString(index++, request.getReason());
                statement.setInt(index++, request.getStatus());
                statement.setLong(index++, request.getUpdateDt());
                statement.setInt(index++, request.getFromReadStatus() ? 1 : 0);
                statement.setInt(index++, request.getToReadStatus() ? 1 : 0);
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void persistOrUpdateFriendData(final FriendData request) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into t_friend (`_uid`, `_friend_uid`, `_alias`, `_state`, `_dt`) values(?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_alias` = ?," +
                    "`_state` = ?," +
                    "`_dt` = ?";


                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setString(index++, request.getUserId());
                statement.setString(index++, request.getFriendUid());
                statement.setString(index++, request.getAlias());
                statement.setInt(index++, request.getState());
                statement.setLong(index++, request.getTimestamp());
                statement.setString(index++, request.getAlias());
                statement.setInt(index++, request.getState());
                statement.setLong(index++, request.getTimestamp());
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    boolean removeGroupInfoFromDB(String groupId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "delete from t_group where _gid = ?";


            statement = connection.prepareStatement(sql);
            int index = 1;
            statement.setString(index++, groupId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
        return false;
    }

    boolean removeGroupMemberFromDB(String groupId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "delete from t_group_member where _gid = ?";

            statement = connection.prepareStatement(sql);
            int index = 1;
            statement.setString(index++, groupId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
        return false;
    }

    void updateChannelInfo(final WFCMessage.ChannelInfo channelInfo) {
        LOG.info("Database update channel info {} {}", channelInfo.getTargetId(), channelInfo.getUpdateDt());
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into t_channel (`_cid`" +
                    ", `_name`" +
                    ", `_portrait`" +
                    ", `_owner`" +
                    ", `_status`" +
                    ", `_desc`" +
                    ", `_extra`" +
                    ", `_secret`" +
                    ", `_callback`" +
                    ", `_automatic`" +
                    ", `_dt`) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE `_name`=?" +
                    ", `_portrait`=?" +
                    ", `_owner`=?" +
                    ", `_status`=?" +
                    ", `_desc`=?" +
                    ", `_extra`=?" +
                    ", `_secret`=?" +
                    ", `_callback`=?" +
                    ", `_automatic`=?" +
                    ", `_dt`=?";

                statement = connection.prepareStatement(sql);

                int index = 1;

                statement.setString(index++, channelInfo.getTargetId());
                statement.setString(index++, channelInfo.getName());
                statement.setString(index++, channelInfo.getPortrait());
                statement.setString(index++, channelInfo.getOwner());
                statement.setInt(index++, channelInfo.getStatus());
                statement.setString(index++, channelInfo.getDesc());
                statement.setString(index++, channelInfo.getExtra());
                statement.setString(index++, channelInfo.getSecret());
                statement.setString(index++, channelInfo.getCallback());
                statement.setInt(index++, channelInfo.getAutomatic());
                statement.setLong(index++, channelInfo.getUpdateDt() == 0 ? System.currentTimeMillis() : channelInfo.getUpdateDt());

                statement.setString(index++, channelInfo.getName());
                statement.setString(index++, channelInfo.getPortrait());
                statement.setString(index++, channelInfo.getOwner());
                statement.setInt(index++, channelInfo.getStatus());
                statement.setString(index++, channelInfo.getDesc());
                statement.setString(index++, channelInfo.getExtra());
                statement.setString(index++, channelInfo.getSecret());
                statement.setString(index++, channelInfo.getCallback());
                statement.setInt(index++, channelInfo.getAutomatic());
                statement.setLong(index++, channelInfo.getUpdateDt() == 0 ? System.currentTimeMillis() : channelInfo.getUpdateDt());

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void removeChannelInfo(final String channelId) {
        LOG.info("Database remove channel {}", channelId);
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "delete from t_channel where `_cid`=?";
                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setString(index++, channelId);

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    WFCMessage.ChannelInfo getPersistChannelInfo(String channelId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_name`" +
                ", `_portrait`" +
                ", `_owner`" +
                ", `_status`" +
                ", `_desc`" +
                ", `_extra`" +
                ", `_secret`" +
                ", `_callback`" +
                ", `_automatic`" +
                ", `_dt` from t_channel  where `_cid` = ?";

            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, channelId);

            rs = statement.executeQuery();
            if (rs.next()) {
                String strValue;
                int intValue;
                WFCMessage.ChannelInfo.Builder builder = WFCMessage.ChannelInfo.newBuilder();
                index = 1;

                builder.setTargetId(channelId);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setName(strValue);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setPortrait(strValue);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setOwner(strValue);

                intValue = rs.getInt(index++);
                builder.setStatus(intValue);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setDesc(strValue);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setExtra(strValue);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setSecret(strValue);

                strValue = rs.getString(index++);
                strValue = (strValue == null ? "" : strValue);
                builder.setCallback(strValue);

                intValue = rs.getInt(index++);
                builder.setAutomatic(intValue);

                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);

                return builder.build();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }


    void persistChannelListener(final String groupId, final List<String> memberList) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                connection.setAutoCommit(false);

                String sql = "insert into t_channel_listener (`_cid`" +
                    ", `_mid`" +
                    ", `_dt`) values(?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_dt` = ?";

                statement = connection.prepareStatement(sql);

                long dt = System.currentTimeMillis();
                for (String member : memberList) {
                    int index = 1;
                    statement.setString(index++, groupId);
                    statement.setString(index++, member);
                    statement.setLong(index++, dt);
                    statement.setLong(index++, dt);
                    int count = statement.executeUpdate();
                    LOG.info("Update rows {}", count);
                }

                connection.commit();
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                if (connection != null) {
                    try {
                        connection.commit();
                        connection.setAutoCommit(true);
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    List<String> getChannelListener(String channelId) {
        List<String> out = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_mid` from t_channel_listener where _cid = ?";
            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, channelId);

            rs = statement.executeQuery();
            while (rs.next()) {

                String value = rs.getString(index++);
                if (!StringUtil.isNullOrEmpty(value)) {
                    out.add(value);
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
    }

    List<WFCMessage.ChannelInfo> searchChannelFromDB(String keyword, boolean buzzy, int page) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        ArrayList<WFCMessage.ChannelInfo> out = new ArrayList<>();

        try {
            connection = DBUtil.getConnection();
            String sql = "select `_cid`, `_name`" +
                ", `_portrait`" +
                ", `_owner`" +
                ", `_status`" +
                ", `_desc`" +
                ", `_extra`" +
                ", `_dt` from t_channel";
            if (buzzy) {
                sql += " where `_name` like ?";
            } else {
                sql += " where `_name` = ?";
            }

            sql += " and _status = 0";

            sql += " limit 20";


            if (page > 0) {
                sql += "offset = '" + page * 20 + "'";
            }


            statement = connection.prepareStatement(sql);
            int index = 1;
            if (buzzy) {
                statement.setString(index++, "%" + keyword + "%");
            } else {
                statement.setString(index++, keyword);
            }

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.ChannelInfo.Builder builder = WFCMessage.ChannelInfo.newBuilder();
                index = 1;

                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setTargetId(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setName(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setPortrait(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setOwner(value);

                int status = rs.getInt(index++);
                builder.setStatus(status);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setDesc(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setExtra(value);

                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);

                out.add(builder.build());
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
    }

    void updateChannelListener(final String channelId, final String listener, final boolean listen) {
        LOG.info("Database remove channel {}", channelId);
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql;
                if (listen) {
                    sql = "insert into t_channel_listener (`_cid`" +
                        ", `_mid`" +
                        ", `_dt`) values(?, ?, ?)" +
                        " ON DUPLICATE KEY UPDATE " +
                        "`_dt` = ?";
                    statement = connection.prepareStatement(sql);
                    int index = 1;
                    statement.setString(index++, channelId);
                    statement.setString(index++, listener);
                    statement.setLong(index++, System.currentTimeMillis());
                    statement.setLong(index++, System.currentTimeMillis());

                    int count = statement.executeUpdate();
                    LOG.info("Update rows {}", count);
                } else {
                    sql = "delete from t_channel_listener where `_cid`=? and `_mid`=?";

                    statement = connection.prepareStatement(sql);
                    int index = 1;
                    statement.setString(index++, channelId);
                    statement.setString(index++, listener);

                    int count = statement.executeUpdate();
                    LOG.info("Update rows {}", count);
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    public Set<String> getSensitiveWord() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        Set<String> out = new HashSet<>();

        try {
            connection = DBUtil.getConnection();
            String sql = "select `_word` from t_sensitiveword";

            statement = connection.prepareStatement(sql);
            rs = statement.executeQuery();
            while (rs.next()) {
                String value = rs.getString(1);
                out.add(value);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
    }

    void deleteSensitiveWord(final String word) {
        LOG.info("delete sensitive word {}", word);
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();

                String sql = "delete from t_sensitiveword where `_word` = ?";
                statement = connection.prepareStatement(sql);

                statement.setString(1, word);
                int c = statement.executeUpdate();
                LOG.info("Update rows {}", c);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void persistSensitiveWord(final String word) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();

                String sql = "insert into t_sensitiveword (`_word`) values(?)";

                statement = connection.prepareStatement(sql);
                statement.setString(1, word);
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    private String getUserMessageTable(String uid) {
        if (DBUtil.IsEmbedDB) {
            return "t_user_messages";
        }
        int hashId = Math.abs(uid.hashCode())%128;
        return "t_user_messages_" + hashId;
    }
}
