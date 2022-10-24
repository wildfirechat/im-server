# Linux Service 方式运行
除了命令行方式直接执行IM服务外，还可以以linux systemd service方式来运行，注意以这种方式运行，im服务的配置还是需要按照常规方法来配置。

## 获取软件包
如果是社区版可以下载野火release或则会自己源码编译，得到软件压缩包```distribution-bundle-tar.tar.gz```、```im-server.deb```和```im-server.rpm```。如果是专业版使用专业版邮件里的链接下载软件压缩包，下载后先解压一次，得到```distribution-bundle-tar.tar.gz```、```im-server.deb```和```im-server.rpm```。
> ```im-server.deb```和```im-server.rpm```文件可能带有版本号，下面使用过程中，请注意修正为实际的文件名称。

## 手动部署
### 依赖
野火IM依赖JRE1.8手动部署需要手动安装JRE1.8，确保命令:```java -version```能看到正确的java版本信息才行。

### 部署软件包
创建```/opt/im-server```目录，把软件包```distribution-bundle-tar.tar.gz```解压到这个目录下。解压后这个目录下有```bin```、```config```、```lib```、```systemd```等目录。

### 放置systemd server file
把```systemd```目录下的```im-server.service```文件放到```/usr/lib/systemd/system/```目录下。然后执行命令```sudo systemctl daemon-reload```。

### 目录结构
所有目录都在```/opt/im-server```目录下。包括日志目录、配置目录等。

### 测试
根据下面管理服务的说明，启动服务，查看控制台日志，确认启动没有异常，服务器本地执行 ```curl -v http://127.0.0.1/api/version``` 能够返回版本的JSON信息。

## 安装部署
### 依赖
安装包安装将会自动安装依赖，不需要手动安装java。如果服务器上有其他版本的Java，请注意可能的冲突问题。

### 部署软件包
可以直接安装```deb```和```rpm```格式的安装包，在debian系的linux系统（Ubuntu等使用```apt```命令安装软件的系统）中，使用命令：
```shell
sudo apt install ./im-server.deb
```

在红帽系的linux系统（Centos等使用```yum```命令安装软件的系统）中，使用命令:
```shell
sudo yum install ./im-server.rpm
```

注意在上述两个命令中，都使用的是本地安装，注意安装包名前的```./```路径。如果使用```dpkg -i ./im-server.deb```命令将不会安装依赖。 

### 目录结构
* /etc/im-server/config     配置文件目录
* /opt/im-server            程序目录
* /var/log/im-server        日志目录
* /var/lib/im-server/h2db   H2数据库目录，如果使用mysql则不会使用
* /var/lib/im-server/media  内置对象存储数据目录，如果使用非内置，则目录不会使用。


### 测试
根据下面管理服务的说明，启动服务，查看控制台日志，确认启动没有异常，服务器本地执行 ```curl -v http://127.0.0.1/api/version``` 能够返回版本的JSON信息。

## 管理服务
* 刷新配置，当安装或者更新后需要执行： ```sudo systemctl daemon-reload```
* 启动服务： ```sudo systemctl start im-server```
* 停止服务： ```sudo systemctl stop im-server```
* 重启服务： ```sudo systemctl restart im-server```
* 查看服务状态：```sudo systemctl status im-server```
* 设置开机自启动：```sudo systemctl enable im-server```
* 禁止开机自启动：```sudo systemctl disable im-server```
* 查看控制台日志: ```journalctl -f -u im-server```

## 修改服务内存大小
修改```/opt/im-server/bin/wildfirechat.sh```文件的倒数3、4行。打开Xmx和Xms配置，设置为合适的内存大小。

## 配置
需要对IM服务配置来达到最好的执行效果，手动部署配置文件在````/opt/im-server/config````目录下，安装部署的配置文件在````/etc/im-server/config````目录下。

## 日志
手动部署的日志文件在```/opt/im-server/logs```目录下，安装部署的日志在```/var/log/im-server```目录下。如果需要提供日志给野火官方，请把这个目录下的日志和制台日志(```journalctl -f -u im-server```)一起发给野火。

手动部署也可以修改目录，可以修改配置文件目录下的```config/log4j2.xml```修改日志的路径。

