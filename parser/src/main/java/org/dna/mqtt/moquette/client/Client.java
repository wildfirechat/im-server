package org.dna.mqtt.moquette.client;

import java.net.InetSocketAddress;
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
import org.dna.mqtt.moquette.ConnectionException;
import org.dna.mqtt.moquette.MQTTException;
import org.dna.mqtt.moquette.PublishException;
import org.dna.mqtt.moquette.SubscribeException;
import org.dna.mqtt.moquette.proto.ConnAckDecoder;
import org.dna.mqtt.moquette.proto.ConnectEncoder;
import org.dna.mqtt.moquette.proto.DisconnectEncoder;
import org.dna.mqtt.moquette.proto.PingReqEncoder;
import org.dna.mqtt.moquette.proto.PublishDecoder;
import org.dna.mqtt.moquette.proto.PublishEncoder;
import org.dna.mqtt.moquette.proto.SubAckDecoder;
import org.dna.mqtt.moquette.proto.SubscribeEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.proto.messages.DisconnectMessage;
import org.dna.mqtt.moquette.proto.messages.PingReqMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import org.dna.mqtt.moquette.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The API to connect to a MQTT server.
 * 
 * Pass to the constructor the host and port to connect, invoke the connect 
 * message, invoke publish to send notifications messages, invoke subscribe
 * with a callback to be notified when something happen on the desired topic.
 * 
 * @author andrea
 */
public final class Client {

    private static final Logger LOG = LoggerFactory.getLogger(Client.class);
//    private static final String HOSTNAME = /*"localhost"*/ "127.0.0.1";
//    private static final int PORT = Server.PORT;
    private static final long CONNECT_TIMEOUT = 3 * 1000L; // 3 seconds
    private static final long SUBACK_TIMEOUT = 4 * 1000L;
    private static final int KEEPALIVE_SECS = 3;
    private static final int NUM_SCHEDULER_TIMER_THREAD = 1;
    final static int DEFAULT_RETRIES = 3;
    private int m_connectRetries = DEFAULT_RETRIES;
    
    private String m_hostname;
    private int m_port;
    
    //internal mangement used for conversation with the server
    private IoConnector m_connector;
    private IoSession m_session;
    private CountDownLatch m_connectBarrier;
    private CountDownLatch m_subscribeBarrier;
    private byte m_returnCode;
    
    //TODO synchronize the access
    //Refact the da model should be a list of callback for each topic
    private Map<String, IPublishCallback> m_subscribersList = new HashMap<String, IPublishCallback>();
    private ScheduledExecutorService m_scheduler;
    private ScheduledFuture m_pingerHandler;
    private boolean m_executedOperation;

    public Client(String host, int port) {
        m_hostname = host;
        m_port = port;
        init();
    }

    protected void init() {
        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(new ConnAckDecoder());
        decoder.addMessageDecoder(new SubAckDecoder());
        decoder.addMessageDecoder(new PublishDecoder());

        DemuxingProtocolEncoder encoder = new DemuxingProtocolEncoder();
        encoder.addMessageEncoder(ConnectMessage.class, new ConnectEncoder());
        encoder.addMessageEncoder(PublishMessage.class, new PublishEncoder());
        encoder.addMessageEncoder(SubscribeMessage.class, new SubscribeEncoder());
        encoder.addMessageEncoder(DisconnectMessage.class, new DisconnectEncoder());
        encoder.addMessageEncoder(PingReqMessage.class, new PingReqEncoder());
           
        m_connector = new NioSocketConnector();

//        m_connector.getFilterChain().addLast("logger", new LoggingFilter());
        m_connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(encoder, decoder));

        m_connector.setHandler(new ClientMQTTHandler(this));
        m_connector.getSessionConfig().setReadBufferSize(2048);
        m_connector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, Server.DEFAULT_CONNECT_TIMEOUT);
        
        m_scheduler = Executors.newScheduledThreadPool(NUM_SCHEDULER_TIMER_THREAD);
    }

    public void connect() throws MQTTException {
        int retries = 0;
        
        try {
            ConnectFuture future = m_connector.connect(new InetSocketAddress(m_hostname, m_port));
            LOG.debug("Client waiting to connect to server");
            future.awaitUninterruptibly();
            m_session = future.getSession();
        } catch (RuntimeIoException e) {
            LOG.debug("Failed to connect, retry " + retries + " of ("+m_connectRetries+")", e);
        }
        
        if (retries == m_connectRetries) {
            throw new MQTTException("Can't connect to the server after " + retries + "retries");
        }
        
        m_connectBarrier = new CountDownLatch(1);
        
        //send a message over the session
        ConnectMessage connMsg = new ConnectMessage();
        connMsg.setKeepAlive(KEEPALIVE_SECS);
        connMsg.setClientID("FAKE_MSG_ID1"); //TODO create a generator for the ID
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
        
        m_executedOperation = true;
        final Runnable pingerDeamon = new Runnable() {
            public void run() { 
                if (!m_executedOperation) {
                    //send a ping req
                    PingReqMessage msg = new PingReqMessage();
                    m_session.write(msg);
                } 
                m_executedOperation = false;
            }
        };
        m_pingerHandler = m_scheduler.scheduleWithFixedDelay(pingerDeamon, KEEPALIVE_SECS, KEEPALIVE_SECS, TimeUnit.SECONDS);
    }
    
    /**
     * Publish to the connected server the payload message to the given topic.
     * It's admitted to publish a 0 -length payload.
     */
    public void publish(String topic, byte[] payload) throws PublishException{
        PublishMessage msg = new PublishMessage();
        msg.setTopicName(topic);
        msg.setPayload(payload);
        
        //Untill the server could handle all the Qos levels
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
        
        m_executedOperation = true;
    }
    
    public void subscribe(String topic, IPublishCallback publishCallback) {
        LOG.info("subscribe invoked");
        SubscribeMessage msg = new SubscribeMessage();
        msg.addSubscription(new SubscribeMessage.Couple((byte)AbstractMessage.QOSType.MOST_ONE.ordinal(), topic));
        msg.setQos(AbstractMessage.QOSType.LEAST_ONE);
        
        WriteFuture wf = m_session.write(msg);
        try {
            wf.await();
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
            throw new SubscribeException(ex);
        }
        LOG.info("subscribe message sent");
        
        Throwable ex = wf.getException();
        if (ex != null) {
            throw new SubscribeException(ex);
        }
        
        //TODO register the publishCallback in some registry to be notified
        m_subscribersList.put(topic, publishCallback);
        
        //wait for the SubAck
        m_subscribeBarrier = new CountDownLatch(1);
        
        //suspend until the server respond with CONN_ACK
        boolean unlocked = false; 
        try {
            LOG.info("subscribe waiting for suback");
            unlocked = m_subscribeBarrier.await(SUBACK_TIMEOUT, TimeUnit.MILLISECONDS); //TODO parametrize
        } catch (InterruptedException iex) {
            throw new SubscribeException(iex);
        }
        
        //if not arrive into certain limit, raise an error
        if (!unlocked) {
            throw new SubscribeException("Subscribe timeout elapsed unless server responded with a SUB_ACK");
        }
        
        m_executedOperation = true;
        
        //TODO check the ACK messageID
    }
    
    
    /**
     *  TODO extract this SPI method in a SPI
     */
    protected void connectionAckCallback(byte returnCode) {
        LOG.info("connectionAckCallback invoked");
        m_returnCode = returnCode;
        m_connectBarrier.countDown();
    }
    
    
    protected void subscribeAckCallback() {
        LOG.info("subscribeAckCallback invoked");
        m_subscribeBarrier.countDown();
    }
    
    
    protected void publishCallback(String topic, byte[] payload) {
        m_subscribersList.get(topic).published(topic, payload);
    }
    
    
    public void close() {
        //stop the pinger
        m_pingerHandler.cancel(false);
        
        //send the CLOSE message
        m_session.write(new DisconnectMessage());
        
        // wait until the summation is done
        m_session.getCloseFuture().awaitUninterruptibly();
        m_connector.dispose();
    }

}
