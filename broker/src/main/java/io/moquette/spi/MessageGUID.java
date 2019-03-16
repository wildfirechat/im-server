/*
 * Copyright (c) 2012-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.moquette.spi;

import java.io.Serializable;

/**
 * Value object for GUIDs of messages.
 */
public class MessageGUID implements Serializable {

    private static final long serialVersionUID = 4315161987111542406L;
    private final String guid;

    public MessageGUID(String guid) {
        this.guid = guid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        MessageGUID that = (MessageGUID) o;

        return guid != null ? guid.equals(that.guid) : that.guid == null;
    }

    @Override
    public int hashCode() {
        return guid != null ? guid.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MessageGUID{" + "guid='" + guid + '\'' + '}';
    }
}
