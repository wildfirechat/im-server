/*
 * Copyright (c) 2012-2017 The original author or authors
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
package io.moquette.server.netty.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.librato.metrics.reporter.Librato;
import io.moquette.server.config.IConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

import java.util.concurrent.TimeUnit;

import static io.moquette.BrokerConstants.METRICS_LIBRATO_EMAIL_PROPERTY_NAME;
import static io.moquette.BrokerConstants.METRICS_LIBRATO_TOKEN_PROPERTY_NAME;
import static io.moquette.BrokerConstants.METRICS_LIBRATO_SOURCE_PROPERTY_NAME;
import static io.netty.channel.ChannelHandler.Sharable;

/**
 * Pipeline handler use to track some MQTT metrics.
 */
@Sharable
public final class DropWizardMetricsHandler extends ChannelInboundHandlerAdapter {
    private MetricRegistry metrics = new MetricRegistry();
    private Meter publishesMetrics = metrics.meter("publish.requests");

    public void init(IConfig props) {
        this.metrics = new MetricRegistry();
        this.publishesMetrics = metrics.meter("publish.requests");
//        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
//            .convertRatesTo(TimeUnit.SECONDS)
//            .convertDurationsTo(TimeUnit.MILLISECONDS)
//            .build();
//        reporter.start(1, TimeUnit.MINUTES);
        final String email = props.getProperty(METRICS_LIBRATO_EMAIL_PROPERTY_NAME);
        final String token = props.getProperty(METRICS_LIBRATO_TOKEN_PROPERTY_NAME);
        final String source = props.getProperty(METRICS_LIBRATO_SOURCE_PROPERTY_NAME);

        Librato.reporter(this.metrics, email, token)
            .setSource(source)
            .start(10, TimeUnit.SECONDS);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        MqttMessage msg = (MqttMessage) message;
        MqttMessageType messageType = msg.fixedHeader().messageType();
        switch (messageType) {
            case PUBLISH:
                publishesMetrics.mark();
                break;
            default:
                break;
        }
        ctx.fireChannelRead(message);
    }

}
