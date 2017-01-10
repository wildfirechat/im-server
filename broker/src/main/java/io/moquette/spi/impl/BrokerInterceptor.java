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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.Interceptor;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;
import io.moquette.parser.proto.messages.ConnectMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.config.IConfig;
import io.moquette.spi.impl.subscriptions.Subscription;

/**
 * An interceptor that execute the interception tasks asynchronously.
 *
 * @author Wagner Macedo
 */
final class BrokerInterceptor implements Interceptor {
    private final Map<Class<?>, List<InterceptHandler>> handlers;
    private final ExecutorService executor;

	private BrokerInterceptor(int poolSize, List<InterceptHandler> handlers) {
		this.handlers = new HashMap<>();
		for (Class<?> messageType : InterceptHandler.ALL_MESSAGE_TYPES) {
			this.handlers.put(messageType, new CopyOnWriteArrayList<InterceptHandler>());
		}
		for (InterceptHandler handler : handlers) {
			this.addInterceptHandler(handler);
		}
		executor = Executors.newFixedThreadPool(poolSize);
	}
	
	/**
	 * Configures a broker interceptor, with a thread pool of one thread.
	 * @param handlers
	 */
	public BrokerInterceptor(List<InterceptHandler> handlers) {
		this(1, handlers);
	}
	
	/**
	 * Configures a broker interceptor using the pool size specified in the
	 * IConfig argument.
	 */
	public BrokerInterceptor(IConfig props, List<InterceptHandler> handlers) {
		this(Integer.parseInt(props.getProperty(BrokerConstants.BROKER_INTERCEPTOR_THREAD_POOL_SIZE, "1")), handlers);
	}

    /**
     * Shutdown graciously the executor service
     */
	void stop() {
		executor.shutdown();
		try {
			executor.awaitTermination(10L, TimeUnit.SECONDS);
		} catch (InterruptedException e) {}
		if (!executor.isTerminated()) {
			executor.shutdownNow();
		}
	}

    @Override
    public void notifyClientConnected(final ConnectMessage msg) {
        for (final InterceptHandler handler : this.handlers.get(InterceptConnectMessage.class)) {
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
        for (final InterceptHandler handler : this.handlers.get(InterceptDisconnectMessage.class)) {
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
        for (final InterceptHandler handler : this.handlers.get(InterceptConnectionLostMessage.class)) {
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
        for (final InterceptHandler handler : this.handlers.get(InterceptPublishMessage.class)) {
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
        for (final InterceptHandler handler : this.handlers.get(InterceptSubscribeMessage.class)) {
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
        for (final InterceptHandler handler : this.handlers.get(InterceptUnsubscribeMessage.class)) {
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
        for (final InterceptHandler handler : this.handlers.get(InterceptAcknowledgedMessage.class)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handler.onMessageAcknowledged(msg);
                }
            });
        }
    }

	@Override
	public void addInterceptHandler(InterceptHandler interceptHandler) {
		for (Class<?> interceptMessageType : getInterceptedMessageTypes(interceptHandler)) {
			this.handlers.get(interceptMessageType).add(interceptHandler);
		}
	}

	@Override
    public void removeInterceptHandler(InterceptHandler interceptHandler) {
    	for (Class<?> interceptMessageType : getInterceptedMessageTypes(interceptHandler)) {
    		this.handlers.get(interceptMessageType).remove(interceptHandler);
    	}
    }

	private static Class<?>[] getInterceptedMessageTypes(InterceptHandler interceptHandler) {
		Class<?> interceptedMessageTypes[] = interceptHandler.getInterceptedMessageTypes();
		if (interceptedMessageTypes == null) {
			return InterceptHandler.ALL_MESSAGE_TYPES;
		}
		return interceptedMessageTypes;
	}
}
