@Grab(group='org.eclipse.jetty.websocket', module='websocket-client', version='9.2.0.M1', ext='jar')
@Grab(group='org.slf4j', module='slf4j-api', version='1.7.7')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.7.7')

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
@WebSocket
public class MQTTWebSocket {

    private static final Logger LOG = LoggerFactory.getLogger(MQTTWebSocket.class);

    private Session session;

    private final CountDownLatch connectSentLatch;

    public MQTTWebSocket() {
        this.connectSentLatch = new CountDownLatch(1);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("onClose - Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
        System.out.println("onConnect - Got connect");
        this.session = session;

        //Trivial MQTT Connect message
//        assertEquals(12, out.readByte()); //remaining length
//        verifyString("MQIsdp", out);
//        assertEquals(0x03, out.readByte()); //protocol version
//        assertEquals(0x32, out.readByte()); //flags
//        assertEquals(2, out.readByte()); //keepAliveTimer msb
//        assertEquals(0, out.readByte()); //keepAliveTimer lsb

        byte[] rawMsg = [ 0x10, //message type
                          0x11, //remaining length 17 bytes
                          0x00, 0x06, //MQIspd length
                          'M' as char, 'Q' as char, 'I' as char, 's' as char,'d' as char, 'p' as char,
                          0x03, //protocol version
                          0x32, //flags
                          0x00, 0x0A,// keepAlive Timer, 10 seconds
                          0x00, 0x03, 0x41, 0x42, 0x43 // A, B, C as client ID
        ] as byte[]
        ByteBuffer msg = ByteBuffer.wrap(rawMsg);

        //DBG
        println "onConnect - send bytes " + rawMsg.encodeHex().toString()
        //DBG

//        try {
        session.getRemote().sendBytes(msg);
        println("onConnect - Sent connect bytes");
        this.connectSentLatch.countDown();
        //session.close(StatusCode.NORMAL, "I'm done");
        println("onConnect - After connect");

        println "send disconnect"
        disconnect()
    }

    public boolean awaitConnected(int duration, TimeUnit unit) throws InterruptedException {
        return this.connectSentLatch.await(duration, unit);
    }

    /**
     * Send a disconnect message to the server
     * */
    public void disconnect() {
        byte[] rawMsg = [0xE0, 0x00] as byte[]
        println "disconnect - send bytes " + rawMsg.encodeHex().toString()
        ByteBuffer msg = ByteBuffer.wrap(rawMsg);
        this.session.getRemote().sendBytes(msg)
    }

    @OnWebSocketMessage
    public void onMessage(Session session, byte[] buf, int offset, int length) {
        System.out.printf("onMessage - Got msg: %s%n", buf);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable cause) {
        System.out.printf("onError - Got exception: %s%n", cause);
    }
}


String destUri = "ws://localhost:8080/mqtt"
//String destUri = "ws://test.mosquitto.org:8080/"
WebSocketClient client = new WebSocketClient()
MQTTWebSocket socket = new MQTTWebSocket();
client.start();
URI echoUri = new URI(destUri);
ClientUpgradeRequest request = new ClientUpgradeRequest();
client.connect(socket, echoUri, request);
println "main - Connecting to : ${echoUri}"
boolean connected = socket.awaitConnected(4, TimeUnit.SECONDS);
println "main - After wait for awaitConnected was ${connected}"

//println "send disconnect"
//socket.disconnect()
