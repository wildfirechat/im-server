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

import org.eclipse.moquette.server.config.FilesystemConfig;
import org.eclipse.moquette.server.config.IConfig;
import org.eclipse.moquette.server.config.MemoryConfig;
import org.eclipse.moquette.server.netty.NettyAcceptor;
import org.eclipse.moquette.spi.impl.ProtocolProcessor;
import org.eclipse.moquette.spi.impl.SimpleMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.eclipse.moquette.commons.Constants.PERSISTENT_STORE_PROPERTY_NAME;
/**
 * Launch a  configured version of the server.
 * @author andrea
 */
public class Server {
    
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    
    private ServerAcceptor m_acceptor;

    public static void main(String[] args) throws IOException {
        final Server server = new Server();
        server.startServer();
        System.out.println("Server started, version 0.8-SNAPSHOT");
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
     * located at m_config/moquette.conf
     */
    public void startServer() throws IOException {
        final IConfig config = new FilesystemConfig();
        startServer(config);
    }

    /**
     * Starts Moquette bringing the configuration from the given file
     */
    public void startServer(File configFile) throws IOException {
        LOG.info("Using m_config file: " + configFile.getAbsolutePath());
        final IConfig config = new FilesystemConfig(configFile);
        startServer(config);
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
        final IConfig config = new MemoryConfig(configProps);
        startServer(config);
    }

    /**
     * Starts Moquette bringing the configuration files from the given Config implementation.
     */
    public void startServer(IConfig config) throws IOException {
        final String handlerProp = System.getProperty("intercept.handler");
        if (handlerProp != null) {
            config.setProperty("intercept.handler", handlerProp);
        }
        LOG.info("Persistent store file: " + config.getProperty(PERSISTENT_STORE_PROPERTY_NAME));
        final ProtocolProcessor processor = SimpleMessaging.getInstance().init(config);

        m_acceptor = new NettyAcceptor();
        m_acceptor.initialize(processor, config);
    }
    
    public void stopServer() {
    	LOG.info("Server stopping...");
        m_acceptor.close();
        SimpleMessaging.getInstance().shutdown();
        LOG.info("Server stopped");
    }
}
