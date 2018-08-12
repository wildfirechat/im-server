@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.0.1', ext='jar')


import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

/**
 * A client that connect and disconnect multiple times, to check that on disconnection there isn't
 * any resource leakage.
 * */

if (args.size() != 3) {
    println "usage: <host> <num_serial_connections> <local client name>"
    return
}

String host = args[0]
long numSerialConnection = args[1] as long
String clientName = args[2]

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore1 = new MqttDefaultFilePersistence(tmpDir + "/" + clientName)
MqttClient client1 = new MqttClient("tcp://${host}:1883", "ConCli${clientName}", dataStore1)

println "start loppoing to ${numSerialConnection} conn-discon"
(1..numSerialConnection).each {
    client1.connect()
    client1.disconnect()
    print "."
}
println "Done"
