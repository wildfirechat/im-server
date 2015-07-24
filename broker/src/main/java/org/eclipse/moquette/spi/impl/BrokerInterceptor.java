/*
 * Copyright (c) 2012-2014 The original author or authors
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

/**
 * An interceptor that execute the interception tasks asynchronously.
 *
 * @author Wagner Macedo
 */
final class BrokerInterceptor implements Interceptor {
    private final InterceptHandler handler;

    BrokerInterceptor(InterceptHandler handler) {
        this.handler = handler;
    }

    @Override
    public void notifyClientConnected(final ConnectMessage msg) {
        handler.onConnect(msg.readOnlyClone());
    }

    @Override
    public void notifyClientDisconnected(final String clientID) {
        handler.onDisconnect(clientID);
    }

    @Override
    public void notifyTopicPublished(final PublishMessage msg) {
        handler.onPublish(msg.readOnlyClone());
    }

    @Override
    public void notifyTopicSubscribed(final Subscription sub) {
        handler.onSubscribe(sub.clone());
    }

    @Override
    public void notifyTopicUnsubscribed(final Subscription sub) {
        handler.onUnsubscribe(sub.clone());
    }
}
