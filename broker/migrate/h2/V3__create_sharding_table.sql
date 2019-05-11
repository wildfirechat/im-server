
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
  `_dt` DATETIME NOT NULL DEFAULT NOW(),
  UNIQUE INDEX `message_uid_index` (`_mid` ASC)
);

DROP TABLE IF EXISTS `t_user_messages`;
CREATE TABLE `t_user_messages` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_uid` varchar(64) NOT NULL,
    `_seq` bigint(20) NOT NULL,
    `_dt` DATETIME NOT NULL DEFAULT NOW(),
    UNIQUE INDEX `message_seq_uid_index` (`_uid` DESC, `_seq` DESC)
);
