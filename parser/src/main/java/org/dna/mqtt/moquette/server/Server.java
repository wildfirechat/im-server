package org.dna.mqtt.moquette.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolDecoder;
import org.apache.mina.filter.codec.demux.DemuxingProtocolEncoder;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.dna.mqtt.moquette.messaging.spi.impl.SimpleMessaging;
import org.dna.mqtt.moquette.proto.ConnAckEncoder;
import org.dna.mqtt.moquette.proto.ConnectDecoder;
import org.dna.mqtt.moquette.proto.DisconnectDecoder;
import org.dna.mqtt.moquette.proto.MQTTLoggingFilter;
import org.dna.mqtt.moquette.proto.PingReqDecoder;
import org.dna.mqtt.moquette.proto.PingRespEncoder;
import org.dna.mqtt.moquette.proto.PublishDecoder;
import org.dna.mqtt.moquette.proto.PublishEncoder;
import org.dna.mqtt.moquette.proto.SubAckEncoder;
import org.dna.mqtt.moquette.proto.SubscribeDecoder;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.PingRespMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Launch a  configured version of the server.
 * @author andrea
 */
public class Server {
    
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    
    public static final int PORT = 9191;
    public static final int DEFAULT_CONNECT_TIMEOUT = 10;
    private IoAcceptor m_acceptor;
    
    public static void main(String[] args) throws IOException {
        new Server().startServer();
        
    }
    
    protected void startServer() throws IOException {
        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(new ConnectDecoder());
        decoder.addMessageDecoder(new PublishDecoder());
        decoder.addMessageDecoder(new SubscribeDecoder());
        decoder.addMessageDecoder(new DisconnectDecoder());
        decoder.addMessageDecoder(new PingReqDecoder());
        
        DemuxingProtocolEncoder encoder = new DemuxingProtocolEncoder();
//        encoder.addMessageEncoder(ConnectMessage.class, new ConnectEncoder());
        encoder.addMessageEncoder(ConnAckMessage.class, new ConnAckEncoder());
        encoder.addMessageEncoder(SubAckMessage.class, new SubAckEncoder());
        encoder.addMessageEncoder(PublishMessage.class, new PublishEncoder());
        encoder.addMessageEncoder(PingRespMessage.class, new PingRespEncoder());
        
        m_acceptor = new NioSocketAcceptor();
        
        m_acceptor.getFilterChain().addLast( "logger", new MQTTLoggingFilter("SERVER LOG") );
        m_acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter(encoder, decoder));

        MQTTHandler handler = new MQTTHandler();
        SimpleMessaging messaging = new SimpleMessaging();
        //TODO fix this hugly wiring
        handler.setMessaging(messaging);
        messaging.setNotifier(handler);
        
        
        m_acceptor.setHandler(handler);
        ((NioSocketAcceptor)m_acceptor).setReuseAddress(true);
        ((NioSocketAcceptor)m_acceptor).getSessionConfig().setReuseAddress(true);
        m_acceptor.getSessionConfig().setReadBufferSize( 2048 );
        m_acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, DEFAULT_CONNECT_TIMEOUT );
        m_acceptor.bind( new InetSocketAddress(PORT) );
        LOG.info("Server binded");
        
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                stopServer();
            }
        });
    }
    
    protected void stopServer() {
        LOG.info("Server stopping...");
        for(IoSession session: m_acceptor.getManagedSessions().values()) {
            if(session.isConnected() && !session.isClosing()){
                session.close(false);
            }
        }
        
        m_acceptor.unbind();
        m_acceptor.dispose();
        LOG.info("Server stopped");
    }

}
