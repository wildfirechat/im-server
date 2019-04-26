/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;


@Handler(IMTopic.UploadDeviceTokenTopic)
public class UploadDeviceTokenHandler extends IMHandler<WFCMessage.UploadDeviceTokenRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.UploadDeviceTokenRequest request, Qos1PublishHandler.IMCallback callback) {
            MemorySessionStore.Session session = m_sessionsStore.getSession(clientID);
            session.setPlatform(request.getPlatform());
            session.setAppName(request.getAppName());
            if (request.getPlatform() == ProtoConstants.Platform.Platform_iOS && request.getPushType() == 2) {
                session.setVoipDeviceToken(request.getDeviceToken());
                session.setPushType(0);//如果有voip token，肯定是生产环境的推送
                m_sessionsStore.updateSessionToken(session, true);
            } else {
                session.setDeviceToken(request.getDeviceToken());
                session.setPushType(request.getPushType());
                m_sessionsStore.updateSessionToken(session, false);
            }

            return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
