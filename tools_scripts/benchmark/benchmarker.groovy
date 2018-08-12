//@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-snapshots/')
//@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.0.1-SNAPSHOT')
//@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
//@Grab(group='org.eclipse.paho', module='mqtt-client', version='0.4.0')
@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.2.0')

import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
//import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.CountDownLatch
import static groovy.json.JsonOutput.*
import java.nio.ByteBuffer


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
        //println "Received message ${new String(message.payload)} on topic [${topic}]"
        if (!firstMessageReceived) {
            m_startTime = System.currentTimeMillis()
            firstMessageReceived = true
        }
        if (topic == "/exit") {
            long stopTime = System.currentTimeMillis()
            long spentTime = stopTime - m_startTime
            println "/exit received"
            println "subscriber disconnected, received ${m_numReceived} messages in ${spentTime} ms (from first received to last one on topic /exit)"
            client.disconnect()
            m_latch.countDown()
        } else {
            ByteBuffer bb = ByteBuffer.wrap(message.payload)
            long sentTime = bb.getLong()
            long delay = System.currentTimeMillis() - sentTime
            //println "-received in ${delay}(ms) on ${topic} with QoS ${message.qos}"
            //distribution[delay] = distribution[delay] != null ? distribution[delay]++ : 1
            if (distribution[delay] != null) {
                distribution[delay]++
            } else {
                distribution[delay] = 1
            }
            m_numReceived++
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}


def publishQos0(client, long numMessages) {
    print "publishing.."
    long startTime = System.currentTimeMillis()
    (1..numMessages).each {
        byte[] bytes = ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array()
        client.publish('topic', bytes, 0, false)
    }
    client.publish('/exit', 'Exit'.bytes, 0, false)
    long stopTime = System.currentTimeMillis()
    long spentTime = stopTime -startTime
    println "published in ${spentTime} ms"
}

if (args.size() < 2) {
    println "Usage benchmarker <host> <numMessages>"
    println "groovy benchmarker.groovy localhost 1000 500 test1"
    return
}

String host = args[0]
long numMessages = args[1] as long
String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir)
//MemoryPersistence dataStore = new MemoryPersistence()

int rnd = (Math.random() * 100) as int
MqttAsyncClient client = new MqttAsyncClient("tcp://${host}:1883", "SubscriberClient${rnd}", dataStore)
def callback = new SubscriberCallback()
client.callback = callback
client.connect().waitForCompletion(1000)
client.subscribe("topic", 0)
println "subscribed to topic"
client.subscribe("/exit", 0)
println "subscribed to /exit"

publishQos0(client, numMessages)

callback.waitFinish()
Map sorted = callback.distribution.sort( { k1, k2 -> k1 <=> k2 } as Comparator)
//println prettyPrint(toJson(sorted))
File csv = new File("perf.csv")
sorted.each {delay, count -> csv << "${delay}, ${count}\r\n"}



