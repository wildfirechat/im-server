import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient

import java.util.concurrent.CountDownLatch
import org.eclipse.jetty.toolchain.perf.PlatformTimer

class BenchmarkPublisher {
    static class PublishCallback implements IMqttActionListener {
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            println "PUB: Publish failed: " + exception
            th.printStackTrace()
            System.exit(2)
        }

        public void onSuccess(IMqttToken asyncActionToken) {
        }
    }

    static class ExitTopicCallback implements IMqttActionListener {

        private final long startedTime
        private final long numToSend
        private final IMqttAsyncClient connection
        private final CountDownLatch stopLatch

        ExitTopicCallback(long startedTime, long numToSend, IMqttAsyncClient connection, CountDownLatch stopLatch) {
            this.startedTime = startedTime
            this.numToSend = numToSend
            this.connection = connection
            this.stopLatch = stopLatch
        }

        public void onSuccess(IMqttToken asyncActionToken) {
            long stopTime = System.currentTimeMillis()
            long spentTime = stopTime - startedTime
            println "PUB: published in ${spentTime} ms"

            this.connection.disconnect()
            double msgPerSec = (numToSend / spentTime) * 1000
            println "PUB: speed $msgPerSec msg/sec"
            this.stopLatch.countDown()
        }

        public void onFailure(IMqttToken asyncActionToken, Throwable th) {
            println "PUB: Publish failed: " + th
            System.exit(2);
        }
    }

    private int qos = 0
    private boolean retain = false
    private int numToSend
    private int messagesPerSecond
    private String dialog_id
    private IMqttAsyncClient client
    private CountDownLatch m_latch

    def connect() {
        MqttConnectOptions connectOptions = new MqttConnectOptions()
        connectOptions.cleanSession = true
        this.client.connect(connectOptions, null, new IMqttActionListener() {
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                println "PUB: connect fail"
                println "Excp: ${exception}"
                System.exit(2)
            }

            public void onSuccess(IMqttToken asyncActionToken) {
                println "PUB: Successfully connected to server"
            }
        })

        this.m_latch = new CountDownLatch(1)
    }

    def firePublishes() {
        long pauseMicroseconds = (1 / this.messagesPerSecond) * 1000 * 1000
        println "PUB: Pause over the each message sent ${pauseMicroseconds} microsecs"

        print "PUB: publishing.."
        CountDownLatch m_latch = new CountDownLatch(1)
        final long startTime = System.currentTimeMillis()

        //initialize the timer
        PlatformTimer timer = PlatformTimer.detect()
        def pubCallback = new PublishCallback()
        println ""
        (1..numToSend).each {
            long nanos = System.nanoTime()
            byte[] message = "Hello world!!-${nanos}".bytes
            this.client.publish("/topic" + dialog_id, message, qos, retain, null, pubCallback)
            print '.'
            timer.sleep(pauseMicroseconds)
        }

        def exitCallback = new ExitTopicCallback(startTime, numToSend, this.client, m_latch)
        long nanosExit = System.nanoTime()
        byte[] exitMessage = "Hello world!!-${nanosExit}".bytes
        this.client.publish("/exit" + dialog_id, exitMessage, qos, retain, null, exitCallback)
    }


    def waitFinish() {
        m_latch.await()
    }

}
