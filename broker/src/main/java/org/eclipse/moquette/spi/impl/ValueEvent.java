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

import com.lmax.disruptor.EventFactory;
import org.eclipse.moquette.spi.impl.events.MessagingEvent;

/**
 * Carrier value object for the RingBuffer.
 *
 * @author andrea
 */
public final class ValueEvent {

    private MessagingEvent m_event;

    public MessagingEvent getEvent() {
        return m_event;
    }

    public void setEvent(MessagingEvent event) {
        m_event = event;
    }
    
    public final static EventFactory<ValueEvent> EVENT_FACTORY = new EventFactory<ValueEvent>() {

        public ValueEvent newInstance() {
            return new ValueEvent();
        }
    };
}
