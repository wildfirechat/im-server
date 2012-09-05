package org.dna.mqtt.bechnmark.mina;

import java.io.IOException;
import java.net.InetSocketAddress;
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
import org.dna.mqtt.moquette.MQTTException;
import org.dna.mqtt.moquette.PublishException;
import org.dna.mqtt.moquette.proto.*;
import org.dna.mqtt.moquette.proto.messages.*;
import org.dna.mqtt.moquette.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class MQTTBulkClient {
    public int NUM_MESSAGES = 10000;
    
    private static final Logger LOG = LoggerFactory.getLogger(MQTTBulkClient.class);
    
    private String m_hostname;
    private int m_port = 1883;
    //internal management used for conversation with the server
    private IoConnector m_connector;
    private IoSession m_session;
    boolean m_withWaitWriteFuture = false;
    
    public static void main(String[] args) throws IOException {
        MQTTBulkClient client = new MQTTBulkClient();
        if (args.length > 0) {
            client.m_hostname = args[0];
            client.m_withWaitWriteFuture = Boolean.parseBoolean(args[1]);
            client.NUM_MESSAGES = Integer.parseInt(args[2]);
        } else {
            client.m_hostname = "localhost";
        }
        client.init();
        client.connect();

        long start = System.currentTimeMillis();
        for (int i = 0; i < client.NUM_MESSAGES; i++) {
            client.publish("/topic", "Hello world".getBytes(), AbstractMessage.QOSType.MOST_ONE, false);
        }        
        long stop = System.currentTimeMillis();
        LOG.info("Client sent " + client.NUM_MESSAGES + " in " + (stop - start) + " ms");
        client.shutdown();
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

        m_connector.setHandler(new DummyClientHandler());
        m_connector.getSessionConfig().setReadBufferSize(2048);
        m_connector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, Server.DEFAULT_CONNECT_TIMEOUT);
    }
    
    public void connect() throws MQTTException {
        try {
            ConnectFuture future = m_connector.connect(new InetSocketAddress(m_hostname, m_port));
            LOG.debug("Client waiting to connect to server");
            future.awaitUninterruptibly();
            m_session = future.getSession();
        } catch (RuntimeIoException e) {
            LOG.debug("Failed to connect", e);
        }
    }
    
    public void publish(String topic, byte[] payload, AbstractMessage.QOSType qos, boolean retain) throws PublishException {
        PublishMessage msg = new PublishMessage();
        msg.setRetainFlag(retain);
        msg.setTopicName(topic);
        msg.setPayload(payload);

        //QoS 0 case
        msg.setQos(AbstractMessage.QOSType.MOST_ONE);
        WriteFuture wf = m_session.write(msg);
        
        if (m_withWaitWriteFuture) {
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
    }
    
    public void shutdown() {
        m_connector.dispose();
    }
}
