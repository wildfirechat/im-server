@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.10')
//@GrabResolver(name='Paho', root="https://repo.eclipse.org/content/repositories/paho-releases/")
//@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.0.2')

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic

class PublisherThread extends Thread {

    private final int pubId
    private final String host = "localhost"
    private final byte[] payload
    private final int numToSend

    PublisherThread(int pubId, int numToSend, byte[] payload) {
        this.numToSend = numToSend
        this.payload = payload
        this.pubId = pubId
    }

    @Override
    public void run() {
        MQTT mqtt = new MQTT()
        //mqtt.setHost("test.mosquitto.org", 1883);
        mqtt.setHost(host, 1883)
        mqtt.setCleanSession(true)
        mqtt.setHost(host, 1883)

        mqtt.setClientId("PublisherClient${pubId}")
        BlockingConnection connection = mqtt.blockingConnection()
        connection.connect()
        (1..numToSend).each {
            connection.publish("/topic${pubId}", this.payload, QoS.AT_MOST_ONCE, false)
        }
        connection.publish("/exit${pubId}", 'Exit'.bytes, QoS.AT_MOST_ONCE, false)
        connection.disconnect()
    }
}

if (args.size() < 1) {
    println "Usage: groovy issue79_stress_multipub.groovy num_publishers"
}

final byte[] payload = 'Hello world!!'.bytes

int numPublishers = args[0] as Integer
(1..numPublishers).each { numPub ->
    def pubTh = new PublisherThread(numPub, 5, payload)
    pubTh.start()
}

println "Started all publisher threads"