@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.2.0')

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

if (args.size() < 2) {
    println "Usage subscriber <host> <QoS>"
    return
}

String host = args[0]
int qos = args[1] as int
MemoryPersistence persistence = new MemoryPersistence()

MqttClient client = new MqttClient("tcp://${host}:1883", "SubscriberClient", persistence)
client.connect()
client.subscribe("topic", 0)
println "subscribed to topic"
client.unsubscribe("topic")
println "unsubscribed from topic"

client.disconnect()
client.close()
