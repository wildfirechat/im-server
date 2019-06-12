
DROP TABLE IF EXISTS `t_sensitive_messages`;
CREATE TABLE `t_sensitive_messages` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    `_content_type` int(11) NOT NULL DEFAULT '0',
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;
