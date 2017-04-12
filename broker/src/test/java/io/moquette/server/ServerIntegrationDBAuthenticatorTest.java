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

package io.moquette.server;

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import io.moquette.spi.impl.security.DBAuthenticator;
import io.moquette.spi.impl.security.DBAuthenticatorTest;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Properties;
import static org.junit.Assert.assertTrue;

public class ServerIntegrationDBAuthenticatorTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationDBAuthenticatorTest.class);

    static MqttClientPersistence s_dataStore;
    static MqttClientPersistence s_pubDataStore;
    static DBAuthenticatorTest dbAuthenticatorTest;

    Server m_server;
    IMqttClient m_client;
    IMqttClient m_publisher;
    MessageCollector m_messagesCollector;
    IConfig m_config;

    @BeforeClass
    public static void beforeTests() throws NoSuchAlgorithmException, SQLException, ClassNotFoundException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        s_dataStore = new MqttDefaultFilePersistence(tmpDir);
        s_pubDataStore = new MqttDefaultFilePersistence(tmpDir + File.separator + "publisher");
        dbAuthenticatorTest = new DBAuthenticatorTest();
        dbAuthenticatorTest.setup();
    }

    protected void startServer() throws IOException {
        m_server = new Server();
        final Properties configProps = addDBAuthenticatorConf(IntegrationUtils.prepareTestProperties());
        m_config = new MemoryConfig(configProps);
        m_server.startServer(m_config);
    }

    private void stopServer() {
        m_server.stopServer();
    }

    private Properties addDBAuthenticatorConf(Properties properties) {
        properties.put(BrokerConstants.AUTHENTICATOR_CLASS_NAME, DBAuthenticator.class.getCanonicalName());
        properties.put(BrokerConstants.DB_AUTHENTICATOR_DRIVER, DBAuthenticatorTest.ORG_H2_DRIVER);
        properties.put(BrokerConstants.DB_AUTHENTICATOR_URL, DBAuthenticatorTest.JDBC_H2_MEM_TEST);
        properties.put(BrokerConstants.DB_AUTHENTICATOR_QUERY, "SELECT PASSWORD FROM ACCOUNT WHERE LOGIN=?");
        properties.put(BrokerConstants.DB_AUTHENTICATOR_DIGEST, DBAuthenticatorTest.SHA_256);
        return properties;
    }

    @Before
    public void setUp() throws Exception {
        startServer();

        m_client = new MqttClient("tcp://localhost:1883", "TestClient", s_dataStore);
        m_messagesCollector = new MessageCollector();
        m_client.setCallback(m_messagesCollector);

        m_publisher = new MqttClient("tcp://localhost:1883", "Publisher", s_pubDataStore);
    }

    @After
    public void tearDown() throws Exception {
        if (m_client != null && m_client.isConnected()) {
            m_client.disconnect();
        }

        if (m_publisher != null && m_publisher.isConnected()) {
            m_publisher.disconnect();
        }

        stopServer();
    }

    @AfterClass
    public static void shutdown() {
        dbAuthenticatorTest.teardown();
    }

    @Test
    public void connectWithValidCredentials() throws Exception {
        LOG.info("*** connectWithCredentials ***");
        m_client = new MqttClient("tcp://localhost:1883", "Publisher", s_pubDataStore);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("dbuser");
        options.setPassword("password".toCharArray());
        m_client.connect(options);
        assertTrue(true);
    }

    @Test
    public void connectWithWrongCredentials() {
        LOG.info("*** connectWithWrongCredentials ***");
        try {
            m_client = new MqttClient("tcp://localhost:1883", "Publisher", s_pubDataStore);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName("dbuser");
            options.setPassword("wrongPassword".toCharArray());
            m_client.connect(options);
        } catch (MqttException e) {
            if (e instanceof MqttSecurityException) {
                assertTrue(true);
                return;
            } else {
                assertTrue(e.getMessage(), false);
                return;
            }
        }
        assertTrue("must not be connected. cause : wrong password given to client", false);
    }

}
