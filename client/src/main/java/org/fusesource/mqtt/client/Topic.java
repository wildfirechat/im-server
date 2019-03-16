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

import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Topic {

    private final UTF8Buffer name;
    private final QoS qos;

    public Topic(String name, QoS qos) {
        this(new UTF8Buffer(name), qos);
    }

    public Topic(UTF8Buffer name, QoS qos) {
        this.name = name;
        this.qos = qos;
    }

    public UTF8Buffer name() {
        return name;
    }

    public QoS qos() {
        return qos;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o.getClass() != this.getClass()) return false;
        if (this == o) return true;

        Topic topic = (Topic) o;

        if (name != null ? !name.equals(topic.name) : topic.name != null) return false;
        if (qos != topic.qos) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (qos != null ? qos.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "{ name=" + name +
                ", qos=" + qos +
                " }";
    }
}
