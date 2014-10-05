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
package org.eclipse.moquette.bundle;

import org.eclipse.moquette.server.Server;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start and stop a configured version of the MQTT broker server.
 *
 * @author Didier Donsez
 * @todo add config admin for port (in NettyAcceptor)
 */
public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private Server server = null;

    public void start(BundleContext bundleContext) throws Exception {
        server = new Server();
        server.startServer();
        LOG.info("Moquette MQTT broker started, version 0.7-SNAPSHOT");
    }

    public void stop(BundleContext bundleContext) throws Exception {
        server.stopServer();
        LOG.info("Moquette MQTT broker stopped, version 0.7-SNAPSHOT");
    }
}
