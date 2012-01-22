package org.dna.mqtt.moquette.server;

import org.apache.mina.core.write.WriteRequest;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class MQTTHandlerTest {

    byte m_returnCode;
    
    IoSession m_session;
    
    @Before
    public void setUp() {
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
        MQTTHandler h = new MQTTHandler();
        ConnectMessage connMsg = new ConnectMessage();
        connMsg.setProcotolVersion((byte) 0x02);

        //Exercise
        h.handleConnect(m_session, connMsg);
        
        //Verify
        assertEquals(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION, m_returnCode);
    }
    
    
    @Test
    public void testConnect_badClientID() {
        MQTTHandler h = new MQTTHandler();
        ConnectMessage connMsg = new ConnectMessage();
        connMsg.setProcotolVersion((byte) 0x03);
        connMsg.setClientID("extremely_long_clientID_grtaer_than_23");

        //Exercise
        h.handleConnect(m_session, connMsg);
        
        //Verify
        assertEquals(ConnAckMessage.IDENTIFIER_REJECTED, m_returnCode);
    }
}
