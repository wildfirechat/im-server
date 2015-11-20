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
package io.moquette.server;

import io.moquette.server.config.MemoryConfig;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import io.moquette.server.config.IConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;


/**
 * Integration test to check the function of Moquette with a WebSocket channel.
 * 
 * @author andrea
 */
public class ServerIntegrationWebSocketTest {
    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationWebSocketTest.class);

    Server m_server;
    WebSocketClient client;
    IConfig m_config;

    protected void startServer() throws IOException {
        m_server = new Server();
        final Properties configProps = IntegrationUtils.prepareTestPropeties();
        m_config = new MemoryConfig(configProps);
        m_server.startServer(m_config);
    }

    @Before
    public void setUp() throws Exception {
        startServer();
        client = new WebSocketClient();
    }

    @After
    public void tearDown() throws Exception {
        client.stop();
        
        m_server.stopServer();
        IntegrationUtils.cleanPersistenceFile(m_config);
    }
    
    @Test
    public void checkPlainConnect() throws Exception {
        LOG.info("*** checkPlainConnect ***");
        String destUri = "ws://localhost:" + io.moquette.commons.Constants.WEBSOCKET_PORT;
        
        MQTTWebSocket socket = new MQTTWebSocket();
        client.start();
        URI echoUri = new URI(destUri);
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        client.connect(socket, echoUri, request);
        LOG.info("Connecting to : %s", echoUri);
        boolean connected = socket.awaitConnected(4, TimeUnit.SECONDS);
        
        assertTrue(connected);
    }
}
