alter table `t_user_messages` add column `_line` int(11) NOT NULL DEFAULT 0;
alter table `t_user_messages` ADD INDEX `message_mid_line_uid_index` ( `_uid` DESC, `_line` DESC, `_mid` DESC );
