package io.moquette.performance.clients.paho;

import org.eclipse.paho.client.mqttv3.*;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

class SubscriberCallback implements MqttCallback {

    int m_numReceived = 0;
    long m_startTime;
    boolean firstMessageReceived = false;

    private CountDownLatch m_latch = new CountDownLatch(1);

    //Maps delay to num of messages reached with that delay
    protected Map<Long, Long> distribution = new HashMap<>();

    IMqttAsyncClient client;

    void waitFinish() throws InterruptedException {
        m_latch.await();
    }

    public void connectionLost(Throwable cause) {
        m_latch.countDown();
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        //println "Received message ${new String(message.payload)} on topic [${topic}]"
        if (!firstMessageReceived) {
            m_startTime = System.currentTimeMillis();
            firstMessageReceived = true;
        }
        if (topic == "/exit") {
            long stopTime = System.currentTimeMillis();
            long spentTime = stopTime - m_startTime;
            System.out.println("/exit received");
            System.out.println("subscriber disconnected, received ${m_numReceived} messages in ${spentTime} ms (from first received to last one on topic /exit)");
            client.disconnect();
            m_latch.countDown();
        } else {
            ByteBuffer bb = ByteBuffer.wrap(message.getPayload());
            long sentTime = bb.getLong();
            long delay = System.currentTimeMillis() - sentTime;
            //println "-received in ${delay}(ms) on ${topic} with QoS ${message.qos}"
            //distribution[delay] = distribution[delay] != null ? distribution[delay]++ : 1
            Long value = distribution.get(delay);
            if (value != null) {
                distribution.put(delay, value++);
            } else {
                distribution.put(delay, 1L);
            }
            m_numReceived++;
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
