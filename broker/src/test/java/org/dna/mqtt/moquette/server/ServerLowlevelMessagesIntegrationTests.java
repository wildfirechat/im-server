package org.dna.mqtt.moquette.server;

import java.io.File;
import java.io.IOException;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.testclient.Client;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class ServerLowlevelMessagesIntegrationTests {
    Server m_server;
    Client m_client;
    
    protected void startServer() throws IOException {
        m_server = new Server();
        m_server.startServer();
    }

    @Before
    public void setUp() throws Exception {
        startServer();
        m_client = new Client("localhost");
    }

    @After
    public void tearDown() throws Exception {
        m_client.close();
        
        m_server.stopServer();
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    public void elapseKeepAliveTime() throws InterruptedException {
        int keepAlive = 2; //secs
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setProcotolVersion((byte)3);
        connectMessage.setClientID("FAKECLNT");
        connectMessage.setKeepAlive(keepAlive);
        m_client.sendMessage(connectMessage);
        
        //wait 2 times the keepAlive
        Thread.sleep(keepAlive * 2 * 1000);
        
        assertTrue(m_client.isConnectionLost());
    }
}
