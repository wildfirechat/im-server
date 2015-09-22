//@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.10')
@GrabResolver(name='Paho', root="https://repo.eclipse.org/content/repositories/paho-releases/")
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.0.2')

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import org.eclipse.paho.client.mqttv3.MqttException

MqttDefaultFilePersistence subDataStore = new MqttDefaultFilePersistence("/tmp/sub")
MqttClient subClient = new MqttClient("tcp://localhost:1883", "ConnectTestCLI", subDataStore)
MqttConnectOptions connectOptions = new MqttConnectOptions()
connectOptions.setUserName("gino")
connectOptions.setPassword("fake".toCharArray())
connectOptions.setKeepAliveInterval(5)
println "Before connect with bad"
try {
    subClient.connect(connectOptions)
} catch (MqttException mex) {
    println "OK, catched connect with bad credentials, wait 4x keepalive"
    sleep(20 * 1000)
}
println "Before connect with ok credentials"
connectOptions = new MqttConnectOptions()
connectOptions.setUserName("testuser")
connectOptions.setPassword("passwd".toCharArray())
subClient.connect(connectOptions)
println "Successfull connected"

subClient.disconnect()
println "Finished!"

//import org.fusesource.mqtt.client.BlockingConnection
//import org.fusesource.mqtt.client.MQTT
//"Usage: groovy issue88_reconnect_after_bad_credentials.groovy"

//MQTT mqtt = new MQTT()
//def host = "localhost"
//mqtt.setCleanSession(true)
//mqtt.setHost(host, 1883)
//mqtt.setUserName("gino")
//mqtt.setPassword("fake")
//
//mqtt.setClientId("BadConnecter_client")
//BlockingConnection connection = mqtt.blockingConnection()
//connection.connect()
//
////try a valid conn
//mqtt.setUserName("testuser")
//mqtt.setPassword("passwd")
//connection = mqtt.blockingConnection()
//connection.connect()
//
//connection.disconnect()
//println "exited"


