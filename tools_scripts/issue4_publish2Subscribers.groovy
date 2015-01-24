@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.5')

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic

String host = args[0]
MQTT mqtt = new MQTT()
mqtt.setHost(host, 1883)
mqtt.setCleanSession(true)
mqtt.setClientId("SubscriberClient")

BlockingConnection subscriber = mqtt.blockingConnection()
subscriber.connect()
println "Connected to ${host}"

Topic[] topics = [new Topic("/topic", QoS.AT_MOST_ONCE)]
byte[] qoses = subscriber.subscribe(topics)
println "Subscribed to ${topics}"

//start a publisher
MQTT mqtt2 = new MQTT()
mqtt2.setHost(host, 1883)
BlockingConnection publisher = mqtt2.blockingConnection()
publisher.connect()
publisher.publish("/topic", 'Hello world!!'.bytes, QoS.AT_MOST_ONCE, false)

//check subscriber received
Message message = subscriber.receive()
assert "/topic" == message.topic
assert "Hello world!!" == new String(message.payload)
message.ack()

println "shutdown subscriber"
subscriber.disconnect()

println "shutdown publisher"
publisher.disconnect()

println "Kill your broker and start it again, then trivialPublish.groovy"