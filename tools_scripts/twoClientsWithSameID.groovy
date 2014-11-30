@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.0.1', ext='jar')


import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

String host = args[0]

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore1 = new MqttDefaultFilePersistence(tmpDir + "/client1")
MqttClient client1 = new MqttClient("tcp://${host}:1883", "ConnectedClient", dataStore1)
client1.connect()
println "Client1 connected"
client1.subscribe("topic", 0)
println "Client1 subscribed to topic"

sleep(500)

//now connect a second client but with the same ID
MqttDefaultFilePersistence dataStore2 = new MqttDefaultFilePersistence(tmpDir + "/client2")
MqttClient client2 = new MqttClient("tcp://${host}:1883", "ConnectedClient", dataStore2)
client2.connect()
println "Client2 connected"
client2.subscribe("topic", 0)
println "Client2 subscribed to topic"

client1.disconnect()
println "Client1 disconnected"

client2.disconnect()
println "Client2 disconnected"