/*
 * Copyright (c) 2012-2015 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.moquette.server;

import java.io.IOException;
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
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
    }
    
    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
        System.out.printf("Got connect: %s%n", session);
        LOG.info("Got connect: %s%n", session);
        this.session = session;
        
        //Trivial MQTT Connect message
//        assertEquals(12, out.readByte()); //remaining length
//        verifyString("MQIsdp", out);
//        assertEquals(0x03, out.readByte()); //protocol version
//        assertEquals(0x32, out.readByte()); //flags
//        assertEquals(2, out.readByte()); //keepAliveTimer msb
//        assertEquals(0, out.readByte()); //keepAliveTimer lsb
        
        ByteBuffer msg = ByteBuffer.wrap(new byte[] { 0x10, //message type
            0x0C, //remaining lenght 12 bytes
            'M', 'Q','I','s','d','p',
            0x03, //protocol version
            0x32, //flags
            0x02, 0x00// keepAlive Timer, 2 seconds
        });
//        try {
            session.getRemote().sendBytes(msg);
            this.connectSentLatch.countDown();
            session.close(StatusCode.NORMAL, "I'm done");
//        } catch (IOException t) {
//            LOG.error(null, t);
//        }
    }
    
    public boolean awaitConnected(int duration, TimeUnit unit) throws InterruptedException {
        return this.connectSentLatch.await(duration, unit);
    }
 
    @OnWebSocketMessage
    public void onMessage(Session session, byte[] buf, int offset, int length) {
        System.out.printf("Got msg: %s%n", buf);
    }
    
    @OnWebSocketError
    public void onError(Session session, Throwable cause) {
        System.out.printf("Got exception: %s%n", cause);
    }
}
