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
package io.moquette.proto.messages;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author andrea
 */
public class SubscribeMessage extends MessageIDMessage {

    public static class Couple {

        private byte m_qos;
        private String m_topicFilter;

        public Couple(byte qos, String topic) {
            m_qos = qos;
            m_topicFilter = topic;
        }
        
        public byte getQos() {
            return m_qos;
        }

        public String getTopicFilter() {
            return m_topicFilter;
        }
    }
    private List<Couple> m_subscriptions = new ArrayList<Couple>();

    public SubscribeMessage() {
        //Subscribe has always QoS 1
        m_messageType = AbstractMessage.SUBSCRIBE;
        m_qos = AbstractMessage.QOSType.LEAST_ONE;
    }
    
    public List<Couple> subscriptions() {
        return m_subscriptions;
    }

    public void addSubscription(Couple subscription) {
        m_subscriptions.add(subscription);
    }
}
