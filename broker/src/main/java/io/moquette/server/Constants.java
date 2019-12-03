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

package io.moquette.server;

/**
 * Server constants keeper
 */
public final class Constants {

    public static final String ATTR_CLIENTID = "ClientID";
    public static final String CLEAN_SESSION = "cleanSession";
    public static final String KEEP_ALIVE = "keepAlive";


    public static int MAX_MESSAGE_QUEUE = 1024; // number of messages
    public static int MAX_CHATROOM_MESSAGE_QUEUE = 256; // number of chatroom messages

    private Constants() {
    }
}
