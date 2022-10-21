# Linux Service 方式运行
除了命令行方式直接执行IM服务外，还可以以linux systemd service方式来运行，注意以这种方式运行，im服务的配置还是需要按照常规方法来配置。

## 获取软件包
如果是社区版可以下载野火release或则会自己源码编译，得到软件压缩包```distribution-bundle-tar.tar.gz```。如果是专业版使用专业版邮件里的链接下载软件压缩包，下载后先解压一次，得到```distribution-bundle-tar.tar.gz```压缩包。

## 部署软件包
创建```/usr/local/wildfirechat/im```目录，把软件包解压到这个目录下。解压后这个目录下有```bin```、```config```、```lib```等目录。
> 专业版软件包压缩了2次，先解压出```distribution-bundle-tar.tar.gz```，再拷贝到IM目录再次解压，所以注意确认目录下有bin、config等目录才对。

## 放置Server File
把```wildfire-im.service```放到```/etc/systemd/system/```目录下。

## 管理服务
* 启动： ```sudo systemctl start wildfire-im.service```
* 停止： ```sudo systemctl stop wildfire-im.service```
* 重启： ```sudo systemctl restart wildfire-im.service```
* 查看控制台日志: ```journalctl -f -u wildfire-im.service```

## 日志
日志文件在```/usr/local/wildfirechat/im/logs```目录下。如果需要提供日志给野火官方，请把这个目录下的日志和制台日志(```journalctl -f -u wildfire-im.service```)一起发给野火。

## 配置
需要对IM服务配置来达到最好的执行效果，配置文件在````/usr/local/wildfirechat/im/config````目录下。另外还可以设置服务的内存大小，修改```/usr/local/wildfirechat/im/bin/wildfirechat.sh```文件的倒数3、4行。打开Xmx和Xms配置，设置为合适的内存大小。