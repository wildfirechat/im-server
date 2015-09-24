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
package org.eclipse.moquette.proto;

import org.eclipse.moquette.proto.messages.AbstractMessage;

/**
 * Common utils methodd used in codecs.
 * 
 * @author andrea
 */
public class Utils {
    
     public static final int MAX_LENGTH_LIMIT = 268435455;

//    /**
//     * Read 2 bytes from in buffer first MSB, and then LSB returning as int.
//     */
//    static int readWord(IoBuffer in) {
//        int msb = in.get() & 0x00FF; //remove sign extension due to casting
//        int lsb = in.get() & 0x00FF;
//        msb = (msb << 8) | lsb ;
//        return msb;
//    }
//    
//    /**
//     * Writes as 2 bytes the int value into buffer first MSB, and then LSB.
//     */
//    static void writeWord(IoBuffer out, int value) {
//        out.put((byte) ((value & 0xFF00) >> 8)); //msb
//        out.put((byte) (value & 0x00FF)); //lsb
//    }
//
//    /**
//     * Decode the variable remaining lenght as defined in MQTT v3.1 specification 
//     * (section 2.1).
//     * 
//     * @return the decoded length or -1 if needed more data to decode the length field.
//     */
//    static int decodeRemainingLenght(IoBuffer in) {
//        int multiplier = 1;
//        int value = 0;
//        byte digit;
//        do {
//            if (in.remaining() < 1) {
//                return -1;
//            }
//            digit = in.get();
//            value += (digit & 0x7F) * multiplier;
//            multiplier *= 128;
//        } while ((digit & 0x80) != 0);
//        return value;
//    }
    
    /**
     * Return the number of bytes to encode the gicen remaining length value
     */
    static int numBytesToEncode(int len) {
        if (0 <= len && len <= 127) return 1;
        if (128 <= len && len <= 16383) return 2;
        if (16384 <= len && len <= 2097151) return 3;
        if (2097152 <= len && len <= 268435455) return 4;
        throw new IllegalArgumentException("value shoul be in the range [0..268435455]");
    }
    
//    /**
//     * Encode the value in the format defined in specification as variable length
//     * array.
//     * 
//     * @throws IllegalArgumentException if the value is not in the specification bounds
//     *  [0..268435455].
//     */
//    static IoBuffer encodeRemainingLength(int value) throws IllegalAccessException {
//        if (value > MAX_LENGTH_LIMIT || value < 0) {
//            throw new IllegalAccessException("Value should in range 0.." + MAX_LENGTH_LIMIT + " found " + value);
//        }
//
//        IoBuffer encoded = IoBuffer.allocate(4);
//        byte digit;
//        do {
//            digit = (byte) (value % 128);
//            value = value / 128;
//            // if there are more digits to encode, set the top bit of this digit
//            if (value > 0) {
//                digit = (byte) (digit | 0x80);
//            }
//            encoded.put(digit);
//        } while (value > 0);
//        encoded.flip();
//        return encoded;
//    }
//    
//    static MessageDecoderResult checkDecodable(byte type, IoBuffer in) {
//        if (in.remaining() < 1) {
//            return MessageDecoderResult.NEED_DATA;
//        }
//        byte h1 = in.get();
//        byte messageType = (byte) ((h1 & 0x00F0) >> 4);
//        
//        int remainingLength = Utils.decodeRemainingLenght(in);
//        if (remainingLength == -1) {
//            return MessageDecoderResult.NEED_DATA;
//        }
//        
//        //check remaining length
//        if (in.remaining() < remainingLength) {
//            return MessageDecoderResult.NEED_DATA;
//        }
//        
//        return messageType == type ? MessageDecoderResult.OK : MessageDecoderResult.NOT_OK;
//    }
//    
//    /**
//     * Return the IoBuffer with string encoded as MSB, LSB and UTF-8 encoded
//     * string content.
//     */
//    static IoBuffer encodeString(String str) {
//        IoBuffer out = IoBuffer.allocate(2).setAutoExpand(true);
//        byte[] raw;
//        try {
//            raw = str.getBytes("UTF-8");
//            //NB every Java platform has got UTF-8 encoding by default, so this 
//            //exception are never raised.
//        } catch (UnsupportedEncodingException ex) {
//            LoggerFactory.getLogger(ConnectEncoder.class).error(null, ex);
//            return null;
//        }
//        Utils.writeWord(out, raw.length);
//        out.put(raw).flip();
//        return out;
//    }
//    
//    
//    /**
//     * Load a string from the given buffer, reading first the two bytes of len
//     * and then the UTF-8 bytes of the string.
//     * 
//     * @return the decoded string or null if NEED_DATA
//     */
//    static String decodeString(IoBuffer in) throws UnsupportedEncodingException {
//        if (in.remaining() < 2) {
//            return null;
//        }
//        int strLen = Utils.readWord(in);
//        if (in.remaining() < strLen) {
//            return null;
//        }
//        byte[] strRaw = new byte[strLen];
//        in.get(strRaw);
//
//        return new String(strRaw, "UTF-8");
//    }
    
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

    /**
     * Converts MQTT message type to a textual description.
     * */
    public static String msgType2String(int type) {
        switch (type) {
            case AbstractMessage.CONNECT: return "CONNECT";
            case AbstractMessage.CONNACK: return "CONNACK";
            case AbstractMessage.PUBLISH: return "PUBLISH";
            case AbstractMessage.PUBACK: return "PUBACK";
            case AbstractMessage.PUBREC: return "PUBREC";
            case AbstractMessage.PUBREL: return "PUBREL";
            case AbstractMessage.PUBCOMP: return "PUBCOMP";
            case AbstractMessage.SUBSCRIBE: return "SUBSCRIBE";
            case AbstractMessage.SUBACK: return "SUBACK";
            case AbstractMessage.UNSUBSCRIBE: return "UNSUBSCRIBE";
            case AbstractMessage.UNSUBACK: return "UNSUBACK";
            case AbstractMessage.PINGREQ: return "PINGREQ";
            case AbstractMessage.PINGRESP: return "PINGRESP";
            case AbstractMessage.DISCONNECT: return "DISCONNECT";
            default: throw  new RuntimeException("Can't decode message type " + type);
        }
    }
}
