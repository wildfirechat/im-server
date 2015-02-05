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
//mqtt.setHost("test.mosquitto.org", 1883);
mqtt.setHost(host, 1883)
mqtt.setCleanSession(true)
mqtt.setHost(host, 1883)
mqtt.setClientId("PublisherClient")

long startTime = System.currentTimeMillis()
CountDownLatch m_latch = new CountDownLatch(1)

final CallbackConnection connection = mqtt.callbackConnection()
//connection.resume()
connection.connect(new Callback<Void>() {
    public void onFailure(Throwable th) {
        println "connect fail"
        System.exit(2);
    }

    // Once we connect..
    public void onSuccess(Void v) {

    }
})

String topic = "/topic"
byte[] message = 'Hello world!!'.bytes
QoS qos = QoS.AT_MOST_ONCE
boolean retain = false

print "publishing.."
startTime = System.currentTimeMillis()

new Task() {
    long sent = 0;
    public void run() {
        final Task publish = this

        connection.publish(topic, message, qos, retain, new Callback<Void>() {
            public void onSuccess(Void value) {
                sent ++;

                if( sent < numToSend ) {
                    connection.getDispatchQueue().execute(publish);
                } else {
                    connection.getDispatchQueue().execute(new Runnable() {
                        public void run() {
                            connection.publish("/exit", message, qos, retain, new Callback<Void>() {
                                public void onSuccess(Void value2) {
                                    long stopTime = System.currentTimeMillis()
                                    long spentTime = stopTime -startTime
                                    println "published in ${spentTime} ms"

                                    connection.disconnect(null)
                                    double msgPerSec = (numToSend / spentTime) * 1000
                                    println "speed $msgPerSec msg/sec"

                                    connection.disconnect(new Callback<Void>() {
                                        public void onSuccess(Void valueSuccess2) {
                                            m_latch.countDown();
                                        }
                                        public void onFailure(Throwable th2) {
                                            m_latch.countDown();
                                        }
                                    });
                                }

                                public void onFailure(Throwable th) {
                                    println "Publish failed: " + th

                                    System.exit(2);
                                }
                            });
                        }
                    });

                }
            }
            public void onFailure(Throwable th) {
                println "Publish failed: " + th

                System.exit(2);
            }
        });
    }
}.run();

m_latch.await()