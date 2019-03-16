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

import java.io.IOException;
import java.net.ProtocolException;

import org.fusesource.mqtt.client.QoS;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public final class MessageSupport {

    private MessageSupport() throws InstantiationException {
        throw new InstantiationException("This class is not for instantiation");
    }

    /**
     * All command objects implement this interface.
     */
    static public interface Message {
        public byte messageType();
        public Message decode(MQTTFrame frame) throws ProtocolException;
        public MQTTFrame encode();
    }


    /**
     * All command objects that can get acked implement this interface.
     */
    static public interface Acked extends Message {
        public boolean dup();
        public Acked dup(boolean dup);
        public QoS qos();
        public short messageId();
        public Acked messageId(short messageId);
    }

    static protected UTF8Buffer readUTF(DataByteArrayInputStream is) throws ProtocolException {
        int size = is.readUnsignedShort();
        Buffer buffer = is.readBuffer(size);
        if (buffer == null || buffer.length != size) {
            throw new ProtocolException("Invalid message encoding");
        }
        return buffer.utf8();
    }

    static protected void writeUTF(DataByteArrayOutputStream os, Buffer buffer) throws IOException {
        os.writeShort(buffer.length);
        os.write(buffer);
    }

    static abstract public class AckBase {

        private short messageId;
        public byte[] payload;

        abstract byte messageType();

        protected AckBase decode(MQTTFrame frame) throws ProtocolException {
            assert(frame.buffers.length == 1);
            DataByteArrayInputStream is = new DataByteArrayInputStream(frame.buffers[0]);
            messageId = is.readShort();
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
                os.writeShort(messageId);

                MQTTFrame frame = new MQTTFrame();
                frame.commandType(messageType());
                return frame.buffer(os.toBuffer());
            } catch (IOException e) {
                throw new RuntimeException("The impossible happened");
            }
        }

        public short messageId() {
            return messageId;
        }

        protected AckBase messageId(short messageId) {
            this.messageId = messageId;
            return this;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()+"{" +
                "messageId=" + messageId +
                '}';
        }
    }

    static abstract public class EmptyBase {
        abstract  byte messageType();

        protected EmptyBase decode(MQTTFrame frame) throws ProtocolException {
            return this;
        }
        public MQTTFrame encode() {
            return new MQTTFrame().commandType(messageType());
        }
    }


   /**
    * <p>
    * </p>
    *
    * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
    */
   static public class HeaderBase {

       protected byte header;

       protected byte header() {
           return header;
       }
       protected HeaderBase header(byte header) {
           this.header = header;
           return this;
       }

       protected byte messageType() {
           return (byte) ((header & 0xF0) >>> 4);
       }
       protected HeaderBase commandType(int type) {
           this.header &= 0x0F;
           this.header |= (type << 4) & 0xF0;
           return this;
       }

       protected QoS qos() {
           return QoS.values()[((header & 0x06) >>> 1)];
       }
       protected HeaderBase qos(QoS qos) {
           this.header &= 0xF9;
           this.header |= (qos.ordinal() << 1) & 0x06;
           return this;
       }

       protected boolean dup() {
           return (header & 0x08) > 0;
       }
       protected HeaderBase dup(boolean dup) {
           if(dup) {
               this.header |= 0x08;
           } else {
               this.header &= 0xF7;
           }
           return this;
       }

       protected boolean retain() {
           return (header & 0x01) > 0;
       }
       protected HeaderBase retain(boolean retain) {
           if(retain) {
               this.header |= 0x01;
           } else {
               this.header &= 0xFE;
           }
           return this;
       }

   }


}
