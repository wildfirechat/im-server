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
sleep(3000)

//now connect a second client but with the same ID
MqttDefaultFilePersistence dataStore2 = new MqttDefaultFilePersistence(tmpDir + "/client2")
MqttClient client2 = new MqttClient("tcp://${host}:1883", "ConnectedClient", dataStore2)
client2.connect()
println "Client2 connected"
try {
    client1.subscribe("topic", 2)
    println "BAD! Client1 subscribed to topic but its session has to be dropped out by client2"
} catch (Exception ex) {
    println "Ok, client1 has been dropped out"
}
sleep(3000)

//now connect a second client but with the same ID
MqttDefaultFilePersistence dataStorePublisher = new MqttDefaultFilePersistence(tmpDir + "/publisher")
MqttClient publisher = new MqttClient("tcp://${host}:1883", "Publisher", dataStorePublisher)
publisher.connect()
println "publisher connected"
client2.subscribe("topic", 2)
println "Client2 subscribed to topic"
sleep(3000)

print "Publisher is going to publish.."
publisher.publish('topic', 'Hello world!!'.bytes, 2, false)
println "Ok mate, done!"

try {
    client1.disconnect()
    println "BAD! Client1 has been able to call disconnect, but it has to be already dropped out"
} catch (Exception ex) {
    println "Ok, client1 can't disconnect because it was already disconnected"
}

client2.disconnect()
println "Client2 disconnected"

publisher.disconnect()
println "publisher disconnected"