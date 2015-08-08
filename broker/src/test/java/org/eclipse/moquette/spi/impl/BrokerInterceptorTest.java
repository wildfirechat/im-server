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
package org.eclipse.moquette.spi.impl;

import org.eclipse.moquette.interception.InterceptHandler;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.ConnectMessage;
import org.eclipse.moquette.proto.messages.PublishMessage;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * @author Wagner Macedo
 */
public class BrokerInterceptorTest {
    // value to check for changes after every notification
    private static final AtomicInteger n = new AtomicInteger(0);

    // Interceptor loaded with a custom InterceptHandler special for the tests
    private static final class MockObserver implements InterceptHandler {
        @Override
        public void onConnect(ConnectMessage msg) {
            n.set(40);
        }

        @Override
        public void onDisconnect(String clientID) {
            n.set(50);
        }

        @Override
        public void onPublish(PublishMessage msg) {
            n.set(60);
        }

        @Override
        public void onSubscribe(Subscription sub) {
            n.set(70);
        }

        @Override
        public void onUnsubscribe(String topic) {
            n.set(80);
        }
    }

    private static final BrokerInterceptor interceptor = new BrokerInterceptor(Arrays.<InterceptHandler>asList(new MockObserver()));

    @BeforeClass
    public static void beforeAllTests() {
        // check if any of the handler methods was called before notifications
        assertEquals(0, n.get());
    }

    @AfterClass
    public static void afterAllTests() {
        interceptor.stop();
    }

    /* Used to wait handler notification by the interceptor internal thread */
    private static void interval() throws InterruptedException {
        Thread.sleep(100);
    }

    @Test
    public void testNotifyClientConnected() throws Exception {
        interceptor.notifyClientConnected(new ConnectMessage());
        interval();
        assertEquals(40, n.get());
    }

    @Test
    public void testNotifyClientDisconnected() throws Exception {
        interceptor.notifyClientDisconnected("cli1234");
        interval();
        assertEquals(50, n.get());
    }

    @Test
    public void testNotifyTopicPublished() throws Exception {
        interceptor.notifyTopicPublished(new PublishMessage());
        interval();
        assertEquals(60, n.get());
    }

    @Test
    public void testNotifyTopicSubscribed() throws Exception {
        interceptor.notifyTopicSubscribed(new Subscription("cli1", "o2", AbstractMessage.QOSType.MOST_ONE, true));
        interval();
        assertEquals(70, n.get());
    }

    @Test
    public void testNotifyTopicUnsubscribed() throws Exception {
        interceptor.notifyTopicUnsubscribed("o2");
        interval();
        assertEquals(80, n.get());
    }
}
