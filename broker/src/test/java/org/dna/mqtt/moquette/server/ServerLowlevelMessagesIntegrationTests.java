/*
 * Copyright (c) 2012-2014 The original author or authors
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
package org.dna.mqtt.moquette.server;

import java.io.File;
import java.io.IOException;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.testclient.Client;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class ServerLowlevelMessagesIntegrationTests {
    Server m_server;
    Client m_client;
    
    protected void startServer() throws IOException {
        m_server = new Server();
        m_server.startServer();
    }

    @Before
    public void setUp() throws Exception {
        startServer();
        m_client = new Client("localhost");
    }

    @After
    public void tearDown() throws Exception {
        m_client.close();
        
        m_server.stopServer();
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    public void elapseKeepAliveTime() throws InterruptedException {
        int keepAlive = 2; //secs
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setProcotolVersion((byte)3);
        connectMessage.setClientID("FAKECLNT");
        connectMessage.setKeepAlive(keepAlive);
        m_client.sendMessage(connectMessage);
        
        //wait 2 times the keepAlive
        Thread.sleep(keepAlive * 2 * 1000);
        
        assertTrue(m_client.isConnectionLost());
    }
}
