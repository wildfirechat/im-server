DROP TABLE IF EXISTS `t_file`;
CREATE TABLE `t_file` (
  `_mid` bigint(20) NOT NULL PRIMARY KEY,
  `_from` varchar(64) NOT NULL,
  `_type` tinyint NOT NULL DEFAULT 0,
  `_target` varchar(64) NOT NULL,
  `_line` int(11) NOT NULL DEFAULT 0,
  `_name` varchar(128) DEFAULT '',
  `_url` varchar(1024) NOT NULL DEFAULT '',
  `_size` int(11) NOT NULL DEFAULT 0,
  `_download_count` int(11) DEFAULT 0,
  `_dt` bigint(20) NOT NULL,
  INDEX `file_conv_index` (`_type`, `_line`, `_target`, `_mid`),
  INDEX `file_user_index` (`_from`, `_mid`)
);
