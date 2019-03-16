/**
 * Copyright (C) 2010-2012, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.mqtt.codec;

import org.fusesource.hawtbuf.*;

import java.io.IOException;
import java.net.ProtocolException;
import static org.fusesource.mqtt.codec.MessageSupport.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class CONNACK implements Message {

    public static final byte TYPE = 2;

    public byte[] payload;

    public static enum Code {
        CONNECTION_ACCEPTED,
        CONNECTION_REFUSED_UNACCEPTED_PROTOCOL_VERSION,
        CONNECTION_REFUSED_IDENTIFIER_REJECTED,
        CONNECTION_REFUSED_SERVER_UNAVAILABLE,
        CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD,
        CONNECTION_REFUSED_NOT_AUTHORIZED;
    }

    private Code code = Code.CONNECTION_ACCEPTED;


    
    public byte messageType() {
        return TYPE;
    }

    public CONNACK decode(MQTTFrame frame) throws ProtocolException {
        assert(frame.buffers.length == 1);
        DataByteArrayInputStream is = new DataByteArrayInputStream(frame.buffers[0]);
        is.skip(1);
        byte c = is.readByte();
        if( c >= Code.values().length ) {
            throw new ProtocolException("Invalid CONNACK encoding");
        }
        code = Code.values()[c];
        payload = new byte[is.available()];
        try {
            is.read(payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    public MQTTFrame encode() {
        try {
            DataByteArrayOutputStream os = new DataByteArrayOutputStream(2);
            os.writeByte(0);
            os.writeByte(code.ordinal());

            MQTTFrame frame = new MQTTFrame();
            frame.commandType(TYPE);
            return frame.buffer(os.toBuffer());
        } catch (IOException e) {
            throw new RuntimeException("The impossible happened");
        }
    }

    public Code code() {
        return code;
    }

    public CONNACK code(Code code) {
        this.code = code;
        return this;
    }

    @Override
    public String toString() {
        return "CONNACK{" +
                "code=" + code +
                '}';
    }
}
