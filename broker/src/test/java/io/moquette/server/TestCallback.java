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
package io.moquette.server;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 *
 * @author andrea
 */
class TestCallback implements MqttCallback {

        private MqttMessage m_message;
        private String m_topic;
        private CountDownLatch m_latch = new CountDownLatch(1);

        public MqttMessage getMessage(boolean checkElapsed) {
            try {
                boolean elapsed = !m_latch.await(1, TimeUnit.SECONDS);
                if (elapsed && checkElapsed) {
                    throw new IllegalStateException("Elapsed the timeout to get the result");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return m_message;
        }

        public String getTopic() {
            try {
                m_latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return m_topic;
        }

        void reinit() {
            m_latch = new CountDownLatch(1);
            m_message = null;
            m_topic = null;
        }

        public void connectionLost(Throwable cause) {
        }

        public void messageArrived(String topic, MqttMessage message) throws Exception {
            m_message = message;
            m_topic = topic;
            m_latch.countDown();
        }

        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }
