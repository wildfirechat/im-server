package org.dna.mqtt.moquette.server;

import org.apache.mina.core.write.WriteRequest;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class MQTTHandlerTest {

    @Test
    public void testHandleConnect_BadProtocol() {
        MQTTHandler h = new MQTTHandler();
        ConnectMessage connMsg = new ConnectMessage();
        connMsg.setProcotolVersion((byte) 0x02);

        IoSession session = new DummySession();
        session.getFilterChain().addFirst("MessageCatcher", new IoFilterAdapter() {

            @Override
            public void filterWrite(NextFilter nextFilter, IoSession session,
                    WriteRequest writeRequest) throws Exception {
                try {
                    ConnAckMessage buf = (ConnAckMessage) writeRequest.getMessage();
                    assertEquals(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION, buf.getReturnCode());
                } catch (Exception ex) {
                    throw new AssertionError("Wrong return code");
                }
            }
        });

        //Exercise
        h.handleConnect(session, connMsg);
    }
}