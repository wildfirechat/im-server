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
import org.eclipse.moquette.interception.Interceptor;
import org.eclipse.moquette.proto.messages.ConnectMessage;
import org.eclipse.moquette.proto.messages.PublishMessage;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An interceptor that execute the interception tasks asynchronously.
 *
 * @author Wagner Macedo
 */
final class BrokerInterceptor implements Interceptor {
    private final List<InterceptHandler> handlers;
    private final ExecutorService executor;

    BrokerInterceptor(List<InterceptHandler> handlers) {
        this.handlers = handlers;
        executor = Executors.newFixedThreadPool(1);
    }

    /**
     * Shutdown graciously the executor service
     */
    void stop() {
        executor.shutdown();
    }

    @Override
    public void notifyClientConnected(final ConnectMessage msg) {
        final ConnectMessage copied = new UnmodifiableConnectMessage(msg);
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onConnect(copied);
                }
            });
        }
    }

    @Override
    public void notifyClientDisconnected(final String clientID) {
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onDisconnect(clientID);
                }
            });
        }
    }

    @Override
    public void notifyTopicPublished(final PublishMessage msg) {
        final PublishMessage copied = new UnmodifiablePublishMessage(msg);
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onPublish(copied);
                }
            });
        }
    }

    @Override
    public void notifyTopicSubscribed(final Subscription sub) {
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onSubscribe(sub);
                }
            });
        }
    }

    @Override
    public void notifyTopicUnsubscribed(final String sub) {
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onUnsubscribe(sub);
                }
            });
        }
    }
}
