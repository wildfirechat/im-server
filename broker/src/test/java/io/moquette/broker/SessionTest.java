package io.moquette.broker;

import io.moquette.broker.subscriptions.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Test;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.*;

public class SessionTest {

    @Test
    public void testPubAckDrainMessagesRemainingInQueue() {
        final Queue<SessionRegistry.EnqueuedMessage> queuedMessages = new ConcurrentLinkedQueue<>();
        final Session client = new Session("Subscriber", true, null, queuedMessages);
        final EmbeddedChannel testChannel = new EmbeddedChannel();
        MQTTConnection mqttConnection = new MQTTConnection(testChannel, null, null, null, null);
        client.markConnected();
        client.bind(mqttConnection);

        final Topic destinationTopic = new Topic("/a/b");
        sendQoS1To(client, destinationTopic, "Hello World!");
        // simulate a filling of inflight space and start pushing on queue
        for (int i = 0; i < 10; i++) {
            sendQoS1To(client, destinationTopic, "Hello World " + i + "!");
        }

        assertEquals("Inflight zone must be full, and the 11th message must be queued",
            1, queuedMessages.size());
        // Exercise
        client.pubAckReceived(1);

        // Verify
        assertTrue("Messages should be drained", queuedMessages.isEmpty());
    }

    private void sendQoS1To(Session client, Topic destinationTopic, String message) {
        final ByteBuf payload = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, message);
        client.sendPublishOnSessionAtQos(destinationTopic, MqttQoS.AT_LEAST_ONCE, payload);
    }

}
