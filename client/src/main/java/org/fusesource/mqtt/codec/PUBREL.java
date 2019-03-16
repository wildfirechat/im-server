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

import org.fusesource.mqtt.client.QoS;
import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.IOException;
import java.net.ProtocolException;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class PUBREL extends MessageSupport.HeaderBase implements MessageSupport.Message, MessageSupport.Acked {

    public static final byte TYPE = 6;

    private short messageId;

    public byte messageType() {
        return TYPE;
    }
    
    public PUBREL() {
        qos(QoS.AT_LEAST_ONCE);
    }

    public PUBREL decode(MQTTFrame frame) throws ProtocolException {
        assert(frame.buffers.length == 1);
        header(frame.header());
        DataByteArrayInputStream is = new DataByteArrayInputStream(frame.buffers[0]);
        messageId = is.readShort();
        return this;
    }
    
    public MQTTFrame encode() {
        try {
            DataByteArrayOutputStream os = new DataByteArrayOutputStream(2);
            os.writeShort(messageId);

            MQTTFrame frame = new MQTTFrame();
            frame.header(header());
            frame.commandType(TYPE);
            return frame.buffer(os.toBuffer());
        } catch (IOException e) {
            throw new RuntimeException("The impossible happened");
        }
    }


    @Override
    public boolean dup() {
        return super.dup();
    }

    @Override
    public PUBREL dup(boolean dup) {
        return (PUBREL) super.dup(dup);
    }

    @Override
    public QoS qos() {
        return super.qos();
    }

    public short messageId() {
        return messageId;
    }

    public PUBREL messageId(short messageId) {
        this.messageId = messageId;
        return this;
    }

    @Override
    public String toString() {
        return "PUBREL{" +
                "dup=" + dup() +
                ", qos=" + qos() +
                ", messageId=" + messageId +
                '}';
    }
}
