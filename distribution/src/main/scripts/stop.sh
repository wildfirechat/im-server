#!/bin/sh

pid=`ps -ef | grep wildfirechat.server.Server | grep -v grep | awk '{print $2}'`

if [ -z $pid ]; then
    echo "野火IM服务不存在"
    exit 0
fi

kill -15 $pid

for i in {1..30}
do
  pid=`ps -ef | grep wildfirechat.server.Server | grep -v grep | awk '{print $2}'`
  if [ -z $pid ]; then
    echo "野火IM服务已结束"
    exit 0
  else
    echo "正在结束中，请等待..."
    sleep 1
  fi
done

echo "正常结束失败，强制结束！"
kill -9 $pid
