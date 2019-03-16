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
import org.fusesource.hawtbuf.Buffer;

/**
* <p>
* </p>
*
* @author <a href="http://hiramchirino.com">Hiram Chirino</a>
*/
public class MQTTFrame extends MessageSupport.HeaderBase {

    private static final Buffer[] NO_BUFFERS = new Buffer[0];

    public Buffer[] buffers = NO_BUFFERS;

    public MQTTFrame() {
    }
    public MQTTFrame( Buffer buffer) {
        this(new Buffer[]{buffer});
    }
    public MQTTFrame( Buffer[] buffers) {
        this.buffers = buffers;
    }

    public Buffer[] buffers() {
        return buffers;
    }
    public MQTTFrame buffers(Buffer...buffers) {
        this.buffers = buffers;
        return this;
    }

    public MQTTFrame buffer(Buffer buffer) {
        this.buffers = new Buffer[]{buffer};
        return this;
    }

    @Override
    public byte header() {
        return super.header();
    }

    @Override
    public MQTTFrame header(byte header) {
        return (MQTTFrame)super.header(header);
    }

    @Override
    public byte messageType() {
        return super.messageType();
    }

    @Override
    public MQTTFrame commandType(int type) {
        return (MQTTFrame)super.commandType(type);
    }

    @Override
    public boolean dup() {
        return super.dup();
    }

    @Override
    public MQTTFrame dup(boolean dup) {
        return (MQTTFrame) super.dup(dup);
    }

    @Override
    public QoS qos() {
        return super.qos();
    }

    @Override
    public MQTTFrame qos(QoS qos) {
        return (MQTTFrame) super.qos(qos);
    }

    @Override
    public boolean retain() {
        return super.retain();
    }

    @Override
    public MQTTFrame retain(boolean retain) {
        return (MQTTFrame) super.retain(retain);
    }

    @Override
    public String toString() {
        String type = "unknown";
        switch(messageType()) {
            case CONNECT.TYPE:
                type = "CONNECT";
                break;
            case CONNACK.TYPE:
                type = "CONNACK";
                break;
            case DISCONNECT.TYPE:
                type = "DISCONNECT";
                break;
            case PINGREQ.TYPE:
                type = "PINGREQ";
                break;
            case PINGRESP.TYPE:
                type = "PINGRESP";
                break;
            case SUBSCRIBE.TYPE:
                type = "SUBSCRIBE";
                break;
            case UNSUBSCRIBE.TYPE:
                type = "UNSUBSCRIBE";
                break;
            case UNSUBACK.TYPE:
                type = "UNSUBACK";
                break;
            case PUBLISH.TYPE:
                type = "PUBLISH";
                break;
            case SUBACK.TYPE:
                type = "SUBACK";
                break;
            case PUBACK.TYPE:
                type = "PUBACK";
                break;
            case PUBREC.TYPE:
                type = "PUBREC";
                break;
            case PUBREL.TYPE:
                type = "PUBREL";
                break;
            case PUBCOMP.TYPE:
                type = "PUBCOMP";
                break;
            default:
        }

        return "MQTTFrame { type: "+type+", qos: "+qos()+", dup:"+dup()+" }";
    }
}
