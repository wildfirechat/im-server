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
package org.eclipse.moquette.spi.impl.events;

import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.server.ServerChannel;

/**
 *
 * @author andrea
 */
public class OutputMessagingEvent extends MessagingEvent {
    private ServerChannel m_channel;
    private AbstractMessage m_message;

    public OutputMessagingEvent(ServerChannel channel, AbstractMessage message) {
        m_channel = channel;
        m_message = message;
    }

    public ServerChannel getChannel() {
        return m_channel;
    }

    public AbstractMessage getMessage() {
        return m_message;
    }
    
    
}
