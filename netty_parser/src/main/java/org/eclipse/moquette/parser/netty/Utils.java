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
package org.eclipse.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.Attribute;
import io.netty.util.AttributeMap;
import java.io.UnsupportedEncodingException;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class Utils {
    
    public static final int MAX_LENGTH_LIMIT = 268435455;
    
    public static final byte VERSION_3_1 = 3;
    public static final byte VERSION_3_1_1 = 4;
    
    static byte readMessageType(ByteBuf in) {
        byte h1 = in.readByte();
        byte messageType = (byte) ((h1 & 0x00F0) >> 4);
        return messageType;
    }
    
    static boolean checkHeaderAvailability(ByteBuf in) {
        if (in.readableBytes() < 1) {
            return false;
        }
        //byte h1 = in.get();
        //byte messageType = (byte) ((h1 & 0x00F0) >> 4);
        in.skipBytes(1); //skip the messageType byte
        
        int remainingLength = Utils.decodeRemainingLenght(in);
        if (remainingLength == -1) {
            return false;
        }
        
        //check remaining length
        if (in.readableBytes() < remainingLength) {
            return false;
        }
        
        //return messageType == type ? MessageDecoderResult.OK : MessageDecoderResult.NOT_OK;
        return true;
    }
    
    /**
     * Decode the variable remaining length as defined in MQTT v3.1 specification 
     * (section 2.1).
     * 
     * @return the decoded length or -1 if needed more data to decode the length field.
     */
    static int decodeRemainingLenght(ByteBuf in) {
        int multiplier = 1;
        int value = 0;
        byte digit;
        do {
            if (in.readableBytes() < 1) {
                return -1;
            }
            digit = in.readByte();
            value += (digit & 0x7F) * multiplier;
            multiplier *= 128;
        } while ((digit & 0x80) != 0);
        return value;
    }
    
    /**
     * Encode the value in the format defined in specification as variable length
     * array.
     * 
     * @throws IllegalArgumentException if the value is not in the specification bounds
     *  [0..268435455].
     */
    static ByteBuf encodeRemainingLength(int value) throws CorruptedFrameException {
        if (value > MAX_LENGTH_LIMIT || value < 0) {
            throw new CorruptedFrameException("Value should in range 0.." + MAX_LENGTH_LIMIT + " found " + value);
        }

        ByteBuf encoded = Unpooled.buffer(4);
        byte digit;
        do {
            digit = (byte) (value % 128);
            value = value / 128;
            // if there are more digits to encode, set the top bit of this digit
            if (value > 0) {
                digit = (byte) (digit | 0x80);
            }
            encoded.writeByte(digit);
        } while (value > 0);
        return encoded;
    }
    
    /**
     * Load a string from the given buffer, reading first the two bytes of len
     * and then the UTF-8 bytes of the string.
     * 
     * @return the decoded string or null if NEED_DATA
     */
    static String decodeString(ByteBuf in) throws UnsupportedEncodingException {
        return new String(readFixedLengthContent(in), "UTF-8");
    }

    /**
     * Read a byte array from the buffer, use two bytes as length information followed by length bytes.
     * */
    static byte[] readFixedLengthContent(ByteBuf in) throws UnsupportedEncodingException {
        if (in.readableBytes() < 2) {
            return null;
        }
        int strLen = in.readUnsignedShort();
        if (in.readableBytes() < strLen) {
            return null;
        }
        byte[] strRaw = new byte[strLen];
        in.readBytes(strRaw);

        return strRaw;
    }

    /**
     * Return the IoBuffer with string encoded as MSB, LSB and UTF-8 encoded
     * string content.
     */
    public static ByteBuf encodeString(String str) {
        byte[] raw;
        try {
            raw = str.getBytes("UTF-8");
            //NB every Java platform has got UTF-8 encoding by default, so this 
            //exception are never raised.
        } catch (UnsupportedEncodingException ex) {
            LoggerFactory.getLogger(Utils.class).error(null, ex);
            return null;
        }
        return encodeFixedLengthContent(raw);
    }

    /**
     * Return the IoBuffer with string encoded as MSB, LSB and bytes array content.
     */
    public static ByteBuf encodeFixedLengthContent(byte[] content) {
        ByteBuf out = Unpooled.buffer(2);
        out.writeShort(content.length);
        out.writeBytes(content);
        return out;
    }

    /**
     * Return the number of bytes to encode the given remaining length value
     */
    static int numBytesToEncode(int len) {
        if (0 <= len && len <= 127) return 1;
        if (128 <= len && len <= 16383) return 2;
        if (16384 <= len && len <= 2097151) return 3;
        if (2097152 <= len && len <= 268435455) return 4;
        throw new IllegalArgumentException("value shoul be in the range [0..268435455]");
    }
    
    static byte encodeFlags(AbstractMessage message) {
        byte flags = 0;
        if (message.isDupFlag()) {
            flags |= 0x08;
        }
        if (message.isRetainFlag()) {
            flags |= 0x01;
        }
        
        flags |= ((message.getQos().byteValue() & 0x03) << 1);
        return flags;
    }
    
    static boolean isMQTT3_1_1(AttributeMap attrsMap) {
        Attribute<Integer> versionAttr = attrsMap.attr(MQTTDecoder.PROTOCOL_VERSION);
        Integer protocolVersion = versionAttr.get();
        if (protocolVersion == null) {
            return true;
        } 
        return protocolVersion == VERSION_3_1_1;
    }
}
