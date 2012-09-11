package org.dna.mqtt.moquette.client;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolDecoder;
import org.apache.mina.filter.codec.demux.DemuxingProtocolEncoder;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.dna.mqtt.commons.Constants;
import org.dna.mqtt.commons.MessageIDGenerator;
import org.dna.mqtt.moquette.ConnectionException;
import org.dna.mqtt.moquette.MQTTException;
import org.dna.mqtt.moquette.PublishException;
import org.dna.mqtt.moquette.SubscribeException;
import org.dna.mqtt.moquette.proto.*;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.proto.messages.DisconnectMessage;
import org.dna.mqtt.moquette.proto.messages.MessageIDMessage;
import org.dna.mqtt.moquette.proto.messages.PingReqMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubscribeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The API to connect to a MQTT server.
 *
 * Pass to the constructor the host and port to connect, invoke the connect
 * message, invoke publish to send notifications messages, invoke subscribe with
 * a callback to be notified when something happen on the desired topic.
 * 
 * NB this class is not thread safe so MUST be used by only one thread at time.
 *
 * @author andrea
 */
public final class Client {

    final static int DEFAULT_RETRIES = 3;
    final static int RETRIES_QOS_GT0 = 3;
    private static final Logger LOG = LoggerFactory.getLogger(Client.class);
//    private static final String HOSTNAME = /*"localhost"*/ "127.0.0.1";
//    private static final int PORT = Server.PORT;
    private static final long CONNECT_TIMEOUT = 3 * 1000L; // 3 seconds
    private static final long SUBACK_TIMEOUT = 4 * 1000L;
    private static final int KEEPALIVE_SECS = 3;
    private static final int NUM_SCHEDULER_TIMER_THREAD = 1;
    private int m_connectRetries = DEFAULT_RETRIES;
    private String m_hostname;
    private int m_port;
    //internal management used for conversation with the server
    private IoConnector m_connector;
    private IoSession m_session;
    private CountDownLatch m_connectBarrier;
    private CountDownLatch m_subscribeBarrier;
    private int m_receivedSubAckMessageID;
    private byte m_returnCode;
    //TODO synchronize the access
    //Refact the da model should be a list of callback for each topic
    private Map<String, IPublishCallback> m_subscribersList = new HashMap<String, IPublishCallback>();
    private ScheduledExecutorService m_scheduler;
    private ScheduledFuture m_pingerHandler;
    private String m_macAddress;
    private MessageIDGenerator m_messageIDGenerator = new MessageIDGenerator();
    /**Maintain the list of in flight messages for QoS > 0*/
//    private Set<Integer> m_inflightIDs = new HashSet<Integer>();
    
    private String m_clientID;
    
    final Runnable pingerDeamon = new Runnable() {
        public void run() {
            LOG.debug("Pingreq sent");
            //send a ping req
            m_session.write(new PingReqMessage());
        }
    };

    public Client(String host, int port) {
        m_hostname = host;
        m_port = port;
        init();
    }
    
    /**
     * Constructor in which the user could provide it's own ClientID
     */
    public Client(String host, int port, String clientID) {
        this(host, port);
        m_clientID = clientID;
    }

    protected void init() {
        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(new ConnAckDecoder());
        decoder.addMessageDecoder(new SubAckDecoder());
        decoder.addMessageDecoder(new UnsubAckDecoder());
        decoder.addMessageDecoder(new PublishDecoder());
        decoder.addMessageDecoder(new PubAckDecoder());
        decoder.addMessageDecoder(new PingRespDecoder());

        DemuxingProtocolEncoder encoder = new DemuxingProtocolEncoder();
        encoder.addMessageEncoder(ConnectMessage.class, new ConnectEncoder());
        encoder.addMessageEncoder(PublishMessage.class, new PublishEncoder());
        encoder.addMessageEncoder(SubscribeMessage.class, new SubscribeEncoder());
        encoder.addMessageEncoder(UnsubscribeMessage.class, new UnsubscribeEncoder());
        encoder.addMessageEncoder(DisconnectMessage.class, new DisconnectEncoder());
        encoder.addMessageEncoder(PingReqMessage.class, new PingReqEncoder());

        m_connector = new NioSocketConnector();

//        m_connector.getFilterChain().addLast("logger", new LoggingFilter());
        m_connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(encoder, decoder));

        m_connector.setHandler(new ClientMQTTHandler(this));
        m_connector.getSessionConfig().setReadBufferSize(2048);
        m_connector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, Constants.DEFAULT_CONNECT_TIMEOUT);

        m_scheduler = Executors.newScheduledThreadPool(NUM_SCHEDULER_TIMER_THREAD);
        
        m_macAddress = readMACAddress();
    }

    /**
     * Connects to the server with clean session set to true, do not maintains
     * client topic subscriptions
     */
    public void connect() throws MQTTException {
        connect(true);
    }
    
    
    public void connect(boolean cleanSession) throws MQTTException {
        int retries = 0;

        try {
            ConnectFuture future = m_connector.connect(new InetSocketAddress(m_hostname, m_port));
            LOG.debug("Client waiting to connect to server");
            future.awaitUninterruptibly();
            m_session = future.getSession();
        } catch (RuntimeIoException e) {
            LOG.debug("Failed to connect, retry " + retries + " of (" + m_connectRetries + ")", e);
        }

        if (retries == m_connectRetries) {
            throw new MQTTException("Can't connect to the server after " + retries + "retries");
        }

        m_connectBarrier = new CountDownLatch(1);

        //send a message over the session
        ConnectMessage connMsg = new ConnectMessage();
        connMsg.setKeepAlive(KEEPALIVE_SECS);
        if (m_clientID == null) {
            m_clientID = generateClientID();
        }
        connMsg.setClientID(m_clientID);
        connMsg.setCleanSession(cleanSession);
        
        m_session.write(connMsg);

        //suspend until the server respond with CONN_ACK
        boolean unlocked = false;
        try {
            unlocked = m_connectBarrier.await(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS); //TODO parametrize
        } catch (InterruptedException ex) {
            throw new ConnectionException(ex);
        }

        //if not arrive into certain limit, raise an error
        if (!unlocked) {
            throw new ConnectionException("Connection timeout elapsed unless server responded with a CONN_ACK");
        }

        //also raise an error when CONN_ACK is received with some error code inside
        if (m_returnCode != ConnAckMessage.CONNECTION_ACCEPTED) {
            String errMsg;
            switch (m_returnCode) {
                case ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION:
                    errMsg = "Unacceptable protocol version";
                    break;
                case ConnAckMessage.IDENTIFIER_REJECTED:
                    errMsg = "Identifier rejected";
                    break;
                case ConnAckMessage.SERVER_UNAVAILABLE:
                    errMsg = "Server unavailable";
                    break;
                case ConnAckMessage.BAD_USERNAME_OR_PASSWORD:
                    errMsg = "Bad username or password";
                    break;
                case ConnAckMessage.NOT_AUTHORIZED:
                    errMsg = "Not authorized";
                    break;
                default:
                    errMsg = "Not idetified erro code " + m_returnCode;
            }
            throw new ConnectionException(errMsg);
        }

        updatePinger();
    }

    /**
     * Publish to the connected server the payload message to the given topic.
     * It's admitted to publish a 0 -length payload.
     */
    public void publish(String topic, byte[] payload) throws PublishException {
        publish(topic, payload, false);
    }

    /**
     * Publish by default with QoS 0
     * */
    public void publish(String topic, byte[] payload, boolean retain) throws PublishException {
        publish(topic, payload, AbstractMessage.QOSType.MOST_ONE, retain);
    }

    public void publish(String topic, byte[] payload, AbstractMessage.QOSType qos, boolean retain) throws PublishException {
        PublishMessage msg = new PublishMessage();
        msg.setRetainFlag(retain);
        msg.setTopicName(topic);
        msg.setPayload(payload);

        //Untill the server could handle all the Qos 2 level
        if (qos != AbstractMessage.QOSType.MOST_ONE) {
            msg.setQos(AbstractMessage.QOSType.LEAST_ONE);
            int messageID = m_messageIDGenerator.next();
            msg.setMessageID(messageID);

            try {
                manageSendQoS1(msg);
            } catch (Throwable ex) {
                throw new MQTTException(ex);
            }
        } else {
            //QoS 0 case
            msg.setQos(AbstractMessage.QOSType.MOST_ONE);
            WriteFuture wf = m_session.write(msg);
            try {
                wf.await();
            } catch (InterruptedException ex) {
                LOG.debug(null, ex);
                throw new PublishException(ex);
            }

            Throwable ex = wf.getException();
            if (ex != null) {
                throw new PublishException(ex);
            }
        }

        updatePinger();
    }

    public void subscribe(String topic, IPublishCallback publishCallback) {
        LOG.info("subscribe invoked");
        SubscribeMessage msg = new SubscribeMessage();
        msg.addSubscription(new SubscribeMessage.Couple((byte) AbstractMessage.QOSType.MOST_ONE.ordinal(), topic));
        msg.setQos(AbstractMessage.QOSType.LEAST_ONE);
        int messageID = m_messageIDGenerator.next();
        msg.setMessageID(messageID);
//        m_inflightIDs.add(messageID);
        register(topic, publishCallback);
        
        try {
            manageSendQoS1(msg);
        } catch(Throwable ex) {
            //in case errors arise, remove the registration because the subscription
            // hasn't get well
            unregister(topic);
            throw new MQTTException(ex);
        }

        updatePinger();
    }
    
    
    public void unsubscribe(String... topics) {
        LOG.info("unsubscribe invoked");
        UnsubscribeMessage msg = new UnsubscribeMessage();
        for (String topic : topics) {
            msg.addTopic(topic);
        }
        msg.setQos(AbstractMessage.QOSType.LEAST_ONE);
        int messageID = m_messageIDGenerator.next();
        msg.setMessageID(messageID);
//        m_inflightIDs.add(messageID);
//        register(topic, publishCallback);
        try {
            manageSendQoS1(msg);
        } catch(Throwable ex) {
            //in case errors arise, remove the registration because the subscription
            // hasn't get well
//            unregister(topic);
            throw new MQTTException(ex);
        }
        
        for (String topic : topics) {
            unregister(topic);
        }

//        register(topic, publishCallback);
        updatePinger();
    }
    
    private void manageSendQoS1(MessageIDMessage msg) throws Throwable{
        int messageID = msg.getMessageID();
        boolean unlocked = false;
        for (int retries = 0; retries < RETRIES_QOS_GT0 || !unlocked; retries++) {
            LOG.debug("manageSendQoS1 retry " + retries);
            if (retries > 0) {
                msg.setDupFlag(true);
            }

            WriteFuture wf = m_session.write(msg);
            wf.await();
            LOG.info("message sent");

            Throwable ex = wf.getException();
            if (ex != null) {
                throw ex;
            }

            //wait for the SubAck
            m_subscribeBarrier = new CountDownLatch(1);

            //suspend until the server respond with CONN_ACK
            LOG.info("subscribe waiting for suback");
            unlocked = m_subscribeBarrier.await(SUBACK_TIMEOUT, TimeUnit.MILLISECONDS); //TODO parametrize
        }

        //if not arrive into certain limit, raise an error
        if (!unlocked) {
            throw new SubscribeException(String.format("Server doesn't replyed with a SUB_ACK after %d replies", RETRIES_QOS_GT0));
        } else {
            //check if message ID match
            if (m_receivedSubAckMessageID != messageID) {
                throw new SubscribeException(String.format("Server replyed with "
                + "a broken MessageID in SUB_ACK, expected %d but received %d", 
                messageID, m_receivedSubAckMessageID));
            }
        }
    }

    /**
     * TODO extract this SPI method in a SPI
     */
    protected void connectionAckCallback(byte returnCode) {
        LOG.info("connectionAckCallback invoked");
        m_returnCode = returnCode;
        m_connectBarrier.countDown();
    }

    protected void subscribeAckCallback(int messageID) {
        LOG.info("subscribeAckCallback invoked");
        m_subscribeBarrier.countDown();
        m_receivedSubAckMessageID = messageID;
    }
    
    void unsubscribeAckCallback(int messageID) {
        LOG.info("unsubscribeAckCallback invoked");
        //NB we share the barrier because in futur will be a single barrier for all
        m_subscribeBarrier.countDown();
        m_receivedSubAckMessageID = messageID;
    }

    void publishAckCallback(Integer messageID) {
        LOG.info("publishAckCallback invoked");
        m_subscribeBarrier.countDown();
        m_receivedSubAckMessageID = messageID;
    }

    protected void publishCallback(String topic, byte[] payload) {
        IPublishCallback callback = m_subscribersList.get(topic);
        if (callback == null) {
            String msg = String.format("Can't find any publish callback fr topic %s", topic);
            LOG.error(msg);
            throw new MQTTException(msg);
        }
        callback.published(topic, payload);
    }

    /**
     * In the current pinger is not ye executed, then cancel it and schedule
     * another by KEEPALIVE_SECS
     */
    private void updatePinger() {
        if (m_pingerHandler != null) {
            m_pingerHandler.cancel(false);
        }
        m_pingerHandler = m_scheduler.scheduleWithFixedDelay(pingerDeamon, KEEPALIVE_SECS, KEEPALIVE_SECS, TimeUnit.SECONDS);
    }

    private String readMACAddress() {
        try {
            NetworkInterface network = NetworkInterface.getNetworkInterfaces().nextElement();

            byte[] mac = network.getHardwareAddress();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], ""));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new MQTTException("Can't retrieve host MAC address", ex);
        }
    }
    
    private String generateClientID() {
        double rnd = Math.random();
        String id =  "Moque" + m_macAddress + Math.round(rnd*1000);
        LOG.debug("Generated ClientID " + id);
        return id;
    }

    public void close() {
        //stop the pinger
        m_pingerHandler.cancel(false);

        //send the CLOSE message
        m_session.write(new DisconnectMessage());

        // wait until the summation is done
        m_session.getCloseFuture().awaitUninterruptibly();
//        m_connector.dispose();
    }
    
    
    public void shutdown() {
        m_connector.dispose();
    }

    /**
     * Used only to re-register the callback with the topic, not to send any
     * subscription message to the server, used when a client reconnect to an 
     * existing session on the server
     */
    public void register(String topic, IPublishCallback publishCallback) {
        //register the publishCallback in some registry to be notified
        m_subscribersList.put(topic, publishCallback);
    }
    
    
    //Remove the registration of the callback from the topic
    private void unregister(String topic) {
        m_subscribersList.remove(topic);
    }
}
