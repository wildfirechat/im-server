package org.dna.mqtt.moquette.server;

import org.apache.mina.core.write.WriteRequest;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author andrea
 */
public class MQTTHandlerTest {

    byte m_returnCode;
    IoSession m_session;
    MQTTHandler h;
    ConnectMessage connMsg;
    
    @Before
    public void setUp() {
        h = new MQTTHandler();
        connMsg = new ConnectMessage();
        connMsg.setProcotolVersion((byte) 0x03);
        
        m_session = new DummySession();
        m_session.getFilterChain().addFirst("MessageCatcher", new IoFilterAdapter() {

            @Override
            public void filterWrite(NextFilter nextFilter, IoSession session,
                    WriteRequest writeRequest) throws Exception {
                try {
                    ConnAckMessage buf = (ConnAckMessage) writeRequest.getMessage();
                    m_returnCode = buf.getReturnCode();
                } catch (Exception ex) {
                    throw new AssertionError("Wrong return code");
                }
            }
        });
    }
    
    @Test
    public void testHandleConnect_BadProtocol() {
        connMsg.setProcotolVersion((byte) 0x02);

        //Exercise
        h.handleConnect(m_session, connMsg);
        
        //Verify
        assertEquals(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION, m_returnCode);
    }
    
    
    @Test
    public void testConnect_badClientID() {
        connMsg.setClientID("extremely_long_clientID_greater_than_23");

        //Exercise
        h.handleConnect(m_session, connMsg);
        
        //Verify
        assertEquals(ConnAckMessage.IDENTIFIER_REJECTED, m_returnCode);
    }
    
    @Test
    public void testWill() {
        connMsg.setClientID("123");
        connMsg.setWillFlag(true);
        connMsg.setWillTopic("topic");
        connMsg.setWillMessage("Topic message");
        IMessaging mockedMessaging = mock(IMessaging.class);

        //Exercise
        h.setMessaging(mockedMessaging);
        h.handleConnect(m_session, connMsg);
        
        //Verify
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_returnCode);
        verify(mockedMessaging).publish(eq("topic"), eq("Topic message"), anyByte(), anyBoolean());
    }
}
