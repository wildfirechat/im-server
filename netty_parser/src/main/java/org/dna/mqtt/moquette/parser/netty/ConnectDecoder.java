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
package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;

/**
 *
 * @author andrea
 */
public class ConnectDecoder extends DemuxDecoder {

    @Override
    void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws UnsupportedEncodingException {
        in.resetReaderIndex();
        //Common decoding part
        ConnectMessage message = new ConnectMessage();
        if (!decodeCommonHeader(message, in)) {
            in.resetReaderIndex();
            return;
        }
        int remainingLength = message.getRemainingLength();
        int start = in.readerIndex();

        //Connect specific decoding part
        //ProtocolName 8 bytes
        if (in.readableBytes() < 12) {
            in.resetReaderIndex();
            return;
        }
        byte[] encProtoName = new byte[6];
        in.skipBytes(2); //size, is 0x06
        in.readBytes(encProtoName);
        String protoName = new String(encProtoName, "UTF-8");
        if (!"MQIsdp".equals(protoName)) {
            in.resetReaderIndex();
            throw new CorruptedFrameException("Invalid protoName: " + protoName);
        }
        message.setProtocolName(protoName);

        //ProtocolVersion 1 byte (value 0x03)
        message.setProcotolVersion(in.readByte());

        //Connection flag
        byte connFlags = in.readByte();
        boolean cleanSession = ((connFlags & 0x02) >> 1) == 1 ? true : false;
        boolean willFlag = ((connFlags & 0x04) >> 2) == 1 ? true : false;
        byte willQos = (byte) ((connFlags & 0x18) >> 3);
        if (willQos > 2) {
            in.resetReaderIndex();
            throw new CorruptedFrameException("Expected will QoS in range 0..2 but found: " + willQos);
        }
        boolean willRetain = ((connFlags & 0x20) >> 5) == 1 ? true : false;
        boolean passwordFlag = ((connFlags & 0x40) >> 6) == 1 ? true : false;
        boolean userFlag = ((connFlags & 0x80) >> 7) == 1 ? true : false;
        //a password is true iff user is true.
        if (!userFlag && passwordFlag) {
            in.resetReaderIndex();
            throw new CorruptedFrameException("Expected password flag to true if the user flag is true but was: " + passwordFlag);
        }
        message.setCleanSession(cleanSession);
        message.setWillFlag(willFlag);
        message.setWillQos(willQos);
        message.setWillRetain(willRetain);
        message.setPasswordFlag(passwordFlag);
        message.setUserFlag(userFlag);

        //Keep Alive timer 2 bytes
        //int keepAlive = Utils.readWord(in);
        int keepAlive = in.readUnsignedShort();
        message.setKeepAlive(keepAlive);

        if (remainingLength == 12) {
            out.add(message);
            return;
        }

        //Decode the ClientID
        String clientID = Utils.decodeString(in);
        if (clientID == null) {
            in.resetReaderIndex();
            return;
        }
        message.setClientID(clientID);

        //Decode willTopic
        if (willFlag) {
            String willTopic = Utils.decodeString(in);
            if (willTopic == null) {
                in.resetReaderIndex();
                return;
            }
            message.setWillTopic(willTopic);
        }

        //Decode willMessage
        if (willFlag) {
            String willMessage = Utils.decodeString(in);
            if (willMessage == null) {
                in.resetReaderIndex();
                return;
            }
            message.setWillMessage(willMessage);
        }

        //Compatibility check wieth v3.0, remaining length has precedence over
        //the user and password flags
        int readed = in.readerIndex() - start;
        if (readed == remainingLength) {
            out.add(message);
            return;
        }

        //Decode username
        if (userFlag) {
            String userName = Utils.decodeString(in);
            if (userName == null) {
                in.resetReaderIndex();
                return;
            }
            message.setUsername(userName);
        }

        readed = in.readerIndex() - start;
        if (readed == remainingLength) {
            out.add(message);
            return;
        }

        //Decode password
        if (passwordFlag) {
            String password = Utils.decodeString(in);
            if (password == null) {
                in.resetReaderIndex();
                return;
            }
            message.setPassword(password);
        }

        out.add(message);
    }
    
}
