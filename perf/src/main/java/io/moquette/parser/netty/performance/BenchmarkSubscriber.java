/*
 * Copyright (c) 2012-2018 The original author or authors
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

package io.moquette.parser.netty.performance;

import org.HdrHistogram.Histogram;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static java.nio.charset.StandardCharsets.UTF_8;

class BenchmarkSubscriber {

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkSubscriber.class);

    class SubscriberCallback implements MqttCallback {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            //used to catch the first message published
            if (!alreadyStarted) {
                startTime = System.currentTimeMillis();
                alreadyStarted = true;
            }

            //println "Received a publish on topic ${topic}"
            if (topic.startsWith("/exit")) {
                LOG.info("SUB: ok Exit!");
                LOG.debug("Before disconnect");
//                client.disconnect(1000) //wait max 1 sec
                //client.disconnectForcibly()
                //client.close()
                LOG.debug("After disconnect");
                long stopTime = System.currentTimeMillis();
                long spentTime = stopTime - startTime;
                LOG.info("SUB: {} received", topic);
                LOG.info("SUB: subscriber disconnected, received {} messages in {} ms", numReceived, spentTime);
                double msgPerSec = (numReceived / spentTime) * 1000;
                LOG.info("SUB: Speed: {} msg/sec", msgPerSec);
                LOG.info("SUB: Latency diagram [microsecs]");
                histogram.outputPercentileDistribution(System.out, 1000.0); //nanos/1000
                m_latch.countDown();
            } else {
                String payload = new String(message.getPayload(), UTF_8);
                //long sentTime = Long.parseLong(payload.split("-")[1]);
                long sentTime = Long.parseLong(payload.split("-")[1]);
                long delay = System.nanoTime() - sentTime;
                histogram.recordValue(delay);
//                print '+'
                numReceived++;
//        if ((numReceived % 10000) == 0) {
//            print '.'
//        }
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            LOG.error("Client Subscriber lost connection, caused by ", cause);
            try {
                client.disconnect();
            } catch (MqttException mex) {
                LOG.error(null, mex);
            }
            LOG.error("SUB: Something failed during message reception");
            m_latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    private int numReceived;
    private long startTime = System.currentTimeMillis();
    private CountDownLatch m_latch = new CountDownLatch(1);
    private boolean alreadyStarted;
    private Histogram histogram = new Histogram(5);

    private final IMqttAsyncClient client;
    private final String dialog_id;

    BenchmarkSubscriber(MqttAsyncClient client, String dialog_id) {
        this.client = client;
        this.dialog_id = dialog_id;
    }

    public void connect() throws MqttException {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(true);
        this.client.setCallback(new SubscriberCallback());
        this.client.connect(connectOptions, null, new IMqttActionListener() {

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                LOG.error("SUB: connect fail", exception);
                System.exit(2);
            }

            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                try {
                    client.subscribe("/topic" + dialog_id, 1, null, new IMqttActionListener() {

                        @Override
                        public void onFailure(IMqttToken asyncActionToken2, Throwable exception) {
                            try {
                                client.disconnect();
                            } catch (MqttException mex) {
                                LOG.error(null, mex);
                            }
                        }

                        @Override
                        public void onSuccess(IMqttToken asyncActionToken2) {
                            LOG.info("SUB: subscribed to /topic{} qos: 1", dialog_id);
                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }

                try {
                    client.subscribe("/exit" + dialog_id, 1, null, new IMqttActionListener() {

                        @Override
                        public void onFailure(IMqttToken asyncActionToken2, Throwable exception) {
                            try {
                                client.disconnect();
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                            m_latch.countDown();
                        }

                        @Override
                        public void onSuccess(IMqttToken asyncActionToken2) {
                            LOG.info("SUB: subscribed to /exit{} qos: 1", dialog_id);
                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void waitFinish() throws InterruptedException, MqttException {
        this.m_latch.await();
        this.client.disconnect(1000);
    }

}
