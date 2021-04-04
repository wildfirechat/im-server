alter table `t_user_messages` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages` add column `_target` varchar(129) NOT NULL DEFAULT '';
alter table `t_user_messages` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);
