@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.2.0', ext='jar')


import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A client that try all the simple MQTT commands
 * */

if (args.size() != 2) {
    println "usage: <host> <local client name>"
    return
}

class SubscriberCallback implements MqttCallback {

    private CountDownLatch m_latch = new CountDownLatch(1)

    void waitFinish() {
        boolean reachZero = m_latch.await(4, TimeUnit.SECONDS)
        if (!reachZero) {
            println "Expired the time to receive the publish"
        }
    }

    void renewWaiter() {
        m_latch = new CountDownLatch(1)
    }

    void messageArrived(String topic, MqttMessage message) throws Exception {
        println "Received on [$topic] message: [$message]"
        m_latch.countDown()
    }

    void deliveryComplete(IMqttDeliveryToken token) {
        println "Delivery complete"
    }

    void connectionLost(Throwable th) {
        println "Connection lost"
        m_latch.countDown()
    }
}


String host = args[0]
String clientName = args[1]

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore1 = new MqttDefaultFilePersistence(tmpDir + "/" + clientName)
MqttClient client1 = new MqttClient("tcp://${host}:1883", "ConCli${clientName}", dataStore1)
def callback = new SubscriberCallback()
client1.callback = callback

println "start test"
print "connect..."
client1.connect()
println "OK!"

print "subscribe..."
client1.subscribe("/news", 0)
println "OK!"

/*print "publish qos0..."
client1.publish("/news", "Moquette is going to big refactoring, QoS0".bytes, 0, false)
callback.waitFinish()
println "OK!"
callback.renewWaiter()

print "publish qos1..."
client1.publish("/news", "Moquette is going to big refactoring, QoS1".bytes, 1, false)
callback.waitFinish()
println "OK!"
callback.renewWaiter()*/

print "publish qos2..."
client1.publish("/news", "Moquette is going to big refactoring, QoS2".bytes, 2, false)
callback.waitFinish()
println "OK!"

print "unsubscribe..."
client1.unsubscribe("/news")
println "OK!"

print "disconnect..."
client1.disconnect()
println "OK!"
println "Done"
System.exit(0)
