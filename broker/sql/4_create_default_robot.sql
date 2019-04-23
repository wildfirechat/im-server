insert into t_user (`_uid`,`_name`,`_display_name`,`_portrait`,`_dt`) values ('FireRobot','FireRobot','小火','http://cdn2.wildfirechat.cn/robot.png',0);

insert into t_robot (`_uid`,`_owner`,`_secret`,`_callback`,`_state`,`_dt`) values ('FireRobot', 'FireRobot', '123456', 'http://192.168.0.10:8883/robot/recvmsg', 0, 0);
