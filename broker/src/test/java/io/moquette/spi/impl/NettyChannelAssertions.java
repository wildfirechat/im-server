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

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Some useful assertions used by Netty's EmbeddedChannel in tests.
 */
final class NettyChannelAssertions {

    static void assertEqualsConnAck(MqttConnectReturnCode expectedCode, Object connAck) {
        assertEqualsConnAck(null, expectedCode, connAck);
    }

    static void assertEqualsConnAck(String msg, MqttConnectReturnCode expectedCode, Object connAck) {
        assertTrue("connAck is not an instance of ConnAckMessage", connAck instanceof MqttConnAckMessage);
        MqttConnAckMessage connAckMsg = (MqttConnAckMessage) connAck;

        if (msg == null)
            assertEquals(expectedCode, connAckMsg.variableHeader().connectReturnCode());
        else
            assertEquals(msg, expectedCode, connAckMsg.variableHeader().connectReturnCode());
    }

    static void assertConnAckAccepted(EmbeddedChannel channel) {
        channel.flush();
        assertEqualsConnAck(CONNECTION_ACCEPTED, channel.readOutbound());
    }

    static void assertEqualsSubAck(/* byte expectedCode, */ Object subAck) {
        assertTrue(subAck instanceof MqttSubAckMessage);
        // SubAckMessage connAckMsg = (SubAckMessage) connAck;
        // assertEquals(expectedCode, connAckMsg.getReturnCode());
    }

    private NettyChannelAssertions() {
    }
}
