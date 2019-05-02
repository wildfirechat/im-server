#!/bin/bash

show_help_info(){
  echo "###  Usage: ./initial_db.sh -u{user} -p{password}"
  echo "###  初始化数据库。"
  echo "###"

}

for i in "$@"
do
  PFLAG=`echo $i|cut -b1-2`
  PPARAM=`echo $i|cut -b3-`
  if [ $PFLAG = "-u" ]; then
    USER=${PPARAM}
  elif [ $PFLAG = "-p" ]; then
    PASSWORD=${PPARAM}
  elif [ $PFLAG = "-h" ]; then
    show_help_info
    exit 1
  fi
done

echo

if [ "${USER}" = "" ]; then
  show_help_info
  exit 1
elif [ "${PASSWORD}" = "" ]; then
  show_help_info
  exit 1
fi


mysql -u${USER} -p${PASSWORD} -e"source 1_create_database.sql;"
mysql -u${USER} -p${PASSWORD} -e"use wfchat; source 2_create_table.sql; source 3_create_default_chatroom.sql; source 4_create_default_robot.sql; source 5_add_friend_alias.sql;"

for((i=0;i<36;i++));
do
  create_message_tbl_sql="USE wfchat;
  DROP TABLE IF EXISTS \`t_messages_${i}\`;
  CREATE TABLE \`t_messages_${i}\` (
    \`id\` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    \`_mid\` bigint(20) NOT NULL,
    \`_from\` varchar(64) NOT NULL,
    \`_type\` tinyint NOT NULL DEFAULT 0,
    \`_target\` varchar(64) NOT NULL,
    \`_line\` int(11) NOT NULL DEFAULT 0,
    \`_data\` BLOB NOT NULL,
    \`_searchable_key\` TEXT DEFAULT NULL,
    \`_dt\` DATETIME NOT NULL,
    INDEX \`message_uid_index\` (\`_mid\` DESC)
  )
  ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
  "

  echo "create table t_messages_$i"
  mysql -u${USER} -p${PASSWORD} -e"${create_message_tbl_sql}"
done


for((i=0;i<128;i++));
do
  create_user_message_tbl_sql="USE wfchat;
  DROP TABLE IF EXISTS \`t_user_messages_${i}\`;
  CREATE TABLE \`t_user_messages_${i}\` (
    \`id\` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    \`_mid\` bigint(20) NOT NULL,
    \`_uid\` varchar(64) NOT NULL,
    \`_seq\` bigint(20) NOT NULL,
    \`_dt\` DATETIME NOT NULL DEFAULT NOW(),
    INDEX \`message_seq_uid_index\` (\`_uid\` DESC, \`_seq\` DESC)
  )
  ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
  "

  echo "create table t_user_messages_$i"
  mysql -u${USER} -p${PASSWORD} -e"${create_user_message_tbl_sql}"
done
