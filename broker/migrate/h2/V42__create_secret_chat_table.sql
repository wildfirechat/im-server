
DROP TABLE IF EXISTS `t_secret_chat`;
CREATE TABLE `t_secret_chat` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_from` varchar(64) DEFAULT '',
  `_from_cid` varchar(64) DEFAULT '',
  `_to` varchar(64) DEFAULT '',
  `_to_cid` varchar(64) DEFAULT '',
  `_state` tinyint NOT NULL DEFAULT 0,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `secret_chat_uid_index` (`_uid` DESC)
);
