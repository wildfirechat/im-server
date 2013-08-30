package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.HashMap;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.SubscriptionsStore;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.server.ConnectionDescriptor;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author andrea
 */
public class ProtocolProcessorTest {
    IoSession m_session;
    byte m_returnCode;
    ConnectMessage connMsg;
    ProtocolProcessor m_processor;
    
    @Before
    public void setUp() throws InterruptedException {
        connMsg = new ConnectMessage();
        connMsg.setProcotolVersion((byte) 0x03);

        m_session = new DummySession();

        m_session.getFilterChain().addFirst("MessageCatcher", new IoFilterAdapter() {

            @Override
            public void filterWrite(IoFilter.NextFilter nextFilter, IoSession session,
                                    WriteRequest writeRequest) throws Exception {
                try {
                    AbstractMessage receivedMessage = (AbstractMessage) writeRequest.getMessage();
                    if (receivedMessage instanceof ConnAckMessage) {
                        ConnAckMessage buf = (ConnAckMessage) receivedMessage;
                        m_returnCode = buf.getReturnCode();
                    }
                } catch (Exception ex) {
                    throw new AssertionError("Wrong return code");
                }
            }
        });

        //sleep to let the messaging batch processor to process the initEvent
        Thread.sleep(300);
        
        IStorageService storageService = new HawtDBStorageService();
        storageService.initStore();

        SubscriptionsStore subscriptions = new SubscriptionsStore();
        subscriptions.init(storageService);
        m_processor = new ProtocolProcessor();
        IMessaging mockedMessaging = mock(IMessaging.class);
        m_processor.init(new HashMap<String, ConnectionDescriptor>(), subscriptions, storageService, mockedMessaging);
    }
    
    @Test
    public void testHandleConnect_BadProtocol() {
        connMsg.setProcotolVersion((byte) 0x02);

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION, m_returnCode);
    }
    
    @Test
    public void testConnect_badClientID() {
        connMsg.setClientID("extremely_long_clientID_greater_than_23");

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.IDENTIFIER_REJECTED, m_returnCode);
    }

    @Test
    public void testWill() {
        connMsg.setClientID("123");
        connMsg.setWillFlag(true);
        connMsg.setWillTopic("topic");
        connMsg.setWillMessage("Topic message");
        

        //Exercise
        //m_handler.setMessaging(mockedMessaging);
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_returnCode);
        //TODO verify the call
        /*verify(mockedMessaging).publish(eq("topic"), eq("Topic message".getBytes()),
                any(AbstractMessage.QOSType.class), anyBoolean(), eq("123"), any(IoSession.class));*/
    }
}
