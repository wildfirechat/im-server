## 野火IM解决方案

野火IM是一套跨平台、全开源的即时通讯解决方案，主要包含以下内容。

| 仓库                                                         | 说明                                                    | 备注                                           |
| ------------------------------------------------------------ | ------------------------------------------------------- | ---------------------------------------------- |
| [android-chat](https://github.com/wildfirechat/android-chat) | 野火IM Android SDK源码和App源码                                      | 可以很方便地进行二次开发，或集成到现有应用当中 |
| [ios-chat](https://github.com/wildfirechat/ios-chat)         | 野火IM iOS SDK源码和App源码                                          | 可以很方便地进行二次开发，或集成到现有应用当中 |
| [pc-chat](https://github.com/wildfirechat/pc-chat)           | 基于[Electron](https://electronjs.org/)开发的PC平台应用 |                                                |
| [proto](https://github.com/wildfirechat/proto)               | 野火IM的协议栈实现                                      |                                                |
| [server](https://github.com/wildfirechat/server)             | IM server                                               |                                                |
| [app server](https://github.com/wildfirechat/app_server)     | 应用服务端                                              |                                                |
| [robot_server](https://github.com/wildfirechat/robot_server) | 机器人服务端                                            |                                                |
| [push_server](https://github.com/wildfirechat/push_server)   | 推送服务器                                              |                                                |
| [docs](https://github.com/wildfirechat/docs)                 | 野火IM相关文档，包含设计、概念、开发、使用说明          |                                                |

# server
本工程为野火IM 社区版IM服务软件。野火IM作为一个通用的即时通讯解决方案，可以集成到各种应用中。请阅读[docs](http://docs.wildfirechat.cn)或下载服务器[发布版本](https://github.com/wildfirechat/server/releases)


开发一套IM系统真的很艰辛，请路过的朋友们给点个star，支持我们坚持下去🙏🙏🙏🙏🙏


### 联系我们
问题讨论请加群：822762829

<img src="http://static.wildfirechat.cn/qr_qqgroup.jpeg" width = 50% height = 50% />

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

## 特别感谢
1. [moquette](https://github.com/moquette-io/moquette) 本项目是基于此项目二次开发而来，处理MQTT相关业务。
2. [loServer](https://github.com/looly/loServer) 本项目使用loServer处理HTTP相关业务。

*** 对他们表示诚挚的感谢🙏 ***

## License

1. Under the Creative Commons Attribution-NoDerivs 3.0 Unported license. See the [LICENSE](https://github.com/wildfirechat/server/blob/wildfirechat/LICENSE) file for details.
2. Under the Anti 996 License. See the [Anti 996 LICENSE](https://github.com/wildfirechat/server/blob/wildfirechat/LICENSE_996) file for details.
