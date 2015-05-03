//@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-snapshots/')
//@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.0.1-SNAPSHOT')
@GrabResolver(name='Paho', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='mqtt-client', version='0.4.0')

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore

SSLSocketFactory configureSSLSocketFactory() {
    KeyStore ks = KeyStore.getInstance("JKS");
    println "using client key store at: " + new File("clientkeystore.jks").absolutePath
    InputStream jksInputStream = new FileInputStream("clientkeystore.jks")
//    InputStream jksInputStream = getClass().getClassLoader().getResourceAsStream("clientkeystore.jks");
    ks.load(jksInputStream, "passw0rdcli".toCharArray());

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, "passw0rd".toCharArray());

    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ks);

    SSLContext sc = SSLContext.getInstance("TLS");
    TrustManager[] trustManagers = tmf.getTrustManagers();
    sc.init(kmf.getKeyManagers(), trustManagers, null);

    SSLSocketFactory ssf = sc.getSocketFactory();
    return ssf;
}

String tmpDir = System.getProperty("java.io.tmpdir")
MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir)

MqttClient client = new MqttClient("ssl://localhost:8883", "SSLClientTest", dataStore)
SSLSocketFactory ssf = configureSSLSocketFactory()
MqttConnectOptions options = new MqttConnectOptions()
options.setSocketFactory(ssf)
client.connect(options)
MqttMessage message = new MqttMessage('Hello world!!'.bytes)
message.setQos(2)
print "publishing.."
client.getTopic("log").publish(message)
println "published"
client.disconnect()
println "disconnected"