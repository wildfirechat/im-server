/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import com.hazelcast.core.Member;
import io.moquette.spi.ClientSession;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

import static io.moquette.BrokerConstants.HZ_Cluster_Node_External_IP;
import static io.moquette.BrokerConstants.HZ_Cluster_Node_External_Long_Port;

@Handler(IMTopic.RouteTopic)
public class RouteHandler extends IMHandler<WFCMessage.RouteRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.RouteRequest request, Qos1PublishHandler.IMCallback callback) {
        Member member = mServer.getHazelcastInstance().getCluster().getLocalMember();
        String serverIp = member.getStringAttribute(HZ_Cluster_Node_External_IP);
        String longPort = member.getStringAttribute(HZ_Cluster_Node_External_Long_Port);

        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);
        boolean isSessionAlreadyStored = clientSession != null;
        if (!isSessionAlreadyStored) {
            m_sessionsStore.loadActiveSession(fromUser, clientID);
        } else {
            m_sessionsStore.updateExistSession(fromUser, clientID, request, true);
        }

        WFCMessage.RouteResponse response = WFCMessage.RouteResponse.newBuilder().setHost(serverIp).setLongPort(Integer.parseInt(longPort)).setShortPort(80).build();

        byte[] data = response.toByteArray();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
