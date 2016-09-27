@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='mqtt-client', version='0.4.0')
@Grab(group='org.eclipse.jetty.toolchain', module='jetty-perf-helper', version='1.0.5')

import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import org.HdrHistogram.Histogram
import org.eclipse.jetty.toolchain.perf.PlatformTimer

import java.nio.ByteBuffer
import java.util.concurrent.Semaphore

/**
 * Performance metric test for issue 204.
 * One topic per client
 * Flow:
 * [MEASURE START]
 * - connect
 * - subscribe to topic "test"+i
 * - publish something on "test"+i
 * - unsubscribe from "test"+i
 * - disconnect
 * [MEASURE STOP]
 * */

class WorkflowTask implements MqttCallback {
    private MqttAsyncClient client
    private int clientCounter
    private long startTime
    private long numOfIterations
    Histogram histogram
    private int pauseMicroseconds
    private PlatformTimer timer = PlatformTimer.detect()
    private Semaphore disconnectReceived = new Semaphore(0)

    void execute() {
        client.callback = this

        (1..numOfIterations).each { it ->
            startTime = System.nanoTime()
            client.connect().waitForCompletion(1000)
            println "connected ${it}"
            client.subscribe("/topic${clientCounter}", 0)
            println "subscribed to /topic${clientCounter}"
            //byte[] bytes = ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array()
            byte[] bytes = "TestPayload".bytes
            client.publish("/topic${clientCounter}", bytes, 0, false)
            println "published to /topic${clientCounter}..wait for receive of message"

            disconnectReceived.acquire()
            println "Acquired semaphore, value ${disconnectReceived.availablePermits()}"
            client.disconnect().waitForCompletion(1000)
            println "Disconnected"
            long stopTime = System.nanoTime()
            long spentNanoSeconds = stopTime - startTime
            histogram.recordValue(spentNanoSeconds)

            //timer.sleep(pauseMicroseconds)
        }
        println "Latency diagram (micro secs):\n"
        histogram.outputPercentileDistribution(System.out, 1000.0)
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        println "Received published"
        if (topic != "/topic${clientCounter}".toString()) {
            println "Error received a publish on not matching topic ${topic}"
            return
        }
        long stopTime = System.nanoTime()
        long spentNanoSeconds = stopTime - startTime

        disconnectReceived.release()
        println "Released semaphore, value ${disconnectReceived.availablePermits()}"
    }

    public void connectionLost(Throwable cause) {
//        println "Client ${clientCounter} lost connection"
    }

    public void deliveryComplete(IMqttDeliveryToken token) {

    }

}

MqttAsyncClient createClient(int i) {
    String tmpDir = System.getProperty("java.io.tmpdir")
    MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir)
    new MqttAsyncClient("tcp://localhost:1883", "TestClient${i}", dataStore)
}

Histogram histogram = new Histogram(5)
if (args.size() != 2) {
    println "Usage:"
    println "\n groovy conSubPubUnsub_issue204.groovy <num of iterations per second> <seconds>"
    return 1
}
int numOfIterationsPerSecond = args[0] as int
int duration = args[1] as int
long pauseMicroseconds = (1 / numOfIterationsPerSecond) * 1000 * 1000
long numOfIterations = numOfIterationsPerSecond * duration

MqttAsyncClient client = createClient(0)
def task = new WorkflowTask(client: client, clientCounter: 0, histogram: histogram,
        pauseMicroseconds: pauseMicroseconds, numOfIterations: numOfIterations)

task.execute()

//println "Latency diagram:\n"
//histogram.outputPercentileDistribution(System.out, 1000.0)