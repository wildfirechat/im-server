/**
 * Copyright (C) 2010-2012, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.mqtt.client;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.HexSupport;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.transport.DefaultTransportListener;
import org.fusesource.hawtdispatch.transport.HeartBeatMonitor;
import org.fusesource.hawtdispatch.transport.SslTransport;
import org.fusesource.hawtdispatch.transport.TcpTransport;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.mqtt.codec.CONNACK;
import org.fusesource.mqtt.codec.DISCONNECT;
import org.fusesource.mqtt.codec.MQTTFrame;
import org.fusesource.mqtt.codec.MQTTProtocolCodec;
import org.fusesource.mqtt.codec.MessageSupport.Acked;
import org.fusesource.mqtt.codec.PINGREQ;
import org.fusesource.mqtt.codec.PINGRESP;
import org.fusesource.mqtt.codec.PUBACK;
import org.fusesource.mqtt.codec.PUBCOMP;
import org.fusesource.mqtt.codec.PUBLISH;
import org.fusesource.mqtt.codec.PUBREC;
import org.fusesource.mqtt.codec.PUBREL;
import org.fusesource.mqtt.codec.SUBACK;
import org.fusesource.mqtt.codec.SUBSCRIBE;
import org.fusesource.mqtt.codec.UNSUBACK;
import org.fusesource.mqtt.codec.UNSUBSCRIBE;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fusesource.hawtbuf.Buffer.utf8;
import static org.fusesource.hawtdispatch.Dispatch.createQueue;


/**
 * <p>
 * A callback based non/blocking Connection interface to MQTT.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class CallbackConnection {
    
    private static class Request {
        private final MQTTFrame frame;
        private final short id;
        private final Callback cb;

        Request(int id, MQTTFrame frame, Callback cb) {
            this.id = (short) id;
            this.cb = cb;
            this.frame = frame;
        }
    }

    private static final ExtendedListener DEFAULT_LISTENER = new ExtendedListener(){
        public void onConnected() {
        }
        public void onDisconnected() {
        }
        public void onPublish(UTF8Buffer utf8Buffer, Buffer buffer, Runnable runnable) {
            this.onFailure(createListenerNotSetError());
        }
        public void onPublish(UTF8Buffer topic, Buffer body, Callback<Callback<byte[]>> ack) {
            this.onFailure(createListenerNotSetError());
        }
        public void onFailure(Throwable value) {
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), value);
        }
    };

    private final DispatchQueue queue;
    private final MQTT mqtt;
    private Transport transport;
    private ExtendedListener listener = DEFAULT_LISTENER;
    private Runnable refiller;
    private Map<Short, Request> requests = new ConcurrentHashMap<Short, Request>();
    private LinkedList<Request> overflow = new LinkedList<Request>();
    private final HashMap<Short, Callback<byte[]>> processed = new HashMap<Short, Callback<byte[]>>();
    private Throwable failure;
    private boolean disconnected = false;
    private HeartBeatMonitor heartBeatMonitor;
    private long pingedAt;
    private long reconnects = 0;
    private final AtomicInteger suspendCount = new AtomicInteger(0);
    private final AtomicInteger suspendChanges = new AtomicInteger(0);

    private final HashMap<UTF8Buffer, QoS> activeSubs = new HashMap<UTF8Buffer, QoS>();


    public CallbackConnection(MQTT mqtt) {
        this.mqtt = mqtt;
        if(this.mqtt.dispatchQueue == null) {
            this.queue = createQueue("mqtt client");
        } else {
            this.queue = this.mqtt.dispatchQueue;
        }
    }

    public void connect(final Callback<byte[]> cb) {
        assert cb !=null : "Callback should not be null.";

        if( transport!=null ) {
            cb.onFailure(new IllegalStateException("Already connected"));
            return;
        }
        try {
            createTransport(new LoginHandler(cb, true));
        } catch (Throwable e) {
            // This error happens when the MQTT config is invalid, reattempting
            // wont fix this case.
            cb.onFailure(e);
        }
    }

    void reconnect() {
        try {
            // And reconnect.
            createTransport(new LoginHandler(new Callback<byte[]>() {
                public void onSuccess(byte[] value) {

                    mqtt.tracer.debug("Restoring MQTT connection state");
                    // Setup a new overflow so that the replay can be sent out before the original overflow list.
                    LinkedList<Request> originalOverflow = overflow;
                    Map<Short, Request> originalRequests = requests;
                    overflow = new LinkedList<Request>();
                    requests = new ConcurrentHashMap<Short, Request>();

                    // Restore any active subscriptions.
                    if (!activeSubs.isEmpty()) {
                        ArrayList<Topic> topics = new ArrayList<Topic>(activeSubs.size());
                        for (Map.Entry<UTF8Buffer, QoS> entry : activeSubs.entrySet()) {
                            topics.add(new Topic(entry.getKey(), entry.getValue()));
                        }
                        send(new SUBSCRIBE().topics(topics.toArray(new Topic[topics.size()])), null);
                    }

                    // Replay any un-acked requests..
                    for (Map.Entry<Short, Request> entry : originalRequests.entrySet()) {
                        MQTTFrame frame = entry.getValue().frame;
                        frame.dup(true); // set the dup flag as these frames were previously transmitted.
                        send(entry.getValue());
                    }

                    // Replay the original overflow
                    for (Request request : originalOverflow) {
                        // Stuff in the overflow never got sent out.. so no need to set the dup flag
                        send(request);
                    }

                }

                public void onFailure(Throwable value) {
                    handleFatalFailure(value);
                }
            }, false));
        } catch (Throwable e) {
            handleFatalFailure(e);
        }
    }
    void handleSessionFailure(Throwable error) {
        // Socket failure, should we try to reconnect?
        if( !disconnected && (mqtt.reconnectAttemptsMax<0 || reconnects < mqtt.reconnectAttemptsMax ) ) {

            mqtt.tracer.debug("Reconnecting transport");
            // Cleanup the previous transport.
            if(heartBeatMonitor!=null) {
                heartBeatMonitor.stop();
                heartBeatMonitor = null;
            }
            final Transport t = transport;
            transport = null;

            if(t!=null) {
                t.stop(new Task() {
                    @Override
                    public void run() {
                        listener.onDisconnected();
                        reconnect();
                    }
                });
            } else {
                reconnect();
            }

        } else {
            // nope.
            handleFatalFailure(error);
        }
    }

    void reconnect(final Callback<Transport> onConnect) {
        long reconnectDelay = mqtt.reconnectDelay;
        if( reconnectDelay> 0 && mqtt.reconnectBackOffMultiplier > 1.0 ) {
            reconnectDelay = (long) Math.pow(mqtt.reconnectDelay*reconnects, mqtt.reconnectBackOffMultiplier);
        }
        reconnectDelay = Math.min(reconnectDelay, mqtt.reconnectDelayMax);
        reconnects += 1;
        queue.executeAfter(reconnectDelay, TimeUnit.MILLISECONDS, new Task() {
            @Override
            public void run() {
                if(disconnected) {
                    onConnect.onFailure(createDisconnectedError());
                } else {
                    try {
                        createTransport(onConnect);
                    } catch (Exception e) {
                        onConnect.onFailure(e);
                    }
                }
            }
        });
    }

    /**
     * Creates and start a transport to the MQTT server.  Passes it to the onConnect
     * once the transport is connected.
     *
     * @param onConnect
     * @throws Exception
     */
    void createTransport(final Callback<Transport> onConnect) throws Exception {
        mqtt.tracer.debug("Connecting");
        String scheme = mqtt.host.getScheme();

        final Transport transport;
        if( "tcp".equals(scheme) ) {
            transport = new TcpTransport();
        }  else if( SslTransport.protocol(scheme)!=null ) {
            SslTransport ssl = new SslTransport();
            if( mqtt.sslContext == null ) {
                mqtt.sslContext = SSLContext.getDefault();
            }
            ssl.setSSLContext(mqtt.sslContext);
            transport = ssl;
        } else {
            throw new Exception("Unsupported URI scheme '"+scheme+"'");
        }

        if( mqtt.blockingExecutor == null ) {
            mqtt.blockingExecutor = MQTT.getBlockingThreadPool();
        }
        transport.setBlockingExecutor(mqtt.blockingExecutor);
        transport.setDispatchQueue(queue);
        transport.setProtocolCodec(new MQTTProtocolCodec());

        if( transport instanceof TcpTransport ) {
            TcpTransport tcp = (TcpTransport)transport;
            tcp.setMaxReadRate(mqtt.maxReadRate);
            tcp.setMaxWriteRate(mqtt.maxWriteRate);
            tcp.setReceiveBufferSize(mqtt.receiveBufferSize);
            tcp.setSendBufferSize(mqtt.sendBufferSize);
            tcp.setTrafficClass(mqtt.trafficClass);
            tcp.setUseLocalHost(mqtt.useLocalHost);
            tcp.connecting(mqtt.host, mqtt.localAddress);
        }

        transport.setTransportListener(new DefaultTransportListener(){
            @Override
            public void onTransportConnected() {
                mqtt.tracer.debug("Transport connected");
                if(disconnected) {
                    onFailure(createDisconnectedError());
                } else {
                    onConnect.onSuccess(transport);
                }
            }

            @Override
            public void onTransportFailure(final IOException error) {
                mqtt.tracer.debug("Transport failure: %s", error);
                onFailure(error);
            }

            private void onFailure(final Throwable error) {
                if(!transport.isClosed()) {
                    transport.stop(new Task() {
                        @Override
                        public void run() {
                            onConnect.onFailure(error);
                        }
                    });
                }
            }
        });
        transport.start(NOOP);
    }

    class LoginHandler implements Callback<Transport> {
        private final Callback<byte[]> cb;
        private final boolean initialConnect;

        LoginHandler(Callback<byte[]> cb, boolean initialConnect) {
            this.cb = cb;
            this.initialConnect = initialConnect;
        }

        public void onSuccess(final Transport transport) {
            transport.setTransportListener(new DefaultTransportListener() {
                @Override
                public void onTransportFailure(IOException error) {
                    mqtt.tracer.debug("Transport failure: %s", error);
                    transport.stop(NOOP);
                    onFailure(error);
                }

                @Override
                public void onTransportCommand(Object command) {
                    MQTTFrame response = (MQTTFrame) command;
                    mqtt.tracer.onReceive(response);
                    try {
                        switch (response.messageType()) {
                            case CONNACK.TYPE:
                                CONNACK connack = new CONNACK().decode(response);
                                switch (connack.code()) {
                                    case CONNECTION_ACCEPTED:
                                        mqtt.tracer.debug("MQTT login accepted");
                                        onSessionEstablished(transport);
                                        listener.onConnected();
                                        cb.onSuccess(connack.payload);
                                        queue.execute(new Task() {
                                            @Override
                                            public void run() {
                                                drainOverflow();
                                            }
                                        });
                                        break;
                                    default:
                                        mqtt.tracer.debug("MQTT login rejected");
                                        // Bad creds or something. No point in reconnecting.
                                        transport.stop(NOOP);
                                        cb.onFailure(new MQTTException("Could not connect: " + connack.code(), connack));
                                }
                                break;
                            default:
                                mqtt.tracer.debug("Received unexpected MQTT frame: %d", response.messageType());
                                // Naughty MQTT server? No point in reconnecting.
                                transport.stop(NOOP);
                                cb.onFailure(new IOException("Could not connect. Received unexpected command: " + response.messageType()));

                        }
                    } catch (ProtocolException e) {
                        mqtt.tracer.debug("Protocol error: %s", e);
                        transport.stop(NOOP);
                        cb.onFailure(e);
                    }
                }
            });
            transport.resumeRead();
            if( mqtt.connect.clientId() == null ) {
                String id = hex(transport.getLocalAddress())+Long.toHexString(System.currentTimeMillis()/1000);
                if(id.length() > 23) {
                    id = id.substring(0,23);
                }
                mqtt.connect.clientId(utf8(id));
            }
            MQTTFrame encoded = mqtt.connect.encode();
            boolean accepted = transport.offer(encoded);
            mqtt.tracer.onSend(encoded);
            mqtt.tracer.debug("Logging in");
            assert accepted: "First frame should always be accepted by the transport";
        }
        
        private boolean tryReconnect() {
            if(initialConnect) {
                return mqtt.connectAttemptsMax<0 || reconnects < mqtt.connectAttemptsMax;
            }
            
            return mqtt.reconnectAttemptsMax<0 || reconnects < mqtt.reconnectAttemptsMax;
        }

        public void onFailure(Throwable value) {
            // Socket failure, should we try to reconnect?
            if( !disconnected && tryReconnect() ) {
                reconnect(this);
            } else {
                // nope.
                cb.onFailure(value);
            }
        }
    }

    private boolean onRefillCalled =false;
    public void onSessionEstablished(Transport transport) {
        this.transport = transport;
        if( suspendCount.get() > 0 ) {
            this.transport.suspendRead();
        }
        this.transport.setTransportListener(new DefaultTransportListener() {
            @Override
            public void onTransportCommand(Object command) {
                MQTTFrame frame = (MQTTFrame) command;
                mqtt.tracer.onReceive(frame);
                processFrame(frame);
            }
            @Override
            public void onRefill() {
                onRefillCalled =true;
                drainOverflow();
            }

            @Override
            public void onTransportFailure(IOException error) {
                handleSessionFailure(error);
            }
        });
        pingedAt = 0;
        if(mqtt.getKeepAlive()>0) {
            heartBeatMonitor = new HeartBeatMonitor();
            heartBeatMonitor.setWriteInterval((mqtt.getKeepAlive() * 1000) / 2);
            heartBeatMonitor.setTransport(this.transport);
            heartBeatMonitor.suspendRead(); // to match the suspended state of the transport.
            heartBeatMonitor.setOnKeepAlive(new Task() {
                @Override
                public void run() {
                    // Don't care if the offer is rejected, just means we have data outbound.
                    if(!disconnected && pingedAt==0) {
                        MQTTFrame encoded = new PINGREQ().encode();
                        if(CallbackConnection.this.transport.offer(encoded)) {
                            mqtt.tracer.onSend(encoded);
                            final long now = System.currentTimeMillis();
                            final long suspends = suspendChanges.get();
                            pingedAt = now;
                            queue.executeAfter(CallbackConnection.this.mqtt.getKeepAlive(), TimeUnit.SECONDS, new Task() {
                                @Override
                                public void run() {
                                    if (now == pingedAt) {
                                        // if the connection remained suspend we will never get the ping response..
                                        // Looks like the user has forgoton to resume the connection
                                        if (suspends == suspendChanges.get() && suspendCount.get() > 0) {
                                            // Since the connection has been suspended, we can't really
                                            // check to see if we are getting the ping responses.
                                            mqtt.tracer.debug("The connection has remained suspended for an extended period of time so it cannot do proper keep alive processing.  Did you forget to resume the connection?");
                                        } else {
                                            mqtt.tracer.debug("Ping timeout");
                                            handleSessionFailure(new ProtocolException("Ping timeout").fillInStackTrace());
                                        }
                                    }
                                }
                            });
                        }
                    }
                    
                }
            });
            heartBeatMonitor.start();
        }
    }

    public Transport transport() {
        return transport;
    }

    public DispatchQueue getDispatchQueue() {
        return queue;
    }

    public void resume() {
        suspendChanges.incrementAndGet();
        if( suspendCount.decrementAndGet() == 0 && this.transport!=null ) {
            this.transport.resumeRead();
            if(this.heartBeatMonitor!=null){
                this.heartBeatMonitor.resumeRead();
            }
        }
    }

    public void suspend() {
        suspendChanges.incrementAndGet();
        if( suspendCount.incrementAndGet() == 1 && this.transport!=null ) {
            this.transport.suspendRead();
            if(this.heartBeatMonitor!=null){
                this.heartBeatMonitor.suspendRead();
            }
        }
    }

    public CallbackConnection refiller(Runnable refiller) {
        queue.assertExecuting();
        this.refiller = refiller;
        return this;
    }

    public CallbackConnection listener(final Listener original) {
        if( original instanceof ExtendedListener ) {
            this.listener = (ExtendedListener) original;
        } else {
            this.listener = new ExtendedListener() {
                public void onPublish(UTF8Buffer topic, Buffer body, final Callback<Callback<byte[]>> ack) {
                    original.onPublish(topic, body, new Runnable() {
                        public void run() {
                            ack.onSuccess(null);
                        }
                    });
                }
                public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
                    original.onPublish(topic, body, ack);
                }

                public void onConnected() {
                    original.onConnected();
                }

                public void onDisconnected() {
                    original.onDisconnected();
                }

                public void onFailure(Throwable value) {
                    original.onFailure(value);
                }
            };
        }
        return this;
    }

    public boolean full() {
        queue.assertExecuting();
        return this.transport.full();
    }

    public Throwable failure() {
        queue.assertExecuting();
        return failure;
    }

    public void disconnect(boolean clearSession, final Callback<Void> onComplete) {
        if( disconnected ) {
            if(onComplete!=null){
                onComplete.onSuccess(null);
            }
            return;
        }

        disconnected = true;
        final short requestId = getNextMessageId();
        final Runnable stop = new Runnable() {
            private boolean executed = false;
            public void run() {
                if(!executed) {
                    executed = true;
                    requests.remove(requestId);

                    if(heartBeatMonitor!=null) {
                        heartBeatMonitor.stop();
                        heartBeatMonitor = null;
                    }
                    transport.stop(new Task() {
                        @Override
                        public void run() {
                            listener.onDisconnected();
                            if (onComplete != null) {
                                onComplete.onSuccess(null);
                            }
                        }
                    });
                }
            }
        };
        
        Callback<Void> cb = new Callback<Void>() {
            public void onSuccess(Void v) {
                // To make sure DISCONNECT has been flushed out to the socket
                onRefillCalled = false;
                refiller = new Runnable() {
                    public void run() {
                        if(onRefillCalled) {
                            stop.run();
                        }
                    }
                };
                if(transport != null){
                    transport.flush();
                }
            }
            public void onFailure(Throwable value) {
                stop.run();
            }
        };
        
        // Pop the frame into a request so it we get notified
        // of any failures so we continue to stop the transport.
        if(transport!=null) {
            MQTTFrame frame = new DISCONNECT().encode().dup(clearSession);
            send(new Request(getNextMessageId(), frame, cb));
        } else {
            cb.onSuccess(null);
        }
    }

    /**
     * Kills the connection without a graceful disconnect.
     * @param onComplete
     */
    public void kill(final Callback<Void> onComplete) {
        if( disconnected ) {
            if(onComplete!=null){
                onComplete.onSuccess(null);
            }
            return;
        }
        disconnected = true;
        if(heartBeatMonitor!=null) {
            heartBeatMonitor.stop();
            heartBeatMonitor = null;
        }
        transport.stop(new Task() {
            @Override
            public void run() {
                listener.onDisconnected();
                if (onComplete != null) {
                    onComplete.onSuccess(null);
                }
            }
        });
    }

    public void publish(String topic, byte[] payload, QoS qos, boolean retain, Callback<byte[]> cb) {
        publish(utf8(topic), new Buffer(payload), qos, retain, cb);
    }

    public void publish(UTF8Buffer topic, Buffer payload, QoS qos, boolean retain, Callback<byte[]> cb) {
        queue.assertExecuting();
        if( disconnected ) {
            cb.onFailure(createDisconnectedError());
            return;
        }
        PUBLISH command = new PUBLISH().qos(qos).retain(retain);
        command.topicName(topic).payload(payload);
        send(command, cb);
    }

    public void subscribe(final Topic[] topics, Callback<byte[]> cb) {
        if(topics==null) {
            throw new IllegalArgumentException("topics must not be null");
        }
        queue.assertExecuting();
        if( disconnected ) {
            cb.onFailure(createDisconnectedError());
            return;
        }
        if( listener == DEFAULT_LISTENER ) {
            cb.onFailure(createListenerNotSetError());
        } else {
            send(new SUBSCRIBE().topics(topics), new ProxyCallback<byte[]>(cb){
                @Override
                public void onSuccess(byte[] value) {
                    for (Topic topic : topics) {
                        activeSubs.put(topic.name(), topic.qos());
                    }
                    if(next!=null) {
                        next.onSuccess(value);
                    }
                }
            });
        }
    }

    public void unsubscribe(final UTF8Buffer[] topics, Callback<Void> cb) {
        queue.assertExecuting();
        if( disconnected ) {
            cb.onFailure(createDisconnectedError());
            return;
        }
        send(new UNSUBSCRIBE().topics(topics), new ProxyCallback(cb){
            @Override
            public void onSuccess(Object value) {
                for (UTF8Buffer topic : topics) {
                    activeSubs.remove(topic);
                }
                if(next!=null) {
                    next.onSuccess(value);
                }
            }
        });
    }

    private void send(Acked command, Callback cb) {
        short id = 0;
        if(command.qos() != QoS.AT_MOST_ONCE) {
            id = getNextMessageId();
            command.messageId(id);
        }
        send(new Request(id, command.encode(), cb));
    }

    private void send(Request request) {
        if( failure !=null ) {
            if( request.cb!=null ) {
                request.cb.onFailure(failure);
            }
        } else {
            // Put the request in the map before sending it over the wire. 
            if(request.id!=0) {
                this.requests.put(request.id, request);
            }

            if( overflow.isEmpty() && transport!=null && transport.offer(request.frame) ) {
                mqtt.tracer.onSend(request.frame);
                if(request.id==0) {
                    if( request.cb!=null ) {
                        ((Callback<Void>)request.cb).onSuccess(null);
                    }
                    
                }
            } else {
                // Remove it from the request.
                this.requests.remove(request.id);
                overflow.addLast(request);
            }
        }
    }

    private short nextMessageId = 1;
    private short getNextMessageId() {
        short rc = nextMessageId;
        nextMessageId++;
        if(nextMessageId==0) {
            nextMessageId=1;
        }
        return rc;
    }

    private void drainOverflow() {
        queue.assertExecuting();
        if( overflow.isEmpty() || transport==null ){
            return;
        }
        Request request;
        while((request=overflow.peek())!=null) {
            if( this.transport.offer(request.frame) ) {
                mqtt.tracer.onSend(request.frame);
                overflow.removeFirst();
                if(request.id==0) {
                    if( request.cb!=null ) {
                        ((Callback<Void>)request.cb).onSuccess(null);
                    }
                } else {
                    this.requests.put(request.id, request);
                }
            } else {
                break;
            }
        }
        if( overflow.isEmpty() ) {
            if( refiller!=null ) {
                try {
                    refiller.run();
                } catch (Throwable e) {
                    Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        }
    }


    private void completeRequest(short id, byte originalType, Object arg) {
        Request request = requests.remove(id);
        if( request!=null ) {
            assert originalType==request.frame.messageType();
            if(request.cb!=null) {
                if( arg==null ) {
                    ((Callback<Void>)request.cb).onSuccess(null);
                } else {
                    ((Callback<Object>)request.cb).onSuccess(arg);
                }
            }
        } else {
            handleFatalFailure(new ProtocolException("Command from server contained an invalid message id: " + id));
        }
    }

    private void processFrame(MQTTFrame frame) {
        try {
            switch(frame.messageType()) {
                case PUBLISH.TYPE: {
                    PUBLISH publish = new PUBLISH().decode(frame);
                    toReceiver(publish);
                    break;
                }
                case PUBREL.TYPE:{
                    PUBREL ack = new PUBREL().decode(frame);
                    Callback<byte[]> onRel = processed.remove(ack.messageId());
                    PUBCOMP response = new PUBCOMP();
                    response.messageId(ack.messageId());
                    send(new Request(0, response.encode(), null));
                    if( onRel!=null ) {
                        onRel.onSuccess(null);
                    }
                    break;
                }
                case PUBACK.TYPE:{
                    PUBACK ack = new PUBACK().decode(frame);
                    completeRequest(ack.messageId(), PUBLISH.TYPE, ack.payload);
                    break;
                }
                case PUBREC.TYPE:{
                    PUBREC ack = new PUBREC().decode(frame);
                    PUBREL response = new PUBREL();
                    response.messageId(ack.messageId());
                    send(new Request(0, response.encode(), null));
                    break;
                }
                case PUBCOMP.TYPE:{
                    PUBCOMP ack = new PUBCOMP().decode(frame);
                    completeRequest(ack.messageId(), PUBLISH.TYPE, null);
                    break;
                }
                case SUBACK.TYPE: {
                    SUBACK ack = new SUBACK().decode(frame);
                    completeRequest(ack.messageId(), SUBSCRIBE.TYPE, ack.grantedQos());
                    break;
                }
                case UNSUBACK.TYPE: {
                    UNSUBACK ack = new UNSUBACK().decode(frame);
                    completeRequest(ack.messageId(), UNSUBSCRIBE.TYPE, null);
                    break;
                }
                case PINGRESP.TYPE: {
                    pingedAt = 0;
                    break;
                }
                default:
                    throw new ProtocolException("Unexpected MQTT command type: "+frame.messageType());
            }
        } catch (Throwable e) {
            handleFatalFailure(e);
        }
    }

    static public final Task NOOP = Dispatch.NOOP;

    private void toReceiver(final PUBLISH publish) {
        if( listener !=null ) {
            try {
                Callback<Callback<byte[]>> cb = null;
                switch( publish.qos() ) {
                    case AT_LEAST_ONCE:
                        cb = new Callback<Callback<byte[]>>() {
                            public void onSuccess(Callback<byte[]> value) {
                                PUBACK response = new PUBACK();
                                response.messageId(publish.messageId());
                                send(new Request(0, response.encode(), null));
                                if( value !=null ) {
                                    value.onSuccess(null);
                                }
                            }
                            public void onFailure(Throwable value) {
                            }
                        };
                        break;
                    case EXACTLY_ONCE:
                        cb = new Callback<Callback<byte[]>>() {
                            public void onSuccess(Callback<byte[]> value) {
                                PUBREC response = new PUBREC();
                                response.messageId(publish.messageId());
                                processed.put(publish.messageId(), value);
                                send(new Request(0, response.encode(), null));
                            }

                            public void onFailure(Throwable value) {
                            }
                        };
                        // Looks like a dup delivery.. filter it out.
                        if( processed.get(publish.messageId())!=null ) {
                            return;
                        }
                        break;
                    case AT_MOST_ONCE:
                        cb = new Callback<Callback<byte[]>>() {
                            public void onSuccess(Callback<byte[]> value) {
                                if (value != null) {
                                    value.onSuccess(null);
                                }
                            }

                            public void onFailure(Throwable value) {
                            }
                        };
                }
                listener.onPublish(publish.topicName(), publish.payload(), cb);
            } catch (Throwable e) {
                handleFatalFailure(e);
            }
        }
    }

    private void handleFatalFailure(Throwable error) {
        if( failure == null ) {
            failure = error;
            
            mqtt.tracer.debug("Fatal connection failure: %s", error);
            // Fail incomplete requests.
            ArrayList<Request> values = new ArrayList(requests.values());
            requests.clear();
            for (Request value : values) {
                if( value.cb!= null ) {
                    value.cb.onFailure(failure);
                }
            }

            ArrayList<Request> overflowEntries = new ArrayList<Request>(overflow);
            overflow.clear();
            for (Request entry : overflowEntries) {
                if( entry.cb !=null ) {
                    entry.cb.onFailure(failure);
                }
            }
            
            if( listener !=null && !disconnected ) {
                try {
                    listener.onFailure(failure);
                } catch (Exception e) {
                    Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        }
    }

    private static IllegalStateException createListenerNotSetError() {
        return (IllegalStateException) new IllegalStateException("No connection listener set to handle message received from the server.").fillInStackTrace();
    }

    private static IllegalStateException createDisconnectedError() {
        return (IllegalStateException) new IllegalStateException("Disconnected").fillInStackTrace();
    }

    static private  String hex(SocketAddress address) {
        if( address instanceof InetSocketAddress ) {
            InetSocketAddress isa = (InetSocketAddress)address;
            return HexSupport.toHexFromBuffer(new Buffer(isa.getAddress().getAddress()))+Integer.toHexString(isa.getPort());
        }
        return "";
    }

}
