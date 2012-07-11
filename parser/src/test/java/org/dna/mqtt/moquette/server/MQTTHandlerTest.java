package org.dna.mqtt.moquette.server;

import org.mockito.ArgumentCaptor;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.apache.mina.core.write.WriteRequest;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
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
    MQTTHandler m_handler;
    ConnectMessage connMsg;
    
    @Before
    public void setUp() {
        m_handler = new MQTTHandler();
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
        m_handler.handleConnect(m_session, connMsg);
        
        //Verify
        assertEquals(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION, m_returnCode);
    }
    
    
    @Test
    public void testConnect_badClientID() {
        connMsg.setClientID("extremely_long_clientID_greater_than_23");

        //Exercise
        m_handler.handleConnect(m_session, connMsg);
        
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
        m_handler.setMessaging(mockedMessaging);
        m_handler.handleConnect(m_session, connMsg);
        
        //Verify
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_returnCode);
        verify(mockedMessaging).publish(eq("topic"), eq("Topic message"), anyByte(), anyBoolean());
    }
    
    
    @Test
    public void testHandleSubscribe() {
        String topicName = "/news";
        SubscribeMessage subscribeMsg = new SubscribeMessage();
        subscribeMsg.setMessageID(0x555);
        subscribeMsg.addSubscription(new SubscribeMessage.Couple(
                (byte)AbstractMessage.QOSType.EXACTLY_ONCE.ordinal(), topicName));
        IMessaging mockedMessaging = mock(IMessaging.class);
        
        m_session.setAttribute(MQTTHandler.ATTR_CLIENTID, "fakeID");
        

        //Exercise
        m_handler.setMessaging(mockedMessaging);
        m_handler.handleSubscribe(m_session, subscribeMsg);
        
        //Verify
        ArgumentCaptor<AbstractMessage.QOSType> argument = ArgumentCaptor.forClass(AbstractMessage.QOSType.class);
        verify(mockedMessaging).subscribe(eq("fakeID"), eq(topicName), argument.capture());
        assertEquals(AbstractMessage.QOSType.EXACTLY_ONCE, argument.getValue());
    }
}
