
DROP TABLE IF EXISTS `t_session`;
CREATE TABLE `t_session` (
  `row_id` varchar(50) not null primary key,
  `del_flag` tinyint(1) default 0 null,
  `create_time` timestamp null,
  `update_time` timestamp null,
  `expire_time` timestamp null,
  `user_id` int null,
  constraint `t_session_pk_2` unique (`user_id`, `expire_time`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;
