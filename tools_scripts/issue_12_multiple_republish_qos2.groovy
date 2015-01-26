@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.5')

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic

//test case moquette 0.7 update for 2015-1-26
//client eclipse.paht.javaclient version :1.0.1
String host = args.size() > 0 ? args[0] : 'localhost'

MQTT mqttPub = new MQTT()
mqttPub.setHost(host, 1883)
mqttPub.setCleanSession(false)
mqttPub.setClientId("PublisherClient")

MQTT mqttSub = new MQTT()
mqttSub.setHost(host, 1883)
mqttSub.setCleanSession(false)
mqttSub.setClientId("FlapSubscriberClient")

BlockingConnection publisher = mqttPub.blockingConnection()
publisher.connect()
println "Publisher connected to ${host}"

BlockingConnection subscriber = mqttSub.blockingConnection()
subscriber.connect()
println "${subscriber} connected to ${host}"
Topic[] topics = [new Topic("/topic", QoS.AT_MOST_ONCE)]
byte[] qoses = subscriber.subscribe(topics)
println "${subscriber} Subscribed to ${topics}"

subscriber.disconnect()
println "${subscriber} disconnects immediately"

println "${publisher} publishes QoS 2 messages on /topic"
publisher.publish("/topic", 'Hello world!!'.bytes, QoS.EXACTLY_ONCE, false)


subscriber = mqttSub.blockingConnection()
subscriber.connect()
println "${subscriber} reconnects to ${host}"

Message message = subscriber.receive()
assert "/topic" == message.topic
String msgBody = new String(message.payload)
assert "Hello world!!" == msgBody
message.ack()
println "${subscriber} received on topic ${message.topic} the message \n\t ${msgBody}"
subscriber.disconnect()
println "${subscriber} disconnects again"

subscriber = mqttSub.blockingConnection()
subscriber.connect()
println "${subscriber} reconnects again to ${host}"

println "Wait receive something..."
message = subscriber.receive()
msgBody = new String(message.payload)
assert "Hello world!!" == msgBody
message.ack()
println "${subscriber} received again on topic ${message.topic} the message \n\t ${msgBody}"

//aclient (clear session false)
//bclient (clear session false)
//
//a -> connection Server
//b -> connection Server
//b -> subscriptions (qos =0)
//b -> distconnection or lostconnection
//
//a ->publish ("abc/topic","test1",qos=2,false);
//
//...
//a ->publish ("abc/topic","test10",qos=2,false);
//
//b -> connection Server
//received message test1
//...
//received message test10
//
//b-> distconnection Server or loast connection
//b->connnection Server
//received message test1
//...
//received message test10
//
//why repeat received message
