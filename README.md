# 野火IM解决方案
野火IM是专业级的即时通讯和实时音视频整体解决方案，由北京野火无限网络科技有限公司维护和支持。

## 功能特性
* 极致地硬件利用率，IM服务最低128M内存即可运行，上不封顶。
* 协议先进，采用MQTT+Protobuf组合，流量和性能极致优化。
* 性能强大，专业版IM服务支持百万在线和集群部署。
* 部署运维简单，依赖服务少，稍加配置一键启动。
* 安全加密。网络连接AES加密。客户端数据库SqlCipher加密。安全无小事。
* 全平台客户端，四端同时在线（移动端，pc端，web端和小程序端），数据和状态多端完美同步。
* 支持国产化。支持国产化操作系统、国产化芯片和国产操作系统。支持国密加密。
* 客户端使用微信[mars](https://github.com/tencent/mars)连接库，野火IM可能是最适应中国网络国情的即时通讯服务。
* 支持加速点加速，即可用于全球应用，也可用于政企内外双网复杂的网络环境。
* 支持阅读回执和在线状态功能，适用于办公环境。
* 音视频多种解决方案，丰俭由人，可自由选择。
* 高级音视频功能强大，支持9人以上的群组视频通话，支持1080P视频，支持会议模式，支持百人以上会议，支持服务器端录制。
* 全私有部署，可不依赖任何第三方服务，完全内网部署。
* 功能齐全，涵盖所有常见即时通讯功能。另外具有强大的可扩展能力。Demo成熟完善，基本可以做到开箱即用，也可把SDK嵌入其它应用。
* 拥有应用开放平台，可以开发和创建自建应用，扩展您的工作台。
* API丰富，方便与其它服务系统的对接。
* 拥有机器人和频道功能。

## 野火开源项目
主要包括以下项目：

| [GitHub仓库地址](https://github.com/wildfirechat)      | [码云仓库地址](https://gitee.com/wfchat)        | 说明                                                                                      
| ------------------------------------------------------------ | ----------------------------------------------------- | --------------------------------------------------------------------------
| [im-server](https://github.com/wildfirechat/im-server)       | [im-server](https://gitee.com/wfchat/im-server)          | 野火社区版IM服务，野火IM的核心服务，处理所有IM相关业务。                                                                                 |
| [app_server](https://github.com/wildfirechat/app_server)     | [app_server](https://gitee.com/wfchat/app_server)     | Demo应用服务，模拟客户的应用服登陆处理逻辑及部分二次开发示例。                                                                                |  
| [robot_server](https://github.com/wildfirechat/robot_server) | [robot_server](https://gitee.com/wfchat/robot_server) | 机器人服务，演示野火机器人对接其它系统的方法。                                                                              |
| [push_server](https://github.com/wildfirechat/push_server)   | [push_server](https://gitee.com/wfchat/push_server)   | 推送服务器，可以对接所有的系统厂商推送服务或者第三方推送服务。                                                                                |
| [wf-minio](https://github.com/wildfirechat/WF-minio)   | [wf-minio](https://gitee.com/wfchat/WF-minio)   | 私有对象存储服务，用来支持野火IM专业版的文件存储。                                                                                |
| [wf-janus](https://github.com/wildfirechat/wf-janus  )   | [wf-janus](https://gitee.com/wfchat/wf-janus  )   | 高级音视频媒体服务。                                                                                |
| [open-platform](https://github.com/wildfirechat/open-platform)   | [open-platform](https://gitee.com/wfchat/open-platform)   | 野火开放平台服务。                                                                                |
| [daily-report](https://github.com/wildfirechat/daily-report)   | [daily-report](https://github.com/wildfirechat/daily-report)   | 野火开放平台日报 demo服务。                                                                                |
| [android-chat](https://github.com/wildfirechat/android-chat) | [android-chat](https://gitee.com/wfchat/android-chat) | 野火IM Android SDK源码和App源码。
| [ios-chat](https://github.com/wildfirechat/ios-chat)         | [ios-chat](https://gitee.com/wfchat/ios-chat)         | 野火IM iOS SDK源码和App源码。
| [pc-chat](https://github.com/wildfirechat/vue-pc-chat)       | [pc-chat](https://gitee.com/wfchat/vue-pc-chat)       | 基于[Electron](https://electronjs.org/)的PC 端，支持Windows、Mac、Linux（包括国产化linux系统和CPU）。                                       |
| [web-chat](https://github.com/wildfirechat/vue-chat)         | [web-chat](https://gitee.com/wfchat/vue-chat)         | 野火IM Web 端, [体验地址](https://web.wildfirechat.cn)                                     |
| [wx-chat](https://github.com/wildfirechat/wx-chat)           | [wx-chat](https://gitee.com/wfchat/wx-chat)           | 小程序平台的Demo(支持微信、百度、阿里、字节、QQ 等小程序平台)                             |  
| [uni-chat](https://github.com/wildfirechat/uni-chat)           | [uni-chat](https://gitee.com/wfchat/uni-chat)           | UniApp平台移动端应用(使用原生插件)                           |  
| [uni-wfc-client](https://github.com/wildfirechat/uni-wfc-client)           | [uni-wfc-client](https://gitee.com/wfchat/uni-wfc-client)           | UniApp平台原生插件                           |  
| [flutter_imclient](https://github.com/wildfirechat/flutter_imclient)           | [flutter_imclient](https://gitee.com/wfchat/flutter_imclient)           | Flutter平台原生插件                           |  
| [docs](https://github.com/wildfirechat/docs)                 | [docs](https://gitee.com/wfchat/docs)                 | 野火IM相关文档，包含设计、概念、开发、使用说明，[在线查看](https://docs.wildfirechat.cn/) |

## 野火开发文档
[在线文档](https://docs.wildfirechat.cn/)

## 野火Demo
请使用微信扫码下载安装体验野火IM移动客户端

![野火IM](http://static.wildfirechat.cn/download_qrcode.png)

Web客户端点击[这里](https://web.wildfirechat.cn)

PC客户端点[这里](https://github.com/wildfirechat/vue-pc-chat/releases)下载安装。

小程序客户端请用微信扫码

![野火IM](http://static.wildfirechat.net/wx.jpg)


## 应用截图
![ios-demo](http://static.wildfirechat.cn/ios-demo.gif)

<img src="http://static.wildfirechat.cn/ios-message-view.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/ios-contact-view.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/ios-discover-view.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/ios-settings-view.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/ios-messagelist-view.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/ios-chat-setting-view.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/ios-takephoto-view.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/ios-record-voice-view.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/ios-location-view.png" width = 50% height = 50% />

<img src="http://static.wildfirechat.cn/ios-voip-view.png" width = 50% height = 50% />


## 编译
在安装JDK1.8以上及maven的前提下，在命令行中执行```mvn clean package```，生成的目标文件在```./distribution/target/distribution-xxxx-bundle-tar.tar.gz```
> 由于使用了一个git的maven插件，如果本地没有git信息就会编译出错，请使用```git clone```的方法下载代码，或者下载压缩包解压后在根目录创建```.git```的空目录。建议用```git clone```的方式下载代码。

## 配置
解压```distribution-xxxx-bundle-tar.tar.gz```，修改解压出来的```config```目录下的```wildfirechat.conf```。修改如下部分：
1. 修改```server.ip```为您的服务器的公网IP，或者域名。如果用域名需要做好域名解析。
2. 可以阅读配置文件注释和文档，对其它配置项进行调整。

## 运行
在程序目录运行如下命令：
```
./bin/wildfirechat.sh
```

## 验证
1. 在浏览器中输入地址 ```http://${ip}/api/version```可以看到返回一个json文件。
2. 部署[应用服务](应用服务)，配置和编译[Android客户端](https://github.com/wildfirechat/android-chat)和[iOS客户端](https://github.com/wildfirechat/ios-chat)进行验证。详情可参考[快速开始](https://docs.wildfirechat.cn/quick_start/)。

## 升级说明
1. 从0.42 版本增加了群成员数限制，默认为2000。如果您从之前的版本升级到这个版本或以后，需要注意到群成员数的限制。升级之后超出限制的群不受影响，但不能继续加人，如果您想修改默认值，可以在升级版本之后，修改t_setting表，把默认的大小改为您期望的人数。另外修改t_group表，把已经存在的群组max_member_count改成您期望的，然后重启。
2. 0.46和0.47版本升级到0.48及以后版本时，可能会提示flyway migrate 38错误，请执行 [修复脚本](https://github.com/wildfirechat/server/blob/wildfirechat/flyway_repaire_migrate_38.sql) 进行修复。0.46和0.47版本之外的版本不会出现此问题。
3. 从0.54之前版本升级到0.54及以后版本时，会提示flyway migrate错误。因为0.54版本删除了sql脚本中默认敏感词的内容，flyway checksum失败。请执行```update flyway_schema_history set checksum = 0 where script = 'V17__add_default_sensitive_word.sql';```来修复。
4. 从0.59之前的版本升级到之后的版本执行数据库升级时间比较长，请耐心等待提示运行成功，避免中途中断。


## 联系我们

商务合作请使用如下邮箱和微信联系：

邮箱: support@wildfirechat.cn  微信1：wfchat 微信2：wildfirechat

## 问题交流

1. 如果大家发现bug，请在GitHub提issue；如果有需求也请给我们提issue。
2. 其他问题，请到[野火IM论坛](http://bbs.wildfirechat.cn/)进行交流学习
3. 关注我们的公众号。我们有新版本发布或者有重大更新会通过公众号通知大家，另外我们也会不定期的发布一些关于野火IM的技术介绍。

<img src="http://static.wildfirechat.cn/wx_wfc_qrcode.jpg" width = 50% height = 50% />

> 我们有研发工程师轮流值班处理issue和论坛，一般简单问题几个小时就会回复一遍，疑难Bug的修改和新需求的开发我们也会尽快解决。

## 特别感谢
1. [moquette](https://github.com/moquette-io/moquette) 本项目是基于此项目二次开发而来，处理MQTT相关业务。
2. [loServer](https://github.com/looly/loServer) 本项目使用loServer处理HTTP相关业务。

*** 对他们表示诚挚的感谢🙏 ***

## License

1. Under the Creative Commons Attribution-NoDerivs 3.0 Unported license. See the [LICENSE](https://github.com/wildfirechat/server/blob/wildfirechat/LICENSE) file for details.
