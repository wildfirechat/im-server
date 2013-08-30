package org.dna.mqtt.moquette.messaging.spi.impl;

import java.io.File;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author andrea
 */
public class SimpleMessagingTest {

    final static String FAKE_CLIENT_ID = "FAKE_123";
    final static String FAKE_TOPIC = "/news";
    SimpleMessaging messaging;

    byte m_returnCode;
    IoSession m_session;
//    MQTTHandler m_handler;
    ConnectMessage connMsg;

    AbstractMessage m_receivedMessage;
    
    @BeforeClass
    public static void beforeClass() {
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Before
    public void setUp() throws InterruptedException {
        messaging = SimpleMessaging.getInstance();
        messaging.init();
        connMsg = new ConnectMessage();
        connMsg.setProcotolVersion((byte) 0x03);

        m_session = new DummySession();

        m_session.getFilterChain().addFirst("MessageCatcher", new IoFilterAdapter() {

            @Override
            public void filterWrite(NextFilter nextFilter, IoSession session,
                                    WriteRequest writeRequest) throws Exception {
                try {
                    m_receivedMessage = (AbstractMessage) writeRequest.getMessage();
                    if (m_receivedMessage instanceof ConnAckMessage) {
                        ConnAckMessage buf = (ConnAckMessage) m_receivedMessage;
                        m_returnCode = buf.getReturnCode();
                    }
                } catch (Exception ex) {
                    throw new AssertionError("Wrong return code");
                }
            }
        });

        //sleep to let the messaging batch processor to process the initEvent
        Thread.sleep(300);
    }

    @After
    public void tearDown() {
        m_receivedMessage = null;
        messaging.stop();
    }

}
