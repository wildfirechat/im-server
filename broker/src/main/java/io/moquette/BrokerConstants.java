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
    public static final String SERVER_IP_PROPERTY_NAME = "server.ip";
    public static final String PORT_PROPERTY_NAME = "port";
    public static final String HOST_PROPERTY_NAME = "host";
    public static final String HTTP_SERVER_PORT = "http_port";
    public static final String HTTP_LOCAL_PORT = "local_port";

    public static final String HTTP_ADMIN_PORT = "http.admin.port";
    public static final String NODE_ID = "node_id";
    public static final String NODE_IDS = "node_ids";
    public static final String DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME = "moquette_store.mapdb";
    public static final String DEFAULT_PERSISTENT_PATH = System.getProperty("user.dir") + File.separator
            + DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME;
    public static final String WEB_SOCKET_PORT_PROPERTY_NAME = "websocket_port";
    public static final String WSS_PORT_PROPERTY_NAME = "secure_websocket_port";
    public static final String SSL_PORT_PROPERTY_NAME = "ssl_port";
    public static final String JKS_PATH_PROPERTY_NAME = "jks_path";
    public static final String KEY_STORE_PASSWORD_PROPERTY_NAME = "key_store_password";
    public static final String KEY_MANAGER_PASSWORD_PROPERTY_NAME = "key_manager_password";
    public static final String AUTHORIZATOR_CLASS_NAME = "authorizator_class";
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
    public static final String HAZELCAST_CLIENT_IP = "hazelcast.client.ip";
    public static final String HAZELCAST_CLIENT_PORT = "hazelcast.client.port";
    public static final String NETTY_SO_BACKLOG_PROPERTY_NAME = "netty.so_backlog";
    public static final String NETTY_SO_REUSEADDR_PROPERTY_NAME = "netty.so_reuseaddr";
    public static final String NETTY_TCP_NODELAY_PROPERTY_NAME = "netty.tcp_nodelay";
    public static final String NETTY_SO_KEEPALIVE_PROPERTY_NAME = "netty.so_keepalive";
    public static final String NETTY_CHANNEL_TIMEOUT_SECONDS_PROPERTY_NAME = "netty.channel_timeout.seconds";
    public static final String NETTY_EPOLL_PROPERTY_NAME = "netty.epoll";
    public static final String METRICS_ENABLE_PROPERTY_NAME = "use_metrics";
    public static final String METRICS_LIBRATO_EMAIL_PROPERTY_NAME = "metrics.librato.email";
    public static final String METRICS_LIBRATO_TOKEN_PROPERTY_NAME = "metrics.librato.token";
    public static final String METRICS_LIBRATO_SOURCE_PROPERTY_NAME = "metrics.librato.source";

    public static final String BUGSNAG_ENABLE_PROPERTY_NAME = "use_bugsnag";
    public static final String BUGSNAG_TOKEN_PROPERTY_NAME = "bugsnag.token";

    public static final String STORAGE_CLASS_NAME = "storage_class";

    public static final String QINIU_SERVER_URL= "qiniu.server_url";
	public static final String QINIU_ACCESS_KEY = "qiniu.access_key";
	public static final String QINIU_SECRET_KEY = "qiniu.secret_key";

    public static final String QINIU_BUCKET_GENERAL_NAME= "qiniu.bucket_general_name";
    public static final String QINIU_BUCKET_GENERAL_DOMAIN = "qiniu.bucket_general_domain";
    public static final String QINIU_BUCKET_IMAGE_NAME= "qiniu.bucket_image_name";
    public static final String QINIU_BUCKET_IMAGE_DOMAIN = "qiniu.bucket_image_domain";
    public static final String QINIU_BUCKET_VOICE_NAME= "qiniu.bucket_voice_name";
    public static final String QINIU_BUCKET_VOICE_DOMAIN = "qiniu.bucket_voice_domain";
    public static final String QINIU_BUCKET_VIDEO_NAME= "qiniu.bucket_video_name";
    public static final String QINIU_BUCKET_VIDEO_DOMAIN = "qiniu.bucket_video_domain";
    public static final String QINIU_BUCKET_FILE_NAME= "qiniu.bucket_file_name";
    public static final String QINIU_BUCKET_FILE_DOMAIN = "qiniu.bucket_file_domain";
    public static final String QINIU_BUCKET_PORTRAIT_NAME= "qiniu.bucket_portrait_name";
    public static final String QINIU_BUCKET_PORTRAIT_DOMAIN = "qiniu.bucket_portrait_domain";
    public static final String QINIU_BUCKET_FAVORITE_NAME= "qiniu.bucket_favorite_name";
    public static final String QINIU_BUCKET_FAVORITE_DOMAIN = "qiniu.bucket_favorite_domain";

    public static final String FILE_STORAGE_ROOT = "local.media.storage.root";
    public static final String USER_QINIU = "media.server.use_qiniu";

    public static final String PUSH_ANDROID_SERVER_ADDRESS = "push.android.server.address";
    public static final String PUSH_IOS_SERVER_ADDRESS = "push.ios.server.address";

    public static final String HZ_Cluster_Node_External_IP = "node_external_ip";
    public static final String HZ_Cluster_Node_External_Long_Port = "node_external_long_port";
    public static final String HZ_Cluster_Node_External_Short_Port = "node_external_short_port";
    public static final String HZ_Cluster_Node_ID = "node_id";
    public static final String HTTP_SERVER_SECRET_KEY = "http.admin.secret_key";
    public static final String CLIENT_PROTO_SECRET_KEY = "client.proto.secret_key";
    public static final String TOKEN_SECRET_KEY = "token.key ";

    public static final String EMBED_DB_PROPERTY_NAME = "embed.db";

    public static final String HZ_Cluster_Master_Node = "master_node";

    public static final String SENSITIVE_Filter_Type = "sensitive.filter.type";

    public static final String MESSAGE_Forward_Url = "message.forward.url";





    public static final int MESSAGE_CONTENT_TYPE_CREATE_GROUP = 104;
    public static final int MESSAGE_CONTENT_TYPE_ADD_GROUP_MEMBER = 105;
    public static final int MESSAGE_CONTENT_TYPE_KICKOF_GROUP_MEMBER = 106;
    public static final int MESSAGE_CONTENT_TYPE_QUIT_GROUP = 107;
    public static final int MESSAGE_CONTENT_TYPE_DISMISS_GROUP = 108;
    public static final int MESSAGE_CONTENT_TYPE_TRANSFER_GROUP_OWNER = 109;
    public static final int MESSAGE_CONTENT_TYPE_CHANGE_GROUP_NAME = 110;
    public static final int MESSAGE_CONTENT_TYPE_MODIFY_GROUP_ALIAS = 111;
    public static final int MESSAGE_CONTENT_TYPE_CHANGE_GROUP_PORTRAIT = 112;
    private BrokerConstants() {
    }
}
