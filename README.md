## 野火IM解决方案

野火IM是专业级即时通讯和实时音视频整体解决方案，由北京野火无限网络科技有限公司维护和支持。

主要特性有：私有部署安全可靠，性能强大，功能齐全，全平台支持，开源率高，部署运维简单，二次开发友好，方便与第三方系统对接或者嵌入现有系统中。详细情况请参考[在线文档](https://docs.wildfirechat.cn)。

主要包括一下项目：

| [GitHub仓库地址(主站)](https://github.com/wildfirechat)      | [码云仓库地址(镜像)](https://gitee.com/wfchat)        | 说明                                                                                      | 备注                                           |
| ------------------------------------------------------------ | ----------------------------------------------------- | ----------------------------------------------------------------------------------------- | ---------------------------------------------- |
| [android-chat](https://github.com/wildfirechat/android-chat) | [android-chat](https://gitee.com/wfchat/android-chat) | 野火IM Android SDK源码和App源码                                                           | 可以很方便地进行二次开发，或集成到现有应用当中 |
| [ios-chat](https://github.com/wildfirechat/ios-chat)         | [ios-chat](https://gitee.com/wfchat/ios-chat)         | 野火IM iOS SDK源码和App源码                                                               | 可以很方便地进行二次开发，或集成到现有应用当中 |
| [pc-chat](https://github.com/wildfirechat/pc-chat)           | [pc-chat](https://gitee.com/wfchat/pc-chat)           | 基于[Electron](https://electronjs.org/)开发的PC平台应用                                   |                                                |
| [web-chat](https://github.com/wildfirechat/web-chat)         | [web-chat](https://gitee.com/wfchat/web-chat)         | Web平台的Demo, [体验地址](http://web.wildfirechat.cn)                                     |                                                |
| [wx-chat](https://github.com/wildfirechat/wx-chat)           | [wx-chat](https://gitee.com/wfchat/wx-chat)           | 微信小程序平台的Demo                                                                      |                                                |
| [server](https://github.com/wildfirechat/server)             | [server](https://gitee.com/wfchat/server)             | IM server                                                                                 |                                                |
| [app server](https://github.com/wildfirechat/app_server)     | [app server](https://gitee.com/wfchat/app_server)     | 应用服务端                                                                                |                                                |
| [robot_server](https://github.com/wildfirechat/robot_server) | [robot_server](https://gitee.com/wfchat/robot_server) | 机器人服务端                                                                              |                                                |
| [push_server](https://github.com/wildfirechat/push_server)   | [push_server](https://gitee.com/wfchat/push_server)   | 推送服务器                                                                                |                                                |
| [docs](https://github.com/wildfirechat/docs)                 | [docs](https://gitee.com/wfchat/docs)                 | 野火IM相关文档，包含设计、概念、开发、使用说明，[在线查看](https://docs.wildfirechat.cn/) |                                                |  |


# server
本工程为野火IM 社区版IM服务软件。野火IM作为一个通用的即时通讯解决方案，既可以直接使用，也可以集成到各种应用中。


开发一套IM系统真的很艰辛，请路过的朋友们给点个star，支持我们坚持下去🙏🙏🙏🙏🙏

### 联系我们

> 商务合作请优先采用邮箱和我们联系。技术问题请到[野火IM论坛](http://bbs.wildfirechat.cn/)发帖交流

1. heavyrain.lee  邮箱: heavyrain.lee@wildfirechat.cn  微信：wildfirechat
2. imndx  邮箱: imndx@wildfirechat.cn  微信：wfchat

### 问题交流

1. 如果大家发现bug，请在GitHub提issue
2. 其他问题，请到[野火IM论坛](http://bbs.wildfirechat.cn/)进行交流学习
3. 微信公众号

<img src="http://static.wildfirechat.cn/wx_wfc_qrcode.jpg" width = 50% height = 50% />

> 强烈建议关注我们的公众号。我们有新版本发布或者有重大更新会通过公众号通知大家，另外我们也会不定期的发布一些关于野火IM的技术介绍。

#### 体验Demo
我们提供了体验demo，请使用微信扫码下载安装体验

![野火IM](http://static.wildfirechat.cn/download_qrcode.png)

#### 应用截图
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
在安装JDK1.8以上及maven的前提下，在命令行中执行```mvn clean compile package```，生成的目标文件在```./distribution/target/distribution-xxxx-bundle-tar.tar.gz```
> 由于使用了一个git的maven插件，如果本地没有git信息就会编译出错，请使用```git clone```的方法下载代码，或者下载压缩包解压后在根目录创建```.git```的空目录。建议用```git clone```的方式下载代码。

## 升级说明
1. 从0.42 版本增加了群成员数限制，默认为2000。如果您从之前的版本升级到这个版本或以后，需要注意到群成员数的限制。升级之后超出限制的群不受影响，但不能继续加人，如果您想修改默认值，可以在升级版本之后，修改t_setting表，把默认的大小改为您期望的人数。另外修改t_group表，把已经存在的群组max_member_count改成您期望的，然后重启。
2. 0.46和0.47版本升级到0.48及以后版本时，可能会提示flyway migrate 38错误，请执行 [修复脚本](https://github.com/wildfirechat/server/blob/wildfirechat/flyway_repaire_migrate_38.sql) 进行修复。0.46和0.47版本之外的版本不会出现此问题。
3. 从0.54之前版本升级到0.54及以后版本时，会提示flyway migrate错误。因为0.54版本删除了sql脚本中默认敏感词的内容，flyway checksum失败。请执行```update flyway_schema_history set checksum = 0 where script = 'V17__add_default_sensitive_word.sql';```来修复。
4. 从0.59之前的版本升级到之后的版本执行数据库升级时间比较长，请耐心等待提示运行成功，避免中途中断。

## 特别感谢
1. [moquette](https://github.com/moquette-io/moquette) 本项目是基于此项目二次开发而来，处理MQTT相关业务。
2. [loServer](https://github.com/looly/loServer) 本项目使用loServer处理HTTP相关业务。

*** 对他们表示诚挚的感谢🙏 ***

## License

1. Under the Creative Commons Attribution-NoDerivs 3.0 Unported license. See the [LICENSE](https://github.com/wildfirechat/server/blob/wildfirechat/LICENSE) file for details.
2. Under the Anti 996 License. See the [Anti 996 LICENSE](https://github.com/wildfirechat/server/blob/wildfirechat/LICENSE_996) file for details.
