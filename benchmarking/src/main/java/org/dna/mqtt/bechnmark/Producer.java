package org.dna.mqtt.bechnmark;

import java.io.*;
import java.net.URISyntaxException;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class only publish MQTT messages to a define topic with a certain frequency.
 * 
 * 
 * @author andrea
 */
public class Producer implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(Producer.class);

    private String m_clientID;
    
    public static final int PUB_LOOP = 100000;
    
    private int m_starIndex;
    private int m_len;
    private long m_startMillis;
    private static final String BENCHMARK_FILE = "producer_bechmark_%s.txt";
    private PrintWriter m_benchMarkOut;
    private ByteArrayOutputStream m_baos = new ByteArrayOutputStream(1024 * 1024 * 2);

    public Producer(String clientID, int start, int len) {
        m_clientID = clientID;
        m_starIndex = start;
        m_len = len;
        m_benchMarkOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m_baos)));
        m_benchMarkOut.println("msg ID, ns");
    }

    public void run() {
        m_startMillis = System.currentTimeMillis();
        MQTT mqtt = new MQTT();
        try {
//            mqtt.setHost("test.mosquitto.org", 1883);
            mqtt.setHost("localhost", 1883);
        } catch (URISyntaxException ex) {
            LOG.error(null, ex);
            return;
        }
        
        mqtt.setClientId(m_clientID);
        BlockingConnection connection = mqtt.blockingConnection();
        try {
            connection.connect();
        } catch (Exception ex) {
            LOG.error("Cant't CONNECT to the server", ex);
            return;
        }

        long time = System.currentTimeMillis() - m_startMillis;
        LOG.info(String.format("Producer %s connected in %d ms", Thread.currentThread().getName(), time));

        LOG.info("Starting from index " + m_starIndex + " up to " + (m_starIndex + m_len));
        m_startMillis = System.currentTimeMillis();
        for (int i = m_starIndex; i < m_starIndex + m_len; i++) {
            try {
//                LOG.info("Publishing");
                String payload = "Hello world MQTT!!" + i;
                connection.publish("/topic", payload.getBytes(), QoS.AT_MOST_ONCE, false);
                m_benchMarkOut.println(String.format("%d, %d", i, System.nanoTime()));
            } catch (Exception ex) {
                LOG.error("Cant't PUBLISH to the server", ex);
                return;
            }
        }

        time = System.currentTimeMillis() - m_startMillis;
        LOG.info(String.format("Producer %s published %d messages in %d ms", Thread.currentThread().getName(), m_len, time));

        m_startMillis = System.currentTimeMillis();
        try {
            LOG.info("Disconneting");
            connection.disconnect();
            LOG.info("Disconnected");
        } catch (Exception ex) {
            LOG.error("Cant't DISCONNECT to the server", ex);
        }

        time = System.currentTimeMillis() - m_startMillis;
        LOG.info(String.format("Producer %s disconnected in %d ms", Thread.currentThread().getName(), time));

        m_benchMarkOut.flush();
        m_benchMarkOut.close();

        try {
            FileOutputStream fw = new FileOutputStream(String.format(BENCHMARK_FILE, m_clientID));
            fw.write(m_baos.toByteArray());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
    
}
