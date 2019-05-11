
DROP TABLE IF EXISTS `t_messages_0`;
CREATE TABLE `t_messages_0` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_1`;
CREATE TABLE `t_messages_1` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_2`;
CREATE TABLE `t_messages_2` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_3`;
CREATE TABLE `t_messages_3` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_4`;
CREATE TABLE `t_messages_4` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_5`;
CREATE TABLE `t_messages_5` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_6`;
CREATE TABLE `t_messages_6` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_7`;
CREATE TABLE `t_messages_7` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_8`;
CREATE TABLE `t_messages_8` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_9`;
CREATE TABLE `t_messages_9` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_10`;
CREATE TABLE `t_messages_10` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_11`;
CREATE TABLE `t_messages_11` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_12`;
CREATE TABLE `t_messages_12` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_13`;
CREATE TABLE `t_messages_13` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_14`;
CREATE TABLE `t_messages_14` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_15`;
CREATE TABLE `t_messages_15` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_16`;
CREATE TABLE `t_messages_16` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_17`;
CREATE TABLE `t_messages_17` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_18`;
CREATE TABLE `t_messages_18` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_19`;
CREATE TABLE `t_messages_19` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_20`;
CREATE TABLE `t_messages_20` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_21`;
CREATE TABLE `t_messages_21` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_22`;
CREATE TABLE `t_messages_22` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_23`;
CREATE TABLE `t_messages_23` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_24`;
CREATE TABLE `t_messages_24` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_25`;
CREATE TABLE `t_messages_25` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_26`;
CREATE TABLE `t_messages_26` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_27`;
CREATE TABLE `t_messages_27` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_28`;
CREATE TABLE `t_messages_28` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_29`;
CREATE TABLE `t_messages_29` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_30`;
CREATE TABLE `t_messages_30` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_31`;
CREATE TABLE `t_messages_31` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_32`;
CREATE TABLE `t_messages_32` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_messages_33`;
CREATE TABLE `t_messages_33` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_34`;
CREATE TABLE `t_messages_34` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_messages_35`;
CREATE TABLE `t_messages_35` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_mid` bigint(20) NOT NULL,
    `_from` varchar(64) NOT NULL,
    `_type` tinyint NOT NULL DEFAULT 0,
    `_target` varchar(64) NOT NULL,
    `_line` int(11) NOT NULL DEFAULT 0,
    `_data` BLOB NOT NULL,
    `_searchable_key` TEXT DEFAULT NULL,
    `_dt` DATETIME NOT NULL,
    INDEX `message_uid_index` (`_mid` DESC)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `t_user_messages_0`;
CREATE TABLE `t_user_messages_0` (
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


DROP TABLE IF EXISTS `t_user_messages_1`;
CREATE TABLE `t_user_messages_1` (
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

DROP TABLE IF EXISTS `t_user_messages_2`;
CREATE TABLE `t_user_messages_2` (
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

DROP TABLE IF EXISTS `t_user_messages_3`;
CREATE TABLE `t_user_messages_3` (
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


DROP TABLE IF EXISTS `t_user_messages_4`;
CREATE TABLE `t_user_messages_4` (
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

DROP TABLE IF EXISTS `t_user_messages_5`;
CREATE TABLE `t_user_messages_5` (
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


DROP TABLE IF EXISTS `t_user_messages_6`;
CREATE TABLE `t_user_messages_6` (
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

DROP TABLE IF EXISTS `t_user_messages_7`;
CREATE TABLE `t_user_messages_7` (
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

DROP TABLE IF EXISTS `t_user_messages_8`;
CREATE TABLE `t_user_messages_8` (
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


DROP TABLE IF EXISTS `t_user_messages_9`;
CREATE TABLE `t_user_messages_9` (
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

DROP TABLE IF EXISTS `t_user_messages_10`;
CREATE TABLE `t_user_messages_10` (
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


DROP TABLE IF EXISTS `t_user_messages_11`;
CREATE TABLE `t_user_messages_11` (
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

DROP TABLE IF EXISTS `t_user_messages_12`;
CREATE TABLE `t_user_messages_12` (
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

DROP TABLE IF EXISTS `t_user_messages_13`;
CREATE TABLE `t_user_messages_13` (
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


DROP TABLE IF EXISTS `t_user_messages_14`;
CREATE TABLE `t_user_messages_14` (
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

DROP TABLE IF EXISTS `t_user_messages_15`;
CREATE TABLE `t_user_messages_15` (
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


DROP TABLE IF EXISTS `t_user_messages_16`;
CREATE TABLE `t_user_messages_16` (
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

DROP TABLE IF EXISTS `t_user_messages_17`;
CREATE TABLE `t_user_messages_17` (
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

DROP TABLE IF EXISTS `t_user_messages_18`;
CREATE TABLE `t_user_messages_18` (
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


DROP TABLE IF EXISTS `t_user_messages_19`;
CREATE TABLE `t_user_messages_19` (
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

DROP TABLE IF EXISTS `t_user_messages_20`;
CREATE TABLE `t_user_messages_20` (
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


DROP TABLE IF EXISTS `t_user_messages_21`;
CREATE TABLE `t_user_messages_21` (
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

DROP TABLE IF EXISTS `t_user_messages_22`;
CREATE TABLE `t_user_messages_22` (
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

DROP TABLE IF EXISTS `t_user_messages_23`;
CREATE TABLE `t_user_messages_23` (
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


DROP TABLE IF EXISTS `t_user_messages_24`;
CREATE TABLE `t_user_messages_24` (
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

DROP TABLE IF EXISTS `t_user_messages_25`;
CREATE TABLE `t_user_messages_25` (
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


DROP TABLE IF EXISTS `t_user_messages_26`;
CREATE TABLE `t_user_messages_26` (
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

DROP TABLE IF EXISTS `t_user_messages_27`;
CREATE TABLE `t_user_messages_27` (
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

DROP TABLE IF EXISTS `t_user_messages_28`;
CREATE TABLE `t_user_messages_28` (
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


DROP TABLE IF EXISTS `t_user_messages_29`;
CREATE TABLE `t_user_messages_29` (
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

DROP TABLE IF EXISTS `t_user_messages_30`;
CREATE TABLE `t_user_messages_30` (
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


DROP TABLE IF EXISTS `t_user_messages_31`;
CREATE TABLE `t_user_messages_31` (
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

DROP TABLE IF EXISTS `t_user_messages_32`;
CREATE TABLE `t_user_messages_32` (
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

DROP TABLE IF EXISTS `t_user_messages_33`;
CREATE TABLE `t_user_messages_33` (
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


DROP TABLE IF EXISTS `t_user_messages_34`;
CREATE TABLE `t_user_messages_34` (
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

DROP TABLE IF EXISTS `t_user_messages_35`;
CREATE TABLE `t_user_messages_35` (
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


DROP TABLE IF EXISTS `t_user_messages_36`;
CREATE TABLE `t_user_messages_36` (
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

DROP TABLE IF EXISTS `t_user_messages_37`;
CREATE TABLE `t_user_messages_37` (
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

DROP TABLE IF EXISTS `t_user_messages_38`;
CREATE TABLE `t_user_messages_38` (
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


DROP TABLE IF EXISTS `t_user_messages_39`;
CREATE TABLE `t_user_messages_39` (
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

DROP TABLE IF EXISTS `t_user_messages_40`;
CREATE TABLE `t_user_messages_40` (
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


DROP TABLE IF EXISTS `t_user_messages_41`;
CREATE TABLE `t_user_messages_41` (
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

DROP TABLE IF EXISTS `t_user_messages_42`;
CREATE TABLE `t_user_messages_42` (
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

DROP TABLE IF EXISTS `t_user_messages_43`;
CREATE TABLE `t_user_messages_43` (
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


DROP TABLE IF EXISTS `t_user_messages_44`;
CREATE TABLE `t_user_messages_44` (
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

DROP TABLE IF EXISTS `t_user_messages_45`;
CREATE TABLE `t_user_messages_45` (
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


DROP TABLE IF EXISTS `t_user_messages_46`;
CREATE TABLE `t_user_messages_46` (
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

DROP TABLE IF EXISTS `t_user_messages_47`;
CREATE TABLE `t_user_messages_47` (
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

DROP TABLE IF EXISTS `t_user_messages_48`;
CREATE TABLE `t_user_messages_48` (
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


DROP TABLE IF EXISTS `t_user_messages_49`;
CREATE TABLE `t_user_messages_49` (
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

DROP TABLE IF EXISTS `t_user_messages_50`;
CREATE TABLE `t_user_messages_50` (
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


DROP TABLE IF EXISTS `t_user_messages_51`;
CREATE TABLE `t_user_messages_51` (
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

DROP TABLE IF EXISTS `t_user_messages_52`;
CREATE TABLE `t_user_messages_52` (
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

DROP TABLE IF EXISTS `t_user_messages_53`;
CREATE TABLE `t_user_messages_53` (
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


DROP TABLE IF EXISTS `t_user_messages_54`;
CREATE TABLE `t_user_messages_54` (
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

DROP TABLE IF EXISTS `t_user_messages_55`;
CREATE TABLE `t_user_messages_55` (
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


DROP TABLE IF EXISTS `t_user_messages_56`;
CREATE TABLE `t_user_messages_56` (
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

DROP TABLE IF EXISTS `t_user_messages_57`;
CREATE TABLE `t_user_messages_57` (
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

DROP TABLE IF EXISTS `t_user_messages_58`;
CREATE TABLE `t_user_messages_58` (
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


DROP TABLE IF EXISTS `t_user_messages_59`;
CREATE TABLE `t_user_messages_59` (
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

DROP TABLE IF EXISTS `t_user_messages_60`;
CREATE TABLE `t_user_messages_60` (
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


DROP TABLE IF EXISTS `t_user_messages_61`;
CREATE TABLE `t_user_messages_61` (
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

DROP TABLE IF EXISTS `t_user_messages_62`;
CREATE TABLE `t_user_messages_62` (
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

DROP TABLE IF EXISTS `t_user_messages_63`;
CREATE TABLE `t_user_messages_63` (
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


DROP TABLE IF EXISTS `t_user_messages_64`;
CREATE TABLE `t_user_messages_64` (
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

DROP TABLE IF EXISTS `t_user_messages_65`;
CREATE TABLE `t_user_messages_65` (
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


DROP TABLE IF EXISTS `t_user_messages_66`;
CREATE TABLE `t_user_messages_66` (
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

DROP TABLE IF EXISTS `t_user_messages_67`;
CREATE TABLE `t_user_messages_67` (
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

DROP TABLE IF EXISTS `t_user_messages_68`;
CREATE TABLE `t_user_messages_68` (
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


DROP TABLE IF EXISTS `t_user_messages_69`;
CREATE TABLE `t_user_messages_69` (
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

DROP TABLE IF EXISTS `t_user_messages_70`;
CREATE TABLE `t_user_messages_70` (
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


DROP TABLE IF EXISTS `t_user_messages_71`;
CREATE TABLE `t_user_messages_71` (
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

DROP TABLE IF EXISTS `t_user_messages_72`;
CREATE TABLE `t_user_messages_72` (
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

DROP TABLE IF EXISTS `t_user_messages_73`;
CREATE TABLE `t_user_messages_73` (
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


DROP TABLE IF EXISTS `t_user_messages_74`;
CREATE TABLE `t_user_messages_74` (
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

DROP TABLE IF EXISTS `t_user_messages_75`;
CREATE TABLE `t_user_messages_75` (
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


DROP TABLE IF EXISTS `t_user_messages_76`;
CREATE TABLE `t_user_messages_76` (
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

DROP TABLE IF EXISTS `t_user_messages_77`;
CREATE TABLE `t_user_messages_77` (
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

DROP TABLE IF EXISTS `t_user_messages_78`;
CREATE TABLE `t_user_messages_78` (
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


DROP TABLE IF EXISTS `t_user_messages_79`;
CREATE TABLE `t_user_messages_79` (
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

DROP TABLE IF EXISTS `t_user_messages_80`;
CREATE TABLE `t_user_messages_80` (
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


DROP TABLE IF EXISTS `t_user_messages_81`;
CREATE TABLE `t_user_messages_81` (
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

DROP TABLE IF EXISTS `t_user_messages_82`;
CREATE TABLE `t_user_messages_82` (
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

DROP TABLE IF EXISTS `t_user_messages_83`;
CREATE TABLE `t_user_messages_83` (
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


DROP TABLE IF EXISTS `t_user_messages_84`;
CREATE TABLE `t_user_messages_84` (
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

DROP TABLE IF EXISTS `t_user_messages_85`;
CREATE TABLE `t_user_messages_85` (
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


DROP TABLE IF EXISTS `t_user_messages_86`;
CREATE TABLE `t_user_messages_86` (
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

DROP TABLE IF EXISTS `t_user_messages_87`;
CREATE TABLE `t_user_messages_87` (
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

DROP TABLE IF EXISTS `t_user_messages_88`;
CREATE TABLE `t_user_messages_88` (
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


DROP TABLE IF EXISTS `t_user_messages_89`;
CREATE TABLE `t_user_messages_89` (
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

DROP TABLE IF EXISTS `t_user_messages_90`;
CREATE TABLE `t_user_messages_90` (
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


DROP TABLE IF EXISTS `t_user_messages_91`;
CREATE TABLE `t_user_messages_91` (
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

DROP TABLE IF EXISTS `t_user_messages_92`;
CREATE TABLE `t_user_messages_92` (
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

DROP TABLE IF EXISTS `t_user_messages_93`;
CREATE TABLE `t_user_messages_93` (
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


DROP TABLE IF EXISTS `t_user_messages_94`;
CREATE TABLE `t_user_messages_94` (
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

DROP TABLE IF EXISTS `t_user_messages_95`;
CREATE TABLE `t_user_messages_95` (
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


DROP TABLE IF EXISTS `t_user_messages_96`;
CREATE TABLE `t_user_messages_96` (
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

DROP TABLE IF EXISTS `t_user_messages_97`;
CREATE TABLE `t_user_messages_97` (
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

DROP TABLE IF EXISTS `t_user_messages_98`;
CREATE TABLE `t_user_messages_98` (
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


DROP TABLE IF EXISTS `t_user_messages_99`;
CREATE TABLE `t_user_messages_99` (
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

DROP TABLE IF EXISTS `t_user_messages_100`;
CREATE TABLE `t_user_messages_100` (
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


DROP TABLE IF EXISTS `t_user_messages_101`;
CREATE TABLE `t_user_messages_101` (
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

DROP TABLE IF EXISTS `t_user_messages_102`;
CREATE TABLE `t_user_messages_102` (
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

DROP TABLE IF EXISTS `t_user_messages_103`;
CREATE TABLE `t_user_messages_103` (
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


DROP TABLE IF EXISTS `t_user_messages_104`;
CREATE TABLE `t_user_messages_104` (
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

DROP TABLE IF EXISTS `t_user_messages_105`;
CREATE TABLE `t_user_messages_105` (
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


DROP TABLE IF EXISTS `t_user_messages_106`;
CREATE TABLE `t_user_messages_106` (
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

DROP TABLE IF EXISTS `t_user_messages_107`;
CREATE TABLE `t_user_messages_107` (
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

DROP TABLE IF EXISTS `t_user_messages_108`;
CREATE TABLE `t_user_messages_108` (
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


DROP TABLE IF EXISTS `t_user_messages_109`;
CREATE TABLE `t_user_messages_109` (
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

DROP TABLE IF EXISTS `t_user_messages_110`;
CREATE TABLE `t_user_messages_110` (
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


DROP TABLE IF EXISTS `t_user_messages_111`;
CREATE TABLE `t_user_messages_111` (
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

DROP TABLE IF EXISTS `t_user_messages_112`;
CREATE TABLE `t_user_messages_112` (
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

DROP TABLE IF EXISTS `t_user_messages_113`;
CREATE TABLE `t_user_messages_113` (
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


DROP TABLE IF EXISTS `t_user_messages_114`;
CREATE TABLE `t_user_messages_114` (
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

DROP TABLE IF EXISTS `t_user_messages_115`;
CREATE TABLE `t_user_messages_115` (
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


DROP TABLE IF EXISTS `t_user_messages_116`;
CREATE TABLE `t_user_messages_116` (
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

DROP TABLE IF EXISTS `t_user_messages_117`;
CREATE TABLE `t_user_messages_117` (
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

DROP TABLE IF EXISTS `t_user_messages_118`;
CREATE TABLE `t_user_messages_118` (
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


DROP TABLE IF EXISTS `t_user_messages_119`;
CREATE TABLE `t_user_messages_119` (
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

DROP TABLE IF EXISTS `t_user_messages_120`;
CREATE TABLE `t_user_messages_120` (
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


DROP TABLE IF EXISTS `t_user_messages_121`;
CREATE TABLE `t_user_messages_121` (
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

DROP TABLE IF EXISTS `t_user_messages_122`;
CREATE TABLE `t_user_messages_122` (
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

DROP TABLE IF EXISTS `t_user_messages_123`;
CREATE TABLE `t_user_messages_123` (
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


DROP TABLE IF EXISTS `t_user_messages_124`;
CREATE TABLE `t_user_messages_124` (
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

DROP TABLE IF EXISTS `t_user_messages_125`;
CREATE TABLE `t_user_messages_125` (
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


DROP TABLE IF EXISTS `t_user_messages_126`;
CREATE TABLE `t_user_messages_126` (
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

DROP TABLE IF EXISTS `t_user_messages_127`;
CREATE TABLE `t_user_messages_127` (
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

