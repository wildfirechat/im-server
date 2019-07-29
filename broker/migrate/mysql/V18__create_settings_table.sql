
DROP TABLE IF EXISTS `t_settings`;
CREATE TABLE `t_settings` (
  `id` int(11) NOT NULL PRIMARY KEY,
  `value` varchar(64) NOT NULL,
  `desc` varchar(128) NOT NULL default ""
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


insert into t_settings(`id`, `value`,`desc`) values (1, "3000", "最大群成员数量");
