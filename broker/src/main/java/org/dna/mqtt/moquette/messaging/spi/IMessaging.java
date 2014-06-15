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
package org.dna.mqtt.moquette.messaging.spi;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.server.ServerChannel;

/**
 * Interface to the underling messaging system used to publish, subscribe.
 * 
 * It's the abstraction of the messaging stuff attached in after the front protocol
 * parsing stuff.
 * 
 * @author andrea
 */
public interface IMessaging {

    void stop();

    void disconnect(ServerChannel session);
    
    void lostConnection(String clientID);

    void handleProtocolMessage(ServerChannel session, AbstractMessage msg);
}
