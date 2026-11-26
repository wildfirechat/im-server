# 野火IM Server Java SDK

野火IM Server Java SDK 是野火IM服务器的官方 Java 语言 SDK，提供了完整的 Admin API、Robot API 和 Channel API 接口封装。

## 功能特性

- **用户管理**：创建用户、获取用户信息、更新用户信息、封禁/解封用户等
- **群组管理**：创建群组、添加/移除成员、转让群主、设置管理员等
- **消息管理**：发送消息、广播消息、撤回消息、删除消息等
- **好友关系**：添加好友、删除好友、获取好友列表、设置黑名单等
- **聊天室管理**：创建聊天室、获取聊天室信息、销毁聊天室等
- **频道管理**：创建频道、订阅/取消订阅、发送频道消息等
- **机器人服务**：机器人消息收发、用户信息查询、群组管理等
- **会议管理**：创建会议、查询会议信息、管理参会者等（高级音视频功能）
- **朋友圈功能**：发布动态、评论、点赞等（需启用朋友圈功能）
- **敏感词管理**：添加/删除敏感词、检查敏感词等

## 安装

### Maven 依赖

```xml
<dependencies>
    <!-- 必需依赖 -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.8.9</version>
    </dependency>
    <dependency>
        <groupId>commons-httpclient</groupId>
        <artifactId>commons-httpclient</artifactId>
        <version>3.1</version>
    </dependency>
    <dependency>
        <groupId>org.apache.httpcomponents</groupId>
        <artifactId>httpclient</artifactId>
        <version>4.5.13</version>
    </dependency>
    <dependency>
        <groupId>commons-codec</groupId>
        <artifactId>commons-codec</artifactId>
        <version>1.15</version>
    </dependency>
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>2.5.0</version>
    </dependency>
    <dependency>
        <groupId>com.googlecode.json-simple</groupId>
        <artifactId>json-simple</artifactId>
        <version>1.1.1</version>
    </dependency>
    
    <!-- SDK JAR (本地路径根据实际情况修改) -->
    <dependency>
        <groupId>cn.wildfirechat</groupId>
        <artifactId>sdk</artifactId>
        <version>1.2.2</version>
        <scope>system</scope>
        <systemPath>${project.basedir}/lib/sdk.jar</systemPath>
    </dependency>
    <dependency>
        <groupId>cn.wildfirechat</groupId>
        <artifactId>common</artifactId>
        <version>1.2.2</version>
        <scope>system</scope>
        <systemPath>${project.basedir}/lib/common.jar</systemPath>
    </dependency>
</dependencies>

<!-- Spring Boot 项目需要添加 includeSystemScope -->
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <includeSystemScope>true</includeSystemScope>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### SDK 获取

1. 从源码编译：本项目位于 `server/sdk` 目录
2. 从发布包获取：在 release 包的 `server_sdk` 目录中

## 快速开始

### Admin 管理 API

```java
import cn.wildfirechat.sdk.AdminConfig;
import cn.wildfirechat.sdk.UserAdmin;
import cn.wildfirechat.sdk.MessageAdmin;
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.Conversation;
import cn.wildfirechat.sdk.messagecontent.TextMessageContent;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.sdk.model.IMResult;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // 使用与 IM 服务相同的密钥；通过环境变量或密钥管理器注入，不要提交到代码仓库。
        String adminSecret = System.getenv("WILDFIRECHAT_ADMIN_SECRET_KEY");
        if (adminSecret == null || adminSecret.trim().isEmpty()) {
            throw new IllegalStateException("WILDFIRECHAT_ADMIN_SECRET_KEY is required");
        }
        AdminConfig.initAdmin("http://localhost:18080", adminSecret);
        
        // 创建用户
        InputOutputUserInfo user = new InputOutputUserInfo();
        user.setUserId("user1");
        user.setName("user1");
        user.setDisplayName("Test User");
        user.setMobile("13900000000");
        
        IMResult<OutputCreateUser> result = UserAdmin.createUser(user);
        if (result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("创建用户成功: " + result.getResult().getUserId());
        }
        
        // 获取用户 Token
        IMResult<OutputGetIMTokenData> tokenResult = UserAdmin.getUserToken(
            "user1", 
            "client1", 
            ProtoConstants.Platform.Platform_Android
        );
        if (tokenResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Token: " + tokenResult.getResult().getToken());
        }
        
        // 发送消息
        Conversation conversation = new Conversation();
        conversation.setType(ProtoConstants.ConversationType.ConversationType_Private);
        conversation.setTarget("user2");
        conversation.setLine(0);
        
        TextMessageContent content = new TextMessageContent("Hello, World!");
        MessagePayload payload = content.encode();
        
        IMResult<SendMessageResult> msgResult = MessageAdmin.sendMessage("user1", conversation, payload);
        if (msgResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("消息发送成功，ID: " + msgResult.getResult().getMessageUid());
        }
    }
}
```

### Robot 机器人服务

```java
import cn.wildfirechat.sdk.RobotService;
import cn.wildfirechat.sdk.messagecontent.TextMessageContent;

public class RobotExample {
    public static void main(String[] args) throws Exception {
        // 初始化机器人服务
        RobotService robot = new RobotService("http://localhost", "robot1", "123456");
        
        // 获取机器人信息
        IMResult<OutputRobot> profile = robot.getProfile();
        if (profile.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("机器人名称: " + profile.getResult().getDisplayName());
        }
        
        // 发送消息给用户
        TextMessageContent content = new TextMessageContent("Hello from robot!");
        IMResult<SendMessageResult> result = robot.sendMessage("user1", content);
        if (result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("机器人消息发送成功");
        }
        
        // 获取用户信息
        IMResult<OutputUserInfo> userInfo = robot.getUserInfo("user1");
        if (userInfo.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("用户显示名: " + userInfo.getResult().getDisplayName());
        }
    }
}
```

### Channel 频道服务

```java
import cn.wildfirechat.sdk.ChannelServiceApi;
import cn.wildfirechat.sdk.messagecontent.TextMessageContent;

public class ChannelExample {
    public static void main(String[] args) throws Exception {
        // 初始化频道服务
        ChannelServiceApi channel = new ChannelServiceApi("http://localhost", "channel1", "secret");
        
        // 发送广播消息给订阅者
        TextMessageContent content = new TextMessageContent("Channel broadcast message");
        IMResult<SendMessageResult> result = channel.broadcastMessage(content);
        if (result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("频道广播消息发送成功");
        }
        
        // 用户订阅频道
        IMResult<Void> subscribeResult = channel.subscribe("user1");
        if (subscribeResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("订阅成功");
        }
        
        // 获取频道订阅者（商业版功能）
        IMResult<OutputStringList> subscribers = channel.getSubscriberList();
        if (subscribers.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("订阅者数量: " + subscribers.getResult().getList().size());
        }
    }
}
```

## 运行测试

SDK 提供了完整的测试示例 `Main.java`：

```bash
# 编译
mvn compile

# 运行测试（使用默认配置）
mvn exec:java -Dexec.mainClass="cn.wildfirechat.sdk.Main"

# 设置为与 IM 服务相同的管理 API 密钥
export WILDFIRECHAT_ADMIN_SECRET_KEY='<the same secret configured on the server>'

# 运行测试（自定义配置）
mvn exec:java -Dexec.mainClass="cn.wildfirechat.sdk.Main" \
    -Dexec.args="http://your-server:18080 http://your-server false false"
```

### 命令行参数

```
Usage: java -jar sdk.jar adminUrl imUrl commercialServer advanceVoip

参数说明:
  adminUrl         - 管理API地址，如 http://localhost:18080
  imUrl            - IM服务地址（机器人/频道使用），如 http://localhost
  commercialServer - 是否为商业版服务器 (true/false)
  advanceVoip      - 是否启用高级音视频功能 (true/false)
```

## 项目结构

```
sdk/
├── src/main/java/cn/wildfirechat/
│   ├── sdk/
│   │   ├── AdminConfig.java          # 配置类
│   │   ├── UserAdmin.java            # 用户管理
│   │   ├── GroupAdmin.java           # 群组管理
│   │   ├── MessageAdmin.java         # 消息管理
│   │   ├── RelationAdmin.java        # 好友关系管理
│   │   ├── ChatroomAdmin.java        # 聊天室管理
│   │   ├── ChannelAdmin.java         # 频道管理
│   │   ├── GeneralAdmin.java         # 通用管理
│   │   ├── SensitiveAdmin.java       # 敏感词管理
│   │   ├── ConferenceAdmin.java      # 会议管理
│   │   ├── MomentsAdmin.java         # 朋友圈管理
│   │   ├── RobotService.java         # 机器人服务
│   │   ├── ChannelServiceApi.java    # 频道服务API
│   │   ├── MeshAdmin.java            # Mesh分布式管理
│   │   └── Main.java                 # 测试主程序
│   │   └── messagecontent/           # 消息内容类
│   │       ├── MessageContent.java
│   │       ├── TextMessageContent.java
│   │       ├── ImageMessageContent.java
│   │       ├── SoundMessageContent.java
│   │       ├── VideoMessageContent.java
│   │       ├── FileMessageContent.java
│   │       └── ...
│   ├── common/                       # 公共模块（依赖）
│   └── proto/                        # 协议常量
└── pom.xml                           # Maven配置
```

## 主要 API 说明

### 用户管理 (UserAdmin)

| 方法 | 说明 |
|------|------|
| `createUser()` | 创建用户 |
| `getUserByName()` | 按用户名获取用户 |
| `getUserByMobile()` | 按手机号获取用户 |
| `getUserByUserId()` | 按用户ID获取用户 |
| `updateUserInfo()` | 更新用户信息 |
| `getUserToken()` | 获取用户IM Token |
| `updateUserBlockStatus()` | 封禁/解封用户 |
| `checkUserOnlineStatus()` | 检查用户在线状态 |

### 群组管理 (GroupAdmin)

| 方法 | 说明 |
|------|------|
| `createGroup()` | 创建群组 |
| `getGroupInfo()` | 获取群组信息 |
| `addGroupMembers()` | 添加群成员 |
| `kickoffGroupMembers()` | 踢出群成员 |
| `transferGroup()` | 转让群主 |
| `dismissGroup()` | 解散群组 |

### 消息管理 (MessageAdmin)

| 方法 | 说明 |
|------|------|
| `sendMessage()` | 发送消息 |
| `broadcastMessage()` | 广播消息 |
| `multicastMessage()` | 群发消息 |
| `recallMessage()` | 撤回消息 |
| `deleteMessage()` | 删除消息 |

### 好友关系 (RelationAdmin)

| 方法 | 说明 |
|------|------|
| `setUserFriend()` | 设置好友关系 |
| `getFriendList()` | 获取好友列表 |
| `setUserBlacklist()` | 设置黑名单 |
| `getUserBlacklist()` | 获取黑名单列表 |

## 接口文档

- [Admin API 文档](https://docs.wildfirechat.cn/server/admin_api/) - 服务端管理接口，包括用户、群组、消息、频道等管理功能
- [Robot API 文档](https://docs.wildfirechat.cn/server/robot_api/) - 机器人服务接口，用于开发机器人应用
- [Channel API 文档](https://docs.wildfirechat.cn/server/channel_api/) - 频道服务接口，用于开发公众号/频道应用
- [IM 开发文档](https://docs.wildfirechat.cn/) - 野火IM完整开发文档

## 其他语言 SDK

| 语言 | GitHub                                                           | Gitee(码云) |
|------|------------------------------------------------------------------|-------------|
| Java SDK | [GitHub](https://github.com/wildfirechat/server/tree/master/sdk) | [Gitee](https://gitee.com/wfchat/im-server/tree/wildfirechat/sdk) |
| Python SDK | [GitHub](https://github.com/wildfirechat/server-sdk-python)  | [Gitee](https://gitee.com/wfchat/im-server/server-sdk-python) |
| Go SDK | [GitHub](https://github.com/wildfirechat/server-sdk-go)          | [Gitee](https://gitee.com/wfchat/server-sdk-go) |
| Node.js SDK | [GitHub](https://github.com/wildfirechat/server-sdk.js)          | [Gitee](https://gitee.com/wfchat/server-sdk.js) |

## 相关链接

- [野火IM 官网](https://wildfirechat.cn/)
- [在线文档](https://docs.wildfirechat.cn/)
- [服务端源码 - GitHub](https://github.com/wildfirechat/server) | [Gitee](https://gitee.com/wfchat/im-server)
- [应用服务源码 - GitHub](https://github.com/wildfirechat/app-server) | [Gitee](https://gitee.com/wfchat/app-server)
- [机器人服务源码 - GitHub](https://github.com/wildfirechat/robot-server) | [Gitee](https://gitee.com/wfchat/robot-server)
- [推送服务源码 - GitHub](https://github.com/wildfirechat/push-server) | [Gitee](https://gitee.com/wfchat/push-server)
- [论坛交流](https://bbs.wildfirechat.cn/)

## 注意事项

1. **Admin API** 使用 18080 端口，具有超级管理权限，理论上不应对外开放，也不应让非内部服务知悉密钥。
2. **Robot API** 和 **Channel API** 使用 IM 服务的公开端口（默认 80），第三方可以使用机器人或频道与 IM 系统进行对接。
3. 商业版服务器支持更多高级功能，如在线用户统计、设备管理等。
4. 高级音视频功能需要单独启用，支持会议管理相关 API。

## 许可证

本项目采用与野火IM相同的许可证。
