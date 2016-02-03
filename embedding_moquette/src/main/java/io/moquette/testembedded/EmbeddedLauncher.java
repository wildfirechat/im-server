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
package io.moquette.testembedded;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.*;
import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.Server;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.ClasspathConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static java.util.Arrays.asList;

    public class EmbeddedLauncher {
    static class PublisherListener extends AbstractInterceptHandler {

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            System.out.println("Received on topic: " + msg.getTopicName() + " content: " + new String(msg.getPayload().array()));
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        final IConfig classPathConfig = new ClasspathConfig();

        final Server mqttBroker = new Server();
        List<? extends InterceptHandler> userHandlers = asList(new PublisherListener());
        mqttBroker.startServer(classPathConfig, userHandlers);

        System.out.println("Broker started press [CTRL+C] to stop");
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Stopping broker");
                mqttBroker.stopServer();
                System.out.println("Broker stopped");
            }
        });

        Thread.sleep(20000);
        System.out.println("Before self publish");
        PublishMessage message = new PublishMessage();
        message.setTopicName("/exit");
        message.setRetainFlag(true);
//        message.setQos(AbstractMessage.QOSType.MOST_ONE);
//        message.setQos(AbstractMessage.QOSType.LEAST_ONE);
        message.setQos(AbstractMessage.QOSType.EXACTLY_ONCE);
        message.setPayload(ByteBuffer.wrap("Hello World!!".getBytes()));
        mqttBroker.internalPublish(message);
        System.out.println("After self publish");
    }
}