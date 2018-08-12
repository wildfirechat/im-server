package io.moquette.broker;

import io.moquette.spi.impl.DebugUtils;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.*;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static org.junit.Assert.*;

class ConnectionTestUtils {

    static void assertConnectAccepted(EmbeddedChannel channel) {
        MqttConnAckMessage connAck = channel.readOutbound();
        final MqttConnectReturnCode connAckReturnCode = connAck.variableHeader().connectReturnCode();
        assertEquals("Connect must be accepted", CONNECTION_ACCEPTED, connAckReturnCode);
    }

    static void verifyReceivePublish(EmbeddedChannel embeddedChannel, String expectedTopic, String expectedContent) {
        MqttPublishMessage receivedPublish = embeddedChannel.flushOutbound().readOutbound();
        assertPublishIsCorrect(expectedTopic, expectedContent, receivedPublish);
    }

    private static void assertPublishIsCorrect(String expectedTopic, String expectedContent, MqttPublishMessage receivedPublish) {
        assertNotNull("Expecting a PUBLISH message", receivedPublish);
        final String decodedPayload = DebugUtils.payload2Str(receivedPublish.payload());
        assertEquals(expectedContent, decodedPayload);
        assertEquals(expectedTopic, receivedPublish.variableHeader().topicName());
    }

    static void verifyReceiveRetainedPublish(EmbeddedChannel embeddedChannel, String expectedTopic, String expectedContent) {
        MqttPublishMessage receivedPublish = embeddedChannel.readOutbound();
        assertPublishIsCorrect(expectedTopic, expectedContent, receivedPublish);
        assertTrue("MUST be retained publish", receivedPublish.fixedHeader().isRetain());
    }

    static void verifyReceiveRetainedPublish(EmbeddedChannel embeddedChannel, String expectedTopic,
                                             String expectedContent, MqttQoS expectedQos) {
        MqttPublishMessage receivedPublish = embeddedChannel.flushOutbound().readOutbound();
        assertEquals(receivedPublish.fixedHeader().qosLevel(), expectedQos);
        assertPublishIsCorrect(expectedTopic, expectedContent, receivedPublish);
        assertTrue("MUST be retained publish", receivedPublish.fixedHeader().isRetain());
    }

    static void verifyPublishIsReceived(EmbeddedChannel embCh, MqttQoS expectedQos, String expectedPayload) {
        final MqttPublishMessage publishReceived = embCh.flushOutbound().readOutbound();
        final String payloadMessage = DebugUtils.payload2Str(publishReceived.payload());
        assertEquals("Sent and received payload must be identical", expectedPayload, payloadMessage);
        assertEquals("Expected QoS don't match", expectedQos, publishReceived.fixedHeader().qosLevel());
    }

    static void verifyNoPublishIsReceived(EmbeddedChannel channel) {
        final Object messageReceived = channel.readOutbound();
        assertNull("Received an out message from processor while not expected", messageReceived);
    }

    static MqttConnectMessage buildConnect(String clientId) {
        return MqttMessageBuilders.connect()
            .clientId(clientId)
            .build();
    }

    static MqttConnectMessage buildConnectNotClean(String clientId) {
        return MqttMessageBuilders.connect()
            .clientId(clientId)
            .cleanSession(false)
            .build();
    }
}
