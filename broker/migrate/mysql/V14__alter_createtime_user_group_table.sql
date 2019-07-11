alter table t_user MODIFY column `_createTime` DATETIME NOT NULL DEFAULT NOW();
alter table t_group MODIFY column `_createTime` DATETIME NOT NULL DEFAULT NOW();
