/*
 * Copyright (c) 2012-2018 The original author or authors
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
package io.moquette.broker;

import io.moquette.broker.security.PermitAllAuthorizatorPolicy;
import io.moquette.broker.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.broker.subscriptions.ISubscriptionsDirectory;
import io.moquette.broker.security.IAuthenticator;
import io.moquette.persistence.MemorySubscriptionsRepository;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttVersion;
import org.junit.Before;
import org.junit.Test;

import static io.moquette.broker.NettyChannelAssertions.assertEqualsConnAck;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionRegistryTest {

    static final String FAKE_CLIENT_ID = "FAKE_123";
    static final String TEST_USER = "fakeuser";
    static final String TEST_PWD = "fakepwd";

    private MQTTConnection connection;
    private EmbeddedChannel channel;
    private SessionRegistry sut;
    private MqttMessageBuilders.ConnectBuilder connMsg;
    private static final BrokerConfiguration ALLOW_ANONYMOUS_AND_ZEROBYTE_CLIENT_ID =
        new BrokerConfiguration(true, true, false);
    private MemoryQueueRepository queueRepository;

    @Before
    public void setUp() {
        connMsg = MqttMessageBuilders.connect().protocolVersion(MqttVersion.MQTT_3_1).cleanSession(true);

        createMQTTConnection(ALLOW_ANONYMOUS_AND_ZEROBYTE_CLIENT_ID);
    }

    private void createMQTTConnection(BrokerConfiguration config) {
        channel = new EmbeddedChannel();
        connection = createMQTTConnection(config, channel);
    }

    private MQTTConnection createMQTTConnection(BrokerConfiguration config, Channel channel) {
        IAuthenticator mockAuthenticator = new MockAuthenticator(singleton(FAKE_CLIENT_ID),
                                                                 singletonMap(TEST_USER, TEST_PWD));

        ISubscriptionsDirectory subscriptions = new CTrieSubscriptionDirectory();
        ISubscriptionsRepository subscriptionsRepository = new MemorySubscriptionsRepository();
        subscriptions.init(subscriptionsRepository);
        queueRepository = new MemoryQueueRepository();

        sut = new SessionRegistry(subscriptions, queueRepository);
        final PostOffice postOffice = new PostOffice(subscriptions, new PermitAllAuthorizatorPolicy(),
                                                     new MemoryRetainedRepository(), sut,
                                                     ConnectionTestUtils.NO_OBSERVERS_INTERCEPTOR);
        return new MQTTConnection(channel, config, mockAuthenticator, sut, postOffice);
    }

    @Test
    public void testConnAckContainsSessionPresentFlag() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
                                        .protocolVersion(MqttVersion.MQTT_3_1_1)
                                        .build();
        NettyUtils.clientID(channel, FAKE_CLIENT_ID);
        NettyUtils.cleanSession(channel, false);

        // Connect a first time
        sut.bindToSession(connection, msg, FAKE_CLIENT_ID);
        // disconnect
        sut.disconnect(FAKE_CLIENT_ID);

        // Exercise, reconnect
        EmbeddedChannel anotherChannel = new EmbeddedChannel();
        MQTTConnection anotherConnection = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZEROBYTE_CLIENT_ID, anotherChannel);
        sut.bindToSession(anotherConnection, msg, FAKE_CLIENT_ID);

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, anotherChannel.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open", anotherChannel.isOpen());
    }

    @Test
    public void connectWithCleanSessionUpdateClientSession() {
        // first connect with clean session true
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID).cleanSession(true).build();
        connection.processConnect(msg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, channel.readOutbound());
        connection.processDisconnect(null);
        assertFalse(channel.isOpen());

        // second connect with clean session false
        EmbeddedChannel anotherChannel = new EmbeddedChannel();
        MQTTConnection anotherConnection = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZEROBYTE_CLIENT_ID,
                                                                anotherChannel);
        MqttConnectMessage secondConnMsg = MqttMessageBuilders.connect()
            .clientId(FAKE_CLIENT_ID)
            .protocolVersion(MqttVersion.MQTT_3_1)
            .build();

        anotherConnection.processConnect(secondConnMsg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, anotherChannel.readOutbound());

        // Verify client session is clean false
        Session session = sut.retrieve(FAKE_CLIENT_ID);
        assertFalse(session.isClean());
    }
}
