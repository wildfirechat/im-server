@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
//@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.0.1', ext='jar')
@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.10')

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic

String host = args.size() == 1 ? args[0] : "localhost"

MQTT mqttWill = new MQTT()
mqttWill.setHost(host, 1883)
mqttWill.setClientId("WillClient")
mqttWill.setWillTopic("/willtopic")
mqttWill.setWillMessage("Will message testment")
//AT_MOST_ONCE,
//AT_LEAST_ONCE,
//EXACTLY_ONCE
mqttWill.willQos = QoS.AT_LEAST_ONCE
BlockingConnection willClient = mqttWill.blockingConnection()
willClient.connect()
println "Will Client connected"


println "Connect a subscriber to the topic"
MQTT mqtt = new MQTT()
//mqtt.setHost("test.mosquitto.org", 1883);
mqtt.setHost(host, 1883)
mqtt.setCleanSession(true)
mqtt.setHost(host, 1883)

mqtt.setClientId("SubscriberClient")
BlockingConnection connection = mqtt.blockingConnection()
connection.connect()
println "Connected to ${host}"

Topic[] topics = [new Topic("/willtopic", QoS.AT_MOST_ONCE)]
byte[] qoses = connection.subscribe(topics)
println "subscribed to /willtopic qos: ${qoses[0]}"

//simulate a disconnection of the willclient
willClient.kill()

Message message = connection.receive()
assert message.topic == "/willtopic"
message.ack()

connection.disconnect()

