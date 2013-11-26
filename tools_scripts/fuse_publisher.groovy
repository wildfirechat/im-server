@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.5')

import java.net.URISyntaxException

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.QoS

String host = args[0]

MQTT mqtt = new MQTT()
//mqtt.setHost("test.mosquitto.org", 1883);
mqtt.setHost(host, 1883)
BlockingConnection connection = mqtt.blockingConnection()
connection.connect()

print "publishing.."
long startTime = System.currentTimeMillis()
(1..100000).each {
    connection.publish("/topic", 'Hello world!!'.bytes, QoS.AT_MOST_ONCE, false)
}
connection.publish('/exit', 'Exit'.bytes, QoS.AT_MOST_ONCE, false)
long stopTime = System.currentTimeMillis()
long spentTime = stopTime -startTime
println "published in ${spentTime} ms"
connection.disconnect()
println "disconnected"