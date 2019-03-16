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
package org.fusesource.mqtt.client;

import org.fusesource.mqtt.codec.MQTTFrame;

/**
 * A subclass of this can be configured on an MQTT connection to
 * get more insight into what it's doing.
 */
public class Tracer {

    /**
     * Override to log/capture debug level messages
     * @param message
     * @param args
     */
    public void debug(String message, Object...args) {
    }

    /**
     * Called when a MQTTFrame sent to the remote peer.
     * @param frame
     */
    public void onSend(MQTTFrame frame) {
    }

    /**
     * Called when a MQTTFrame is received from the remote peer.
     * @param frame
     */
    public void onReceive(MQTTFrame frame) {
    }

}
