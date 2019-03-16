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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.Server;
import io.moquette.server.config.IConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.IStore;
import io.moquette.spi.impl.security.PermitAllAuthorizator;
import io.moquette.spi.impl.security.TokenAuthenticator;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;

/**
 * It's main responsibility is bootstrap the ProtocolProcessor.
 */
public class ProtocolProcessorBootstrapper {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolProcessorBootstrapper.class);
    public static final String INMEMDB_STORE_CLASS = "io.moquette.persistence.MemoryStorageService";

    private ISessionsStore m_sessionsStore;

    private Runnable storeShutdown;

    private final ProtocolProcessor m_processor = new ProtocolProcessor();
    private ConnectionDescriptorStore connectionDescriptors;

    public ProtocolProcessorBootstrapper() {
    }

    /**
     * Initialize the processing part of the broker.
     *
     * @param props
     *            the properties carrier where some props like port end host could be loaded. For
     *            the full list check of configurable properties check wildfirechat.conf file.
     * @param embeddedObservers
     *            a list of callbacks to be notified of certain events inside the broker. Could be
     *            empty list of null.
     * @param authenticator
     *            an implementation of the authenticator to be used, if null load that specified in
     *            config and fallback on the default one (permit all).
     * @param authorizator
     *            an implementation of the authorizator to be used, if null load that specified in
     *            config and fallback on the default one (permit all).
     * @param server
     *            the server to init.
     * @return the processor created for the broker.
     */
    public ProtocolProcessor init(IConfig props, List<? extends InterceptHandler> embeddedObservers,
            IAuthenticator authenticator, IAuthorizator authorizator, Server server, IStore store) {
        IMessagesStore messagesStore;
        messagesStore = store.messagesStore();
        m_sessionsStore = store.sessionsStore();
        storeShutdown = new Runnable() {
            @Override
            public void run() {
                store.close();
            }
        };

        LOG.info("Configuring message interceptors...");

        List<InterceptHandler> observers = new ArrayList<>(embeddedObservers);
        String interceptorClassName = props.getProperty(BrokerConstants.INTERCEPT_HANDLER_PROPERTY_NAME);
        if (interceptorClassName != null && !interceptorClassName.isEmpty()) {
            InterceptHandler handler = loadClass(interceptorClassName, InterceptHandler.class, Server.class, server);
            if (handler != null) {
                observers.add(handler);
            }
        }
        BrokerInterceptor interceptor = new BrokerInterceptor(props, observers);

        LOG.info("Configuring MQTT authenticator...");
        authenticator = new TokenAuthenticator();
        

        LOG.info("Configuring MQTT authorizator...");
        String authorizatorClassName = props.getProperty(BrokerConstants.AUTHORIZATOR_CLASS_NAME, "");
        if (authorizator == null && !authorizatorClassName.isEmpty()) {
            authorizator = loadClass(authorizatorClassName, IAuthorizator.class, IConfig.class, props);
        }

        if (authorizator == null) {
            authorizator = new PermitAllAuthorizator();
            LOG.info("An {} authorizator instance will be used", authorizator.getClass().getName());
        }

        LOG.info("Initializing connection descriptor store...");
        connectionDescriptors = new ConnectionDescriptorStore(m_sessionsStore);

        LOG.info("Initializing MQTT protocol processor...");
        m_processor.init(connectionDescriptors, messagesStore, m_sessionsStore, authenticator, authorizator, interceptor, server);
        return m_processor;
    }

    @SuppressWarnings("unchecked")
    private <T, U> T loadClass(String className, Class<T> intrface, Class<U> constructorArgClass, U props) {
        T instance = null;
        try {
            // check if constructor with constructor arg class parameter
            // exists
            LOG.info("Invoking constructor with {} argument. ClassName={}, interfaceName={}",
                    constructorArgClass.getName(), className, intrface.getName());
            instance = this.getClass().getClassLoader()
                .loadClass(className)
                .asSubclass(intrface)
                .getConstructor(constructorArgClass)
                .newInstance(props);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            LOG.warn("Unable to invoke constructor with {} argument. ClassName={}, interfaceName={}, cause={}, errorMessage={}",
                    constructorArgClass.getName(), className, intrface.getName(), ex.getCause(), ex.getMessage());
            return null;
        } catch (NoSuchMethodException | InvocationTargetException e) {
            try {
                LOG.info("Invoking default constructor. ClassName={}, interfaceName={}",
                        className, intrface.getName());
                // fallback to default constructor
                instance = this.getClass().getClassLoader()
                    .loadClass(className)
                    .asSubclass(intrface)
                    .newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                LOG.error("Unable to invoke default constructor. ClassName={}, interfaceName={}, cause={}, errorMessage={}",
                        className, intrface.getName(), ex.getCause(), ex.getMessage());
                return null;
            }
        }

        return instance;
    }

    public ISessionsStore getSessionsStore() {
        return m_sessionsStore;
    }

    public void shutdown() {
        if (storeShutdown != null)
            storeShutdown.run();
        if (m_processor != null)
            m_processor.shutdown();
//        if (m_interceptor != null)
//            m_interceptor.stop();
    }

    public ConnectionDescriptorStore getConnectionDescriptors() {
        return connectionDescriptors;
    }
}
