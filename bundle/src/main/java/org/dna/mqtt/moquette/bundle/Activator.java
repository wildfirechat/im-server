package org.dna.mqtt.moquette.bundle;

import org.dna.mqtt.moquette.server.Server;
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
        LOG.info("Moquette MQTT broker started, version 0.5-SNAPSHOT");
    }

    public void stop(BundleContext bundleContext) throws Exception {
        server.stopServer();
        LOG.info("Moquette MQTT broker stopped, version 0.5-SNAPSHOT");
    }
}
