--  IM服务在2020.10.6-2020-11.2号之间的版本升级到最新版本时，可能会提示出现如下的错误：
--  Exception in thread "main" org.flywaydb.core.api.FlywayException: Validate failed: Detected resolved migration not applied to database: 38
--      at org.flywaydb.core.Flyway.doValidate(Flyway.java:1482)
--      at org.flywaydb.core.Flyway.access$100(Flyway.java:85)
--      at org.flywaydb.core.Flyway$1.execute(Flyway.java:1364)
--      at org.flywaydb.core.Flyway$1.execute(Flyway.java:1356)
--      ...
--
--  当出现这个问题后，执行这个sql就可以修复了。
--
--
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
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


alter table `t_group` modify column `_searchable` int(11) NOT NULL DEFAULT 0;

update flyway_schema_history set installed_rank = 40 where version = 40;

insert into flyway_schema_history values (38,'38','alter group searchable column','SQL','V38__alter_group_searchable_column.sql',-524664704,'root','2020-07-21 14:43:50',1126,1),(39,'39','create files table','SQL','V39__create_files_table.sql',1144437159,'root','2020-08-03 03:15:04',503,1);
