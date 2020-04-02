
DROP TABLE IF EXISTS `t_chatroom_blacklist`;
CREATE TABLE `t_chatroom_blacklist` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_cid` varchar(64) NOT NULL,
  `_uid` varchar(64) NOT NULL,
  `_state` tinyint NOT NULL DEFAULT 0,
  `_expired_time` bigint(20) NOT NULL,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `cr_bl_uid_index` (`_cid`,_uid)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `t_chatroom_manager`;
CREATE TABLE `t_chatroom_manager` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_cid` varchar(64) NOT NULL,
  `_uid` varchar(64) NOT NULL,
  `_state` tinyint NOT NULL DEFAULT 0,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `cr_man_uid_index` (`_cid`,_uid)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;



