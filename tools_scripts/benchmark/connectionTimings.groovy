@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='mqtt-client', version='0.4.0')

/**
 * Ths script is meant to measure the connection mean time from a client.
 * It loops connecting and disconnecting to the broker.
 * */

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir)

MqttClient client = new MqttClient("tcp://localhost:1883", "ReconnectingClient", dataStore)
10.times {
    println "conneting.."
    long start = System.currentTimeMillis()
    client.connect()
    long time = System.currentTimeMillis() - start
    print "took ${time} ms "

    MqttMessage message = new MqttMessage('Hello world!!'.bytes)
    message.setQos(0)
    long startPub = System.currentTimeMillis()
    client.getTopic("log").publish(message)
    long pubTime = System.currentTimeMillis() - startPub
    print " pub (qos 0): ${pubTime} ms "

    client.disconnect()
    println "disconnected"
}
println "Finished"