package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

/**
 * Decoder for the CONNECT message
 * 
 * @author andrea
 */
public class ConnectDecoder extends MqttDecoder {

    public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
        return Utils.checkDecodable(AbstractMessage.CONNECT, in);
    }

    public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        //Common decoding part
        ConnectMessage message = new ConnectMessage();
        if (decodeCommonHeader(message, in) == NEED_DATA) {
            return NEED_DATA;
        }
        int remainingLength = message.getRemainingLength();
        int start = in.position();

        //Connect specific decoding part
        //ProtocolName 8 bytes
        if (in.remaining() < 12) {
            return NEED_DATA;
        }
        byte[] encProtoName = new byte[6];
        in.skip(2); //size, is 0x06
        in.get(encProtoName);
        String protoName = new String(encProtoName, "UTF-8");
        if (!"MQIsdp".equals(protoName)) {
            return NOT_OK;
        }
        message.setProtocolName(protoName);

        //ProtocolVersion 1 byte (value 0x03)
        message.setProcotolVersion(in.get());

        //Connection flag
        byte connFlags = in.get();
        boolean cleanSession = ((connFlags & 0x02) >> 1) == 1 ? true : false;
        boolean willFlag = ((connFlags & 0x04) >> 2) == 1 ? true : false;
        byte willQos = (byte) ((connFlags & 0x18) >> 3);
        if (willQos > 2) {
            return NOT_OK; //admitted values for willqos are 0, 1, 2
        }
        boolean willRetain = ((connFlags & 0x20) >> 5) == 1 ? true : false;
        boolean passwordFlag = ((connFlags & 0x40) >> 6) == 1 ? true : false;
        boolean userFlag = ((connFlags & 0x80) >> 7) == 1 ? true : false;
        //a password is true iff user is true.
        if (!userFlag && passwordFlag) {
            return NOT_OK;
        }
        message.setCleanSession(cleanSession);
        message.setWillFlag(willFlag);
        message.setWillQos(willQos);
        message.setWillRetain(willRetain);
        message.setPasswordFlag(passwordFlag);
        message.setUserFlag(userFlag);

        //Keep Alive timer 2 bytes
        int keepAlive = Utils.readWord(in);
        message.setKeepAlive(keepAlive);

        if (remainingLength == 12) {
            out.write(message);
            return OK;
        }

        //Decode the ClientID
        String clientID = Utils.decodeString(in);
        if (clientID == null) {
            return NEED_DATA;
        }
        message.setClientID(clientID);

        //Decode willTopic
        if (willFlag) {
            String willTopic = Utils.decodeString(in);
            if (willTopic == null) {
                return NEED_DATA;
            }
            message.setWillTopic(willTopic);
        }

        //Decode willMessage
        if (willFlag) {
            String willMessage = Utils.decodeString(in);
            if (willMessage == null) {
                return NEED_DATA;
            }
            message.setWillMessage(willMessage);
        }

        //Compatibility check wieth v3.0, remaining length has precedence over
        //the user and password flags
        int readed = in.position() - start;
        if (readed == remainingLength) {
            out.write(message);
            return OK;
        }

        //Decode username
        if (userFlag) {
            String userName = Utils.decodeString(in);
            if (userName == null) {
                return NEED_DATA;
            }
            message.setUsername(userName);
        }

        readed = in.position() - start;
        if (readed == remainingLength) {
            out.write(message);
            return OK;
        }

        //Decode password
        if (passwordFlag) {
            String password = Utils.decodeString(in);
            if (password == null) {
                return NEED_DATA;
            }
            message.setPassword(password);
        }

        out.write(message);
        return OK;
    }
}
