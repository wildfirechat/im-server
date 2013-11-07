@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='mqtt-client', version='0.4.0')

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

String host = args[0]

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir)

MqttClient client = new MqttClient("tcp://${host}:1883", "PublisherClient", dataStore)
client.connect()
MqttMessage message = new MqttMessage('Hello world!!'.bytes)
message.setQos(0)
print "publishing.."
client.publish('log', 'Hello world!!'.bytes, 0, false)

client.publish('/exit', 'Exit'.bytes, 0, false)
client.disconnect()
println "disconnected"