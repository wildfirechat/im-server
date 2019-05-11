
insert into t_user (`_uid`,`_name`,`_display_name`,`_portrait`,`_type`,`_dt`) values ('FireRobot','FireRobot','小火','http://cdn2.wildfirechat.cn/robot.png',1,1);

insert into t_robot (`_uid`,`_owner`,`_secret`,`_callback`,`_state`,`_dt`) values ('FireRobot', 'FireRobot', '123456', 'http://127.0.0.1:8883/robot/recvmsg', 0, 1);
