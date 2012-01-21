package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

/**
 *
 * @author andrea
 */
public class ConnectEncoder implements MessageEncoder<ConnectMessage> {

    public void encode(IoSession session, ConnectMessage message, ProtocolEncoderOutput out) throws Exception {
        IoBuffer staticHeaderBuff = IoBuffer.allocate(12);
        staticHeaderBuff.put(Utils.encodeString("MQIsdp"));
        
        //version 
        staticHeaderBuff.put((byte)0x03);
        
        //connection flags and Strings
        byte connectionFlags = 0;
        if (message.isCleanSession()) {
            connectionFlags |= 0x02;
        }
        if (message.isWillFlag()) {
            connectionFlags |= 0x04;
        }
        connectionFlags |= ((message.getWillQos() & 0x03) << 3);
        if (message.isWillRetain()) {
            connectionFlags |= 0x020;
        }
        if (message.isPasswordFlag()) {
            connectionFlags |= 0x040;
        }
        if (message.isUserFlag()) {
            connectionFlags |= 0x080;
        }
        staticHeaderBuff.put(connectionFlags);

        //Keep alive timer
        Utils.writeWord(staticHeaderBuff, message.getKeepAlive());
        staticHeaderBuff.flip();

        //Variable part
        IoBuffer variableHeaderBuff = IoBuffer.allocate(12).setAutoExpand(true);
        if (message.getClientID() != null) {
            variableHeaderBuff.put(Utils.encodeString(message.getClientID()));
            if (message.isWillFlag()) {
                variableHeaderBuff.put(Utils.encodeString(message.getWillTopic()));
                variableHeaderBuff.put(Utils.encodeString(message.getWillMessage()));
            }
            if (message.isUserFlag() && message.getUsername() != null) {
                variableHeaderBuff.put(Utils.encodeString(message.getUsername()));
                if (message.isPasswordFlag() && message.getPassword() != null) {
                    variableHeaderBuff.put(Utils.encodeString(message.getPassword()));
                }
            }
        }
        variableHeaderBuff.flip();

        int variableHeaderSize = variableHeaderBuff.remaining();
        IoBuffer buff = IoBuffer.allocate(14 + variableHeaderSize);
        buff.put((byte) (AbstractMessage.CONNECT << 4));
        buff.put(Utils.encodeRemainingLength(12 + variableHeaderSize));
        buff.put(staticHeaderBuff).put(variableHeaderBuff).flip();

        out.write(buff);
    }
}
