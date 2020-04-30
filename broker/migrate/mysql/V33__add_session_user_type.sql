alter table t_user_session add column `_user_type` tinyint DEFAULT 0 COMMENT "0, normal user; 1, robot; 2, device;";
