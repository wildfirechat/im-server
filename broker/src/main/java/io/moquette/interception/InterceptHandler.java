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
package io.moquette.interception;

import io.moquette.proto.messages.AbstractMessage;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;

/**
 * This interface is used to inject code for intercepting broker events.
 * <p>
 * The events can act only as observers.
 * <p>
 * Almost every method receives a subclass of {@link AbstractMessage}, except
 * <code>onDisconnect</code> that receives the client id string and
 * <code>onSubscribe</code> and <code>onUnsubscribe</code> that receive a
 * {@link Subscription} object.
 *
 * @author Wagner Macedo
 */
public interface InterceptHandler {

    void onConnect(InterceptConnectMessage msg);

    void onDisconnect(InterceptDisconnectMessage msg);

    void onPublish(InterceptPublishMessage msg);

    void onSubscribe(InterceptSubscribeMessage msg);

    void onUnsubscribe(InterceptUnsubscribeMessage msg);
}
