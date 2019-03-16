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

import java.net.ProtocolException;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class PINGRESP extends MessageSupport.EmptyBase implements MessageSupport.Message {

    public static final byte TYPE = 13;

    public byte messageType() {
        return TYPE;
    }

    @Override
    public PINGRESP decode(MQTTFrame frame) throws ProtocolException {
        return (PINGRESP) super.decode(frame);
    }

    @Override
    public String toString() {
        return "PINGRESP";
    }

}
