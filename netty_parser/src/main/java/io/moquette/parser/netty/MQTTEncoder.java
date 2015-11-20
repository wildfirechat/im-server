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
package io.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.HashMap;
import java.util.Map;
import io.moquette.proto.messages.AbstractMessage;

/**
 *
 * @author andrea
 */
public class MQTTEncoder extends MessageToByteEncoder<AbstractMessage> {
    
    private Map<Byte, DemuxEncoder> m_encoderMap = new HashMap<Byte, DemuxEncoder>();
    
    public MQTTEncoder() {
       m_encoderMap.put(AbstractMessage.CONNECT, new ConnectEncoder());
       m_encoderMap.put(AbstractMessage.CONNACK, new ConnAckEncoder());
       m_encoderMap.put(AbstractMessage.PUBLISH, new PublishEncoder());
       m_encoderMap.put(AbstractMessage.PUBACK, new PubAckEncoder());
       m_encoderMap.put(AbstractMessage.SUBSCRIBE, new SubscribeEncoder());
       m_encoderMap.put(AbstractMessage.SUBACK, new SubAckEncoder());
       m_encoderMap.put(AbstractMessage.UNSUBSCRIBE, new UnsubscribeEncoder());
       m_encoderMap.put(AbstractMessage.DISCONNECT, new DisconnectEncoder());
       m_encoderMap.put(AbstractMessage.PINGREQ, new PingReqEncoder());
       m_encoderMap.put(AbstractMessage.PINGRESP, new PingRespEncoder());
       m_encoderMap.put(AbstractMessage.UNSUBACK, new UnsubAckEncoder());
       m_encoderMap.put(AbstractMessage.PUBCOMP, new PubCompEncoder());
       m_encoderMap.put(AbstractMessage.PUBREC, new PubRecEncoder());
       m_encoderMap.put(AbstractMessage.PUBREL, new PubRelEncoder());
    }
    
    @Override
    protected void encode(ChannelHandlerContext chc, AbstractMessage msg, ByteBuf bb) throws Exception {
        DemuxEncoder encoder = m_encoderMap.get(msg.getMessageType());
        if (encoder == null) {
            throw new CorruptedFrameException("Can't find any suitable decoder for message type: " + msg.getMessageType());
        }
        encoder.encode(chc, msg, bb);
    }
}
