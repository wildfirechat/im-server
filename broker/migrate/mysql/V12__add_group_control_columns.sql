alter table `t_group` add column `_mute` tinyint NOT NULL DEFAULT 0;
alter table `t_group` add column `_join_type` tinyint NOT NULL DEFAULT 0;
alter table `t_group` add column `_private_chat` tinyint NOT NULL DEFAULT 0;
alter table `t_group` add column `_searchable` tinyint NOT NULL DEFAULT 0;
