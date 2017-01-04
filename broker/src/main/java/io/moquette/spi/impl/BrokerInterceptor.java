/*
 * Copyright (c) 2012-2017 The original author or authorsgetRockQuestions()
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
package io.moquette.spi.impl;

import io.moquette.interception.InterceptHandler;
import io.moquette.interception.Interceptor;
import io.moquette.interception.messages.*;
import io.moquette.parser.proto.messages.ConnectMessage;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.parser.proto.messages.PublishMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
        this.handlers = new CopyOnWriteArrayList<>(handlers);
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
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onConnect(new InterceptConnectMessage(msg));
                }
            });
        }
    }

    @Override
    public void notifyClientDisconnected(final String clientID, final String username ) {
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onDisconnect(new InterceptDisconnectMessage(clientID, username));
                }
            });
        }
    }

    @Override
    public void notifyClientConnectionLost(final String clientID, final String username) {
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onConnectionLost(new InterceptConnectionLostMessage(clientID, username));
                }
            });
        }
    }

    @Override
    public void notifyTopicPublished(final PublishMessage msg, final String clientID, final String username) {
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onPublish(new InterceptPublishMessage(msg, clientID, username));
                }
            });
        }
    }

    @Override
    public void notifyTopicSubscribed(final Subscription sub, final String username) {
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onSubscribe(new InterceptSubscribeMessage(sub, username));
                }
            });
        }
    }

    @Override
    public void notifyTopicUnsubscribed(final String topic, final String clientID, final String username) {
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onUnsubscribe(new InterceptUnsubscribeMessage(topic, clientID, username));
                }
            });
        }
    }

	@Override
	public void notifyMessageAcknowledged( final InterceptAcknowledgedMessage msg ) {
        for (final InterceptHandler handler : this.handlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onMessageAcknowledged(msg);
                }
            });
        }
    }

    @Override
    public boolean addInterceptHandler(InterceptHandler interceptHandler) {
        return this.handlers.add(interceptHandler);
    }

    @Override
    public boolean removeInterceptHandler(InterceptHandler interceptHandler) {
        return this.handlers.remove(interceptHandler);
    }
}
