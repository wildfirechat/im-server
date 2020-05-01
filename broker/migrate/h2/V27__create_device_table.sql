
DROP TABLE IF EXISTS `t_thing`;

DROP TABLE IF EXISTS `t_device`;
CREATE TABLE `t_device` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_token` varchar(64) DEFAULT '',
  `_state` tinyint DEFAULT 0,
  `_extra` TEXT DEFAULT NULL,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `device_uid_index` (`_uid` ASC)
);

DROP TABLE IF EXISTS `t_user_device`;
CREATE TABLE `t_user_device` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_did` varchar(64) DEFAULT '',
  INDEX `user_device_uid_index` (`_uid` ASC),
  INDEX `user_device_did_index` (`_did` ASC)
);
