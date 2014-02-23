package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;

/**
 *
 * @author andrea
 */
public class ConnectEncoder extends DemuxEncoder<ConnectMessage> {

    @Override
    protected void encode(ChannelHandlerContext chc, ConnectMessage message, ByteBuf out) {
        ByteBuf staticHeaderBuff = chc.alloc().buffer(12);
        ByteBuf buff = chc.alloc().buffer();
        ByteBuf variableHeaderBuff = chc.alloc().buffer(12);
        try {
            staticHeaderBuff.writeBytes(Utils.encodeString("MQIsdp"));

            //version 
            staticHeaderBuff.writeByte(0x03);

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
            staticHeaderBuff.writeByte(connectionFlags);

            //Keep alive timer
            staticHeaderBuff.writeShort(message.getKeepAlive());

            //Variable part
            if (message.getClientID() != null) {
                variableHeaderBuff.writeBytes(Utils.encodeString(message.getClientID()));
                if (message.isWillFlag()) {
                    variableHeaderBuff.writeBytes(Utils.encodeString(message.getWillTopic()));
                    variableHeaderBuff.writeBytes(Utils.encodeString(message.getWillMessage()));
                }
                if (message.isUserFlag() && message.getUsername() != null) {
                    variableHeaderBuff.writeBytes(Utils.encodeString(message.getUsername()));
                    if (message.isPasswordFlag() && message.getPassword() != null) {
                        variableHeaderBuff.writeBytes(Utils.encodeString(message.getPassword()));
                    }
                }
            }

            int variableHeaderSize = variableHeaderBuff.readableBytes();
            buff.writeByte(AbstractMessage.CONNECT << 4);
            buff.writeBytes(Utils.encodeRemainingLength(12 + variableHeaderSize));
            buff.writeBytes(staticHeaderBuff).writeBytes(variableHeaderBuff);

            out.writeBytes(buff);
        } finally {
            staticHeaderBuff.release();
            buff.release();
            variableHeaderBuff.release();
        }
    }
    
}
