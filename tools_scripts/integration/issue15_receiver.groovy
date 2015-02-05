@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.5')

import java.net.URISyntaxException

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic

class Reciever {
    MQTT mqtt = new MQTT()
    BlockingConnection connection
    
    public void connectAndSubscribe(boolean cleanSession) {
        mqtt.setCleanSession(cleanSession)
        mqtt.setHost("localhost", 1883)

        mqtt.setClientId("reciever")
        connection = mqtt.blockingConnection()
        connection.connect()
        Topic[] topics = [new Topic("/topic", QoS.AT_LEAST_ONCE)]
        byte[] qoses = connection.subscribe(topics)
    }
}


Reciever r = new Reciever()
r.connectAndSubscribe(false)
Message message
3.times { i ->
    message = r.connection.receive()
    println "Topic: ${message.topic}, payload: ${new String(message.payload)}"
    message.ack()
}
r.connection.kill()
//r.connection.disconnect()

Thread.sleep(3000)

r.connectAndSubscribe(false)
3.times { i ->
    message = r.connection.receive()
    println "Topic: ${message.topic}, payload: ${new String(message.payload)}"
    message.ack()
}
r.connection.disconnect()

