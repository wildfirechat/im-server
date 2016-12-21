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

import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import org.fusesource.hawtdispatch.Task
import org.eclipse.jetty.toolchain.perf.PlatformTimer

class BenchmarkPublisher {
    static class PublishCallback implements Callback<Void> {
        public void onSuccess(Void value) {
            //ok published
            //increment counter
        }

        public void onFailure(Throwable th) {
            println "PUB: Publish failed: " + th
            th.printStackTrace()
            System.exit(2)
        }
    }

    static class ExitTopicCallback implements Callback<Void> {

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
            println "PUB: published in ${spentTime} ms"

            this.connection.disconnect(null)
            double msgPerSec = (numToSend / spentTime) * 1000
            println "PUB: speed $msgPerSec msg/sec"
            this.stopLatch.countDown()
        }

        public void onFailure(Throwable th) {
            println "PUB: Publish failed: " + th
            System.exit(2);
        }
    }

    private QoS qos = QoS.AT_MOST_ONCE
    private boolean retain = false
    private int numToSend
    private int messagesPerSecond
    private String dialog_id
    private MQTT mqtt
    private CallbackConnection connection
    private CountDownLatch m_latch

    def connect() {
        this.connection = mqtt.callbackConnection()
        //connection.resume()
        connection.connect(new Callback<Void>() {
            public void onFailure(Throwable th) {
                println "PUB: connect fail"
                System.exit(2);
            }

            // Once we connect..
            public void onSuccess(Void v) {
                println "PUB: Successfully connected to server"
            }
        })
        this.m_latch = new CountDownLatch(1)
    }

    def firePublishes() {
        long pauseMicroseconds = (1 / this.messagesPerSecond) * 1000 * 1000
        println "PUB: Pause over the each message sent ${pauseMicroseconds} microsecs"
        //byte[] message = 'Hello world!!'.bytes

        print "PUB: publishing.."
        CountDownLatch m_latch = new CountDownLatch(1)
        final long startTime = System.currentTimeMillis()

        //initialize the timer
        PlatformTimer timer = PlatformTimer.detect()
        def pubCallback = new PublishCallback()
        (1..numToSend).each {
            connection.getDispatchQueue().execute(new Task() {
                public void run() {
                    long nanos = System.nanoTime()
                    byte[] message = "Hello world!!-${nanos}".bytes
                    connection.publish("/topic" + dialog_id, message, qos, retain, pubCallback)
                    print ".\n"
                }
            })
            timer.sleep(pauseMicroseconds)
        }

        def exitCallback = new ExitTopicCallback(startTime, numToSend, connection, m_latch)
        connection.getDispatchQueue().execute(new Task() {
            public void run() {
                long nanos = System.nanoTime()
                byte[] message = "Now exit!!-${nanos}".bytes
                connection.publish("/exit" + dialog_id, message, qos, retain, exitCallback)
            }
        })
    }


    def waitFinish() {
        //wait finish...
        m_latch.await()
    }

}