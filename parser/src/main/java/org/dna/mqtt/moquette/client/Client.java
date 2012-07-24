package org.dna.mqtt.moquette.client;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolDecoder;
import org.apache.mina.filter.codec.demux.DemuxingProtocolEncoder;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.dna.mqtt.moquette.ConnectionException;
import org.dna.mqtt.moquette.MQTTException;
import org.dna.mqtt.moquette.PublishException;
import org.dna.mqtt.moquette.proto.ConnAckDecoder;
import org.dna.mqtt.moquette.proto.ConnectEncoder;
import org.dna.mqtt.moquette.proto.DisconnectEncoder;
import org.dna.mqtt.moquette.proto.PublishEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.proto.messages.DisconnectMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.dna.mqtt.moquette.server.Server;

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

    private static final Logger LOG = Logger.getLogger(Client.class.getName());
//    private static final String HOSTNAME = /*"localhost"*/ "127.0.0.1";
//    private static final int PORT = Server.PORT;
    private static final long CONNECT_TIMEOUT = 3 * 1000L; // 30 seconds
    final static int DEFAULT_RETRIES = 3;
    private int m_connectRetries = DEFAULT_RETRIES;
    
    private String m_hostname;
    private int m_port;
    
    //internal mangement used for conversation with the server
    private IoConnector m_connector;
    private IoSession m_session;
    private CountDownLatch m_connectBarrier;
    private byte m_returnCode;

    public Client(String host, int port) {
        m_hostname = host;
        m_port = port;
        init();
    }

    protected void init() {
        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(new ConnAckDecoder());

        DemuxingProtocolEncoder encoder = new DemuxingProtocolEncoder();
        encoder.addMessageEncoder(ConnectMessage.class, new ConnectEncoder());
        encoder.addMessageEncoder(PublishMessage.class, new PublishEncoder());
        encoder.addMessageEncoder(DisconnectMessage.class, new DisconnectEncoder());

        m_connector = new NioSocketConnector();

        m_connector.getFilterChain().addLast("logger", new LoggingFilter());
        m_connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(encoder, decoder));

        m_connector.setHandler(new ClientMQTTHandler(this));
        m_connector.getSessionConfig().setReadBufferSize(2048);
        m_connector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, Server.DEFAULT_CONNECT_TIMEOUT);
    }

    public void connect() throws MQTTException {
        int retries = 0;
        
        //TODO perhaps by proto deinifinition should be an implicit retry policy
        for (; retries < m_connectRetries; retries++) {
            try {
                ConnectFuture future = m_connector.connect(new InetSocketAddress(m_hostname, m_port));
                LOG.fine("Client waiting to connect to server");
                future.awaitUninterruptibly();
                m_session = future.getSession();
                break;
            } catch (RuntimeIoException e) {
                LOG.log(Level.FINE, "Failed to connect, retry " + retries + " of ("+m_connectRetries+")", e);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, "Error while sleeping for retry", ex);
            }
        }
        
        if (retries == m_connectRetries) {
            throw new MQTTException("Can't connect to the server after " + retries + "retries");
        }
        
        m_connectBarrier = new CountDownLatch(1);
        
        //send a message over the session
        ConnectMessage connMsg = new ConnectMessage();
        connMsg.setKeepAlive(3);
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
            LOG.log(Level.SEVERE, null, ex);
            throw new PublishException(ex);
        }
        
        Throwable ex = wf.getException();
        if (ex != null) {
            throw new PublishException(ex);
        }
    }
    
    
    /**
     *  TODO extract this SPI method in a SPI
     */
    protected void connectionAckCallback(byte returnCode) {
        LOG.info("connectionAckCallback invoked");
        m_returnCode = returnCode;
        m_connectBarrier.countDown();
    }
    
    public void close() {
        //send the CLOSE message
        m_session.write(new DisconnectMessage());
        
        // wait until the summation is done
        m_session.getCloseFuture().awaitUninterruptibly();
        m_connector.dispose();
    }

}
