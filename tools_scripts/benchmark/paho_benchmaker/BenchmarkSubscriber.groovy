import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback


import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.HdrHistogram.Histogram

class BenchmarkSubscriber {
    class SubscriberCallback implements MqttCallback {

//        private final CountDownLatch m_latch
//        SubscriberCallback()

        public void messageArrived(String topic, MqttMessage message) throws Exception {
            //used to catch the first message published
            if (!alreadyStarted) {
                startTime = System.currentTimeMillis()
                alreadyStarted = true
            }

            //println "Received a publish on topic ${topic}"
            if (topic.toString().startsWith("/exit")) {
                println "SUB: ok Exit!"
                exit = true
                println "Before disconnect"
                println "Before disconnect clienst $client"
//                client.disconnect(1000) //wait max 1 sec
                //client.disconnectForcibly()
                //client.close()
                println "After disconnect"
                long stopTime = System.currentTimeMillis()
                long spentTime = stopTime - startTime
                println "SUB: " + topic.toString() + " received"
                println "SUB: subscriber disconnected, received ${numReceived} messages in ${spentTime} ms"
                double msgPerSec = (numReceived / spentTime) * 1000
                println "SUB: Speed: $msgPerSec msg/sec"
                println "SUB: Latency diagram [microsecs]"
                histogram.outputPercentileDistribution(System.out, 1000.0); //nanos/1000
                m_latch.countDown()
            } else {
                String payload = new String(message.payload)
                long sentTime = payload.split('-')[1] as long
                long delay = System.nanoTime() - sentTime
                histogram.recordValue(delay)
                print '+'
                numReceived++
//        if ((numReceived % 10000) == 0) {
//            print '.'
//        }
            }

        }

        public void connectionLost(Throwable cause) {
            println "Client ${clientCounter} lost connection, caused by ${cause}"
            client.disconnect()
            println "SUB: Something failed during message reception"
            th.printStackTrace()
            m_latch.countDown()
        }

        public void deliveryComplete(IMqttDeliveryToken token) {

        }

    }

    private int numReceived = 0
    private long startTime = System.currentTimeMillis()
    private boolean exit = false
    private CountDownLatch m_latch = new CountDownLatch(1)
    private boolean alreadyStarted = false
    private Histogram histogram = new Histogram(5)

    private IMqttAsyncClient client
    private String dialog_id

    def connect() {
        MqttConnectOptions connectOptions = new MqttConnectOptions()
        connectOptions.cleanSession = true
        this.client.callback = new SubscriberCallback()
        this.client.connect(connectOptions, null, new IMqttActionListener() {

            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                println "SUB: connect fail"
                println "Excp: ${exception}"
                System.exit(2)
            }

            public void onSuccess(IMqttToken asyncActionToken) {
                this.client.subscribe("/topic" + dialog_id, 1, null, new IMqttActionListener() {
                    public void onFailure(IMqttToken asyncActionToken2, Throwable exception) {
                        this.client.disconnect()
                    }

                    public void onSuccess(IMqttToken asyncActionToken2) {
                        println "SUB: subscribed to /topic${dialog_id} qos: 1"
                    }
                })

                this.client.subscribe("/exit" + dialog_id, 1, null, new IMqttActionListener() {
                    public void onFailure(IMqttToken asyncActionToken2, Throwable exception) {
                        this.client.disconnect()
                        m_latch.countDown()
                    }

                    public void onSuccess(IMqttToken asyncActionToken2) {
                        println "SUB: subscribed to /exit${dialog_id} qos: 1"
                    }
                })
            }
        })
    }


    def waitFinish() {
        this.m_latch.await()
    }

}
