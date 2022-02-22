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

package io.moquette.spi.impl;

import static cn.wildfirechat.common.ErrorCode.*;
import static io.moquette.spi.impl.Utils.readBytesAndRewind;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.server.ThreadPoolExecutorWrapper;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.action.ClassUtil;
import cn.wildfirechat.pojos.OutputCheckUserOnline;
import io.moquette.persistence.ServerAPIHelper;
import io.moquette.imhandler.Handler;
import io.moquette.imhandler.IMHandler;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.Server;
import io.moquette.spi.impl.security.AES;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;
import win.liyufan.im.RateLimiter;
import win.liyufan.im.Utility;
import win.liyufan.im.extended.mqttmessage.ModifiedMqttPubAckMessage;

public class Qos1PublishHandler extends QosPublishHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Qos1PublishHandler.class);

    private final IMessagesStore m_messagesStore;
    private final ConnectionDescriptorStore connectionDescriptors;
    private final MessagesPublisher publisher;
    private final ISessionsStore m_sessionStore;
    private final ThreadPoolExecutorWrapper m_imBusinessExecutor;

    private HashMap<String, IMHandler> m_imHandlers = new HashMap<>();

    public Qos1PublishHandler(IAuthorizator authorizator, IMessagesStore messagesStore, BrokerInterceptor interceptor,
                              ConnectionDescriptorStore connectionDescriptors, MessagesPublisher messagesPublisher,
                              ISessionsStore sessionStore, ThreadPoolExecutorWrapper executorService, Server server) {
        super(authorizator);
        this.m_messagesStore = messagesStore;
        this.connectionDescriptors = connectionDescriptors;
        this.publisher = messagesPublisher;
        this.m_sessionStore = sessionStore;
        this.m_imBusinessExecutor = executorService;
        IMHandler.init(m_messagesStore, m_sessionStore, publisher, m_imBusinessExecutor, server);
        registerAllAction();
    }

    private void registerAllAction() {
        try {
            for (Class cls:ClassUtil.getAllAssignedClass(IMHandler.class)) {
                Handler annotation = (Handler)cls.getAnnotation(Handler.class);
                if(annotation != null) {
                    IMHandler handler = (IMHandler) com.xiaoleilu.hutool.util.ClassUtil.newInstance(cls);
                    m_imHandlers.put(annotation.value(), handler);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    public void onApiMessage(String fromUser, String clientId, byte[] message, int requestId, String from, String request, ProtoConstants.RequestSourceType requestSourceType) {
        if (request.equals(ServerAPIHelper.CHECK_USER_ONLINE_REQUEST)) {
            checkUserOnlineHandler(message, ackPayload -> ServerAPIHelper.sendResponse(ERROR_CODE_SUCCESS.getCode(), ackPayload, from, requestId));
        } else {
            imHandler(clientId, fromUser, request, message, (errorCode, ackPayload) -> {
                if (requestId > 0) {
                    byte[] response = new byte[ackPayload.readableBytes()];
                    ackPayload.readBytes(response);
                    ReferenceCountUtil.release(ackPayload);
                    ServerAPIHelper.sendResponse(errorCode.getCode(), response, from, requestId);
                }
            }, requestSourceType);
        }
    }

    void checkUserOnlineHandler(byte[] payloadContent, RouteCallback callback) {
        m_imBusinessExecutor.execute(() -> {
            String userId = new String(payloadContent);

            int status;
            Collection<MemorySessionStore.Session> useSessions = m_sessionStore.sessionForUser(userId);
            OutputCheckUserOnline out = new OutputCheckUserOnline();

            for (MemorySessionStore.Session session : useSessions) {
                if (session.getDeleted() > 0) {
                    continue;
                }

                ConnectionDescriptor descriptor = connectionDescriptors.getConnection(session.getClientID());
                if (descriptor == null) {
                    status = 1;
                } else {
                    status = 0;
                }

                out.addSession(userId, session.getClientID(), session.getPlatform(), status, session.getLastActiveTime());
            }

            callback.onRouteHandled(new Gson().toJson(out).getBytes());
        });
    }

	void imHandler(String clientID, String fromUser, String topic, byte[] payloadContent, IMCallback callback, ProtoConstants.RequestSourceType requestSourceType) {
        LOG.info("imHandler fromUser={}, topic={}", fromUser, topic);
        IMCallback wrapper = (errorcode, ackPayload) -> {
            ackPayload.resetReaderIndex();
            byte code = ackPayload.readByte();
            if(ackPayload.readableBytes() > 0) {
                byte[] data = new byte[ackPayload.readableBytes()];
                ackPayload.getBytes(1, data);
                try {
                    //clientID 为空的是server api请求。客户端不允许clientID为空
                    if (!StringUtil.isNullOrEmpty(clientID)) {
                        //在route时，使用系统根密钥。当route成功后，用户都使用用户密钥
                        if (topic.equals(IMTopic.GetTokenTopic)) {
                            data = AES.AESEncrypt(data, "");
                        } else {
                            MemorySessionStore.Session session = m_sessionStore.getSession(clientID);
                            if (session != null && session.getUsername().equals(fromUser)) {
                                if (data.length > 7*1024 && session.getMqttVersion().protocolLevel() >= MqttVersion.Wildfire_1.protocolLevel()) {
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    GZIPOutputStream gzip = null;
                                    try {
                                        gzip = new GZIPOutputStream(out);
                                        gzip.write(data);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Utility.printExecption(LOG, e);
                                    } finally {
                                        if(gzip != null) {
                                            gzip.close();
                                        }
                                    }
                                    data = out.toByteArray();
                                    code = (byte)ErrorCode.ERROR_CODE_SUCCESS_GZIPED.code;
                                }

                                data = AES.AESEncrypt(data, session.getSecret());
                            }
                        }
                    }
                    ackPayload.clear();
                    ackPayload.resetWriterIndex();
                    ackPayload.writeByte(code);
                    ackPayload.writeBytes(data);
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                }
            }
            ackPayload.resetReaderIndex();
            try {
                callback.onIMHandled(errorcode, ackPayload);
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
        };

        IMHandler handler = m_imHandlers.get(topic);
        if (handler != null) {
            handler.doHandler(clientID, fromUser, topic, payloadContent, wrapper, requestSourceType);
        } else {
            LOG.error("imHandler unknown topic={}", topic);
            ByteBuf ackPayload = Unpooled.buffer();
            ackPayload.ensureWritable(1).writeByte(ERROR_CODE_NOT_IMPLEMENT.getCode());
            try {
                wrapper.onIMHandled(ERROR_CODE_NOT_IMPLEMENT, ackPayload);
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
        }
    }


    public interface IMCallback {
        void onIMHandled(ErrorCode errorCode, ByteBuf ackPayload);
    }

    interface RouteCallback {
        void onRouteHandled(byte[] ackPayload);
    }

    void receivedPublishQos1(Channel channel, MqttPublishMessage msg) {
        // verify if topic can be write
        final Topic topic = new Topic(msg.variableHeader().topicName());
        String clientID = NettyUtils.clientID(channel);
        String username = NettyUtils.userName(channel);
        if (!m_authorizator.canWrite(topic, username, clientID)) {
            LOG.error("MQTT client is not authorized to publish on topic. CId={}, topic={}", clientID, topic);
            return;
        }

        final int messageID = msg.variableHeader().packetId();
        String imtopic = topic.getTopic();
        ByteBuf payload = msg.payload();
        byte[] payloadContent = readBytesAndRewind(payload);
        if(payloadContent.length == 0) {
            ByteBuf ackPayload = Unpooled.buffer();
            ackPayload.ensureWritable(1).writeByte(ERROR_CODE_INVALID_DATA.getCode());
            sendPubAck(clientID, messageID, ackPayload, ERROR_CODE_INVALID_DATA);
            return;
        }
        
        MemorySessionStore.Session session = m_sessionStore.getSession(clientID);
        payloadContent = AES.AESDecrypt(payloadContent, session.getSecret(), true);
        imHandler(clientID, username, imtopic, payloadContent, (errorCode, ackPayload) -> sendPubAck(clientID, messageID, ackPayload, errorCode), ProtoConstants.RequestSourceType.Request_From_User);
    }

    private void sendPubAck(String clientId, int messageID, ByteBuf payload, ErrorCode errorCode) {
        LOG.trace("sendPubAck invoked");
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, AT_MOST_ONCE, false, 0);
        ModifiedMqttPubAckMessage pubAckMessage = new ModifiedMqttPubAckMessage(fixedHeader, from(messageID), payload);

        try {
            if (connectionDescriptors == null) {
                throw new RuntimeException("Internal bad error, found connectionDescriptors to null while it should " +
                    "be initialized, somewhere it's overwritten!!");
            }
            LOG.debug("clientIDs are {}", connectionDescriptors);
            if (!connectionDescriptors.isConnected(clientId)) {
                throw new RuntimeException(String.format("Can't find a ConnectionDescriptor for client %s in cache %s",
                    clientId, connectionDescriptors));
            }
            connectionDescriptors.sendMessage(pubAckMessage, messageID, clientId, errorCode);
        } catch (Throwable t) {
            LOG.error(null, t);
        }
    }

}
