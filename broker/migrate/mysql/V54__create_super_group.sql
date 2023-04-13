alter table `t_group` add column `_super_group` tinyint NOT NULL DEFAULT 0;

DROP TABLE IF EXISTS `t_group_messages`;
CREATE TABLE `t_group_messages` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_0`;
CREATE TABLE `t_group_messages_0` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message0_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_1`;
CREATE TABLE `t_group_messages_1` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message1_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_2`;
CREATE TABLE `t_group_messages_2` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message2_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_3`;
CREATE TABLE `t_group_messages_3` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message3_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_4`;
CREATE TABLE `t_group_messages_4` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message4_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_5`;
CREATE TABLE `t_group_messages_5` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message5_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_6`;
CREATE TABLE `t_group_messages_6` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message6_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_7`;
CREATE TABLE `t_group_messages_7` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message7_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_8`;
CREATE TABLE `t_group_messages_8` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message8_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_9`;
CREATE TABLE `t_group_messages_9` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message9_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_10`;
CREATE TABLE `t_group_messages_10` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message10_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_11`;
CREATE TABLE `t_group_messages_11` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message11_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_12`;
CREATE TABLE `t_group_messages_12` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message12_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_13`;
CREATE TABLE `t_group_messages_13` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message13_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_14`;
CREATE TABLE `t_group_messages_14` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message14_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_15`;
CREATE TABLE `t_group_messages_15` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message15_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_16`;
CREATE TABLE `t_group_messages_16` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message16_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_17`;
CREATE TABLE `t_group_messages_17` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message17_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_18`;
CREATE TABLE `t_group_messages_18` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message18_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_19`;
CREATE TABLE `t_group_messages_19` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message19_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;



DROP TABLE IF EXISTS `t_group_messages_20`;
CREATE TABLE `t_group_messages_20` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message20_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_21`;
CREATE TABLE `t_group_messages_21` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message21_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_22`;
CREATE TABLE `t_group_messages_22` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message22_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_23`;
CREATE TABLE `t_group_messages_23` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message23_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_24`;
CREATE TABLE `t_group_messages_24` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message24_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_25`;
CREATE TABLE `t_group_messages_25` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message25_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_26`;
CREATE TABLE `t_group_messages_26` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message26_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_27`;
CREATE TABLE `t_group_messages_27` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message27_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_28`;
CREATE TABLE `t_group_messages_28` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message28_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_29`;
CREATE TABLE `t_group_messages_29` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message29_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;


DROP TABLE IF EXISTS `t_group_messages_30`;
CREATE TABLE `t_group_messages_30` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message30_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_31`;
CREATE TABLE `t_group_messages_31` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message31_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_32`;
CREATE TABLE `t_group_messages_32` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message32_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_33`;
CREATE TABLE `t_group_messages_33` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message33_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_34`;
CREATE TABLE `t_group_messages_34` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message34_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_35`;
CREATE TABLE `t_group_messages_35` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message35_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_36`;
CREATE TABLE `t_group_messages_36` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message36_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_37`;
CREATE TABLE `t_group_messages_37` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message37_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_38`;
CREATE TABLE `t_group_messages_38` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message38_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_39`;
CREATE TABLE `t_group_messages_39` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message39_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;


DROP TABLE IF EXISTS `t_group_messages_40`;
CREATE TABLE `t_group_messages_40` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message40_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_41`;
CREATE TABLE `t_group_messages_41` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message41_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_42`;
CREATE TABLE `t_group_messages_42` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message42_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_43`;
CREATE TABLE `t_group_messages_43` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message43_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_44`;
CREATE TABLE `t_group_messages_44` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message44_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_45`;
CREATE TABLE `t_group_messages_45` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message45_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_46`;
CREATE TABLE `t_group_messages_46` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message46_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_47`;
CREATE TABLE `t_group_messages_47` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message47_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_48`;
CREATE TABLE `t_group_messages_48` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message48_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_49`;
CREATE TABLE `t_group_messages_49` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message49_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;


DROP TABLE IF EXISTS `t_group_messages_50`;
CREATE TABLE `t_group_messages_50` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message50_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_51`;
CREATE TABLE `t_group_messages_51` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message51_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_52`;
CREATE TABLE `t_group_messages_52` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message52_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_53`;
CREATE TABLE `t_group_messages_53` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message53_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_54`;
CREATE TABLE `t_group_messages_54` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message54_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_55`;
CREATE TABLE `t_group_messages_55` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message55_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_56`;
CREATE TABLE `t_group_messages_56` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message56_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_57`;
CREATE TABLE `t_group_messages_57` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message57_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_58`;
CREATE TABLE `t_group_messages_58` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message58_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_59`;
CREATE TABLE `t_group_messages_59` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message59_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;


DROP TABLE IF EXISTS `t_group_messages_60`;
CREATE TABLE `t_group_messages_60` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message60_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_61`;
CREATE TABLE `t_group_messages_61` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message61_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_62`;
CREATE TABLE `t_group_messages_62` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message62_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_63`;
CREATE TABLE `t_group_messages_63` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message63_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_64`;
CREATE TABLE `t_group_messages_64` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message64_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_65`;
CREATE TABLE `t_group_messages_65` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message65_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_66`;
CREATE TABLE `t_group_messages_66` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message66_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_67`;
CREATE TABLE `t_group_messages_67` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message67_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_68`;
CREATE TABLE `t_group_messages_68` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message68_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_69`;
CREATE TABLE `t_group_messages_69` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message69_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;


DROP TABLE IF EXISTS `t_group_messages_70`;
CREATE TABLE `t_group_messages_70` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message70_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_71`;
CREATE TABLE `t_group_messages_71` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message71_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_72`;
CREATE TABLE `t_group_messages_72` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message72_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_73`;
CREATE TABLE `t_group_messages_73` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message73_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_74`;
CREATE TABLE `t_group_messages_74` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message74_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_75`;
CREATE TABLE `t_group_messages_75` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message75_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_76`;
CREATE TABLE `t_group_messages_76` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message76_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_77`;
CREATE TABLE `t_group_messages_77` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message77_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_78`;
CREATE TABLE `t_group_messages_78` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message78_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_79`;
CREATE TABLE `t_group_messages_79` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message79_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;


DROP TABLE IF EXISTS `t_group_messages_80`;
CREATE TABLE `t_group_messages_80` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message80_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_81`;
CREATE TABLE `t_group_messages_81` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message81_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_82`;
CREATE TABLE `t_group_messages_82` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message82_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_83`;
CREATE TABLE `t_group_messages_83` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message83_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_84`;
CREATE TABLE `t_group_messages_84` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message84_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_85`;
CREATE TABLE `t_group_messages_85` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message85_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_86`;
CREATE TABLE `t_group_messages_86` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message86_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_87`;
CREATE TABLE `t_group_messages_87` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message87_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_88`;
CREATE TABLE `t_group_messages_88` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message88_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_89`;
CREATE TABLE `t_group_messages_89` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message89_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;


DROP TABLE IF EXISTS `t_group_messages_90`;
CREATE TABLE `t_group_messages_90` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message90_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_91`;
CREATE TABLE `t_group_messages_91` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message91_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_92`;
CREATE TABLE `t_group_messages_92` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message92_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_93`;
CREATE TABLE `t_group_messages_93` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message93_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_94`;
CREATE TABLE `t_group_messages_94` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message94_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_95`;
CREATE TABLE `t_group_messages_95` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message95_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_96`;
CREATE TABLE `t_group_messages_96` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message96_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_97`;
CREATE TABLE `t_group_messages_97` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message97_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_98`;
CREATE TABLE `t_group_messages_98` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message98_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_99`;
CREATE TABLE `t_group_messages_99` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message99_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;


DROP TABLE IF EXISTS `t_group_messages_100`;
CREATE TABLE `t_group_messages_100` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message100_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_101`;
CREATE TABLE `t_group_messages_101` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message101_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_102`;
CREATE TABLE `t_group_messages_102` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message102_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_103`;
CREATE TABLE `t_group_messages_103` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message103_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_104`;
CREATE TABLE `t_group_messages_104` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message104_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_105`;
CREATE TABLE `t_group_messages_105` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message105_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_106`;
CREATE TABLE `t_group_messages_106` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message106_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_107`;
CREATE TABLE `t_group_messages_107` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message107_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_108`;
CREATE TABLE `t_group_messages_108` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message108_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_109`;
CREATE TABLE `t_group_messages_109` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message109_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;


DROP TABLE IF EXISTS `t_group_messages_110`;
CREATE TABLE `t_group_messages_110` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message110_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_111`;
CREATE TABLE `t_group_messages_111` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message111_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_112`;
CREATE TABLE `t_group_messages_112` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message112_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_113`;
CREATE TABLE `t_group_messages_113` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message113_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_114`;
CREATE TABLE `t_group_messages_114` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message114_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_115`;
CREATE TABLE `t_group_messages_115` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message115_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_116`;
CREATE TABLE `t_group_messages_116` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message116_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_117`;
CREATE TABLE `t_group_messages_117` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message117_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_118`;
CREATE TABLE `t_group_messages_118` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message118_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_119`;
CREATE TABLE `t_group_messages_119` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message119_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;


DROP TABLE IF EXISTS `t_group_messages_120`;
CREATE TABLE `t_group_messages_120` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message120_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_121`;
CREATE TABLE `t_group_messages_121` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message121_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_122`;
CREATE TABLE `t_group_messages_122` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message122_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_123`;
CREATE TABLE `t_group_messages_123` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message123_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_124`;
CREATE TABLE `t_group_messages_124` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message124_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_125`;
CREATE TABLE `t_group_messages_125` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message125_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_126`;
CREATE TABLE `t_group_messages_126` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message126_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;

DROP TABLE IF EXISTS `t_group_messages_127`;
CREATE TABLE `t_group_messages_127` (
    `_mid` bigint(20) NOT NULL PRIMARY KEY,
    `_sender` varchar(64) NOT NULL,
    `_gid` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL,
    `_client_id` varchar(64) NULL,
    `_seq` bigint(20) NOT NULL,
    `_persist_flag` int(11) NOT NULL,
    `_mentioned_type` int(11) NOT NULL,
    `_mentioned_targets` BLOB NULL,
    `_to` BLOB NULL,
    `_cont_type` int(11) NOT NULL DEFAULT 0,
    `_duration` int(11) NOT NULL DEFAULT 0,
    `_dt` bigint(20) NOT NULL DEFAULT 0,
    INDEX `group_message127_seq_uid_index` (`_gid` DESC, `_line`, `_seq` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_bin;
