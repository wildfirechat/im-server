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

package io.moquette;

import java.io.File;

public final class BrokerConstants {

    public static final String INTERCEPT_HANDLER_PROPERTY_NAME = "intercept.handler";
    public static final String BROKER_INTERCEPTOR_THREAD_POOL_SIZE = "intercept.thread_pool.size";
    public static final String PERSISTENT_STORE_PROPERTY_NAME = "persistent_store";
    public static final String AUTOSAVE_INTERVAL_PROPERTY_NAME = "autosave_interval";
    public static final String PASSWORD_FILE_PROPERTY_NAME = "password_file";
    public static final String PORT_PROPERTY_NAME = "port";
    public static final String HOST_PROPERTY_NAME = "host";
    public static final String DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME = "moquette_store.mapdb";
    public static final String DEFAULT_PERSISTENT_PATH = System.getProperty("user.dir") + File.separator
            + DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME;
    public static final String WEB_SOCKET_PORT_PROPERTY_NAME = "websocket_port";
    public static final String WSS_PORT_PROPERTY_NAME = "secure_websocket_port";
    public static final String SSL_PORT_PROPERTY_NAME = "ssl_port";
    public static final String JKS_PATH_PROPERTY_NAME = "jks_path";
    public static final String KEY_STORE_PASSWORD_PROPERTY_NAME = "key_store_password";
    public static final String KEY_MANAGER_PASSWORD_PROPERTY_NAME = "key_manager_password";
    public static final String ALLOW_ANONYMOUS_PROPERTY_NAME = "allow_anonymous";
    public static final String ALLOW_ZERO_BYTE_CLIENT_ID_PROPERTY_NAME = "allow_zero_byte_client_id";
    public static final String ACL_FILE_PROPERTY_NAME = "acl_file";
    public static final String AUTHORIZATOR_CLASS_NAME = "authorizator_class";
    public static final String AUTHENTICATOR_CLASS_NAME = "authenticator_class";
    public static final String DB_AUTHENTICATOR_DRIVER = "authenticator.db.driver";
    public static final String DB_AUTHENTICATOR_URL = "authenticator.db.url";
    public static final String DB_AUTHENTICATOR_QUERY = "authenticator.db.query";
    public static final String DB_AUTHENTICATOR_DIGEST = "authenticator.db.digest";
    public static final int PORT = 1883;
    public static final int WEBSOCKET_PORT = 8080;
    public static final String DISABLED_PORT_BIND = "disabled";
    public static final String HOST = "0.0.0.0";
    public static final String NEED_CLIENT_AUTH = "need_client_auth";
    public static final String HAZELCAST_CONFIGURATION = "hazelcast.configuration";
    public static final String NETTY_SO_BACKLOG_PROPERTY_NAME = "netty.so_backlog";
    public static final String NETTY_SO_REUSEADDR_PROPERTY_NAME = "netty.so_reuseaddr";
    public static final String NETTY_TCP_NODELAY_PROPERTY_NAME = "netty.tcp_nodelay";
    public static final String NETTY_SO_KEEPALIVE_PROPERTY_NAME = "netty.so_keepalive";
    public static final String NETTY_CHANNEL_TIMEOUT_SECONDS_PROPERTY_NAME = "netty.channel_timeout.seconds";
    public static final String NETTY_EPOLL_PROPERTY_NAME = "netty.epoll";
    public static final String NETTY_MAX_BYTES_PROPERTY_NAME = "netty.mqtt.message_size";
    public static final int DEFAULT_NETTY_MAX_BYTES_IN_MESSAGE = 8092;
    public static final String METRICS_ENABLE_PROPERTY_NAME = "use_metrics";
    public static final String METRICS_LIBRATO_EMAIL_PROPERTY_NAME = "metrics.librato.email";
    public static final String METRICS_LIBRATO_TOKEN_PROPERTY_NAME = "metrics.librato.token";
    public static final String METRICS_LIBRATO_SOURCE_PROPERTY_NAME = "metrics.librato.source";

    public static final String BUGSNAG_ENABLE_PROPERTY_NAME = "use_bugsnag";
    public static final String BUGSNAG_TOKEN_PROPERTY_NAME = "bugsnag.token";

    public static final String STORAGE_CLASS_NAME = "storage_class";

    private BrokerConstants() {
    }
}
