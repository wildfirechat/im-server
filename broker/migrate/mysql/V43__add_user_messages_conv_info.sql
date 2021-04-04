alter table `t_user_messages` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_0` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_0` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_0` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_0` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_1` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_1` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_1` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_1` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_2` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_2` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_2` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_2` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_3` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_3` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_3` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_3` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_4` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_4` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_4` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_4` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_5` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_5` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_5` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_5` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_6` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_6` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_6` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_6` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_7` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_7` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_7` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_7` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_8` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_8` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_8` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_8` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_9` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_9` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_9` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_9` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_10` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_10` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_10` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_10` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_11` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_11` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_11` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_11` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_12` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_12` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_12` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_12` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_13` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_13` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_13` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_13` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_14` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_14` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_14` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_14` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_15` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_15` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_15` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_15` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_16` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_16` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_16` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_16` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_17` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_17` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_17` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_17` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_18` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_18` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_18` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_18` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_19` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_19` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_19` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_19` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_20` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_20` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_20` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_20` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_21` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_21` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_21` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_21` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_22` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_22` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_22` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_22` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_23` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_23` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_23` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_23` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_24` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_24` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_24` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_24` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_25` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_25` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_25` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_25` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_26` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_26` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_26` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_26` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_27` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_27` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_27` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_27` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_28` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_28` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_28` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_28` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_29` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_29` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_29` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_29` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_30` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_30` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_30` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_30` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_31` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_31` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_31` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_31` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_32` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_32` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_32` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_32` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_33` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_33` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_33` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_33` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_34` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_34` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_34` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_34` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_35` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_35` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_35` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_35` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_36` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_36` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_36` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_36` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_37` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_37` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_37` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_37` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_38` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_38` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_38` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_38` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_39` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_39` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_39` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_39` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_40` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_40` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_40` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_40` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_41` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_41` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_41` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_41` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_42` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_42` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_42` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_42` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_43` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_43` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_43` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_43` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_44` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_44` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_44` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_44` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_45` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_45` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_45` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_45` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_46` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_46` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_46` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_46` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_47` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_47` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_47` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_47` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_48` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_48` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_48` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_48` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_49` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_49` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_49` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_49` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_50` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_50` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_50` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_50` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_51` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_51` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_51` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_51` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_52` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_52` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_52` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_52` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_53` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_53` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_53` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_53` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_54` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_54` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_54` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_54` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_55` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_55` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_55` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_55` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_56` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_56` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_56` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_56` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_57` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_57` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_57` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_57` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_58` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_58` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_58` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_58` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_59` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_59` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_59` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_59` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_60` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_60` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_60` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_60` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_61` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_61` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_61` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_61` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_62` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_62` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_62` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_62` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_63` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_63` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_63` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_63` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_64` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_64` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_64` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_64` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_65` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_65` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_65` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_65` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_66` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_66` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_66` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_66` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_67` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_67` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_67` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_67` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_68` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_68` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_68` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_68` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_69` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_69` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_69` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_69` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_70` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_70` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_70` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_70` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_71` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_71` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_71` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_71` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_72` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_72` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_72` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_72` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_73` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_73` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_73` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_73` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_74` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_74` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_74` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_74` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_75` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_75` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_75` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_75` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_76` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_76` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_76` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_76` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_77` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_77` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_77` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_77` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_78` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_78` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_78` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_78` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_79` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_79` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_79` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_79` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_80` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_80` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_80` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_80` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_81` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_81` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_81` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_81` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_82` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_82` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_82` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_82` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_83` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_83` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_83` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_83` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_84` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_84` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_84` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_84` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_85` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_85` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_85` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_85` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_86` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_86` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_86` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_86` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_87` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_87` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_87` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_87` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_88` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_88` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_88` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_88` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_89` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_89` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_89` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_89` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_90` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_90` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_90` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_90` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_91` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_91` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_91` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_91` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_92` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_92` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_92` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_92` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_93` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_93` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_93` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_93` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_94` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_94` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_94` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_94` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_95` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_95` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_95` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_95` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_96` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_96` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_96` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_96` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_97` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_97` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_97` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_97` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_98` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_98` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_98` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_98` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_99` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_99` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_99` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_99` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_100` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_100` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_100` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_100` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_101` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_101` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_101` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_101` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_102` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_102` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_102` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_102` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_103` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_103` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_103` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_103` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_104` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_104` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_104` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_104` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_105` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_105` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_105` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_105` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_106` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_106` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_106` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_106` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_107` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_107` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_107` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_107` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_108` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_108` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_108` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_108` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_109` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_109` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_109` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_109` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_110` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_110` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_110` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_110` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_111` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_111` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_111` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_111` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_112` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_112` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_112` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_112` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_113` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_113` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_113` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_113` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_114` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_114` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_114` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_114` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_115` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_115` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_115` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_115` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_116` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_116` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_116` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_116` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_117` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_117` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_117` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_117` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_118` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_118` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_118` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_118` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_119` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_119` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_119` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_119` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_120` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_120` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_120` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_120` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_121` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_121` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_121` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_121` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_122` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_122` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_122` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_122` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_123` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_123` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_123` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_123` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_124` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_124` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_124` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_124` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_125` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_125` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_125` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_125` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_126` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_126` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_126` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_126` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);

alter table `t_user_messages_127` add column `_type` tinyint(4) NOT NULL DEFAULT '0';
alter table `t_user_messages_127` add column `_target` varchar(129) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
alter table `t_user_messages_127` add column `_directing` tinyint(1) NOT NULL DEFAULT '0';
alter table `t_user_messages_127` add INDEX `user_messages_conv_index` ( `_uid`, `_type`, `_target`, `_line`, `_mid` desc);
