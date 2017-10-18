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

import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.Server;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.IStore;
import io.moquette.spi.ISubscriptionsStore;
import io.moquette.spi.impl.security.*;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * It's main responsibility is bootstrap the ProtocolProcessor.
 */
public class ProtocolProcessorBootstrapper {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolProcessorBootstrapper.class);
    public static final String INMEMDB_STORE_CLASS = "io.moquette.persistence.MemoryStorageService";

    private ISessionsStore m_sessionsStore;

    private ISubscriptionsStore subscriptionsStore;

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
     *            the full list check of configurable properties check moquette.conf file.
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
            IAuthenticator authenticator, IAuthorizator authorizator, Server server) {
        IMessagesStore messagesStore;
        LOG.info("Initializing messages and sessions stores...");
        String storageClassName = props.getProperty(BrokerConstants.STORAGE_CLASS_NAME, INMEMDB_STORE_CLASS);
        if (storageClassName == null || storageClassName.isEmpty()) {
            LOG.error("storage_class property not defined");
            throw new IllegalArgumentException("Can't find a valid persistence layer");
        }
        final IStore store = instantiateConfiguredStore(storageClassName, props, server.getScheduler());
        if (store == null) {
            throw new IllegalArgumentException("Can't start the persistence layer");
        }
        store.initStore();
        messagesStore = store.messagesStore();
        m_sessionsStore = store.sessionsStore();
        SessionsRepository sessionsRepository = new SessionsRepository(this.m_sessionsStore, server.getScheduler());
        sessionsRepository.init();
        this.subscriptionsStore = m_sessionsStore.subscriptionStore();
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

        LOG.info("Initializing subscriptions store...");
        ISubscriptionsDirectory subscriptions = new CTrieSubscriptionDirectory();
        subscriptions.init(sessionsRepository);

        LOG.info("Configuring MQTT authenticator...");
        String authenticatorClassName = props.getProperty(BrokerConstants.AUTHENTICATOR_CLASS_NAME, "");

        if (authenticator == null && !authenticatorClassName.isEmpty()) {
            authenticator = loadClass(authenticatorClassName, IAuthenticator.class, IConfig.class, props);
        }

        IResourceLoader resourceLoader = props.getResourceLoader();
        if (authenticator == null) {
            String passwdPath = props.getProperty(BrokerConstants.PASSWORD_FILE_PROPERTY_NAME, "");
            if (passwdPath.isEmpty()) {
                authenticator = new AcceptAllAuthenticator();
            } else {
                authenticator = new ResourceAuthenticator(resourceLoader, passwdPath);
            }
            LOG.info("An {} authenticator instance will be used", authenticator.getClass().getName());
        }

        LOG.info("Configuring MQTT authorizator...");
        String authorizatorClassName = props.getProperty(BrokerConstants.AUTHORIZATOR_CLASS_NAME, "");
        if (authorizator == null && !authorizatorClassName.isEmpty()) {
            authorizator = loadClass(authorizatorClassName, IAuthorizator.class, IConfig.class, props);
        }

        if (authorizator == null) {
            String aclFilePath = props.getProperty(BrokerConstants.ACL_FILE_PROPERTY_NAME, "");
            if (aclFilePath != null && !aclFilePath.isEmpty()) {
                authorizator = new DenyAllAuthorizator();
                try {
                    LOG.info("Parsing ACL file. Path = {}", aclFilePath);
                    authorizator = ACLFileParser.parse(resourceLoader.loadResource(aclFilePath));
                } catch (ParseException pex) {
                    LOG.error("Unable to parse ACL file. path=" + aclFilePath, pex);
                }
            } else {
                authorizator = new PermitAllAuthorizator();
            }
            LOG.info("An {} authorizator instance will be used", authorizator.getClass().getName());
        }

        LOG.info("Initializing connection descriptor store...");
        connectionDescriptors = new ConnectionDescriptorStore(sessionsRepository);

        LOG.info("Initializing MQTT protocol processor...");
        boolean allowAnonymous = Boolean
                .parseBoolean(props.getProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, "true"));
        boolean allowZeroByteClientId = Boolean
                .parseBoolean(props.getProperty(BrokerConstants.ALLOW_ZERO_BYTE_CLIENT_ID_PROPERTY_NAME, "false"));
        m_processor.init(connectionDescriptors, subscriptions, messagesStore, m_sessionsStore, authenticator,
                allowAnonymous, allowZeroByteClientId, authorizator, interceptor,
            sessionsRepository);
        return m_processor;
    }

    private IStore instantiateConfiguredStore(String storageClassName, IConfig props,
                                              ScheduledExecutorService scheduledExecutor) {
        LOG.info("Loading storage class {}", storageClassName);
        Class<? extends IStore> storageClass;
        try {
            storageClass = this.getClass().getClassLoader()
                .loadClass(storageClassName)
                .asSubclass(IStore.class);
        } catch (ClassNotFoundException cnfex) {
            LOG.error("Cannot find storage class " + storageClassName + " in classpath", cnfex);
            return null;
        }

        final Constructor<? extends IStore> constructor;
        try {
            constructor = storageClass
                .getConstructor(IConfig.class, ScheduledExecutorService.class);
        } catch (NoSuchMethodException nsmex) {
            LOG.error("Cannot find constructor with required params IConfig, ScheduledExecutorService ", nsmex);
            return null;
        }
        try {
            return constructor.newInstance(props, scheduledExecutor);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            LOG.error("Cannot instantiate the " + storageClassName + " instance", ex);
        }

        return null;
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
                LOG.info("Invoking default constructor. ClassName={}, interfaceName={}", className, intrface.getName());
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

    public List<Subscription> getSubscriptions() {
        return this.subscriptionsStore.listAllSubscriptions();
    }

    public void shutdown() {
        if (storeShutdown != null)
            storeShutdown.run();
        if (m_processor != null)
            m_processor.shutdown();
    }

    public ConnectionDescriptorStore getConnectionDescriptors() {
        return connectionDescriptors;
    }
}
