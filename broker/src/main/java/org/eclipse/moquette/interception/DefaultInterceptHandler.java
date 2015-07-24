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
package org.eclipse.moquette.interception;

import org.eclipse.moquette.proto.messages.*;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;

/**
 * Default empty implementation that can be used to create custom handlers
 * without defining every method.
 *
 * @author Wagner Macedo
 */
public class DefaultInterceptHandler implements InterceptHandler {
    @Override
    public void onConnect(ConnectMessage msg) {
    }

    @Override
    public void onDisconnect(String clientID) {
    }

    @Override
    public void onPublish(PublishMessage msg) {
    }

    @Override
    public void onSubscribe(Subscription sub) {
    }

    @Override
    public void onUnsubscribe(Subscription sub) {
    }
}
