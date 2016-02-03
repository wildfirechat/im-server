/*
 * Copyright (c) 2012-2015 The original author or authors
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

import io.moquette.parser.proto.messages.ConnAckMessage;
import io.moquette.parser.proto.messages.SubAckMessage;
import io.netty.channel.embedded.EmbeddedChannel;

import static io.moquette.parser.proto.messages.ConnAckMessage.CONNECTION_ACCEPTED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Some useful assertions used by Netty's EmbeddedChannel in tests.
 */
class NettyChannelAssertions {

    static void assertEqualsConnAck(byte expectedCode, Object connAck) {
        assertTrue(connAck instanceof ConnAckMessage);
        ConnAckMessage connAckMsg = (ConnAckMessage) connAck;
        assertEquals(expectedCode, connAckMsg.getReturnCode());
    }

    static void assertConnAckAccepted(EmbeddedChannel channel) {
        channel.flush();
        assertEqualsConnAck(CONNECTION_ACCEPTED, channel.readOutbound());
    }

    static void assertEqualsSubAck(/*byte expectedCode,*/ Object subAck) {
        assertTrue(subAck instanceof SubAckMessage);
        //SubAckMessage connAckMsg = (SubAckMessage) connAck;
        //assertEquals(expectedCode, connAckMsg.getReturnCode());
    }

}
