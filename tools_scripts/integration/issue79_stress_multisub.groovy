@Grab(group='org.fusesource.mqtt-client', module='mqtt-client', version='1.10')
//@GrabResolver(name='Paho', root="https://repo.eclipse.org/content/repositories/paho-releases/")
//@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.0.2')

import org.fusesource.mqtt.client.BlockingConnection
import org.fusesource.mqtt.client.MQTT
import org.fusesource.mqtt.client.Message
import org.fusesource.mqtt.client.QoS
import org.fusesource.mqtt.client.Topic

class SubscriberThread extends Thread {

    private final int subId
    private final String host = "localhost"
    private final boolean oneClientPerTopic

    SubscriberThread(int subId, boolean oneClientPerTopic) {
        this.subId = subId
        this.oneClientPerTopic = oneClientPerTopic
    }

    @Override
    public void run() {
        MQTT mqtt = new MQTT()
        //mqtt.setHost("test.mosquitto.org", 1883);
        mqtt.setHost(host, 1883)
        mqtt.setCleanSession(true)
        mqtt.setHost(host, 1883)

        mqtt.setClientId("SubscriberClient${subId}")
        BlockingConnection connection = mqtt.blockingConnection()
        connection.connect()

        def postFix = oneClientPerTopic ? "" : subId
        Topic[] topics = [new Topic("/topic${postFix}", QoS.AT_MOST_ONCE), new Topic("/exit${postFix}", QoS.AT_MOST_ONCE)]
        byte[] qoses = connection.subscribe(topics)
        boolean exit = false
        while (!exit) {
            Message message = connection.receive()
            if (message.topic == "/exit${postFix}") {
                exit = true
            }
            message.ack()
        }
        connection.disconnect()
        println "sub ${subId} exited"
    }
}

if (args.size() < 1) {
    println "Usage: groovy issue79_stress_multisub.groovy num_subscribers [single_topic]"
}

int numSubscriber = args[0] as Integer
boolean singleTopic = (args.size() == 2 && args[1] == "single_topic")
println "Subscribing to ${numSubscriber} topic, with single ${singleTopic}"
(1..numSubscriber).each { numSub ->
    def subTh = new SubscriberThread(numSub, singleTopic)
    subTh.start()
}

println "Started all subscribers threads"