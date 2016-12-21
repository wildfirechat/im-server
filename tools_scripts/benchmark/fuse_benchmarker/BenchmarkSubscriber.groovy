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
import org.HdrHistogram.Histogram

class BenchmarkSubscriber {
    private int numReceived = 0
    private long startTime = System.currentTimeMillis()
    private boolean exit = false
    private CountDownLatch m_latch = new CountDownLatch(1)
    private boolean alreadyStarted = false
    private Histogram histogram = new Histogram(5)
    private CallbackConnection connection

    private MQTT mqtt
    private String dialog_id

//    BenchmarkSubscriber(MQTT mqtt) {
//        this.mqtt = mqtt
//    }
    def connect() {
        this.connection = mqtt.callbackConnection()
        connection.listener(new Listener() {

            public void onDisconnected() {
                println "SUB: listener received disconnection"
            }
            public void onConnected() {
                println "SUB: listener received a connected to ${mqtt.host}"
//        startTime = System.currentTimeMillis()
            }

            public void onPublish(UTF8Buffer topic, Buffer payload, Runnable ack) {
                // You can now process a received message from a topic.
                // Once process execute the ack runnable.
                ack.run()

                //used to catch the first message published
                if (!alreadyStarted) {
                    startTime = System.currentTimeMillis()
                    alreadyStarted = true
                }

                //println "Received a publish on topic ${topic}"
                if (topic.toString().startsWith("/exit")) {
                    println "SUB: ok Exit!"
                    exit = true
                    connection.disconnect(null)
                    long stopTime = System.currentTimeMillis()
                    long spentTime = stopTime - startTime
                    println "SUB: " + topic.toString() + " received"
                    println "SUB: subscriber disconnected, received ${numReceived} messages in ${spentTime} ms"
                    double msgPerSec = (numReceived / spentTime) * 1000
                    println "SUB: Speed: $msgPerSec msg/sec"
                    println "SUB: Latency diagram"
                    histogram.outputPercentileDistribution(System.out, 1000.0); //nanos/1000
                    m_latch.countDown()
                } else {
                    String message = payload as String
                    long sentTime = message.split('-')[1] as long
                    long delay = System.nanoTime() - sentTime
                    histogram.recordValue(delay)
                    print '+'
                    numReceived++
//        if ((numReceived % 10000) == 0) {
//            print '.'
//        }
                }

            }
            public void onFailure(Throwable th) {
                connection.disconnect(null)
                println "SUB: Something failed during message reception"
                th.printStackTrace()
                m_latch.countDown()
            }
        })

        connection.connect(new Callback<Void>() {
            public void onFailure(Throwable value) {
                System.exit(2);
            }

            // Once we connect..
            public void onSuccess(Void v) {
                // Subscribe to /topic
                Topic[] topics = [new Topic("/topic" + dialog_id, QoS.AT_LEAST_ONCE)]
                connection.subscribe(topics, new Callback<byte[]>() {
                    public void onSuccess(byte[] qoses) {
                        // The result of the subcribe request.
                        println "SUB: subscribed to /topic${dialog_id} ${qoses}"
                    }
                    public void onFailure(Throwable value) {
                        connection.disconnect(null); // subscribe failed.
                    }
                });


                // Subscribe to /topic
                Topic[] exitTopic = [new Topic("/exit" + dialog_id, QoS.AT_LEAST_ONCE)]
                connection.subscribe(exitTopic, new Callback<byte[]>() {
                    public void onSuccess(byte[] qoses) {
                        // The result of the subcribe request.
                        println "SUB: subscribed to /exit${dialog_id} ${qoses}"
                    }
                    public void onFailure(Throwable value) {
                        connection.disconnect(null); // subscribe failed.
                        m_latch.countDown()
                    }
                });
            }
        })
    }


    def waitFinish() {
        this.m_latch.await()
    }

}