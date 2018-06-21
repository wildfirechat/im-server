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

package io.moquette.server;

import java.io.IOException;
import java.util.Properties;

import io.moquette.BrokerConstants;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslProvider;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check that Moquette could also handle SSL with OpenSSL provider.
 */
public class ServerIntegrationOpenSSLTest extends ServerIntegrationSSLTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationOpenSSLTest.class);

    @BeforeClass
    public static void beforeTests() {
        LOG.info("try to initialize OpenSSL native library");
        OpenSsl.ensureAvailability();
        LOG.info("OpenSSL initialized");
    }

    @Override
    protected void startServer() throws IOException {
        String file = getClass().getResource("/").getPath();
        System.setProperty("moquette.path", file);
        m_server = new Server();
        Properties sslProps = new Properties();

        sslProps.put(BrokerConstants.SSL_PROVIDER, SslProvider.OPENSSL.name());
        sslProps.put(BrokerConstants.NEED_CLIENT_AUTH, "true");

        sslProps.put(BrokerConstants.SSL_PORT_PROPERTY_NAME, "8883");
        sslProps.put(BrokerConstants.JKS_PATH_PROPERTY_NAME, "serverkeystore.jks");
        sslProps.put(BrokerConstants.KEY_STORE_PASSWORD_PROPERTY_NAME, "passw0rdsrv");
        sslProps.put(BrokerConstants.KEY_MANAGER_PASSWORD_PROPERTY_NAME, "passw0rdsrv");
        sslProps.put(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, IntegrationUtils.localMapDBPath());
        m_server.startServer(sslProps);
    }
}
