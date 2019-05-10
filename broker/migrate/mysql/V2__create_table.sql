
DROP TABLE IF EXISTS `t_messages`;
CREATE TABLE `t_messages` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_mid` bigint(20) NOT NULL,
  `_from` varchar(64) NOT NULL,
  `_type` tinyint NOT NULL DEFAULT 0,
  `_target` varchar(64) NOT NULL,
  `_line` int(11) NOT NULL DEFAULT 0,
  `_data` BLOB NOT NULL,
  `_searchable_key` TEXT DEFAULT NULL,
  `_dt` DATETIME NOT NULL,
  UNIQUE INDEX `message_uid_index` (`_mid` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_user_messages`;
CREATE TABLE `t_user_messages` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_mid` bigint(20) NOT NULL,
  `_uid` varchar(64) NOT NULL,
  `_seq` bigint(20) NOT NULL,
  `_dt` DATETIME NOT NULL DEFAULT NOW(),
  INDEX `message_seq_uid_index` (`_uid` DESC, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_group`;
CREATE TABLE `t_group` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_gid` varchar(64) NOT NULL,
  `_name` varchar(64) DEFAULT '',
  `_portrait` varchar(1024) DEFAULT '',
  `_owner` varchar(64) DEFAULT '',
  `_type` tinyint NOT NULL DEFAULT 0,
  `_extra` TEXT DEFAULT NULL,
  `_dt` bigint(20) NOT NULL,
  `_member_count` int(11) DEFAULT 0,
  `_member_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `group_gid_index` (`_gid` DESC),
  INDEX `group_name_index` (`_name` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_group_member`;
CREATE TABLE `t_group_member` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_gid` varchar(64) NOT NULL,
  `_mid` varchar(64) DEFAULT '',
  `_alias` varchar(64) DEFAULT '',
  `_type` tinyint DEFAULT 0 COMMENT "0普通成员；1，管理员；2，群主，与Owner相同",
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `user_gid_mid_index` (`_gid`, `_mid`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_name` varchar(64) DEFAULT '',
  `_display_name` varchar(64) DEFAULT '',
  `_gender` int(11) NOT NULL DEFAULT 0,
  `_portrait` varchar(1024) DEFAULT '',
  `_mobile` varchar(64) DEFAULT '',
  `_email` varchar(64) DEFAULT '',
  `_address` varchar(64) DEFAULT '',
  `_company` varchar(64) DEFAULT '',
  `_social` varchar(64) DEFAULT '',
  `_passwd_md5` varchar(64) DEFAULT '',
  `_salt` varchar(64) DEFAULT '',
  `_extra` TEXT DEFAULT NULL,
  `_type` tinyint DEFAULT 0,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `user_uid_index` (`_uid` ASC),
  UNIQUE INDEX `user_name_index` (`_name` ASC),
  INDEX `user_display_name_index` (`_display_name` ASC),
  INDEX `user_mobile_index` (`_mobile` ASC),
  INDEX `user_email_index` (`_email` ASC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_user_status`;
CREATE TABLE `t_user_status` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_status` int(11) NOT NULL DEFAULT 0,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `user_status_uid_index` (`_uid` ASC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_friend_request`;
CREATE TABLE `t_friend_request` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_friend_uid` varchar(64) NOT NULL,
  `_reason` TEXT DEFAULT NULL,
  `_status` tinyint NOT NULL DEFAULT 0,
  `_dt` bigint(20) NOT NULL,
  `_from_read_status` tinyint DEFAULT 0,
  `_to_read_status` tinyint DEFAULT 0,
  UNIQUE INDEX `fr_user_target_index` (`_uid` ASC, `_friend_uid` ASC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_friend`;
CREATE TABLE `t_friend` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_friend_uid` varchar(64) NOT NULL,
  `_state` tinyint DEFAULT 0 COMMENT "0, normal; 1, deleted; 2, blacked",
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `f_user_target_index` (`_uid` ASC, `_friend_uid` ASC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_user_setting`;
CREATE TABLE `t_user_setting` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_scope` int NOT NULL,
  `_key` varchar(64) NOT NULL,
  `_value` varchar(4096) NOT NULL,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `user_setting_index` (`_uid` ASC, `_scope` ASC, `_key` ASC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_id_generator`;
CREATE TABLE `t_id_generator` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci
AUTO_INCREMENT = 3;

DROP TABLE IF EXISTS `t_channel`;
CREATE TABLE `t_channel` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_cid` varchar(64) NOT NULL,
  `_name` varchar(64) DEFAULT '',
  `_portrait` varchar(1024) DEFAULT '',
  `_owner` varchar(64) DEFAULT '',
  `_status` tinyint NOT NULL DEFAULT 0,
  `_desc` TEXT DEFAULT NULL,
  `_secret` varchar(64) DEFAULT '',
  `_callback` varchar(1024) DEFAULT '',
  `_extra` TEXT DEFAULT NULL,
  `_automatic` tinyint NOT NULL DEFAULT 0,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `channel_cid_index` (`_cid` DESC),
  INDEX `channel_name_index` (`_name` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_channel_listener`;
CREATE TABLE `t_channel_listener` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_cid` varchar(64) NOT NULL,
  `_mid` varchar(64) DEFAULT '',
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `channel_cid_mid_index` (`_cid`, `_mid`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_user_session`;
CREATE TABLE `t_user_session` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_cid` varchar(64) NOT NULL,
  `_token` varchar(64) DEFAULT '',
  `_voip_token` varchar(64) DEFAULT '',
  `_secret` varchar(64) NOT NULL,
  `_db_secret` varchar(64) NOT NULL,
  `_platform` tinyint DEFAULT 0,
  `_push_type` tinyint DEFAULT 0,
  `_package_name` varchar(64) DEFAULT '',
  `_device_name` varchar(64) DEFAULT '',
  `_device_version` varchar(64) DEFAULT '',
  `_phone_name` varchar(64) DEFAULT '',
  `_language` varchar(64) DEFAULT '',
  `_carrier_name` varchar(64) DEFAULT '',
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `session_uid_cid_index` (`_cid`, `_uid`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_robot`;
CREATE TABLE `t_robot` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_owner` varchar(64) DEFAULT '',
  `_secret` varchar(64) DEFAULT '',
  `_callback` varchar(1024) DEFAULT '',
  `_state` tinyint DEFAULT 0,
  `_extra` TEXT DEFAULT NULL,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `robot_uid_index` (`_uid` ASC),
  INDEX `robot_owner_index` (`_owner` ASC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_thing`;
CREATE TABLE `t_thing` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_owner` varchar(64) DEFAULT '',
  `_token` varchar(64) DEFAULT '',
  `_state` tinyint DEFAULT 0,
  `_extra` TEXT DEFAULT NULL,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `thing_uid_index` (`_uid` ASC),
  INDEX `thing_owner_index` (`_owner` ASC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_chatroom`;
CREATE TABLE `t_chatroom` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_cid` varchar(64) NOT NULL,
  `_title` varchar(64) DEFAULT '',
  `_portrait` varchar(1024) DEFAULT '',
  `_state` tinyint NOT NULL DEFAULT 0,
  `_desc` TEXT DEFAULT NULL,
  `_extra` TEXT DEFAULT NULL,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `chatroom_cid_index` (`_cid` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_sensitiveword`;
CREATE TABLE `t_sensitiveword` (
  `_word` TEXT DEFAULT NULL
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;
