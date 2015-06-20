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
Topic[] topics = [new Topic("#MQTTClient", QoS.AT_MOST_ONCE)]
byte[] qoses = clientSub.subscribe(topics)
println "Sub subscribed"
clientSub.disconnect()
clientSub.kill()
println "Sub disconnected"

println "Finished!!\n\n"
