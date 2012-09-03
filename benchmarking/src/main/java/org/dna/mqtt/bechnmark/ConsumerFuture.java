package org.dna.mqtt.bechnmark;

import java.io.*;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fusesource.mqtt.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class ConsumerFuture implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerFuture.class);
    
    private String m_clientID;
    private static final String BENCHMARK_FILE = "consumer_bechmark.txt";
    private PrintWriter m_benchMarkOut;
    private ByteArrayOutputStream m_baos = new ByteArrayOutputStream(1024 * 1024);

    public ConsumerFuture(String clientID) {
        m_clientID = clientID;
        m_benchMarkOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m_baos)));
        m_benchMarkOut.println("msg ID, ns");
    }
    
    public void run() {
        MQTT mqtt = new MQTT();
        try {
//            mqtt.setHost("test.mosquitto.org", 1883);
            mqtt.setHost("localhost", 1883);
        } catch (URISyntaxException ex) {
            LOG.error(null, ex);
            return;
        }
        
        mqtt.setClientId(m_clientID);
        FutureConnection connection = mqtt.futureConnection();
        Future<Void> futConn = connection.connect();
        
        try {
            futConn.await();
        } catch (Exception ex) {
            LOG.error("Cant't CONNECT to the server", ex);
            return;
        }

        
        Topic[] topics = {new Topic("/topic", QoS.AT_MOST_ONCE)};
        Future<byte[]> futSub = connection.subscribe(topics);
        try {
            byte[] qoses = futSub.await();
            LOG.info("Subscribed to topic");
        } catch (Exception ex) {
            LOG.error("Cant't PUSBLISH to the server", ex);
            return;
        }

        Pattern p = Pattern.compile(".*!!(\\d+)");
        Message message = null;
        long startMillis = System.currentTimeMillis();
        for (int i = 0; i < Producer.PUB_LOOP; i++) {
            Future<Message> futReceive = connection.receive();
            try {
                message = futReceive.await();
                String content = new String(message.getPayload());

                //metrics part
                Matcher matcher = p.matcher(content);
                matcher.matches();
                String numMsg = matcher.group(1);
                m_benchMarkOut.println(String.format("%s, %d", numMsg, System.nanoTime()));
            } catch (Exception ex) {
                LOG.error(null, ex);
                return;
            }
            byte[] payload = message.getPayload();
            StringBuffer sb = new StringBuffer().append("Topic: ").append(message.getTopic())
                    .append(", payload: ").append(new String(payload));
            LOG.debug(sb.toString());
        }

        long time = System.currentTimeMillis() - startMillis;
        LOG.info(String.format("Consumer %s received %d messages in %d ms", Thread.currentThread().getName(), Producer.PUB_LOOP, time));
            
        Future<Void> f4 =  connection.disconnect();
        try {
            LOG.info("Disconneting");
            f4.await();
            LOG.info("Disconnected");
        } catch (Exception ex) {
            LOG.error("Cant't DISCONNECT to the server", ex);
        }

        m_benchMarkOut.close();

        try {
            FileOutputStream fw = new FileOutputStream(BENCHMARK_FILE);
            fw.write(m_baos.toByteArray());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
    
}
