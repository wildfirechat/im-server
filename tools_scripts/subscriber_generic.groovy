@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='mqtt-client', version='0.4.0')

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit



class SubscriberCallback implements MqttCallback {

    int m_numReceived = 0
    long m_startTime
    boolean firstMessageReceived = false

    private CountDownLatch m_latch = new CountDownLatch(1)

    void waitFinish() {
        m_latch.await()
    }

    public void connectionLost(Throwable cause) {
        m_latch.countDown()
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        //println "Received message ${new String(message.payload)} on topic [${topic}]"
        if (!firstMessageReceived) {
            m_startTime = System.currentTimeMillis()
            firstMessageReceived = true
        }
        if (topic == "/exit") {
            long stopTime = System.currentTimeMillis()
            long spentTime = stopTime - m_startTime
            println "/exit received"
            println "subscriber disconnected, received ${m_numReceived} messages in ${spentTime} ms"
            client.disconnect()
            m_latch.countDown()
        } else {
            println "Received $message on topic $topic"
            m_numReceived++
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

String host = args[0]
String topicSubscription = args[1]
String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir)

MqttClient client = new MqttClient("tcp://${host}:1883", "SubscriberClient", dataStore)
def callback = new SubscriberCallback()
client.callback = callback
client.connect()
client.subscribe(topicSubscription, 0)
println "subscribed to $topicSubscription"
client.subscribe("/exit", 0)
println "subscribed to /exit"

callback.waitFinish()