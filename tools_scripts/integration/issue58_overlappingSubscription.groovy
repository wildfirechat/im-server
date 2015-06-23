@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.10')

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic

import java.util.concurrent.TimeUnit

String host = args.size() == 1 ? args[0] : "localhost"

MQTT mqttSub = new MQTT()
mqttSub.setHost(host, 1883)
mqttSub.setClientId("Subscriber")
mqttSub.cleanSession = false
//AT_MOST_ONCE,
//AT_LEAST_ONCE,
//EXACTLY_ONCE
BlockingConnection clientSub = mqttSub.blockingConnection()
clientSub.connect()
println "Sub connected"
Topic[] overlappingSubscriptions = [new Topic("a/+", QoS.EXACTLY_ONCE),
                                    new Topic("a/b", QoS.AT_LEAST_ONCE)]
byte[] qoses = clientSub.subscribe(overlappingSubscriptions)
println "Sub subscribed"

MQTT mqttPub = new MQTT()
mqttPub.setHost(host, 1883)
mqttPub.setClientId("Publisher")
mqttPub.cleanSession = false

BlockingConnection clientPub = mqttPub.blockingConnection()
clientPub.connect()
println "Pub connected"
clientPub.publish("a/b", "Hello world".bytes, QoS.AT_LEAST_ONCE, false)
println "Pub published"
Message msg = clientSub.receive()
println "Sub received message \n\t Topic: ${msg.topic}, payload: ${new String(msg.payload)}"
msg.ack()

//try to receive another NOT want publish
msg = clientSub.receive(1, TimeUnit.SECONDS)
if (msg != null) {
    println "Second possible message, payload: ${new String(msg.payload)}, topic: ${msg.topic}"
}

clientPub.disconnect()
clientPub.kill()
println "Pub disconnected"

clientSub.disconnect()
clientSub.kill()
println "Sub disconnected"

println "Finished!!\n\n"
