@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/")
@Grab(group='org.eclipse.paho', module='mqtt-client', version='0.4.0')

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttDefaultFilePersistence

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir)

MqttClient client = new MqttClient("tcp://localhost:1883", "SampleClient", dataStore)
client.connect()
MqttMessage message = new MqttMessage('Hello world!!'.bytes)
message.setQos(2)
print "publishing.."
client.getTopic("log").publish(message)
println "published"
client.disconnect()
println "disconnected"