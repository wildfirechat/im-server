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
import java.text.ParseException;
import java.util.Properties;
import org.dna.mqtt.moquette.messaging.spi.impl.SimpleMessaging;
import org.dna.mqtt.moquette.server.netty.NettyAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Launch a  configured version of the server.
 * @author andrea
 */
public class Server {
    
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    
    public static final String STORAGE_FILE_PATH = System.getProperty("user.home") + 
            File.separator + "moquette_store.hawtdb";

    private ServerAcceptor m_acceptor;
    SimpleMessaging messaging;
    
    public static void main(String[] args) throws IOException {
        
        final Server server = new Server();
        server.startServer();
        System.out.println("Server started, version 0.6-SNAPSHOT");
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stopServer();
            }
        });
        
    }
    
    public void startServer() throws IOException {
        ConfigurationParser confParser = new ConfigurationParser();
        try {
            String configPath = System.getProperty("moquette.path", null);
            confParser.parse(new File(configPath, "config/moquette.conf"));
        } catch (ParseException pex) {
            LOG.warn("An error occured in parsing configuration, fallback on default configuration", pex);
        }
        Properties configProps = confParser.getProperties();
        
        messaging = SimpleMessaging.getInstance();
        messaging.init(configProps);
        
        m_acceptor = new NettyAcceptor();
        m_acceptor.initialize(messaging, configProps);
    }
    
    public void stopServer() {
        System.out.println("Server stopping...");
        messaging.stop();
        m_acceptor.close();
        System.out.println("Server stopped");
    }
}
