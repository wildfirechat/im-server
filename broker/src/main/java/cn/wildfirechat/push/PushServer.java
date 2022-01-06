/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.push;

import cn.wildfirechat.common.IMExceptionEvent;
import cn.wildfirechat.proto.ProtoConstants;
import com.google.gson.Gson;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.server.config.IConfig;
import io.moquette.spi.ISessionsStore;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.HttpUtils;
import win.liyufan.im.Utility;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.moquette.BrokerConstants.PUSH_ANDROID_SERVER_ADDRESS;
import static io.moquette.BrokerConstants.PUSH_IOS_SERVER_ADDRESS;
import static win.liyufan.im.HttpUtils.HttpPostType.POST_TYPE_Push;

public class PushServer {
    private static final Logger LOG = LoggerFactory.getLogger(PushServer.class);

    public interface PushMessageType {
        int PUSH_MESSAGE_TYPE_NORMAL = 0;
        int PUSH_MESSAGE_TYPE_VOIP_INVITE = 1;
        int PUSH_MESSAGE_TYPE_VOIP_BYE = 2;
        int PUSH_MESSAGE_TYPE_FRIEND_REQUEST = 3;
        int PUSH_MESSAGE_TYPE_VOIP_ANSWER = 4;
        int PUSH_MESSAGE_TYPE_RECALLED = 5;
        int PUSH_MESSAGE_TYPE_DELETED = 6;
    }

    private static PushServer INSTANCE = new PushServer();
    private ISessionsStore sessionsStore;
    private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*5);

    private String androidPushServerUrl;
    private String iOSPushServerUrl;

    private PushServer() {
    }

    public static PushServer getServer() {
        return INSTANCE;
    }

    public void init(IConfig config, ISessionsStore sessionsStore) {
        this.sessionsStore = sessionsStore;
        this.androidPushServerUrl = config.getProperty(PUSH_ANDROID_SERVER_ADDRESS);
        this.iOSPushServerUrl = config.getProperty(PUSH_IOS_SERVER_ADDRESS);
    }

    public void pushMessage(PushMessage pushMessage, String deviceId, String pushContent) {
        LOG.info("try to delivery push diviceId = {}, conversationType = {}, pushContent = {}", deviceId, pushMessage.convType, pushContent);
        executorService.execute(() ->{
                try {
                    pushMessageInternel(pushMessage, deviceId, pushContent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e, IMExceptionEvent.EventType.PUSH_SERVER_Exception);
                }
            });
    }

    private void pushMessageInternel(PushMessage pushMessage, String deviceId, String pushContent) {
        if (pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_NORMAL && StringUtil.isNullOrEmpty(pushContent)) {
            LOG.info("push content empty, deviceId {}", deviceId);
            return;
        }

        MemorySessionStore.Session session = sessionsStore.getSession(deviceId);
        int badge = session.getUnReceivedMsgs();
        if (badge <= 0) {
            badge = 1;
        }
        if (StringUtil.isNullOrEmpty(session.getDeviceToken())) {
            LOG.warn("Device token is empty for device {}", deviceId);
            return;
        }

        pushMessage.packageName = session.getAppName();
        pushMessage.pushType = session.getPushType();
        pushMessage.pushContent = pushContent;
        pushMessage.deviceToken = session.getDeviceToken();
        pushMessage.unReceivedMsg = badge;
        pushMessage.userId = session.getUsername();
        if (session.getPlatform() == ProtoConstants.Platform.Platform_iOS || session.getPlatform() == ProtoConstants.Platform.Platform_Android) {
            String url = androidPushServerUrl;
            if (session.getPlatform() == ProtoConstants.Platform.Platform_iOS) {
                url = iOSPushServerUrl;
                pushMessage.voipDeviceToken = session.getVoipDeviceToken();
            }
            HttpUtils.httpJsonPost(url, new Gson().toJson(pushMessage, pushMessage.getClass()), POST_TYPE_Push);
        } else {
            LOG.info("Not mobile platform {}", session.getPlatform());
        }
    }
}
