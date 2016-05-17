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

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 *
 * @author luca <luca.capra@create-net.org> 
 */
public class ConfigurationClassLoaderTest implements IAuthenticator, IAuthorizator {
    Server m_server;
    IConfig m_config;
    
    protected void startServer(Properties props) throws IOException {
        m_server = new Server();
        m_config = new MemoryConfig(props);
        m_server.startServer(m_config);
    }


    @After
    public void tearDown() throws Exception {
        m_server.stopServer();
        File dbFile = new File(m_config.getProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME));
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    public void loadAuthenticator() throws Exception {
        Properties props = new Properties(IntegrationUtils.prepareTestProperties());
        props.setProperty(BrokerConstants.AUTHENTICATOR_CLASS_NAME, "io.moquette.server.ConfigurationClassLoaderTest");
        startServer(props);
        assertTrue(true);
        m_server.stopServer();
    }
    
    @Test
    public void loadAuthorizator() throws Exception {
        Properties props = new Properties(IntegrationUtils.prepareTestProperties());
        props.setProperty(BrokerConstants.AUTHORIZATOR_CLASS_NAME, "io.moquette.server.ConfigurationClassLoaderTest");
        startServer(props);
        assertTrue(true);
        m_server.stopServer();
    }

    @Override
    public boolean checkValid(String username, byte[] password) {
        return true;
    }

    @Override
    public boolean canWrite(String topic, String user, String client) {
        return true;
    }

    @Override
    public boolean canRead(String topic, String user, String client) {
        return true;
    }
    
}
