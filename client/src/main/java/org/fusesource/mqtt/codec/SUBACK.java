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

import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Arrays;
import static org.fusesource.mqtt.codec.MessageSupport.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class SUBACK implements Message {

    public static final byte[] NO_GRANTED_QOS = new byte[0];
    public static final byte TYPE = 9;

    private short messageId;
    private byte[] grantedQos = NO_GRANTED_QOS;

    public byte messageType() {
        return TYPE;
    }

    public SUBACK decode(MQTTFrame frame) throws ProtocolException {
        assert(frame.buffers.length == 1);
        DataByteArrayInputStream is = new DataByteArrayInputStream(frame.buffers[0]);
        messageId = is.readShort();
        grantedQos = is.readBuffer(is.available()).toByteArray();
        return this;
    }
    
    public MQTTFrame encode() {
        try {
            DataByteArrayOutputStream os = new DataByteArrayOutputStream(2+grantedQos.length);
            os.writeShort(messageId);
            os.write(grantedQos);

            MQTTFrame frame = new MQTTFrame();
            frame.commandType(TYPE);
            return frame.buffer(os.toBuffer());
        } catch (IOException e) {
            throw new RuntimeException("The impossible happened");
        }
    }

    public byte[] grantedQos() {
        return grantedQos;
    }

    public SUBACK grantedQos(byte[] grantedQos) {
        this.grantedQos = grantedQos;
        return this;
    }

    public short messageId() {
        return messageId;
    }

    public SUBACK messageId(short messageId) {
        this.messageId = messageId;
        return this;
    }

    @Override
    public String toString() {
        return "SUBACK{" +
                "grantedQos=" + Arrays.toString(grantedQos) +
                ", messageId=" +messageId +
                '}';
    }
}
