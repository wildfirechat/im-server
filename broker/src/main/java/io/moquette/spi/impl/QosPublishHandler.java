
package io.moquette.spi.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
import io.netty.channel.Channel;

abstract class QosPublishHandler {

    private static final Logger LOG = LoggerFactory.getLogger(QosPublishHandler.class);

    private final IAuthorizator m_authorizator;

    protected QosPublishHandler(IAuthorizator m_authorizator) {
        this.m_authorizator = m_authorizator;
    }

    public boolean checkWriteOnTopic(Topic topic, Channel channel) {
        String clientID = NettyUtils.clientID(channel);
        String username = NettyUtils.userName(channel);
        if (!m_authorizator.canWrite(topic, username, clientID)) {
            LOG.error(
                    "The MQTT client is not authorized to publish on topic. MqttClientId = {}, topic = {}.",
                    clientID,
                    topic);
            return true;
        }
        return false;
    }
}
