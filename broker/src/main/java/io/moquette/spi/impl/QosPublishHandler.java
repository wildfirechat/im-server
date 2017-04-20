/*
 * Copyright (c) 2012-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.moquette.spi.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
import io.netty.channel.Channel;

abstract class QosPublishHandler {

    private static final Logger LOG = LoggerFactory.getLogger(QosPublishHandler.class);

    protected final IAuthorizator m_authorizator;

    protected QosPublishHandler(IAuthorizator m_authorizator) {
        this.m_authorizator = m_authorizator;
    }

    public boolean checkWriteOnTopic(Topic topic, Channel channel) {
        String clientID = NettyUtils.clientID(channel);
        String username = NettyUtils.userName(channel);
        if (!m_authorizator.canWrite(topic, username, clientID)) {
            LOG.error("MQTT client is not authorized to publish on topic. CId={}, topic={}", clientID, topic);
            return true;
        }
        return false;
    }
}
