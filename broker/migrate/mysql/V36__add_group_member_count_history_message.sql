alter table `t_group` add column `_history_message` tinyint NOT NULL DEFAULT 0, COMMENT "new group member can load group history messages";
alter table `t_group` add column `_max_member_count` int(11) NOT NULL DEFAULT 2000, COMMENT "max group member count";
