# Linux Service 方式运行
除了命令行方式直接执行IM服务外，还可以以linux systemd service方式来运行，注意以这种方式运行，im服务的配置还是需要按照常规方法来配置。

## 获取软件包
如果是社区版可以下载野火release或则会自己源码编译，得到软件压缩包```distribution-bundle-tar.tar.gz```。如果是专业版使用专业版邮件里的链接下载软件压缩包，下载后先解压一次，得到```distribution-bundle-tar.tar.gz```压缩包。

## 部署软件包
创建```/usr/local/im-server```目录，把软件包解压到这个目录下。解压后这个目录下有```bin```、```config```、```lib```等目录。
> 专业版软件包压缩了2次，先解压出```distribution-bundle-tar.tar.gz```，再拷贝到IM目录再次解压，所以注意确认目录下有bin、config等目录才对。

## 放置Server File
把```im-server.service```放到```/usr/lib/systemd/system/```目录下。

## 管理服务
* 刷新服务： ```sudo systemctl daemon-reload```，当手动安装后需要执行命令。
* 启动： ```sudo systemctl start im-server.service```。
* 停止： ```sudo systemctl stop im-server.service```。
* 重启： ```sudo systemctl restart im-server.service```。
* 查看控制台日志: ```journalctl -f -u im-server.service```。

## 日志
日志文件在```/usr/local/im-server/logs```目录下。如果需要提供日志给野火官方，请把这个目录下的日志和制台日志(```journalctl -f -u im-server.service```)一起发给野火。

也可以把日志放到```/var/log/im-server```目录下，可以修改```/usr/local/im-server/config/log4j2.xml```修改日志的路径。

## 配置
需要对IM服务配置来达到最好的执行效果，配置文件在````/usr/local/im-server/config````目录下。另外还可以设置服务的内存大小，修改```/usr/local/im-server/bin/wildfirechat.sh```文件的倒数3、4行。打开Xmx和Xms配置，设置为合适的内存大小。
