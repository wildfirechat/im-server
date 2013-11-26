@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.5')

import java.net.URISyntaxException

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

String host = args[0]
MQTT mqtt = new MQTT()
//mqtt.setHost("test.mosquitto.org", 1883);
mqtt.setHost(host, 1883)
mqtt.setCleanSession(true)
mqtt.setHost(host, 1883)

mqtt.setClientId("SubscriberClient")
BlockingConnection connection = mqtt.blockingConnection()
connection.connect()
println "Connected to ${host}"

Topic[] topics = [new Topic("/topic", QoS.AT_MOST_ONCE), new Topic("/exit", QoS.AT_MOST_ONCE)]
byte[] qoses = connection.subscribe(topics)
println "subscribed to /topic qos: ${qoses[0]}"
println "subscribed to /exit qos: ${qoses[1]}"

int numReceived = 0
long startTime = System.currentTimeMillis()
boolean exit = false

while(!exit) {
    Message message = connection.receive()
    if (message.topic == "/exit") {
        exit = true
    } else {
        numReceived++
//        if ((numReceived % 10000) == 0) {
//            print '.'
//        }
    }
    message.ack()
}
connection.disconnect()
long stopTime = System.currentTimeMillis()
long spentTime = stopTime - startTime
println "/exit received"
println "subscriber disconnected, received ${numReceived} messages in ${spentTime} ms"

