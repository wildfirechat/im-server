#!/bin/bash

dest=$1
mvn clean install -Dmaven.test.skip=true
if [ -n "$dest" ];then
scp  distribution/target/distribution-0.10-bundle-tar.tar.gz ${dest}:~/wildfirechat/
ssh ${dest} 'cd ~/wildfirechat; bash deploy.sh'

sleep 20s
curl im.liyufan.win:8080/api/version

echo ""
echo "curl wildfirechat.cn/api/version"
fi
