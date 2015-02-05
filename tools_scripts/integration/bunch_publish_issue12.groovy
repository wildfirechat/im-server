import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttDefaultFilePersistence

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir)

MqttClient client = new MqttClient("tcp://localhost:1883", "SampleClient", dataStore)
client.connect()
println "connected"
(1..10).each {
    //MqttMessage message = new MqttMessage("Hello world!! $it".toString().bytes)
    String s = "Hello World!! $it"
    MqttMessage message = new MqttMessage(s.bytes)
    message.qos = 2
    message.retained = true
    print "publishing.."
    client.getTopic("log").publish(message)
    println "published"
}

client.disconnect()
println "disconnected"