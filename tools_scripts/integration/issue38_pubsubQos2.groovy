@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.10')

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic

String host = args.size() == 1 ? args[0] : "localhost"

MQTT mqttSub = new MQTT()
mqttSub.setHost(host, 1883)
mqttSub.setClientId("Subscriber(A)")
mqttSub.cleanSession = false
//AT_MOST_ONCE,
//AT_LEAST_ONCE,
//EXACTLY_ONCE
BlockingConnection clientSub = mqttSub.blockingConnection()
clientSub.connect()
println "Sub connected"
Topic[] topics = [new Topic("/sys/abc", QoS.AT_MOST_ONCE)]
byte[] qoses = clientSub.subscribe(topics)
println "Sub subscribed"
clientSub.disconnect()
clientSub.kill()
println "Sub disconnected"


MQTT mqttPub = new MQTT()
mqttPub.setHost(host, 1883)
mqttPub.setClientId("Publisher(B)")
mqttPub.cleanSession = false
BlockingConnection clientPub = mqttPub.blockingConnection()
clientPub.connect()
println "Pub connected"
clientPub.publish("/sys/abc", "A message published".bytes, QoS.EXACTLY_ONCE, false)
println "Pub published"

println "Sub reconnects"
mqttSub = new MQTT()
mqttSub.setHost(host, 1883)
mqttSub.setClientId("Subscriber(A)")
mqttSub.cleanSession = false
clientSub = mqttSub.blockingConnection()
clientSub.connect()
println "Sub connected..and try to receive the message"
Message msg = clientSub.receive()
println "Sub received message \n\t Topic: ${msg.topic}, payload: ${new String(msg.payload)}"
msg.ack()
clientSub.disconnect() //or kill
clientPub.disconnect()

println "Finished!!\n\n"
