package org.dna.mqtt.moquette.server.mina;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoServiceStatistics;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolDecoder;
import org.apache.mina.filter.codec.demux.DemuxingProtocolEncoder;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.dna.mqtt.commons.Constants;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.proto.ConnAckEncoder;
import org.dna.mqtt.moquette.proto.ConnectDecoder;
import org.dna.mqtt.moquette.proto.DisconnectDecoder;
import org.dna.mqtt.moquette.proto.MQTTLoggingFilter;
import org.dna.mqtt.moquette.proto.PingReqDecoder;
import org.dna.mqtt.moquette.proto.PingRespEncoder;
import org.dna.mqtt.moquette.proto.PubAckDecoder;
import org.dna.mqtt.moquette.proto.PubAckEncoder;
import org.dna.mqtt.moquette.proto.PubCompDecoder;
import org.dna.mqtt.moquette.proto.PubCompEncoder;
import org.dna.mqtt.moquette.proto.PubRecDecoder;
import org.dna.mqtt.moquette.proto.PubRecEncoder;
import org.dna.mqtt.moquette.proto.PubRelDecoder;
import org.dna.mqtt.moquette.proto.PubRelEncoder;
import org.dna.mqtt.moquette.proto.PublishDecoder;
import org.dna.mqtt.moquette.proto.PublishEncoder;
import org.dna.mqtt.moquette.proto.SubAckEncoder;
import org.dna.mqtt.moquette.proto.SubscribeDecoder;
import org.dna.mqtt.moquette.proto.UnsubAckEncoder;
import org.dna.mqtt.moquette.proto.UnsubscribeDecoder;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.PingRespMessage;
import org.dna.mqtt.moquette.proto.messages.PubAckMessage;
import org.dna.mqtt.moquette.proto.messages.PubCompMessage;
import org.dna.mqtt.moquette.proto.messages.PubRecMessage;
import org.dna.mqtt.moquette.proto.messages.PubRelMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubAckMessage;
import org.dna.mqtt.moquette.server.MQTTHandler;
import org.dna.mqtt.moquette.server.ServerAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author andrea
 */
public class MinaAcceptor implements ServerAcceptor {
    
    private static final Logger LOG = LoggerFactory.getLogger(MinaAcceptor.class);

    private IoAcceptor m_acceptor;
    
    public void initialize(IMessaging messaging) throws IOException {
        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(new ConnectDecoder());
        decoder.addMessageDecoder(new PublishDecoder());
        decoder.addMessageDecoder(new PubAckDecoder());
        decoder.addMessageDecoder(new PubRelDecoder());
        decoder.addMessageDecoder(new PubRecDecoder());
        decoder.addMessageDecoder(new PubCompDecoder());
        decoder.addMessageDecoder(new SubscribeDecoder());
        decoder.addMessageDecoder(new UnsubscribeDecoder());
        decoder.addMessageDecoder(new DisconnectDecoder());
        decoder.addMessageDecoder(new PingReqDecoder());
        
        DemuxingProtocolEncoder encoder = new DemuxingProtocolEncoder();
//        encoder.addMessageEncoder(ConnectMessage.class, new ConnectEncoder());
        encoder.addMessageEncoder(ConnAckMessage.class, new ConnAckEncoder());
        encoder.addMessageEncoder(SubAckMessage.class, new SubAckEncoder());
        encoder.addMessageEncoder(UnsubAckMessage.class, new UnsubAckEncoder());
        encoder.addMessageEncoder(PubAckMessage.class, new PubAckEncoder());
        encoder.addMessageEncoder(PubRecMessage.class, new PubRecEncoder());
        encoder.addMessageEncoder(PubCompMessage.class, new PubCompEncoder());
        encoder.addMessageEncoder(PubRelMessage.class, new PubRelEncoder());
        encoder.addMessageEncoder(PublishMessage.class, new PublishEncoder());
        encoder.addMessageEncoder(PingRespMessage.class, new PingRespEncoder());
        
        m_acceptor = new NioSocketAcceptor();
        
        m_acceptor.getFilterChain().addLast( "logger", new MQTTLoggingFilter("SERVER LOG") );
        m_acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter(encoder, decoder));
        
        MQTTHandler handler = new MQTTHandler();
        
        handler.setMessaging(messaging);
        
        m_acceptor.setHandler(handler);
        ((NioSocketAcceptor)m_acceptor).setReuseAddress(true);
        ((NioSocketAcceptor)m_acceptor).getSessionConfig().setReuseAddress(true);
        m_acceptor.getSessionConfig().setReadBufferSize( 2048 );
        m_acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, Constants.DEFAULT_CONNECT_TIMEOUT );
        m_acceptor.getStatistics().setThroughputCalculationInterval(10);
        m_acceptor.getStatistics().updateThroughput(System.currentTimeMillis());
        m_acceptor.bind( new InetSocketAddress(Constants.PORT) );
        LOG.info("Server binded");
    }

    public void close() {
        //log statistics
        IoServiceStatistics statistics  = m_acceptor.getStatistics();
        statistics.updateThroughput(System.currentTimeMillis());
        System.out.println(String.format("Total read bytes: %d, read throughtput: %f (b/s)", statistics.getReadBytes(), statistics.getReadBytesThroughput()));
        System.out.println(String.format("Total read msgs: %d, read msg throughtput: %f (msg/s)", statistics.getReadMessages(), statistics.getReadMessagesThroughput()));
        
        for(IoSession session: m_acceptor.getManagedSessions().values()) {
            if(session.isConnected() && !session.isClosing()){
                session.close(false);
            }
        }

        m_acceptor.unbind();
        m_acceptor.dispose();
    }
    
}
