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
import com.hazelcast.core.Member;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.spi.ClientSession;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

@Handler(IMTopic.RouteTopic)
public class RouteHandler extends IMHandler<WFCMessage.RouteRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, ProtoConstants.RequestSourceType requestSourceType, WFCMessage.RouteRequest request, Qos1PublishHandler.IMCallback callback) {
        MemorySessionStore.Session session = m_sessionsStore.sessionForClientAndUser(fromUser, clientID);
        if (session == null) {
            ErrorCode errorCode = m_sessionsStore.loadActiveSession(fromUser, clientID);
            if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                return errorCode;
            }
            session = m_sessionsStore.sessionForClientAndUser(fromUser, clientID);
        }

        if (session == null || session.getDeleted() > 0) {
            if(session == null) {
                LOG.error("Session for <{}, {}> not exist", fromUser, clientID);
            } else {
                LOG.error("Session for <{}, {}> deleted", fromUser, clientID);
            }
            return ErrorCode.ERROR_CODE_SECRECT_KEY_MISMATCH;
        }

        String serverIp = mServer.getServerIp();
        String longPort = mServer.getLongPort();
        String shortPort = mServer.getShortPort();

        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);
        boolean isSessionAlreadyStored = clientSession != null;
        if (!isSessionAlreadyStored) {
            m_sessionsStore.loadActiveSession(fromUser, clientID);
        } else {
            m_sessionsStore.updateExistSession(fromUser, clientID, request, true);
        }

        WFCMessage.RouteResponse response = WFCMessage.RouteResponse.newBuilder().setHost(serverIp).setLongPort(Integer.parseInt(longPort)).setShortPort(Integer.parseInt(shortPort)).build();

        byte[] data = response.toByteArray();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
