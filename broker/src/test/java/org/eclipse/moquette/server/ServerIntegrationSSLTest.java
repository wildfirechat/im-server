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
package org.eclipse.moquette.server;

import static org.eclipse.moquette.commons.Constants.JKS_PATH_PROPERTY_NAME;
import static org.eclipse.moquette.commons.Constants.KEY_MANAGER_PASSWORD_PROPERTY_NAME;
import static org.eclipse.moquette.commons.Constants.KEY_STORE_PASSWORD_PROPERTY_NAME;
import static org.eclipse.moquette.commons.Constants.PERSISTENT_STORE_PROPERTY_NAME;
import static org.eclipse.moquette.commons.Constants.SSL_PORT_PROPERTY_NAME;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check that Moquette could also handle SSL.
 * 
 * @author andrea
 */
public class ServerIntegrationSSLTest {
    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationSSLTest.class);

    Server m_server;
    static MqttClientPersistence s_dataStore;

    IMqttClient m_client;
    TestCallback m_callback;

    @BeforeClass
    public static void beforeTests() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        s_dataStore = new MqttDefaultFilePersistence(tmpDir);
    }

    protected void startServer() throws IOException {
        String file = getClass().getResource("/").getPath();
        System.setProperty("moquette.path", file);
        m_server = new Server();

        Properties sslProps = new Properties();
        sslProps.put(SSL_PORT_PROPERTY_NAME, "8883");
        sslProps.put(JKS_PATH_PROPERTY_NAME, "serverkeystore.jks");
        sslProps.put(KEY_STORE_PASSWORD_PROPERTY_NAME, "passw0rdsrv");
        sslProps.put(KEY_MANAGER_PASSWORD_PROPERTY_NAME, "passw0rdsrv");
        sslProps.put(PERSISTENT_STORE_PROPERTY_NAME, IntegrationUtils.localMapDBPath());
        m_server.startServer(sslProps);
    }

    @Before
    public void setUp() throws Exception {
        String dbPath = IntegrationUtils.localMapDBPath();
        File dbFile = new File(dbPath);
        assertFalse(String.format("The DB storagefile %s already exists", dbPath), dbFile.exists());

        startServer();

        m_client = new MqttClient("ssl://localhost:8883", "TestClient", s_dataStore);
//        m_client = new MqttClient("ssl://test.mosquitto.org:8883", "TestClient", s_dataStore);
        
        m_callback = new TestCallback();
        m_client.setCallback(m_callback);
    }

    @After
    public void tearDown() throws Exception {
        if (m_client != null && m_client.isConnected()) {
            m_client.disconnect();
        }

        if (m_server != null) {
            m_server.stopServer();
        }
        String dbPath = IntegrationUtils.localMapDBPath();
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        assertFalse(dbFile.exists());
    }
    
    @Test
    public void checkSupportSSL() throws Exception {
        LOG.info("*** checkSupportSSL ***");
        SSLSocketFactory ssf = configureSSLSocketFactory();
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setSocketFactory(ssf);
        m_client.connect(options);
        m_client.subscribe("/topic", 0);
        m_client.disconnect();
    }

    @Test
    public void checkSupportSSLForMultipleClient() throws Exception {
        LOG.info("*** checkSupportSSLForMultipleClient ***");
        SSLSocketFactory ssf = configureSSLSocketFactory();
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setSocketFactory(ssf);
        m_client.connect(options);
        m_client.subscribe("/topic", 0);
        
        MqttClient secondClient = new MqttClient("ssl://localhost:8883", "secondTestClient", new MemoryPersistence());
        MqttConnectOptions secondClientOptions = new MqttConnectOptions();
        secondClientOptions.setSocketFactory(ssf);
        secondClient.connect(secondClientOptions);
        secondClient.publish("/topic", new MqttMessage("message".getBytes()));
        secondClient.disconnect();
        
        m_client.disconnect();
    }
    /**
     * keystore generated into test/resources with command:
     * 
     * keytool -keystore clientkeystore.jks -alias testclient -genkey -keyalg RSA
     * -> mandatory to put the name surname
     * -> password is passw0rd
     * -> type yes at the end
     * 
     * to generate the crt file from the keystore
     * -- keytool -certreq -alias testclient -keystore clientkeystore.jks -file testclient.csr
     * 
     * keytool -export -alias testclient -keystore clientkeystore.jks -file testclient.crt
     * 
     * to import an existing certificate:
     * keytool -keystore clientkeystore.jks -import -alias testclient -file testclient.crt -trustcacerts
     */
    private SSLSocketFactory configureSSLSocketFactory() throws KeyManagementException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException, CertificateException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream jksInputStream = getClass().getClassLoader().getResourceAsStream("clientkeystore.jks");
        ks.load(jksInputStream, "passw0rd".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "passw0rd".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext sc = SSLContext.getInstance("TLS"); 
        TrustManager[] trustManagers = tmf.getTrustManagers(); 
        sc.init(kmf.getKeyManagers(), trustManagers, null); 

        SSLSocketFactory ssf = sc.getSocketFactory();
        return ssf;
    }
}
