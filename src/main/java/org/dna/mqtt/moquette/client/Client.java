package org.dna.mqtt.moquette.client;

import java.net.InetSocketAddress;
import java.util.logging.Logger;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolDecoder;
import org.apache.mina.filter.codec.demux.DemuxingProtocolEncoder;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.dna.mqtt.moquette.proto.ConnAckDecoder;
import org.dna.mqtt.moquette.proto.ConnectDecoder;
import org.dna.mqtt.moquette.proto.ConnectEncoder;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.server.Server;

/**
 * Simple client to connect to a server
 * 
 * 
 * @author andrea
 */
public class Client {
    
    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    private static final String HOSTNAME = /*"localhost"*/ "127.0.0.1";
    private static final int PORT = Server.PORT;
    private static final long CONNECT_TIMEOUT = 30 * 1000L; // 30 seconds

    public static void main(String[] args) throws Throwable {
        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(new ConnAckDecoder());
        
        DemuxingProtocolEncoder encoder = new DemuxingProtocolEncoder();
        encoder.addMessageEncoder(ConnectMessage.class, new ConnectEncoder());
        
        
        NioSocketConnector connector = new NioSocketConnector();

        // Configure the service.
        connector.setConnectTimeoutMillis(CONNECT_TIMEOUT);
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(encoder, decoder));
        connector.getFilterChain().addLast("logger", new LoggingFilter());
        connector.setHandler(new  ClientMQTTHandler());

        IoSession session;
        for (;;) {
            try {
                ConnectFuture future = connector.connect(new InetSocketAddress(HOSTNAME, PORT));
                LOG.info("Client waiting to connect to server");
                future.awaitUninterruptibly();
                session = future.getSession();
                break;
            } catch (RuntimeIoException e) {
                System.err.println("Failed to connect.");
                e.printStackTrace();
                Thread.sleep(5000);
            }
        }
        
        //send a message over the session
        ConnectMessage connMsg = new ConnectMessage();
        connMsg.setKeepAlive(3);
        session.write(connMsg);
        LOG.info("Client wrote message to server");

        // wait until the summation is done
        session.getCloseFuture().awaitUninterruptibly();
        connector.dispose();
    }
    
}
