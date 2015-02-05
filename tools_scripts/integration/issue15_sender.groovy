//@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.5')

import java.net.URISyntaxException

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.QoS


MQTT mqtt = new MQTT()
//mqtt.setHost("test.mosquitto.org", 1883);
mqtt.setHost("localhost", 1883)

BlockingConnection connection = mqtt.blockingConnection()
connection.connect()

//for (int i = 0; i < 1000; i++) {
(1..1000).each{ i -> 
//        LOG.info("Publishing");
        String payload = "Hello world MQTT!!${i}"
        connection.publish("/topic", payload.getBytes(), QoS.AT_LEAST_ONCE, false)
        println payload
        Thread.sleep 1000
}

connection.disconnect()
