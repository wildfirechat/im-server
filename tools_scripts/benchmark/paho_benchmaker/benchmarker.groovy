//@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.1.0')
@Grab(group='org.eclipse.jetty.toolchain', module='jetty-perf-helper', version='1.0.5')


import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import org.eclipse.paho.client.mqttv3.MqttAsyncClient

//import BenchmarkSubscriber
import BenchmarkPublisher

if (args.size() < 3) {
    println "Usage benchmarker <host> <num messages to sent> <frequency>[msg/sec] :[dialog_id]"
    println "Ex localhost 10000 5000 test1"
    println "should take 2 secs"
    return
}

println "*** Histogram measures are in microseconds ***"

String host = args[0]
int numToSend = args[1] as int
int messagesPerSecond = args[2] as int
String dialog_id = args.size() > 3 ? args[3] : ""

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir)
MqttAsyncClient pub = new MqttAsyncClient("tcp://${host}:1883", "PublisherClient${dialog_id}", dataStore)

MqttDefaultFilePersistence dataStoreSub = new MqttDefaultFilePersistence(tmpDir)
MqttAsyncClient sub = new MqttAsyncClient("tcp://${host}:1883", "SubscriberClient${dialog_id}", dataStoreSub)

BenchmarkSubscriber suscriberBench = new BenchmarkSubscriber(client: sub, dialog_id: dialog_id)
suscriberBench.connect()

BenchmarkPublisher publisherBench = new BenchmarkPublisher(client: pub, numToSend: numToSend,
        messagesPerSecond: messagesPerSecond, dialog_id: dialog_id)
publisherBench.connect()
publisherBench.firePublishes()

suscriberBench.waitFinish()
publisherBench.waitFinish()


