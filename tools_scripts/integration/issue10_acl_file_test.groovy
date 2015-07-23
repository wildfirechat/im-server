@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.10')

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic


String host = args[0]
//start a publisher
MQTT mqtt2 = new MQTT()
mqtt2.setHost(host, 1883)
mqtt2.setUserName("gravity")
mqtt2.setPassword("passwd")
BlockingConnection client = mqtt2.blockingConnection()
client.connect()
String topicOut = "out"
String topicIn = "in"
/*println "try to publish (write) on topic ${topicOut}"

client.publish(topicOut, 'publish test!!'.bytes, QoS.AT_MOST_ONCE, false)
println "published"

println "try to subscribe (read) on topic ${topicIn}"
client.subscribe([new Topic(topicIn, QoS.AT_LEAST_ONCE)] as Topic[])
println "subscribed"*/
println "try to subscribe (read) on topic that should fail, ${topicOut}"
client.subscribe([new Topic(topicOut, QoS.AT_LEAST_ONCE)] as Topic[])
println "if subscribed is an error!!"

/*String topicUntouch = "untouchable"
println "try to subscribe (read) on untouchable topic for this user,  ${topicUntouch}"
client.subscribe([new Topic(topicUntouch, QoS.AT_LEAST_ONCE)] as Topic[])
println "subscribed to untouchable, if subscribed is an error!!"*/

println "shutdown client"
client.disconnect()