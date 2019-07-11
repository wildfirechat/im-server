alter table t_session MODIFY column `create_time` DATETIME NOT NULL DEFAULT NOW();
alter table t_session MODIFY column `update_time` DATETIME NOT NULL DEFAULT NOW();
alter table t_session MODIFY column `expire_time` DATETIME NOT NULL DEFAULT NOW();

