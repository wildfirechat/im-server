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
package org.eclipse.moquette.server;

import org.eclipse.moquette.server.netty.NettyAcceptor;
import org.eclipse.moquette.spi.impl.SimpleMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Properties;

import static org.eclipse.moquette.commons.Constants.PERSISTENT_STORE_PROPERTY_NAME;
/**
 * Launch a  configured version of the server.
 * @author andrea
 */
public class Server {
    
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    
    private ServerAcceptor m_acceptor;
    SimpleMessaging messaging;
    Properties m_properties;
    
    public static void main(String[] args) throws IOException {
        final Server server = new Server();
        server.startServer();
        System.out.println("Server started, version 0.7-SNAPSHOT");
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stopServer();
            }
        });
    }
    
    /**
     * Starts Moquette bringing the configuration from the file 
     * located at config/moquette.conf
     */
    public void startServer() throws IOException {
        String configPath = System.getProperty("moquette.path", null);
        startServer(new File(configPath, "config/moquette.conf"));
    }

    /**
     * Starts Moquette bringing the configuration from the given file
     */
    public void startServer(File configFile) throws IOException {
        LOG.info("Using config file: " + configFile.getAbsolutePath());

        ConfigurationParser confParser = new ConfigurationParser();
        try {
            confParser.parse(configFile);
        } catch (ParseException pex) {
            LOG.warn("An error occurred in parsing configuration, fallback on default configuration", pex);
        }
        m_properties = confParser.getProperties(); 
        startServer(m_properties);
    }
    
    /**
     * Starts the server with the given properties.
     * 
     * Its suggested to at least have the following properties:
     * <ul>
     *  <li>port</li>
     *  <li>password_file</li>
     * </ul>
     */
    public void startServer(Properties configProps) throws IOException {
    	ConfigurationParser confParser = new ConfigurationParser(configProps);
    	m_properties = confParser.getProperties();
        LOG.info("Persistent store file: " + m_properties.get(PERSISTENT_STORE_PROPERTY_NAME));
        messaging = SimpleMessaging.getInstance();
        messaging.init(m_properties);
        
        m_acceptor = new NettyAcceptor();
        m_acceptor.initialize(messaging, m_properties);
    }
    
    public void stopServer() {
    	LOG.info("Server stopping...");
        messaging.stop();
        m_acceptor.close();
        LOG.info("Server stopped");
    }
    
    public Properties getProperties() {
    	return m_properties;
    }
}
