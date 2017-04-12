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
import io.moquette.BrokerConstants;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check that Moquette could also handle SSL with client authentication.
 *
 * <p>
 * Create certificates needed for client authentication
 *
 * <pre>
 * # generate server certificate chain (valid for 10000 days)
 * keytool -genkeypair -alias signedtestserver -dname cn=moquette.eclipse.org -keyalg RSA -keysize 2048 \
 * -keystore signedserverkeystore.jks -keypass passw0rdsrv -storepass passw0rdsrv -validity 10000
 * keytool -genkeypair -alias signedtestserver_sub -dname cn=moquette.eclipse.org -keyalg RSA -keysize 2048 \
 * -keystore signedserverkeystore.jks -keypass passw0rdsrv -storepass passw0rdsrv
 *
 * # sign server subcertificate with server certificate (valid for 10000 days)
 * keytool -certreq -alias signedtestserver_sub -keystore signedserverkeystore.jks -keypass passw0rdsrv \
 * -storepass passw0rdsrv | \
 * keytool -gencert -alias signedtestserver -keystore signedserverkeystore.jks -keypass passw0rdsrv \
 * -storepass passw0rdsrv -validity 10000 | \
 * keytool -importcert -alias signedtestserver_sub -keystore signedserverkeystore.jks -keypass passw0rdsrv \
 * -storepass passw0rdsrv
 *
 * # generate client keypair
 * keytool -genkeypair -alias signedtestclient -dname cn=moquette.eclipse.org -keyalg RSA -keysize 2048 \
 * -keystore signedclientkeystore.jks -keypass passw0rd -storepass passw0rd
 *
 * # create signed client certificate with server subcertificate and import to client keystore (valid for 10000 days)
 * keytool -certreq -alias signedtestclient -keystore signedclientkeystore.jks -keypass passw0rd -storepass passw0rd | \
 * keytool -gencert -alias signedtestserver_sub -keystore signedserverkeystore.jks -keypass passw0rdsrv \
 * -storepass passw0rdsrv -validity 10000 | \
 * keytool -importcert -alias signedtestclient -keystore signedclientkeystore.jks -keypass passw0rd \
 * -storepass passw0rd -noprompt
 *
 * # import server certificates into signed truststore
 * keytool -exportcert -alias signedtestserver -keystore signedserverkeystore.jks -keypass passw0rdsrv \
 * -storepass passw0rdsrv | \
 * keytool -importcert -trustcacerts -noprompt -alias signedtestserver -keystore signedclientkeystore.jks \
 * -keypass passw0rd -storepass passw0rd
 * keytool -exportcert -alias signedtestserver_sub -keystore signedserverkeystore.jks -keypass passw0rdsrv \
 * -storepass passw0rdsrv | \
 * keytool -importcert -trustcacerts -noprompt -alias signedtestserver_sub -keystore signedclientkeystore.jks \
 * -keypass passw0rd -storepass passw0rd
 *
 * # create unsigned client certificate (valid for 10000 days)
 * keytool -genkeypair -alias unsignedtestclient -dname cn=moquette.eclipse.org -validity 10000 -keyalg RSA \
 * -keysize 2048 -keystore unsignedclientkeystore.jks -keypass passw0rd -storepass passw0rd
 *
 * # import server certificates into unsigned truststore
 * keytool -exportcert -alias signedtestserver -keystore signedserverkeystore.jks -keypass passw0rdsrv \
 * -storepass passw0rdsrv | \
 * keytool -importcert -trustcacerts -noprompt -alias signedtestserver -keystore unsignedclientkeystore.jks \
 * -keypass passw0rd -storepass passw0rd
 * keytool -exportcert -alias signedtestserver_sub -keystore signedserverkeystore.jks -keypass passw0rdsrv \
 * -storepass passw0rdsrv | \
 * keytool -importcert -trustcacerts -noprompt -alias signedtestserver_sub -keystore unsignedclientkeystore.jks \
 * -keypass passw0rd -storepass passw0rd
 * </pre>
 * </p>
 */
public class ServerIntegrationSSLClientAuthTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationSSLClientAuthTest.class);

    Server m_server;
    static MqttClientPersistence s_dataStore;

    IMqttClient m_client;
    MessageCollector m_callback;

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
        sslProps.put(BrokerConstants.SSL_PORT_PROPERTY_NAME, "8883");
        sslProps.put(BrokerConstants.JKS_PATH_PROPERTY_NAME, "signedserverkeystore.jks");
        sslProps.put(BrokerConstants.KEY_STORE_PASSWORD_PROPERTY_NAME, "passw0rdsrv");
        sslProps.put(BrokerConstants.KEY_MANAGER_PASSWORD_PROPERTY_NAME, "passw0rdsrv");
        sslProps.put(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, IntegrationUtils.localMapDBPath());
        sslProps.put(BrokerConstants.NEED_CLIENT_AUTH, "true");
        m_server.startServer(sslProps);
    }

    @Before
    public void setUp() throws Exception {
        String dbPath = IntegrationUtils.localMapDBPath();
        File dbFile = new File(dbPath);
        assertFalse(String.format("The DB storagefile %s already exists", dbPath), dbFile.exists());

        startServer();

        m_client = new MqttClient("ssl://localhost:8883", "TestClient", s_dataStore);
        // m_client = new MqttClient("ssl://test.mosquitto.org:8883", "TestClient", s_dataStore);

        m_callback = new MessageCollector();
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
    public void checkClientAuthentication() throws Exception {
        LOG.info("*** checkClientAuthentication ***");
        SSLSocketFactory ssf = configureSSLSocketFactory("signedclientkeystore.jks");

        MqttConnectOptions options = new MqttConnectOptions();
        options.setSocketFactory(ssf);
        m_client.connect(options);
        m_client.subscribe("/topic", 0);
        m_client.disconnect();
    }

    @Test(expected = MqttException.class)
    public void checkClientAuthenticationFail() throws Exception {
        LOG.info("*** checkClientAuthenticationFail ***");
        SSLSocketFactory ssf = configureSSLSocketFactory("unsignedclientkeystore.jks");

        MqttConnectOptions options = new MqttConnectOptions();
        options.setSocketFactory(ssf);
        // actual a "Broken pipe" is thrown, this is not very specific.
        try {
            m_client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private SSLSocketFactory configureSSLSocketFactory(String keystore) throws KeyManagementException,
            NoSuchAlgorithmException, UnrecoverableKeyException, IOException, CertificateException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream jksInputStream = getClass().getClassLoader().getResourceAsStream(keystore);
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
