package io.moquette.performance.clients.paho;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.nio.ByteBuffer;

public class PahoBenchmarker {

    private static void publishQos0(IMqttAsyncClient client, long numMessages) throws MqttException {
        System.out.println("publishing..");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numMessages; i++) {
            byte[] bytes = ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array();
            client.publish("topic", bytes, 0, false);
        }
        client.publish("/exit", "Exit".getBytes(), 0, false);
        long stopTime = System.currentTimeMillis();
        long spentTime = stopTime -startTime;
        System.out.println("published in " + spentTime + " ms");
    }

    public static void main(String[] args) throws MqttException, InterruptedException {
        if (args.length < 2) {
            System.out.println("Usage benchmarker <host> <numMessages>");
            System.out.println("java io.moquette.performance.clients.paho.PahoBenchmarker localhost 1000 500 test1");
            return;
        }

        String host = args[0];
        long numMessages = Long.parseLong(args[1]);
        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
        //MemoryPersistence dataStore = new MemoryPersistence()

        int rnd = (int) (Math.random() * 100);
        MqttAsyncClient client = new MqttAsyncClient("tcp://" + host +":1883",
            "SubscriberClient" + rnd, dataStore);
        SubscriberCallback callback = new SubscriberCallback();
        client.setCallback(callback);
        client.connect().waitForCompletion(1000);
        client.subscribe("topic", 0);
        System.out.println("subscribed to topic");
        client.subscribe("/exit", 0);
        System.out.println("subscribed to /exit");

        publishQos0(client, numMessages);

        callback.waitFinish();
//        Map sorted = callback.distribution.sort( { k1, k2 -> k1 <=> k2 } as Comparator)
//        File csv = new File("perf.csv")
//        sorted.each {delay, count -> csv << "${delay}, ${count}\r\n"}
    }
}
