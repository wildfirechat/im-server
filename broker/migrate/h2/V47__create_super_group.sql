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
);
