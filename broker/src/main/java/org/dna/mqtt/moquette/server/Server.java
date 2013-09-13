package org.dna.mqtt.moquette.server;

import java.io.File;
import java.io.IOException;
import org.dna.mqtt.moquette.messaging.spi.impl.SimpleMessaging;
import org.dna.mqtt.moquette.server.mina.MinaAcceptor;
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
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stopServer();
            }
        });
        
    }
    
    public void startServer() throws IOException {
        messaging = SimpleMessaging.getInstance();
        messaging.init();
        
        m_acceptor = new MinaAcceptor();
        m_acceptor.initialize(messaging);
    }
    
    public void stopServer() {
        LOG.info("Server stopping...");
        messaging.stop();
        m_acceptor.close();
        LOG.info("Server stopped");
    }

}
