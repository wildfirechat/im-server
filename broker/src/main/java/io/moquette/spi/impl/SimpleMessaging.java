/*
 * Copyright (c) 2012-2015 The original author or authors
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

import io.moquette.spi.IMessagesStore;
import io.moquette.commons.Constants;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.config.IConfig;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.security.*;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.persistence.MapDBPersistentStore;
import static io.moquette.commons.Constants.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Singleton class that orchestrate the execution of the protocol.
 *
 * It's main responsibility is instantiate the ProtocolProcessor.
 *
 * @author andrea
 */
public class SimpleMessaging {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleMessaging.class);

    private SubscriptionsStore subscriptions;

    private MapDBPersistentStore m_mapStorage;

    private BrokerInterceptor m_interceptor;

    private static SimpleMessaging INSTANCE;
    
    private final ProtocolProcessor m_processor = new ProtocolProcessor();

    private SimpleMessaging() {
    }

    public static SimpleMessaging getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SimpleMessaging();
        }
        return INSTANCE;
    }

    public ProtocolProcessor init(IConfig props) {
        subscriptions = new SubscriptionsStore();

        m_mapStorage = new MapDBPersistentStore(props);
        m_mapStorage.initStore();
        IMessagesStore messagesStore = m_mapStorage.messagesStore();
        ISessionsStore sessionsStore = m_mapStorage.sessionsStore(messagesStore);

        List<InterceptHandler> observers = new ArrayList<>();
        String interceptorClassName = props.getProperty("intercept.handler");
        if (interceptorClassName != null && !interceptorClassName.isEmpty()) {
            try {
                InterceptHandler handler = Class.forName(interceptorClassName).asSubclass(InterceptHandler.class).newInstance();
                observers.add(handler);
            } catch (Throwable ex) {
                LOG.error("Can't load the intercept handler {}", ex);
            }
        }
        m_interceptor = new BrokerInterceptor(observers);

        subscriptions.init(sessionsStore);

        String configPath = System.getProperty("moquette.path", null);
        String authenticatorClassName = props.getProperty(Constants.AUTHENTICATOR_CLASS_NAME, "");

        IAuthenticator authenticator = null;
        if (!authenticatorClassName.isEmpty()) {
            authenticator = (IAuthenticator)loadClass(authenticatorClassName, IAuthenticator.class);
            LOG.info("Loaded custom authenticator {}", authenticatorClassName);
        }

        if (authenticator == null) {
            String passwdPath = props.getProperty(PASSWORD_FILE_PROPERTY_NAME, "");
            if (passwdPath.isEmpty()) {
                authenticator = new AcceptAllAuthenticator();
            } else {
                authenticator = new FileAuthenticator(configPath, passwdPath);
            }
        }

        IAuthorizator authorizator = null;
        String authorizatorClassName = props.getProperty(Constants.AUTHORIZATOR_CLASS_NAME, "");
        if (!authorizatorClassName.isEmpty()) {
            authorizator = (IAuthorizator)loadClass(authorizatorClassName, IAuthorizator.class);
            LOG.info("Loaded custom authorizator {}", authorizatorClassName);
        }

        if (authorizator == null) {
            String aclFilePath = props.getProperty(ACL_FILE_PROPERTY_NAME, "");
            if (aclFilePath != null && !aclFilePath.isEmpty()) {
                authorizator = new DenyAllAuthorizator();
                File aclFile = new File(configPath, aclFilePath);
                try {
                    authorizator = ACLFileParser.parse(aclFile);
                } catch (ParseException pex) {
                    LOG.error(String.format("Format error in parsing acl file %s", aclFile), pex);
                }
                LOG.info("Using acl file defined at path {}", aclFilePath);
            } else {
                authorizator = new PermitAllAuthorizator();
                LOG.info("Starting without ACL definition");
            }

        }

        boolean allowAnonymous = Boolean.parseBoolean(props.getProperty(ALLOW_ANONYMOUS_PROPERTY_NAME, "true"));
        m_processor.init(subscriptions, messagesStore, sessionsStore, authenticator, allowAnonymous, authorizator, m_interceptor);
        return m_processor;
    }
    
    private Object loadClass(String className, Class<?> cls) {
        Object instance = null;
        try {
            Class<?> clazz = Class.forName(className);

            // check if method getInstance exists
            Method method = clazz.getMethod("getInstance", new Class[] {});
            try {
                instance = method.invoke(null, new Object[] {});
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException ex) {
                LOG.error(null, ex);
                throw new RuntimeException("Cannot call method "+ className +".getInstance", ex);
            }
        }
        catch (NoSuchMethodException nsmex) {
            try {
                instance = this.getClass().getClassLoader()
                        .loadClass(className)
                        .asSubclass(cls)
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                LOG.error(null, ex);
                throw new RuntimeException("Cannot load custom authenticator class " + className, ex);
            }
        } catch (ClassNotFoundException ex) {
            LOG.error(null, ex);
            throw new RuntimeException("Class " + className + " not found", ex);
        } catch (SecurityException ex) {
            LOG.error(null, ex);
            throw new RuntimeException("Cannot call method "+ className +".getInstance", ex);
        }

        return instance;
    }

    public void shutdown() {
        this.m_mapStorage.close();
    }
}
