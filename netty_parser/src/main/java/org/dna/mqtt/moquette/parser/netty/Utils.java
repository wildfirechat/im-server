package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CorruptedFrameException;
import java.io.UnsupportedEncodingException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class Utils {
    
    public static final int MAX_LENGTH_LIMIT = 268435455;
    
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
        if (in.readableBytes() < 2) {
            return null;
        }
        //int strLen = Utils.readWord(in);
        int strLen = in.readShort();
        if (in.readableBytes() < strLen) {
            return null;
        }
        byte[] strRaw = new byte[strLen];
        in.readBytes(strRaw);

        return new String(strRaw, "UTF-8");
    }
    
    
    /**
     * Return the IoBuffer with string encoded as MSB, LSB and UTF-8 encoded
     * string content.
     */
    static ByteBuf encodeString(String str) {
        ByteBuf out = Unpooled.buffer(2);
        byte[] raw;
        try {
            raw = str.getBytes("UTF-8");
            //NB every Java platform has got UTF-8 encoding by default, so this 
            //exception are never raised.
        } catch (UnsupportedEncodingException ex) {
            LoggerFactory.getLogger(Utils.class).error(null, ex);
            return null;
        }
        //Utils.writeWord(out, raw.length);
        out.writeShort(raw.length);
        out.writeBytes(raw);
        return out;
    }
}
