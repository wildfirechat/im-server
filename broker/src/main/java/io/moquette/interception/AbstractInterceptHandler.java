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

import io.moquette.interception.messages.*;

/**
 * Basic abstract class usefull to avoid empty methods creation in subclasses.
 *
 * Created by andrea on 08/12/15.
 */
public abstract class AbstractInterceptHandler implements InterceptHandler {

    @Override
    public void onConnect(InterceptConnectMessage msg) {}

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {}

    @Override
    public void onPublish(InterceptPublishMessage msg) {}

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {}

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {}
}
