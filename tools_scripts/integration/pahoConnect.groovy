@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.0.1', ext='jar')


import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

String host = args[0]

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore1 = new MqttDefaultFilePersistence(tmpDir + "/client1")
MqttClient client1 = new MqttClient("tcp://${host}:1883", "ConnectedClient", dataStore1)
client1.connect()
println "Client1 connected"

println "Ok mate, done!"

client1.disconnect()
println "Client1 disconnected"
