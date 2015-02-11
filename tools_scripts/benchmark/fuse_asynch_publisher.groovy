@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.10')

import java.net.URISyntaxException

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.CallbackConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Listener
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic
import org.fusesource.mqtt.client.Callback
import org.fusesource.hawtbuf.Buffer
import org.fusesource.hawtbuf.UTF8Buffer
import java.util.concurrent.CountDownLatch
import org.fusesource.hawtdispatch.Task

if (args.size() < 2) {
    println "Usage publisher <host> <num messages to sent>"
    return
}

String host = args[0]
int numToSend = args[1] as int
MQTT mqtt = new MQTT()
mqtt.setHost(host, 1883)
mqtt.setCleanSession(true)
mqtt.setClientId("PublisherClient")

final CallbackConnection connection = mqtt.callbackConnection()
//connection.resume()
connection.connect(new Callback<Void>() {
    public void onFailure(Throwable th) {
        println "connect fail"
        System.exit(2);
    }

    // Once we connect..
    public void onSuccess(Void v) {
        println "Succesfully connected to server"
    }
})

byte[] message = 'Hello world!!'.bytes
QoS qos = QoS.AT_MOST_ONCE
boolean retain = false

print "publishing.."
CountDownLatch m_latch = new CountDownLatch(1)
final long startTime = System.currentTimeMillis()

class PublishCallback implements Callback<Void> {
    public void onSuccess(Void value) {
        //ok published
        //increment counter
    }

    public void onFailure(Throwable th) {
        println "Publish failed: " + th
        System.exit(2);
    }
}

class ExitTopicCallback implements Callback<Void> {

    private final long startedTime
    private final long numToSend
    private final CallbackConnection connection
    private final CountDownLatch stopLatch

    ExitTopicCallback(long startedTime, long numToSend, CallbackConnection connection, CountDownLatch stopLatch) {
        this.startedTime = startedTime
        this.numToSend = numToSend
        this.connection = connection
        this.stopLatch = stopLatch
    }

    public void onSuccess(Void value2) {
        long stopTime = System.currentTimeMillis()
        long spentTime = stopTime - startedTime
        println "published in ${spentTime} ms"

        this.connection.disconnect(null)
        double msgPerSec = (numToSend / spentTime) * 1000
        println "speed $msgPerSec msg/sec"
        this.stopLatch.countDown()
    }

    public void onFailure(Throwable th) {
        println "Publish failed: " + th
        System.exit(2);
    }
}


def pubCallback = new PublishCallback()
(1..numToSend).each {
    connection.getDispatchQueue().execute(new Task() {
        public void run() {
            connection.publish("/topic", message, qos, retain, pubCallback)
        }
    })
}


def exitCallback = new ExitTopicCallback(startTime, numToSend, connection, m_latch)
connection.getDispatchQueue().execute(new Task() {
    public void run() {
        connection.publish("/exit", message, qos, retain, exitCallback)
    }
})

//wait finish...
m_latch.await()