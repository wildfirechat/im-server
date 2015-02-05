@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.10')

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
import java.util.concurrent.TimeUnit

if (args.size() != 1) {
    println "Usage subscriber <host>"
    return
}

String host = args[0]
MQTT mqtt = new MQTT()
//mqtt.setHost("test.mosquitto.org", 1883);
mqtt.setHost(host, 1883)
mqtt.setCleanSession(true)
mqtt.setHost(host, 1883)
mqtt.setClientId("SubscriberClient")

int numReceived = 0
long startTime = System.currentTimeMillis()
boolean exit = false
CountDownLatch m_latch = new CountDownLatch(1)

final CallbackConnection connection = mqtt.callbackConnection()
connection.listener(new Listener() {

    public void onDisconnected() {
        println "Listener received disconnection"
    }
    public void onConnected() {
        println "Listener received a connected to ${host}"
        startTime = System.currentTimeMillis()
    }

    public void onPublish(UTF8Buffer topic, Buffer payload, Runnable ack) {
        // You can now process a received message from a topic.
        // Once process execute the ack runnable.
        ack.run()
        //println "Received a publish on topic ${topic}"
        if (topic.toString() == "/exit") {
            println "Ok Exit!"
            exit = true
            connection.disconnect(null)
            long stopTime = System.currentTimeMillis()
            long spentTime = stopTime - startTime
            println "/exit received"
            println "subscriber disconnected, received ${numReceived} messages in ${spentTime} ms"
            double msgPerSec = (numReceived / spentTime) * 1000
            println "speed $msgPerSec msg/sec"
            m_latch.countDown()
        } else {
            numReceived++
//        if ((numReceived % 10000) == 0) {
//            print '.'
//        }
        }

    }
    public void onFailure(Throwable value) {
        connection.disconnect(null)
        println "Some thing failed $value"
        m_latch.countDown()
    }
})

//Topic[] topics = [new Topic("/topic", QoS.AT_MOST_ONCE), new Topic("/exit", QoS.AT_MOST_ONCE)]
//byte[] qoses = connection.subscribe(topics)
//println "subscribed to /topic qos: ${qoses[0]}"
//println "subscribed to /exit qos: ${qoses[1]}"

connection.connect(new Callback<Void>() {
    public void onFailure(Throwable value) {
        System.exit(2);
    }

    // Once we connect..
    public void onSuccess(Void v) {
        // Subscribe to /topic
        Topic[] topics = [new Topic("/topic", QoS.AT_LEAST_ONCE)]
        connection.subscribe(topics, new Callback<byte[]>() {
            public void onSuccess(byte[] qoses) {
                // The result of the subcribe request.
                println "subscribed to /topic ${qoses}"
            }
            public void onFailure(Throwable value) {
                connection.disconnect(null); // subscribe failed.
            }
        });


        // Subscribe to /topic
        Topic[] exitTopic = [new Topic("/exit", QoS.AT_LEAST_ONCE)]
        connection.subscribe(exitTopic, new Callback<byte[]>() {
            public void onSuccess(byte[] qoses) {
                // The result of the subcribe request.
                println "subscribed to /exit ${qoses}"
            }
            public void onFailure(Throwable value) {
                connection.disconnect(null); // subscribe failed.
                m_latch.countDown()
            }
        });
    }
})


m_latch.await()