/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.persistence;

import cn.wildfirechat.pojos.SystemSettingPojo;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import cn.wildfirechat.server.ThreadPoolExecutorWrapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;
import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.model.FriendData;
import io.moquette.server.Server;
import io.moquette.spi.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.DBUtil;
import win.liyufan.im.MessageBundle;
import win.liyufan.im.MessageShardingUtil;
import win.liyufan.im.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.function.Function;


import static cn.wildfirechat.common.IMExceptionEvent.EventType.RDBS_Exception;
import static cn.wildfirechat.proto.ProtoConstants.PersistFlag.Transparent;
import static cn.wildfirechat.proto.ProtoConstants.UserSearchUserType.UserSearchUserType_ONLY_ROBOT;
import static cn.wildfirechat.proto.ProtoConstants.UserSearchUserType.UserSearchUserType_ONLY_USER;
import static io.moquette.BrokerConstants.GROUP_INFO_MARK_DELETION;
import static io.moquette.server.Constants.MAX_MESSAGE_QUEUE;
import static cn.wildfirechat.proto.ProtoConstants.SearchUserType.*;
import static win.liyufan.im.UserSettingScope.kUserSettingPrivacySearchable;

public class DatabaseStore {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseStore.class);
    private final ThreadPoolExecutorWrapper mScheduler;
    private boolean disableRemoteMessageSearch = false;
    private boolean encryptMessage = false;
    private boolean keepGroupInfo = false;
    public void setDisableRemoteMessageSearch(boolean disableRemoteMessageSearch) {
        this.disableRemoteMessageSearch = disableRemoteMessageSearch;
    }

    public void setEncryptMessage(boolean encryptMessage) {
        this.encryptMessage = encryptMessage;
    }

    public DatabaseStore(ThreadPoolExecutorWrapper scheduler) {
        this.mScheduler = scheduler;
        try {
            keepGroupInfo = Boolean.parseBoolean(Server.getServer().getConfig().getProperty(GROUP_INFO_MARK_DELETION, "false"));
        } catch (Exception e) {

        }
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
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }

        return out;
    }

    boolean updateSystemSetting(int id, String value, String desc) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "insert into t_settings " +
                " (`id`, `_value`, `_desc`) values(?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE " +
                "`_value` = ?," +
                "`_desc` = ?";


            statement = connection.prepareStatement(sql);
            int index = 1;
            statement.setLong(index++, id);
            statement.setString(index++, value);
            statement.setString(index++, desc);
            statement.setString(index++, value);
            statement.setString(index++, desc);

            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
            return true;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
        return false;
    }

    SystemSettingPojo getSystemSetting(int id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            connection = DBUtil.getConnection();
            String sql = "select `_value`, `_desc` from t_settings where `id` = ?";

            statement = connection.prepareStatement(sql);
            int index = 1;
            statement.setLong(index++, id);

            rs = statement.executeQuery();
            while (rs.next()) {
                SystemSettingPojo out = new SystemSettingPojo();
                index = 1;

                out.id = id;

                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                out.value = value;

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                out.desc = value;

               return out;
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    List<WFCMessage.User> searchUserByNameMobileUserId(String keyword, int searchType, int userType) {
        ArrayList<WFCMessage.User> out = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
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
                ", `_dt`, `_type` from t_user";

            int sqlParaCount = 1;
            if(searchType == SearchUserType_Name) {
                sql += " where `_name` = ? ";
            } else if(searchType == SearchUserType_Mobile) {
                sql += " where `_mobile` = ? ";
            } else if(searchType == SearchUserType_UserId) {
                sql += " where `_uid` = ? ";
            } else if(searchType == SearchUserType_Name_Mobile || searchType == SearchUserType_Name_Mobile_DisplayName) {
                sqlParaCount = 2;
                sql += " where (`_name` = ? or `_mobile` = ?) ";
            } else {
                sqlParaCount = 3;
                sql += " where (`_name` = ? or `_mobile` = ? or `_uid` = ?) ";
            }

            if(userType == UserSearchUserType_ONLY_USER) {
                sql += " and _type <> 2 "; //can not search device
                sql += " and _type <> 1 ";
            } else if(userType == UserSearchUserType_ONLY_ROBOT) {
                sql += " and _type = 1 ";
            } else {
                sql += " and _type <> 2 "; //can not search device
            }
            sql += " and _deleted = 0 ";

            if(searchType == SearchUserType_Name || searchType == SearchUserType_Mobile || searchType == SearchUserType_UserId) {
                sql += " limit 1";
            } else if(searchType == SearchUserType_Name_Mobile || searchType == SearchUserType_Name_Mobile_DisplayName) {
                sql += " limit 2";
            } else {
                sql += " limit 3";
            }

            statement = connection.prepareStatement(sql);
            for (int i = 0; i < sqlParaCount; i++) {
                statement.setString(i+1, keyword);
            }

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.User.Builder builder = WFCMessage.User.newBuilder();
                int index = 1;

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

                int type = rs.getInt(index++);
                builder.setType( type);

                WFCMessage.User user = builder.build();

                out.add(user);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }

        List<WFCMessage.User> filter = new ArrayList<>();
        for (WFCMessage.User user : out) {
            WFCMessage.UserSettingEntry userSettingEntry = Server.getServer().getStore().messagesStore().getUserSetting(user.getUid(), kUserSettingPrivacySearchable, null);
            if(userSettingEntry != null) {
                int value = 0;
                try {
                    value = StringUtil.isNullOrEmpty(userSettingEntry.getValue())?0:Integer.parseInt(userSettingEntry.getValue());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if((value & ProtoConstants.DisableSearchMask.DisableSearchNameMask) > 0) {
                    if (searchType == SearchUserType_Name) {
                        filter.add(user);
                        continue;
                    } else if(searchType == SearchUserType_Name_Mobile || searchType == SearchUserType_Name_Mobile_DisplayName) {
                        if(keyword.equals(user.getName())) {
                            if(!keyword.equals(user.getMobile())) {
                                filter.add(user);
                                continue;
                            } else if((value & ProtoConstants.DisableSearchMask.DisableSearchMobileMask) > 0) {
                                filter.add(user);
                                continue;
                            }
                        }
                    } else if(searchType == SearchUserType_Name_Mobile_UserId) {
                        if(keyword.equals(user.getName())) {
                            if(keyword.equals(user.getMobile()) && keyword.equals(user.getUid())) {
                                if((value & ProtoConstants.DisableSearchMask.DisableSearchMobileMask) > 0 && (value & ProtoConstants.DisableSearchMask.DisableSearchUserIdMask) > 0) {
                                    filter.add(user);
                                    continue;
                                }
                            } else if(keyword.equals(user.getMobile())) {
                                if((value & ProtoConstants.DisableSearchMask.DisableSearchMobileMask) > 0) {
                                    filter.add(user);
                                    continue;
                                }
                            } else if(keyword.equals(user.getUid())) {
                                if((value & ProtoConstants.DisableSearchMask.DisableSearchUserIdMask) > 0) {
                                    filter.add(user);
                                    continue;
                                }
                            } else {
                                filter.add(user);
                                continue;
                            }
                        }
                    }
                }


                if((value & ProtoConstants.DisableSearchMask.DisableSearchMobileMask) > 0) {
                    if (searchType == SearchUserType_Mobile) {
                        filter.add(user);
                        continue;
                    } else if(searchType == SearchUserType_Name_Mobile || searchType == SearchUserType_Name_Mobile_DisplayName) {
                        if(keyword.equals(user.getMobile())) {
                            if(!keyword.equals(user.getName())) {
                                filter.add(user);
                                continue;
                            } else if((value & ProtoConstants.DisableSearchMask.DisableSearchNameMask) > 0) {
                                filter.add(user);
                                continue;
                            }
                        }
                    } else if(searchType == SearchUserType_Name_Mobile_UserId) {
                        if(keyword.equals(user.getMobile())) {
                            if(keyword.equals(user.getName()) && keyword.equals(user.getUid())) {
                                if((value & ProtoConstants.DisableSearchMask.DisableSearchNameMask) > 0 && (value & ProtoConstants.DisableSearchMask.DisableSearchUserIdMask) > 0) {
                                    filter.add(user);
                                    continue;
                                }
                            } else if(keyword.equals(user.getName())) {
                                if((value & ProtoConstants.DisableSearchMask.DisableSearchNameMask) > 0) {
                                    filter.add(user);
                                    continue;
                                }
                            } else if(keyword.equals(user.getUid())) {
                                if((value & ProtoConstants.DisableSearchMask.DisableSearchUserIdMask) > 0) {
                                    filter.add(user);
                                    continue;
                                }
                            } else {
                                filter.add(user);
                                continue;
                            }
                        }
                    }
                }

                if((value & ProtoConstants.DisableSearchMask.DisableSearchUserIdMask) > 0) {
                    if (searchType == SearchUserType_UserId) {
                        filter.add(user);
                        continue;
                    } else if(searchType == SearchUserType_Name_Mobile_UserId) {
                        if(keyword.equals(user.getUid())) {
                            if(keyword.equals(user.getName()) && keyword.equals(user.getMobile())) {
                                if((value & ProtoConstants.DisableSearchMask.DisableSearchNameMask) > 0 && (value & ProtoConstants.DisableSearchMask.DisableSearchMobileMask) > 0) {
                                    filter.add(user);
                                    continue;
                                }
                            } else if(keyword.equals(user.getName())) {
                                if((value & ProtoConstants.DisableSearchMask.DisableSearchNameMask) > 0) {
                                    filter.add(user);
                                    continue;
                                }
                            } else if(keyword.equals(user.getMobile())) {
                                if((value & ProtoConstants.DisableSearchMask.DisableSearchMobileMask) > 0) {
                                    filter.add(user);
                                    continue;
                                }
                            } else {
                                filter.add(user);
                                continue;
                            }
                        }
                    }
                }
            }
        }
        out.removeAll(filter);

        return out;
    }

    List<WFCMessage.User> searchUserByDisplayName(String keyword, int userType, int page, List<WFCMessage.User> nameOrIdMatched) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        ArrayList<WFCMessage.User> out = new ArrayList<>();

        try {
            connection = DBUtil.getConnection();
            String sql = "select u._uid, u._name, u._display_name, u._portrait, u._mobile, u._gender, u._email, u._address, u._company, u._social, u._extra, u._dt, u._type " +
                " from t_user u left join (select _uid, _value from t_user_setting where _scope = 27) s on u._uid = s._uid " +
                " where u._display_name like ? ESCAPE '!' ";

            if(userType == UserSearchUserType_ONLY_USER) {
                sql += " and u._type <> 2 ";
                sql += " and u._type <> 1 ";
            } else if(userType == UserSearchUserType_ONLY_ROBOT) {
                sql += " and u._type = 1 ";
            } else {
                sql += " and u._type <> 2 ";
            }
            sql += " and u._deleted = 0 ";

            if(nameOrIdMatched.size() == 1) {
                sql += " and u._uid <> ? and u._name <> ?";
            } else if(nameOrIdMatched.size() == 2) {
                sql += " and u._uid <> ? and u._uid <>? and u._name <> ? and u._name <>? ";
            }
            sql += " and (s._value is null or (s._value <> 1 and s._value <> 3 and s._value <> 5 and s._value <> 7)) limit ";

            if(page == 0) {
                sql += (20 - nameOrIdMatched.size());
            } else {
                sql += 20;
            }

            if (page > 0) {
                sql += " offset " + (page * 20 - nameOrIdMatched.size());
            }

            keyword = keyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");

            statement = connection.prepareStatement(sql);
            statement.setString(1, "%" + keyword + "%");
            if(nameOrIdMatched.size() == 1) {
                statement.setString(2, nameOrIdMatched.toArray(new WFCMessage.User[0])[0].getUid());
                statement.setString(3, nameOrIdMatched.toArray(new WFCMessage.User[0])[0].getUid());
            } else if(nameOrIdMatched.size() == 2) {
                statement.setString(2, nameOrIdMatched.toArray(new WFCMessage.User[0])[0].getUid());
                statement.setString(3, nameOrIdMatched.toArray(new WFCMessage.User[0])[1].getUid());
                statement.setString(4, nameOrIdMatched.toArray(new WFCMessage.User[0])[0].getUid());
                statement.setString(5, nameOrIdMatched.toArray(new WFCMessage.User[0])[1].getUid());
            }

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.User.Builder builder = WFCMessage.User.newBuilder();
                int index = 1;

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

                int type = rs.getInt(index++);
                builder.setType(type);

                WFCMessage.User user = builder.build();

                out.add(user);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
    }

    List<WFCMessage.User> searchUserFromDB(String keyword, int searchType, int userType, int page) {
        List<WFCMessage.User> nameOrIdMatched = searchUserByNameMobileUserId(keyword, searchType, userType);
        List<WFCMessage.User> out = new ArrayList<>();
        if(page == 0) {
            out.addAll(nameOrIdMatched);
        }

        if(searchType == SearchUserType_General || searchType == SearchUserType_Name_Mobile_DisplayName) {
            List<WFCMessage.User> general = searchUserByDisplayName(keyword, userType, page, nameOrIdMatched);
            general.removeAll(nameOrIdMatched);
            out.addAll(general);
        }
        return out;
    }

    /**
     * 根据用户名称模糊搜索用户，同时匹配用户名和昵称，只返回未删除的普通用户和机器人
     * @param keyword 搜索关键词
     * @param page 页码，从0开始
     * @param count 每页数量
     * @return 匹配的用户列表
     */
    List<WFCMessage.User> searchUserByName(String keyword, int page, int count) {
        List<WFCMessage.User> out = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            connection = DBUtil.getConnection();
            String sql = "select _uid, _name, _display_name, _portrait, _mobile, _gender, _email, _address, " +
                "_company, _social, _extra, _dt, _type from t_user" +
                " where (_name like ? ESCAPE '!' or _display_name like ? ESCAPE '!')" +
                " and _deleted = 0 and _type <> 2" +
                " order by _dt desc, _uid limit ? offset ?";

            statement = connection.prepareStatement(sql);
            String pattern = "%" + escapeLikeKeyword(keyword) + "%";
            int index = 1;
            statement.setString(index++, pattern);
            statement.setString(index++, pattern);
            statement.setInt(index++, count);
            statement.setLong(index++, (long) page * count);

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.User.Builder builder = WFCMessage.User.newBuilder();
                int columnIndex = 1;

                String value = rs.getString(columnIndex++);
                builder.setUid(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setName(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setDisplayName(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setPortrait(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setMobile(value == null ? "" : value);

                builder.setGender(rs.getInt(columnIndex++));

                value = rs.getString(columnIndex++);
                builder.setEmail(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setAddress(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setCompany(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setSocial(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setExtra(value == null ? "" : value);

                builder.setUpdateDt(rs.getLong(columnIndex++));
                builder.setType(rs.getInt(columnIndex));

                out.add(builder.build());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
    }

    /**
     * 转义 like 语句中的特殊字符，转义符为 '!'
     */
    private static String escapeLikeKeyword(String keyword) {
        return keyword.replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
            .replace("[", "![");
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
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return 0;
    }

    synchronized Collection<WFCMessage.GroupMember>  reloadGroupMemberFromDB(HazelcastInstance hzInstance, String groupId) {
        MultiMap<String, WFCMessage.GroupMember> groupMembers = hzInstance.getMultiMap(MemoryMessagesStore.GROUP_MEMBERS);
        if (groupMembers.get(groupId).size() > 0) {
            return groupMembers.get(groupId);
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_mid`" +
                ", `_alias`" +
                ", `_type`" +
                ", `_dt`, `_create_dt`, `_extra` from t_group_member where _gid = ?";
            statement = connection.prepareStatement(sql);

            statement.setString(1, groupId);
            int index;


            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.GroupMember.Builder builder = WFCMessage.GroupMember.newBuilder();
                index = 1;

                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setMemberId(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setAlias(value);

                int intvalue = rs.getInt(index++);
                builder.setType(intvalue);


                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);

                longValue = rs.getLong(index++);
                builder.setCreateDt(longValue);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setExtra(value);

                WFCMessage.GroupMember member = builder.build();
                groupMembers.put(groupId, member);
            }
            return groupMembers.get(groupId);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return new ArrayList<>();
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
            String sql = "select `_uid`, `_friend_uid`, `_alias`, `_state`, `_blacked`, `_dt`, `_extra` from t_friend";
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

                intvalue = rs.getInt(index++);
                builder.setBlacked(intvalue);

                long longvalue = rs.getLong(index++);
                builder.setTimestamp(longvalue);

                builder.setExtra(rs.getString(index++));

                friendsMap.put(builder.getUserId(), builder);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
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
            String sql = "select `_uid`, `_friend_uid`, `_reason`, `_status`, `_dt`, `_from_read_status`, `_to_read_status`, `_extra` from t_friend_request";
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

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setExtra(value);

                WFCMessage.FriendRequest request = builder.build();
                requestMap.put(request.getFromUid(), request);
                requestMap.put(request.getToUid(), request);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
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
                String table = MessageShardingUtil.getMessageTable(message.getMessageId());
                String sql;
                if (disableRemoteMessageSearch) {
                    sql = "insert into " + table +
                        " (`_mid`, `_from`, `_type`, `_target`, `_line`, `_data`, `_dt`, `_content_type`, `_to`) values(?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                        " ON DUPLICATE KEY UPDATE " +
                        "`_data` = ?," +
                        "`_dt` = ?," +
                        "`_content_type` = ?";
                } else {
                    sql = "insert into " + table +
                        " (`_mid`, `_from`, `_type`, `_target`, `_line`, `_data`, `_searchable_key`, `_dt`, `_content_type`, `_to`) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                        " ON DUPLICATE KEY UPDATE " +
                        "`_data` = ?," +
                        "`_searchable_key` = ?," +
                        "`_dt` = ?," +
                        "`_content_type` = ?";
                }

                String searchableContent = message.getContent().getSearchableContent() == null ? "" : message.getContent().getSearchableContent();

                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setLong(index++, message.getMessageId());
                statement.setString(index++, message.getFromUser());
                statement.setInt(index++, message.getConversation().getType());
                statement.setString(index++, message.getConversation().getTarget());
                statement.setInt(index++, message.getConversation().getLine());
                Blob blob = connection.createBlob();
                blob.setBytes(1, encryptMessageContent(message.getContent().toByteArray(), false));
                statement.setBlob(index++, blob);
                if (!disableRemoteMessageSearch) {
                    statement.setString(index++, searchableContent);
                }
                statement.setTimestamp(index++, new Timestamp(message.getServerTimestamp()));
                statement.setInt(index++, message.getContent().getType());
                String to = message.getToUser();
                if (StringUtil.isNullOrEmpty(message.getToUser())) {
                    if (message.getToList().size() > 0) {
                        to = message.getToList().get(0);
                    }
                }
                statement.setString(index++, to);

                statement.setBlob(index++, blob);
                if (!disableRemoteMessageSearch) {
                    statement.setString(index++, searchableContent);
                }
                statement.setTimestamp(index++, new Timestamp(message.getServerTimestamp()));
                statement.setInt(index++, message.getContent().getType());

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    byte[] encryptMessageContent(byte[] in, boolean force) {
        if(in != null && (encryptMessage || force)) {
            for (int i = 0; i < in.length; i++) {
                in[i] ^= 0xBD;
            }
        }
        return in;
    }

    void persistSensitiveMessage(final WFCMessage.Message message) {
        if(message.getContent().getPersistFlag() == Transparent) {
            return;
        }

        mScheduler.execute(()-> {
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into " + "t_sensitive_messages" +
                    " (`_mid`, `_from`, `_type`, `_target`, `_line`, `_data`, `_searchable_key`, `_dt`, `_content_type`) values(?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_data` = ?," +
                    "`_searchable_key` = ?," +
                    "`_dt` = ?," +
                    "`_content_type` = ?";

                String searchableContent = message.getContent().getSearchableContent() == null ? "" : message.getContent().getSearchableContent();

                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setLong(index++, message.getMessageId());
                statement.setString(index++, message.getFromUser());
                statement.setInt(index++, message.getConversation().getType());
                statement.setString(index++, message.getConversation().getTarget());
                statement.setInt(index++, message.getConversation().getLine());
                Blob blob = connection.createBlob();
                blob.setBytes(1, encryptMessageContent(message.getContent().toByteArray(), false));
                statement.setBlob(index++, blob);
                statement.setString(index++, searchableContent);
                statement.setTimestamp(index++, new Timestamp(message.getServerTimestamp()));
                statement.setInt(index++, message.getContent().getType());

                statement.setBlob(index++, blob);
                statement.setString(index++, searchableContent);
                statement.setTimestamp(index++, new Timestamp(message.getServerTimestamp()));
                statement.setInt(index++, message.getContent().getType());

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
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

                        WFCMessage.MessageContent messageContent = WFCMessage.MessageContent.parseFrom(encryptMessageContent(toByteArray(blob.getBinaryStream()), false));
                        builder.setContent(messageContent);
                        builder.setServerTimestamp(resultSet.getTimestamp(index++).getTime());
                        WFCMessage.Message message = builder.build();
                        out.put(message.getMessageId(),new MessageBundle(message.getMessageId(), message.getFromUser(), null, message));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e, RDBS_Exception);
                } catch (IOException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e, RDBS_Exception);
                } finally {
                    try {
                        if (resultSet!=null) {
                            resultSet.close();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Utility.printExecption(LOG, e, RDBS_Exception);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, null);
        }

        return out;
    }

    public byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    MessageBundle getMessage(long messageId) {
        String sql = "select  `_from`, `_type`, `_target`, `_line`, `_data`, `_dt` from " + MessageShardingUtil.getMessageTable(messageId) +" where _mid = ? order by `_dt` DESC limit 1";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setLong(1, messageId);
            resultSet = statement.executeQuery();
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

                WFCMessage.MessageContent messageContent = WFCMessage.MessageContent.parseFrom(encryptMessageContent(toByteArray(blob.getBinaryStream()), false));
                builder.setContent(messageContent);
                builder.setServerTimestamp(resultSet.getTimestamp(index++).getTime());
                WFCMessage.Message message = builder.build();
                return new MessageBundle(messageId, message.getFromUser(), null, message);
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, resultSet);
        }
        return null;
    }

    void deleteMessage(long messageId) {
        String sql = "delete from " + MessageShardingUtil.getMessageTable(messageId) + " where _mid = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setLong(1, messageId);

            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    List<WFCMessage.Message> loadRemoteMessages(String user, WFCMessage.Conversation conversation, long beforeUid, int count, Collection<Integer> contentTypes, String channelOwner) {
        List<WFCMessage.Message> messages = new ArrayList<>();
        long[] before = new long[1];
        before[0] = beforeUid;
        boolean hasMore = loadRemoteMessagesFromTable(user, conversation, before, count, MessageShardingUtil.getMessageTable(beforeUid), messages, contentTypes, channelOwner);
        while (messages.size() < count && hasMore) {
            hasMore = loadRemoteMessagesFromTable(user, conversation, before, count - messages.size(), MessageShardingUtil.getMessageTable(beforeUid), messages, contentTypes, channelOwner);
        }

        int month = 0;
        while (messages.size() < count && !DBUtil.IsEmbedDB && month++ < 24) {
            String nexTable = MessageShardingUtil.getMessageTable(beforeUid, -month);

            int size = messages.size();
            hasMore = true;
            while (size == messages.size() && hasMore) {
                hasMore = loadRemoteMessagesFromTable(user, conversation, before, count - messages.size(), nexTable, messages, contentTypes, channelOwner);
            }

            if (size < messages.size()) {
                break;
            }
        }

        return messages;
    }

    boolean loadRemoteMessagesFromTable(String user, WFCMessage.Conversation conversation, long[] before, int count, String table, List<WFCMessage.Message> messages, Collection<Integer> contentTypes, String channelOwner) {
        long beforeUid = before[0];
        String sql = "select `_mid`, `_from`, `_type`, `_target`, `_line`, `_data`, `_dt`, `_to` from " + table +" where";
        if (conversation.getType() == ProtoConstants.ConversationType.ConversationType_Private) {
            sql += " _type = ? and _line = ? and _mid < ? and ((_target = ?  and _from = ?) or (_target = ?  and _from = ?)) and (_to = '' or _to = ?)";
        } else if (conversation.getType() == ProtoConstants.ConversationType.ConversationType_Channel && !user.equals(channelOwner)) {
            sql += " _type = ? and _line = ? and _mid < ? and _target = ? and ((_from = ? and (_to = '' or _to = ?)) or (_from = ?))";
        } else {
            sql += " _type = ? and _line = ? and _mid < ? and _target = ?";
        }

        if(contentTypes != null && !contentTypes.isEmpty()) {
            sql += " and _content_type in (";
            boolean first = true;
            for (int i:contentTypes) {
                if(first) {
                    first = false;
                } else {
                    sql += ",";
                }
                sql += i;
            }
            sql += ")";
        }

        sql += " order by `_mid` DESC limit ?";

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
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
                statement.setString(index++, user);
            } else if (conversation.getType() == ProtoConstants.ConversationType.ConversationType_Channel && !user.equals(channelOwner)) {
                statement.setString(index++, channelOwner);
                statement.setString(index++, user);
                statement.setString(index++, user);
            }

            statement.setInt(index++, count);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                count--;
                WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder();
                index = 1;
                builder.setMessageId(resultSet.getLong(index++));
                before[0] = builder.getMessageId();

                builder.setFromUser(resultSet.getString(index++));
                WFCMessage.Conversation.Builder cb = WFCMessage.Conversation.newBuilder();
                cb.setType(resultSet.getInt(index++));
                cb.setTarget(resultSet.getString(index++));
                cb.setLine(resultSet.getInt(index++));
                builder.setConversation(cb.build());
                Blob blob = resultSet.getBlob(index++);

                WFCMessage.MessageContent messageContent = WFCMessage.MessageContent.parseFrom(encryptMessageContent(toByteArray(blob.getBinaryStream()), false));

                builder.setContent(messageContent);

                builder.setServerTimestamp(resultSet.getTimestamp(index++).getTime());
                String to = resultSet.getString(index++);
                if (!StringUtil.isNullOrEmpty(to)) {
                    if (to.equals(user) || builder.getFromUser().equals(user)) {
                        builder.setToUser(to);
                    } else {
                        continue;
                    }
                }
                WFCMessage.Message message = builder.build();
                boolean expired = false;
                if (message.getContent().getExpireDuration() > 0) {
                    if (System.currentTimeMillis() > message.getServerTimestamp() + message.getContent().getExpireDuration()*1000) {
                        expired = true;
                    }
                }

                if (!expired) {
                    boolean duplicated = false;
                    for (WFCMessage.Message msg : messages) {
                        if(message.getMessageId() == msg.getMessageId()) {
                            duplicated = true;
                            break;
                        }
                    }
                    if(!duplicated) {
                        messages.add(message);
                    }
                }
            }

            if (count == 0) {
                return true;
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, resultSet);
        }
        return false;
    }

    void persistUserMessage(final String userId, final  String sender, final long messageId, final long messageSeq, int type, String target, int line, boolean directing, final int messageContentType) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();

                String sql = "insert into " + getUserMessageTable(userId) + " (`_mid`, `_uid`, `_seq`, `_type`, `_target`, `_line`, `_directing`, `_cont_type`) values(?, ?, ?, ?, ?, ?, ?, ?)";

                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setLong(index++, messageId);
                statement.setString(index++, userId);
                statement.setLong(index++, messageSeq);
                statement.setInt(index++, type);
                statement.setString(index++, getPrivateChatUserMessageTarget(type, target, sender));
                statement.setInt(index++, line);
                statement.setInt(index++, directing ? 1 :0);
                statement.setInt(index++, messageContentType);
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    String getPrivateChatUserMessageTarget(int conversationType, String target, String userId) {
        if(conversationType == ProtoConstants.ConversationType.ConversationType_Private) {
            return target.compareTo(userId) > 0 ? (userId + "|" + target) : (target + "|" + userId);
        } else {
            return target;
        }
    }

    void clearUserMessage(final String userId) {
        mScheduler.execute(()->{
            String tableName = getUserMessageTable(userId);
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();

                String sql = "delete from " + tableName + " where _uid = ?";
                statement = connection.prepareStatement(sql);
                statement.setString(1, userId);
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);

                try {
                    if (statement!=null) {
                        statement.close();
                    }
                    statement = null;
                } catch (SQLException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e, RDBS_Exception);
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });

    }

    void removeFavGroup(final String groupId, final List<String> memberIds) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();

            StringBuilder sb = new StringBuilder("update t_user_setting set _value = ?, _dt = ? where _uid in (");
            for (int i = 0; i < memberIds.size(); i++) {
                sb.append("?");
                if (i != memberIds.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append(")");

            sb.append(" and _scope in (5,6,26) and _key = ?");

            statement = connection.prepareStatement(sb.toString());
            int index = 1;
            statement.setString(index++, "0");
            statement.setLong(index++, System.currentTimeMillis());
            for (int i = 0; i < memberIds.size(); i++) {
                statement.setString(index++, memberIds.get(i));
            }
            statement.setString(index++, groupId);

            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
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
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void clearUserSetting(final String userId) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();

                String sql = "delete from t_user_setting where `_uid` = ?";

                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setString(index++, userId);

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
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
            if (out.isEmpty()) {
                WFCMessage.UserSettingEntry.Builder builder = WFCMessage.UserSettingEntry.newBuilder().setScope(999).setKey("").setValue("").setUpdateDt(0);
                out.add(builder.build());
            }
            return out;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }


    void removeGroupUserSettings(String groupId, List<String> users) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();

            StringBuilder sb = new StringBuilder("delete from t_user_setting where _scope in (1,3,5,6,7,19) and _uid in (");
            for (int i = 0; i < users.size(); i++) {
                sb.append("?");
                if (i != users.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append(") and _key like ?");

            statement = connection.prepareStatement(sb.toString());
            int index = 1;
            for (String userId:users) {
                statement.setString(index++, userId);
            }
            statement.setString(index++, "1-_-" + groupId);

            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    void persistGroupInfo(final WFCMessage.GroupInfo groupInfo) {
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
                ", `_mute`" +
                ", `_join_type`" +
                ", `_private_chat`" +
                ", `_searchable`" +
                ", `_deleted`" +
                ", `_member_dt`) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE " +
                "`_name` = ?," +
                "`_portrait` = ?," +
                "`_owner` = ?," +
                "`_type` = ?," +
                "`_extra` = ?," +
                "`_dt` = ?," +
                "`_mute` = ?" +
                ", `_join_type` = ?" +
                ", `_private_chat` = ?" +
                ", `_searchable` = ?" +
                ", `_deleted` = ?" +
                ", `_member_dt` = ?";


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
            statement.setInt(index++, groupInfo.getMute());
            statement.setInt(index++, groupInfo.getJoinType());
            statement.setInt(index++, groupInfo.getPrivateChat());
            statement.setInt(index++, groupInfo.getSearchable());
            statement.setInt(index++, groupInfo.getDeleted());
            statement.setLong(index++, groupInfo.getMemberUpdateDt() == 0 ? System.currentTimeMillis() : groupInfo.getUpdateDt());

            statement.setString(index++, groupInfo.getName());
            statement.setString(index++, groupInfo.getPortrait());
            statement.setString(index++, groupInfo.getOwner());
            statement.setInt(index++, groupInfo.getType());
            statement.setString(index++, groupInfo.getExtra());
            statement.setLong(index++, groupInfo.getUpdateDt() == 0 ? System.currentTimeMillis() : groupInfo.getUpdateDt());
            statement.setInt(index++, groupInfo.getMute());
            statement.setInt(index++, groupInfo.getJoinType());
            statement.setInt(index++, groupInfo.getPrivateChat());
            statement.setInt(index++, groupInfo.getSearchable());
            statement.setInt(index++, groupInfo.getDeleted());
            statement.setLong(index++, groupInfo.getMemberUpdateDt() == 0 ? System.currentTimeMillis() : groupInfo.getUpdateDt());
            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    List<MemorySessionStore.Session> getUserActivedSessions(String uid) {
        String sql = "select  `_cid`, `_package_name`,`_token`,`_voip_token`,`_secret`,`_db_secret`,`_platform`,`_push_type`,`_device_name`,`_device_version`,`_phone_name`,`_language`,`_carrier_name`, `_dt` from t_user_session where `_uid` = ? and `_deleted` = 0";
        Connection connection = null;
        PreparedStatement statement = null;
        List<MemorySessionStore.Session> result = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, uid);

            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int index = 1;
                String cid = resultSet.getString(index++);

                ClientSession clientSession = new ClientSession(cid, Server.getServer().getStore().sessionsStore());
                MemorySessionStore.Session session = new MemorySessionStore.Session(uid, cid, clientSession);

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
                session.setUpdateDt(resultSet.getLong(index++));
                result.add(session);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, resultSet);
        }
        return result;
    }

    void clearUserSessions(String uid) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "delete from t_user_session where `_uid`=?";
            statement = connection.prepareStatement(sql);
            int index = 1;
            statement.setString(index++, uid);

            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    MemorySessionStore.Session getSession(String uid, String clientId, ClientSession clientSession) {
        String sql = "select  `_package_name`,`_token`,`_voip_token`,`_secret`,`_db_secret`,`_platform`,`_push_type`,`_device_name`,`_device_version`,`_phone_name`,`_language`,`_carrier_name`, `_dt`, `_deleted` from t_user_session where `_uid` = ? and `_cid` = ? limit 1";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, uid);
            statement.setString(2, clientId);
            resultSet = statement.executeQuery();
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
                session.setUpdateDt(resultSet.getLong(index++));
                session.setDeleted(resultSet.getInt(index));
                return session;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, resultSet);
        }
        return null;
    }

    void clearOtherSessionToken(String cid, String token, int pushType, boolean voipToken) {
        if (voipToken) {
            return;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();

            String sql = "update t_user_session set `_token` = ?, `_dt` = ? where `_token` = ? and `_push_type` = ? and `_cid` <> ?";

            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, "");
            statement.setLong(index++, System.currentTimeMillis());
            statement.setString(index++, token);
            statement.setInt(index++, pushType);
            statement.setString(index++, cid);

            int c = statement.executeUpdate();
            LOG.info("Update rows {}", c);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }


    void updateSessionToken(String uid, String cid, String token, int pushType, boolean voipToken) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;

            clearOtherSessionToken(cid, token, pushType, voipToken);
            try {
                connection = DBUtil.getConnection();

                String sql ;

                if (voipToken) {
                    sql = "update t_user_session set `_voip_token` = ?, `_dt` = ? where `_uid` = ? and `_cid` = ?";
                } else {
                    sql = "update t_user_session set `_token` = ?, `_push_type` = ?, `_dt` = ? where `_uid` = ? and `_cid` = ?";
                }
                statement = connection.prepareStatement(sql);

                int index = 1;
                statement.setString(index++, token);
                if (!voipToken) {
                    statement.setInt(index++, pushType);
                }
                statement.setLong(index++, System.currentTimeMillis());
                statement.setString(index++, uid);
                statement.setString(index++, cid);

                int c = statement.executeUpdate();
                LOG.info("Update rows {}", c);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void updateSessionDeleted(String uid, String cid, int deleted) {
            Connection connection = null;
            PreparedStatement statement = null;

            try {
                connection = DBUtil.getConnection();

                String sql = "update t_user_session set `_deleted` = ? where `_uid` = ? and `_cid` = ?";

                statement = connection.prepareStatement(sql);

                int index = 1;
                statement.setInt(index++, deleted);
                statement.setString(index++, uid);
                statement.setString(index++, cid);

                int c = statement.executeUpdate();
                LOG.info("Update rows {}", c);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
    }

    void updateSessionPlatform(String uid, String cid, int platform) {
            Connection connection = null;
            PreparedStatement statement = null;

            try {
                connection = DBUtil.getConnection();

                String sql = "update t_user_session set `_platform` = ? where `_uid` = ? and `_cid` = ?";

                statement = connection.prepareStatement(sql);

                int index = 1;
                statement.setInt(index++, platform);
                statement.setString(index++, uid);
                statement.setString(index++, cid);

                int c = statement.executeUpdate();
                LOG.info("Update rows {}", c);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
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
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void updateSessionIp(String uid, String cid, String ip) {
        mScheduler.execute(()->{
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "update t_user_session set _ip = ?  where `_uid` = ? and `_cid` = ?";
                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setString(index++, ip);
                statement.setString(index++, uid);
                statement.setString(index++, cid);

                int c = statement.executeUpdate();
                LOG.info("Update rows {}", c);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }

    void clearMultiEndpoint(String uid, String clientId, int platform) {
        LOG.info("clearMultiEndpoint {}, {}", uid, clientId);
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql;
            if (platform == ProtoConstants.Platform.Platform_Windows || platform == ProtoConstants.Platform.Platform_OSX || platform == ProtoConstants.Platform.Platform_LINUX || platform == ProtoConstants.Platform.Platform_HarmonyPC) {
                sql = "update t_user_session set `_deleted` = ?, `_token` = ?, `_voip_token` = ?, `_dt` = ?  where `_uid`=? and (`_platform` = ? or `_platform` = ? or `_platform` = ? or `_platform` = ?)  and `_cid` <> ? and `_deleted` = 0";
            } else if(platform == ProtoConstants.Platform.Platform_iOS || platform == ProtoConstants.Platform.Platform_Android || platform == ProtoConstants.Platform.Platform_Harmony) {
                sql = "update t_user_session set `_deleted` = ?, `_token` = ?, `_voip_token` = ?, `_dt` = ?  where `_uid`=? and (`_platform` = ? or `_platform` = ? or `_platform` = ?)  and `_cid` <> ? and `_deleted` = 0";
            } else if(platform == ProtoConstants.Platform.Platform_iPad || platform == ProtoConstants.Platform.Platform_APad || platform == ProtoConstants.Platform.Platform_HarmonyPad) {
                sql = "update t_user_session set `_deleted` = ?, `_token` = ?, `_voip_token` = ?, `_dt` = ?  where `_uid`=? and (`_platform` = ? or `_platform` = ? or `_platform` = ?)  and `_cid` <> ? and `_deleted` = 0";
            } else if(platform == ProtoConstants.Platform.Platform_AndroidWearable || platform == ProtoConstants.Platform.Platform_HarmonyWearable) {
                sql = "update t_user_session set `_deleted` = ?, `_token` = ?, `_voip_token` = ?, `_dt` = ?  where `_uid`=? and (`_platform` = ? or `_platform` = ?)  and `_cid` <> ? and `_deleted` = 0";
            } else if(platform == ProtoConstants.Platform.Platform_AndroidTV || platform == ProtoConstants.Platform.Platform_AppleTV || platform == ProtoConstants.Platform.Platform_HarmonyTV) {
                sql = "update t_user_session set `_deleted` = ?, `_token` = ?, `_voip_token` = ?, `_dt` = ?  where `_uid`=? and (`_platform` = ? or `_platform` = ? or `_platform` = ?)  and `_cid` <> ? and `_deleted` = 0";
            } else {
                sql = "update t_user_session set `_deleted` = ?, `_token` = ?, `_voip_token` = ?, `_dt` = ?  where `_uid`=? and `_platform` = ? and `_cid` <> ? and `_deleted` = 0";
            }

            statement = connection.prepareStatement(sql);
            int index = 1;

            statement.setInt(index++, 1);
            statement.setString(index++, "");
            statement.setString(index++, "");
            statement.setLong(index++, System.currentTimeMillis());

            statement.setString(index++, uid);

            if (platform == ProtoConstants.Platform.Platform_Windows || platform == ProtoConstants.Platform.Platform_OSX || platform == ProtoConstants.Platform.Platform_LINUX || platform == ProtoConstants.Platform.Platform_HarmonyPC) {
                statement.setInt(index++, ProtoConstants.Platform.Platform_Windows);
                statement.setInt(index++, ProtoConstants.Platform.Platform_OSX);
                statement.setInt(index++, ProtoConstants.Platform.Platform_LINUX);
                statement.setInt(index++, ProtoConstants.Platform.Platform_HarmonyPC);
            } else if(platform == ProtoConstants.Platform.Platform_iOS || platform == ProtoConstants.Platform.Platform_Android || platform == ProtoConstants.Platform.Platform_Harmony) {
                statement.setInt(index++, ProtoConstants.Platform.Platform_iOS);
                statement.setInt(index++, ProtoConstants.Platform.Platform_Android);
                statement.setInt(index++, ProtoConstants.Platform.Platform_Harmony);
            } else if(platform == ProtoConstants.Platform.Platform_iPad || platform == ProtoConstants.Platform.Platform_APad || platform == ProtoConstants.Platform.Platform_HarmonyPad) {
                statement.setInt(index++, ProtoConstants.Platform.Platform_iPad);
                statement.setInt(index++, ProtoConstants.Platform.Platform_APad);
                statement.setInt(index++, ProtoConstants.Platform.Platform_HarmonyPad);
            } else if(platform == ProtoConstants.Platform.Platform_AndroidWearable || platform == ProtoConstants.Platform.Platform_HarmonyWearable) {
                statement.setInt(index++, ProtoConstants.Platform.Platform_AndroidWearable);
                statement.setInt(index++, ProtoConstants.Platform.Platform_HarmonyWearable);
            } else if(platform == ProtoConstants.Platform.Platform_AndroidTV || platform == ProtoConstants.Platform.Platform_AppleTV || platform == ProtoConstants.Platform.Platform_HarmonyTV) {
                statement.setInt(index++, ProtoConstants.Platform.Platform_AndroidTV);
                statement.setInt(index++, ProtoConstants.Platform.Platform_AppleTV);
                statement.setInt(index++, ProtoConstants.Platform.Platform_HarmonyTV);
            } else {
                statement.setInt(index++, platform);
            }

            statement.setString(index++, clientId);

            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    void clearMultiUser(String uid, String clientId) {
        long start = System.currentTimeMillis();
        LOG.info("clearMultiUser {}, {}", uid, clientId);
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "update t_user_session set _deleted = ?, _token = ?, _voip_token = ?, _dt = ?  where _cid = ? and _uid <> ? and _deleted = 0";

            statement = connection.prepareStatement(sql);
            int index = 1;
            statement.setInt(index++, 1);
            statement.setString(index++, "");
            statement.setString(index++, "");
            statement.setLong(index++, System.currentTimeMillis());

            statement.setString(index++, clientId);
            statement.setString(index++, uid);
            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    MemorySessionStore.Session createSession(String uid, String clientId, ClientSession clientSession, int platform) {
        Connection connection = null;
        PreparedStatement statement = null;
        LOG.info("Database create session {},{}", uid, clientId);
        try {
            connection = DBUtil.getConnection();
            String sql = "insert into t_user_session  (`_uid`,`_cid`,`_platform`,`_secret`,`_db_secret`, `_dt`) values (?,?,?,?,?,?)";

            statement = connection.prepareStatement(sql);

            int index = 1;

            MemorySessionStore.Session session = new MemorySessionStore.Session(uid, clientId, clientSession);
            session.setPlatform(platform);

            statement.setString(index++, uid);
            statement.setString(index++, clientId);
            statement.setInt(index++, platform);


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
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
        return null;
    }

    Set<String> getUserGroupIds(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select `_gid` from t_group_member where `_mid` = ? and `_type` <> 4";
            statement = connection.prepareStatement(sql);

            statement.setString(1, userId);

            rs = statement.executeQuery();
            Set<String> out = new HashSet<>();
            while (rs.next()) {
                String uid = rs.getString(1);
                out.add(uid);
            }
            return out;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    Set<String> getUserGroupIds(String userId, List<Integer> memberTypes) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select _gid from t_group_member where _mid = ? ";
            if(!memberTypes.isEmpty()) {
                sql += " and _type in (";
                for (int i = 0; i < memberTypes.size(); i++) {
                    if(i > 0) {
                        sql += ",";
                    }
                    sql += memberTypes.get(i);
                }
                sql += ")";
            }
            statement = connection.prepareStatement(sql);

            statement.setString(1, userId);

            rs = statement.executeQuery();
            Set<String> out = new HashSet<>();
            while (rs.next()) {
                String uid = rs.getString(1);
                out.add(uid);
            }
            return out;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }


    Set<String> getCommonGroupIds(String userId1, String userId2) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select t1._gid from t_group_member t1, t_group_member t2 where t1._mid = ? and t2._mid = ? and t1._type <> 4 and t2._type <> 4 and t1._gid = t2._gid";
            statement = connection.prepareStatement(sql);

            statement.setString(1, userId1);
            statement.setString(2, userId2);

            rs = statement.executeQuery();
            Set<String> out = new HashSet<>();
            while (rs.next()) {
                String uid = rs.getString(1);
                out.add(uid);
            }
            return out;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    void updateGroupMemberDt(final String groupId, final long dt) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();

            String sql = "update t_group set `_member_dt` = ?, `_dt` = ? where `_gid` = ?";
            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setLong(index++, dt);
            statement.setLong(index++, dt);
            statement.setString(index++, groupId);

            int c = statement.executeUpdate();
            LOG.info("Update rows {}", c);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    void updateGroupMemberCountDt(final String groupId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();

            String sql = "update t_group set `_member_count` = (select count(*) from t_group_member where `_gid` = ? and `_type` <> 4 limit 1), `_dt` = ?, `_member_dt` = `_member_dt` + 1 where `_gid` = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, groupId);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, groupId);

            int c = statement.executeUpdate();
            LOG.info("Update rows {}", c);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    void persistGroupMember(final String groupId, final List<WFCMessage.GroupMember> memberList, boolean updateCreateTime) {
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                connection.setAutoCommit(false);

                String sql = "insert into t_group_member (`_gid`" +
                    ", `_mid`" +
                    ", `_alias`" +
                    ", `_type`" +
                    ", `_dt`, `_create_dt`, `_extra`) values(?, ?, ?, ?, ?, ?,?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_alias` = ?," +
                    "`_type` = ?," +
                    "`_dt` = ?," +
                    "`_extra` = ?";
                if(updateCreateTime) {
                    sql += ", `_create_dt` = ?";
                }

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
                    statement.setLong(index++, member.getCreateDt());
                    statement.setString(index++, member.getExtra());
                    statement.setString(index++, member.getAlias());
                    statement.setInt(index++, member.getType());
                    statement.setLong(index++, dt);
                    statement.setString(index++, member.getExtra());
                    if(updateCreateTime) {
                        statement.setLong(index++, dt);
                    }
                    int count = statement.executeUpdate();
                    LOG.info("Update rows {}", count);
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                if (connection != null) {
                    try {
                        connection.commit();
                        connection.setAutoCommit(true);
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
                DBUtil.closeDB(connection, statement);
            }
    }

    int removeGroupMember(String groupId, List<String> groupMembers) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            StringBuilder sqlBuilder = new StringBuilder("update t_group_member set `_type` = ?, `_dt` = ? where `_mid` in (");
            for (int i = 0; i < groupMembers.size(); i++) {
                sqlBuilder.append("?");
                if (i != groupMembers.size()-1) {
                    sqlBuilder.append(",");
                }
            }
            sqlBuilder.append(")");
            sqlBuilder.append(" and _gid = ?");
            statement = connection.prepareStatement(sqlBuilder.toString());

            int index = 1;
            long current = System.currentTimeMillis();
            statement.setInt(index++, ProtoConstants.GroupMemberType.GroupMemberType_Removed);
            statement.setLong(index++, current);

            for (String memberId:groupMembers) {
                statement.setString(index++, memberId);
            }
            statement.setString(index++, groupId);
            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
        return 0;
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
                ", `_mute`" +
                ", `_join_type`" +
                ", `_private_chat`" +
                ", `_searchable`" +
                ", _deleted" +
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

                intValue = rs.getInt(index++);
                builder.setMute(intValue);

                intValue = rs.getInt(index++);
                builder.setJoinType(intValue);

                intValue = rs.getInt(index++);
                builder.setPrivateChat(intValue);

                intValue = rs.getInt(index++);
                builder.setSearchable(intValue);

                intValue = rs.getInt(index++);
                builder.setDeleted(intValue);

                return builder.build();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    /**
     * 根据群组名称模糊搜索群组，只返回未删除的群组
     * @param keyword 搜索关键词
     * @param page 页码，从0开始
     * @param count 每页数量
     * @return 匹配的群组列表
     */
    List<WFCMessage.GroupInfo> searchGroupByName(String keyword, int page, int count) {
        List<WFCMessage.GroupInfo> out = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            connection = DBUtil.getConnection();
            String sql = "select _gid, _name, _portrait, _owner, _type, _extra, _dt, _member_count, _member_dt, " +
                "_mute, _join_type, _private_chat, _searchable, _deleted from t_group" +
                " where _name like ? ESCAPE '!' and _deleted = 0" +
                " order by _dt desc, _gid limit ? offset ?";

            statement = connection.prepareStatement(sql);
            statement.setString(1, "%" + escapeLikeKeyword(keyword) + "%");
            statement.setInt(2, count);
            statement.setLong(3, (long) page * count);

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.GroupInfo.Builder builder = WFCMessage.GroupInfo.newBuilder();
                int columnIndex = 1;

                String value = rs.getString(columnIndex++);
                builder.setTargetId(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setName(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setPortrait(value == null ? "" : value);

                value = rs.getString(columnIndex++);
                builder.setOwner(value == null ? "" : value);

                builder.setType(rs.getInt(columnIndex++));

                value = rs.getString(columnIndex++);
                builder.setExtra(value == null ? "" : value);

                builder.setUpdateDt(rs.getLong(columnIndex++));
                builder.setMemberCount(rs.getInt(columnIndex++));
                builder.setMemberUpdateDt(rs.getLong(columnIndex++));
                builder.setMute(rs.getInt(columnIndex++));
                builder.setJoinType(rs.getInt(columnIndex++));
                builder.setPrivateChat(rs.getInt(columnIndex++));
                builder.setSearchable(rs.getInt(columnIndex++));
                builder.setDeleted(rs.getInt(columnIndex));

                out.add(builder.build());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
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
                Utility.printExecption(LOG, e, RDBS_Exception);
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
                Utility.printExecption(LOG, e, RDBS_Exception);
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
            Utility.printExecption(LOG, e, RDBS_Exception);
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
                    String passwdMd5 = Base64.getEncoder().encodeToString(md5.digest(password.getBytes("utf-8")));
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
                Utility.printExecption(LOG, e, RDBS_Exception);
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
                Utility.printExecption(LOG, e, RDBS_Exception);
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
                Utility.printExecption(LOG, e, RDBS_Exception);
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
            Utility.printExecption(LOG, e, RDBS_Exception);
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
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
        });
    }
    
    boolean isUidAndNameConflict(String uid, String name) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        long start = System.currentTimeMillis();
        try {
            connection = DBUtil.getConnection();
            String sql = "select _uid from t_user where _name = ? and _uid <> ? limit 1";
            statement = connection.prepareStatement(sql);
            statement.setString(1, name);
            statement.setString(2, uid);
            rs = statement.executeQuery();
            if (rs.next()) {
                String conflictId = rs.getString(1);
                LOG.error("user {} already have name {} !!!", conflictId, name);
                return true;
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return false;
    }

    void updateUser(final WFCMessage.User user) throws Exception {
        LOG.info("Database update user info {} {}", user.getUid(), user.getUpdateDt());
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
                    ", `_deleted`=?" +
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
                statement.setInt(index++, user.getDeleted());
                statement.setLong(index++, user.getUpdateDt() == 0 ? System.currentTimeMillis() : user.getUpdateDt());

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
                throw new Exception(e.getMessage());
            } finally {
                DBUtil.closeDB(connection, statement);
            }
    }

    void deleteUserStatus(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "delete from t_user_status where _uid = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    void updateUserStatus(String userId, int status) {
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
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
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
                    Utility.printExecption(LOG, e, RDBS_Exception);
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
            Utility.printExecption(LOG, e, RDBS_Exception);
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
                ", `_deleted`" +
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

                int deleted = rs.getInt(index++);
                builder.setDeleted(deleted);

                long longValue = rs.getLong(index++);
                if(longValue <= 0)
                    longValue = 1;
                builder.setUpdateDt(longValue);

                return builder.build();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
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
            Utility.printExecption(LOG, e, RDBS_Exception);
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
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    List<WFCMessage.User> getUserInfosByEmail(String email) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        List<WFCMessage.User> outList = new ArrayList<>();
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
                ", `_type`" +
                ", `_deleted`" +
                ", `_dt` from t_user where `_deleted` = 0 and `_email` = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, email);

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.User.Builder builder = WFCMessage.User.newBuilder();
                int index = 1;
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

                int type = rs.getInt(index++);
                builder.setType(type);

                int deleted = rs.getInt(index++);
                builder.setDeleted(deleted);

                long longValue = rs.getLong(index++);
                if(longValue <= 0)
                    longValue = 1;
                builder.setUpdateDt(longValue);

                outList.add(builder.build());
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return outList;
    }

    List<WFCMessage.User> getUserInfosByMobile(String mobile, boolean includeDeleted) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        List<WFCMessage.User> outList = new ArrayList<>();
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
                ", `_type`" +
                ", `_deleted`" +
                ", `_dt` from t_user where `_mobile` = ?";
            if(!includeDeleted) {
                sql += " and `_deleted` = 0";
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, mobile);

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.User.Builder builder = WFCMessage.User.newBuilder();
                int index = 1;
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

                int type = rs.getInt(index++);
                builder.setType(type);

                int deleted = rs.getInt(index++);
                builder.setDeleted(deleted);

                long longValue = rs.getLong(index++);
                if(longValue <= 0)
                    longValue = 1;
                builder.setUpdateDt(longValue);

                outList.add(builder.build());
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return outList;
    }

    List<WFCMessage.User> getAllUsers(int count, int offset) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        List<WFCMessage.User> outList = new ArrayList<>();
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
                ", `_type`" +
                ", `_deleted`" +
                ", `_dt` from t_user where `_deleted` = 0 order by _createTime limit ? offset ?";
            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setInt(1, count);
            statement.setInt(2, offset);


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

                int type = rs.getInt(index++);
                builder.setType(type);

                int deleted = rs.getInt(index++);
                builder.setDeleted(deleted);

                long longValue = rs.getLong(index++);
                if(longValue <= 0)
                    longValue = 1;
                builder.setUpdateDt(longValue);

                outList.add(builder.build());
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return outList;
    }

    List<WFCMessage.Robot> getAllRobots(int count, int offset) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        List<WFCMessage.Robot> outList = new ArrayList<>();

        try {
            connection = DBUtil.getConnection();
            String sql = "select  `_uid`, `_owner`" +
                ", `_secret`" +
                ", `_callback`" +
                ", `_state`" +
                ", `_extra`" +
                ", `_dt` from t_robot limit ? offset ?";
            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setInt(1, count);
            statement.setInt(2, offset);

            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.Robot.Builder builder = WFCMessage.Robot.newBuilder();
                index = 1;
                String value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setUid(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
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

                outList.add(builder.build());
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }

        return outList;
    }

    List<String> getUserRobotIds(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        List<String> outList = new ArrayList<>();
        try {
            connection = DBUtil.getConnection();
            String sql = "select _uid from t_robot where _owner = ? and _state = 0";
            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(1, userId);

            rs = statement.executeQuery();
            while (rs.next()) {
                outList.add(rs.getString(1));
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return outList;
    }


    Set<String> getAllEnds(boolean fromUserTable) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql;
            if(fromUserTable) {
                sql = "select distinct(`_uid`) from t_user_session";
            } else {
                sql = "select distinct(`_uid`) from t_user where `_deleted` = 0 and `_type` <> 1 and `_type` <> 2";
            }
            statement = connection.prepareStatement(sql);

            rs = statement.executeQuery();
            Set<String> out = new HashSet<>();
            while (rs.next()) {
                String uid = rs.getString(1);
                out.add(uid);
            }
            return out;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
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
            String sql = "select `_friend_uid`, `_alias`, `_state`, `_blacked`, `_dt`, `_extra` from t_friend where `_uid` = ?";
            statement = connection.prepareStatement(sql);


            int index = 1;
            statement.setString(index++, userId);


            rs = statement.executeQuery();
            List<FriendData> out = new ArrayList<>();
            while (rs.next()) {
                String uid = rs.getString(1);
                String alias = rs.getString(2);
                int state = rs.getInt(3);
                int blacked = rs.getInt(4);
                long timestamp = rs.getLong(5);
                String extra = rs.getString(6);

                FriendData data = new FriendData(userId, uid, alias, extra, state, blacked, timestamp);
                out.add(data);
            }
            return out;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }


    void removeUserFriend(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "update t_friend  set `_alias` = '', `_state` = 1, `_blacked` = 0, `_dt` = ?, `_extra` = 0 where `_uid` = ? or `_friend_uid` = ?";

            statement = connection.prepareStatement(sql);
            int index = 1;
            statement.setLong(index++, System.currentTimeMillis());
            statement.setString(index++, userId);
            statement.setString(index++, userId);
            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    void removeUserFriendRequest(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "delete from t_friend_request  where _uid = ? or _friend_uid = ?";

            statement = connection.prepareStatement(sql);
            int index = 1;
            statement.setString(index++, userId);
            statement.setString(index++, userId);
            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    List<WFCMessage.FriendRequest> getPersistFriendRequests(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select  `_uid`" +
                ", `_friend_uid`" +
                ", `_reason`" +
                ", `_status`" +
                ", `_dt`" +
                ", `_from_read_status`" +
                ", `_to_read_status`, `_extra` from t_friend_request where `_uid` = ? UNION ALL " +
                "select   `_uid`" +
                ", `_friend_uid`" +
                ", `_reason`" +
                ", `_status`" +
                ", `_dt`" +
                ", `_from_read_status`" +
                ", `_to_read_status`, `_extra` from t_friend_request where `_friend_uid` = ?";
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

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setExtra(value);

                out.add(builder.build());
            }
            return out;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return null;
    }

    void persistFriendRequestUnreadStatus(String userId, long readDt, long updateDt) {
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
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
    }
    //
    void persistOrUpdateFriendRequest(final WFCMessage.FriendRequest request) {
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into t_friend_request (`_uid`, `_friend_uid`, `_reason`, `_status`, `_dt`, `_from_read_status`, `_to_read_status`, `_extra`) values(?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_reason` = ?," +
                    "`_status` = ?," +
                    "`_dt` = ?," +
                    "`_from_read_status` = ?," +
                    "`_to_read_status` = ?," +
                    "`_extra` = ?";

                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setString(index++, request.getFromUid());
                statement.setString(index++, request.getToUid());
                statement.setString(index++, request.getReason());
                statement.setInt(index++, request.getStatus());
                statement.setLong(index++, request.getUpdateDt());
                statement.setInt(index++, request.getFromReadStatus() ? 1 : 0);
                statement.setInt(index++, request.getToReadStatus() ? 1 : 0);
                statement.setString(index++, request.getExtra());

                statement.setString(index++, request.getReason());
                statement.setInt(index++, request.getStatus());
                statement.setLong(index++, request.getUpdateDt());
                statement.setInt(index++, request.getFromReadStatus() ? 1 : 0);
                statement.setInt(index++, request.getToReadStatus() ? 1 : 0);
                statement.setString(index++, request.getExtra());
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
    }

    void persistOrUpdateFriendData(final FriendData request) {
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = DBUtil.getConnection();
                String sql = "insert into t_friend (`_uid`, `_friend_uid`, `_alias`, `_state`, `_blacked`, `_dt`, `_extra`) values(?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    "`_alias` = ?," +
                    "`_state` = ?," +
                    "`_blacked` = ?," +
                    "`_dt` = ?," +
                    "`_extra` = ?";


                statement = connection.prepareStatement(sql);
                int index = 1;
                statement.setString(index++, request.getUserId());
                statement.setString(index++, request.getFriendUid());
                statement.setString(index++, request.getAlias());
                statement.setInt(index++, request.getState());
                statement.setInt(index++, request.getBlacked());
                statement.setLong(index++, request.getTimestamp());
                statement.setString(index++, request.getExtra());
                statement.setString(index++, request.getAlias());
                statement.setInt(index++, request.getState());
                statement.setInt(index++, request.getBlacked());
                statement.setLong(index++, request.getTimestamp());
                statement.setString(index++, request.getExtra());
                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
    }

    boolean removeGroupInfoFromDB(String groupId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql;
            if(keepGroupInfo) {
                sql = "update t_group set _deleted = 1, _dt = _dt+1, _member_count = 0, _member_dt = _member_dt+1 where _gid = ?";
            } else {
                sql = "delete from t_group where _gid = ?";
            }

            statement = connection.prepareStatement(sql);
            int index = 1;
            statement.setString(index++, groupId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
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
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
        return false;
    }

    void updateChannelInfo(final WFCMessage.ChannelInfo channelInfo) {
        LOG.info("Database update channel info {} {}", channelInfo.getTargetId(), channelInfo.getUpdateDt());
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
                    ", `_menu`" +
                    ", `_dt`) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE `_name`=?" +
                    ", `_portrait`=?" +
                    ", `_owner`=?" +
                    ", `_status`=?" +
                    ", `_desc`=?" +
                    ", `_extra`=?" +
                    ", `_secret`=?" +
                    ", `_callback`=?" +
                    ", `_automatic`=?" +
                    ", `_menu`=?" +
                    ", `_dt`=?";

                statement = connection.prepareStatement(sql);

                WFCMessage.ChannelMenuList.Builder builder = WFCMessage.ChannelMenuList.newBuilder();
                if (!channelInfo.getMenuList().isEmpty()) {
                    for (WFCMessage.ChannelMenu menuBtn:channelInfo.getMenuList()) {
                        builder.addMenu(menuBtn);
                    }
                }
                byte[] menuBytes = builder.build().toByteArray();

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
                statement.setBytes(index++, menuBytes);
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
                statement.setBytes(index++, menuBytes);
                statement.setLong(index++, channelInfo.getUpdateDt() == 0 ? System.currentTimeMillis() : channelInfo.getUpdateDt());

                int count = statement.executeUpdate();
                LOG.info("Update rows {}", count);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                DBUtil.closeDB(connection, statement);
            }
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
                Utility.printExecption(LOG, e, RDBS_Exception);
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
                ", `_menu`" +
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

                try {
                    byte[] bytes = null;
                    Blob blob = rs.getBlob(index++);
                    if (blob != null) {
                        bytes = toByteArray(blob.getBinaryStream());
                    }

                    WFCMessage.ChannelMenuList menuButtonList = WFCMessage.ChannelMenuList.parseFrom(bytes);
                    if (menuButtonList.getMenuCount() > 0) {
                        builder.addAllMenu(menuButtonList.getMenuList());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);

                return builder.build();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
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
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Utility.printExecption(LOG, e, RDBS_Exception);
            } finally {
                if (connection != null) {
                    try {
                        connection.commit();
                        connection.setAutoCommit(true);
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
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

                String value = rs.getString(1);
                if (!StringUtil.isNullOrEmpty(value)) {
                    out.add(value);
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
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
                sql += " where `_name` like ? ESCAPE '!' ";
            } else {
                sql += " where `_name` = ?";
            }

            sql += " and _status <> 64";
            sql += " and _status <> 16";
            sql += " and _status <> 17";
            sql += " and _status <> 24";
            sql += " and _status <> 25";

            sql += " limit 20";


            if (page > 0) {
                sql += " offset " + page * 20;
            }

            keyword = keyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");

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
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
    }

    void updateChannelListener(final String channelId, final String listener, final boolean listen) {
        LOG.info("updateChannelListener channel {}, listener {}, listen {}", channelId, listener, listen);
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
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    List<String> getUserChannels(String userId) {
        List<String> out = new ArrayList<>();

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select _cid from t_channel_listener where _mid = ?";
            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, userId);

            rs = statement.executeQuery();
            while (rs.next()) {
                String value = rs.getString(1);
                if (!StringUtil.isNullOrEmpty(value)) {
                    out.add(value);
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
    }

    void clearChannelListener(final String channelId) {
        LOG.info("Database remove channel {}", channelId);
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "delete from t_channel_listener where _cid=?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, channelId);
            int count = statement.executeUpdate();
            LOG.info("Update rows {}", count);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    void batchInsertChannelListener(String channelId, List<String> listeners, boolean listen) throws SQLException {
        LOG.info("updateChannelListener {}", channelId);
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DBUtil.getConnection();
            int size = 0;
            String sql = listen
                ? "insert into t_channel_listener (_cid, _mid, _dt) values(?, ?, ?)"
                : "delete from t_channel_listener where _cid=? and _mid=?";
            statement = connection.prepareStatement(sql);

            for (String listener : listeners) {
                int index = 1;
                statement.setString(index++, channelId);
                statement.setString(index++, listener);
                if (listen) {
                    statement.setLong(index++, System.currentTimeMillis());
                }
                statement.addBatch();
                if(++size%100 == 0) {
                    statement.executeBatch();
                    statement.clearBatch();
                }
            }
            if(size%100 != 0) {
                statement.executeBatch();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
            throw e;
        } finally {
            DBUtil.closeDB(connection, statement);
        }
    }

    public List<String> findMissingMids(String cid, List<String> midList, boolean contain) {
        if (midList == null || midList.isEmpty()) {
            return new ArrayList<>();
        }

        String tempTable = "temp_check_mids_" + System.currentTimeMillis();
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();

            // Step 1: 创建临时表
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TEMPORARY TABLE " + tempTable +
                    " (_mid VARCHAR(64) NOT NULL PRIMARY KEY)");
            }

            // Step 2: 批量插入
            String insertSql = "INSERT IGNORE INTO " + tempTable + " (_mid) VALUES (?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                int count = 0;
                for (String mid : midList) {
                    ps.setString(1, mid);
                    ps.addBatch();
                    if (++count % 1000 == 0) {
                        ps.executeBatch();
                        ps.clearBatch();
                    }
                }
                if (count % 1000 != 0) {
                    ps.executeBatch();
                }
            }

            // Step 3: 查询
            String querySql = "SELECT t._mid FROM " + tempTable + " t " +
                "WHERE " + (contain ? "" : " NOT ") + " EXISTS (" +
                "    SELECT 1 FROM t_channel_listener c " +
                "    WHERE c._cid = ? AND c._mid = t._mid" +
                ")";

            List<String> result = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                ps.setString(1, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(rs.getString(1));
                    }
                }
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("查询缺失mid失败", e);
        } finally {
            // 显式清理临时表
            if (conn != null) {
                try (Statement st = conn.createStatement()) {
                    st.execute("DROP TEMPORARY TABLE IF EXISTS " + tempTable);
                } catch (SQLException ignored) {
                    // 清理失败不影响主流程
                } finally {
                    try { conn.close(); } catch (SQLException ignored) {}
                }
            }
        }
    }

    List<WFCMessage.ChannelInfo> getChannelInfoList(int count, int offset, boolean withDeleted) {
        List<WFCMessage.ChannelInfo> out = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            connection = DBUtil.getConnection();
            String sql = "select _cid, _name" +
                ", _portrait" +
                ", _owner" +
                ", _secret" +
                ", _status" +
                ", _desc" +
                ", _extra" +
                ", _callback" +
                ", _automatic" +
                ", _menu" +
                ", _dt from t_channel";
            if(!withDeleted) {
                sql += " where _status <> 64 ";
            }
            sql += " order by _dt desc ";

            sql += " limit " + count;

            if (offset > 0) {
                sql += " offset " + offset;
            }

            statement = connection.prepareStatement(sql);
            rs = statement.executeQuery();
            while (rs.next()) {
                WFCMessage.ChannelInfo.Builder builder = WFCMessage.ChannelInfo.newBuilder();
                int index = 1;

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

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setSecret(value);

                int status = rs.getInt(index++);
                builder.setStatus(status);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setDesc(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setExtra(value);

                value = rs.getString(index++);
                value = (value == null ? "" : value);
                builder.setCallback(value);

                int intValue = rs.getInt(index++);
                builder.setAutomatic(intValue);

                try {
                    byte[] bytes = null;
                    Blob blob = rs.getBlob(index++);
                    if (blob != null) {
                        bytes = toByteArray(blob.getBinaryStream());
                    }

                    if (bytes != null) {
                        WFCMessage.ChannelMenuList menuButtonList = WFCMessage.ChannelMenuList.parseFrom(bytes);
                        if (menuButtonList.getMenuCount() > 0) {
                            for (int i = 0; i < menuButtonList.getMenuCount(); i++) {
                                builder.addMenu(menuButtonList.getMenu(i));
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                long longValue = rs.getLong(index++);
                builder.setUpdateDt(longValue);

                out.add(builder.build());
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
    }

    int getChannelTotalCount(boolean withDeleted) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            connection = DBUtil.getConnection();
            String sql = "select count(*) from t_channel";
            if(!withDeleted) {
                sql += " where _status <> 64";
            }

            statement = connection.prepareStatement(sql);
            rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return 0;
    }

    public int getChannelListenerCount(String channelId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select count(*) from  t_channel_listener where _cid = ?";

            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, channelId);

            rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return 0;
    }

    public List<String> getChannelListenerList(String channelId, int count, int offset) {
        List<String> out = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select  _mid from t_channel_listener where _cid = ?";

            if(count > 0) {
                sql += " limit " + count;
            }

            if(offset > 0) {
                sql += " offset " + offset;
            }

            statement = connection.prepareStatement(sql);

            int index = 1;
            statement.setString(index++, channelId);

            rs = statement.executeQuery();
            while (rs.next()) {
                String value = rs.getString(1);
                if (!StringUtil.isNullOrEmpty(value)) {
                    out.add(value);
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return out;
    }


    public Set<String> getSensitiveWord() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        Set<String> out = new HashSet<>();

        try {
            connection = DBUtil.getConnection();
            String sql = "select `_word` from t_sensitiveword order by `id` desc";

            statement = connection.prepareStatement(sql);
            rs = statement.executeQuery();
            while (rs.next()) {
                String value = rs.getString(1);
                out.add(value);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
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
                Utility.printExecption(LOG, e, RDBS_Exception);
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
                Utility.printExecption(LOG, e, RDBS_Exception);
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
