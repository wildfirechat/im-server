@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.14')
@Grab(group='org.eclipse.jetty.toolchain', module='jetty-perf-helper', version='1.0.5')

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

import BenchmarkSubscriber
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

MQTT pub = new MQTT()
pub.setHost(host, 1883)
pub.setCleanSession(true)
pub.setClientId("PublisherClient${dialog_id}")

MQTT sub = new MQTT()
//mqtt.setHost("test.mosquitto.org", 1883);
sub.setHost(host, 1883)
sub.setCleanSession(true)
sub.setClientId("SubscriberClient${dialog_id}")

BenchmarkSubscriber suscriberBench = new BenchmarkSubscriber(mqtt: sub, dialog_id: dialog_id)
suscriberBench.connect()

BenchmarkPublisher publisherBench = new BenchmarkPublisher(mqtt: pub, numToSend: numToSend,
        messagesPerSecond: messagesPerSecond, dialog_id: dialog_id)
publisherBench.connect()
publisherBench.firePublishes()

suscriberBench.waitFinish()
publisherBench.waitFinish()


