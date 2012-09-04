package org.dna.mqtt.moquette.server;

import java.io.File;
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
import org.dna.mqtt.moquette.messaging.spi.impl.SimpleMessaging;
import org.dna.mqtt.moquette.proto.*;
import org.dna.mqtt.moquette.proto.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Launch a  configured version of the server.
 * @author andrea
 */
public class Server {
    
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    
    public static final int PORT = 1883;
    public static final int DEFAULT_CONNECT_TIMEOUT = 10;
    public static final String STORAGE_FILE_PATH = System.getProperty("user.home") + 
            File.separator + "moquette_store.hawtdb";
    
    //TODO one notifier per outbound queue, else no serialization of message flow is granted!!
    //private static final int NOTIFIER_POOL_SIZE = 1;
    private IoAcceptor m_acceptor;
    SimpleMessaging messaging;
//    Thread messagingEventLoop;
    //private ExecutorService m_notifierPool/* = Executors.newFixedThreadPool(NOTIFIER_POOL_SIZE)*/;
    
    public static void main(String[] args) throws IOException {
        new Server().startServer();
        
    }
    
    protected void startServer() throws IOException {
        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(new ConnectDecoder());
        decoder.addMessageDecoder(new PublishDecoder());
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
        encoder.addMessageEncoder(PublishMessage.class, new PublishEncoder());
        encoder.addMessageEncoder(PingRespMessage.class, new PingRespEncoder());
        
        m_acceptor = new NioSocketAcceptor();
        
        m_acceptor.getFilterChain().addLast( "logger", new MQTTLoggingFilter("SERVER LOG") );
        m_acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter(encoder, decoder));

        MQTTHandler handler = new MQTTHandler();
        messaging = SimpleMessaging.getInstance();
        messaging.init();
        //TODO fix this hugly wiring
        handler.setMessaging(messaging);
//        messaging.setNotifier(handler);
//        messagingEventLoop = new Thread(messaging);
//        messagingEventLoop.setName("Event Loop" + System.currentTimeMillis());
//        messagingEventLoop.start();
        
        m_acceptor.setHandler(handler);
        ((NioSocketAcceptor)m_acceptor).setReuseAddress(true);
        ((NioSocketAcceptor)m_acceptor).getSessionConfig().setReuseAddress(true);
        m_acceptor.getSessionConfig().setReadBufferSize( 2048 );
        m_acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, DEFAULT_CONNECT_TIMEOUT );
        m_acceptor.getStatistics().setThroughputCalculationInterval(10);
        m_acceptor.getStatistics().updateThroughput(System.currentTimeMillis());
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
        
        messaging.stop();
        //messaging.stop();
//        messagingEventLoop.interrupt();
//        LOG.info("shutting down evet loop");
//        try {
//            messagingEventLoop.join();
//        } catch (InterruptedException ex) {
//            LOG.error(null, ex);
//        }
        
        /*m_notifierPool.shutdown();*/
        
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
        LOG.info("Server stopped");
    }

}
