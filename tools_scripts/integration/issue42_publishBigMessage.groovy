@GrabResolver(name='Paho', root="https://repo.eclipse.org/content/repositories/paho-releases/")
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.0.2')

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch

class SubscriberCallback implements MqttCallback {

    int m_numReceived = 0
    long m_startTime
    boolean firstMessageReceived = false

    private CountDownLatch m_latch = new CountDownLatch(1)

    //Maps delay to num of messages reached with that delay
    protected Map<Long, Long> distribution = [:]

    void waitFinish() {
        m_latch.await()
    }

    public void connectionLost(Throwable cause) {
        m_latch.countDown()
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        println "Received message on topic [${topic}]"
        m_latch.countDown()
//        if (!firstMessageReceived) {
//            m_startTime = System.currentTimeMillis()
//            firstMessageReceived = true
//        }
//        if (topic == "/log") {
//
//            m_latch.countDown()
//        } else {
//            ByteBuffer bb = ByteBuffer.wrap(message.payload)
//            long sentTime = bb.getLong()
//            long delay = System.currentTimeMillis() - sentTime
//            //println "-received in ${delay}(ms) on ${topic} with QoS ${message.qos}"
//            //distribution[delay] = distribution[delay] != null ? distribution[delay]++ : 1
//            if (distribution[delay] != null) {
//                distribution[delay]++
//            } else {
//                distribution[delay] = 1
//            }
//            m_numReceived++
//        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

if (args.size() < 1) {
    println "Usage: groovy issue42_publishBigMessage.groovy </path/to/a/file.tosend>"
}

File toSend = new File(args[0])

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence subDataStore = new MqttDefaultFilePersistence(tmpDir + "/sub")
MqttClient subClient = new MqttClient("tcp://localhost:1883", "FileSubscriber", subDataStore)
def callback = new SubscriberCallback()
subClient.callback = callback
subClient.connect()
subClient.subscribe("/log", 0)


MqttDefaultFilePersistence pubDataStore = new MqttDefaultFilePersistence(tmpDir + "/pub")
MqttClient pubClient = new MqttClient("tcp://localhost:1883", "FilePublisher", pubDataStore)
pubClient.connect()
//MqttMessage message = new MqttMessage(toSend.bytes)
//message.setQos(2)
print "publishing.."
//pubClient.getTopic("log").publish(message)
pubClient.publish('/log', toSend.bytes, 1, false)
println "published"
pubClient.disconnect()
println "disconnected publisher, waiting subscriber.."
callback.waitFinish()
subClient.disconnect()
println "Finished!"