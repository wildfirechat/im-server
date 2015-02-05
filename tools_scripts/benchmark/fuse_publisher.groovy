@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.10')

import java.net.URISyntaxException

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.QoS

if (args.size() < 2) {
    println "Usage publisher <host> <num messages to sent>"
    return
}

String host = args[0]
int numToSend = args[1] as int

MQTT mqtt = new MQTT()
//mqtt.setHost("test.mosquitto.org", 1883);
mqtt.setHost(host, 1883)
BlockingConnection connection = mqtt.blockingConnection()
connection.connect()

print "publishing.."
long startTime = System.currentTimeMillis()
(1..numToSend).each {
    connection.publish("/topic", 'Hello world!!'.bytes, QoS.AT_MOST_ONCE, false)
}
connection.publish('/exit', 'Exit'.bytes, QoS.AT_MOST_ONCE, false)
long stopTime = System.currentTimeMillis()
long spentTime = stopTime -startTime
println "published in ${spentTime} ms"
connection.disconnect()
double msgPerSec = (numToSend / spentTime) * 1000
println "speed $msgPerSec msg/sec"
println "disconnected"