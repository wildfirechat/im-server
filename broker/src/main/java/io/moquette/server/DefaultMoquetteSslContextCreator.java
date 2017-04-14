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

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.spi.security.ISslContextCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Moquette server implementation to load SSL certificate from local filesystem path configured in
 * config file.
 */
class DefaultMoquetteSslContextCreator implements ISslContextCreator {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMoquetteSslContextCreator.class);

    private IConfig props;

    DefaultMoquetteSslContextCreator(IConfig props) {
        this.props = props;
    }

    @Override
    public SSLContext initSSLContext() {
        LOG.info("Checking SSL configuration properties...");
        final String jksPath = props.getProperty(BrokerConstants.JKS_PATH_PROPERTY_NAME);
        LOG.info("Initializing SSL context. KeystorePath = {}.", jksPath);
        if (jksPath == null || jksPath.isEmpty()) {
            // key_store_password or key_manager_password are empty
            LOG.warn("The keystore path is null or empty. The SSL context won't be initialized.");
            return null;
        }

        // if we have the port also the jks then keyStorePassword and keyManagerPassword
        // has to be defined
        final String keyStorePassword = props.getProperty(BrokerConstants.KEY_STORE_PASSWORD_PROPERTY_NAME);
        final String keyManagerPassword = props.getProperty(BrokerConstants.KEY_MANAGER_PASSWORD_PROPERTY_NAME);
        if (keyStorePassword == null || keyStorePassword.isEmpty()) {
            // key_store_password or key_manager_password are empty
            LOG.warn("The keystore password is null or empty. The SSL context won't be initialized.");
            return null;
        }
        if (keyManagerPassword == null || keyManagerPassword.isEmpty()) {
            // key_manager_password or key_manager_password are empty
            LOG.warn("The key manager password is null or empty. The SSL context won't be initialized.");
            return null;
        }

        // if client authentification is enabled a trustmanager needs to be
        // added to the ServerContext
        String sNeedsClientAuth = props.getProperty(BrokerConstants.NEED_CLIENT_AUTH, "false");
        boolean needsClientAuth = Boolean.valueOf(sNeedsClientAuth);

        try {
            LOG.info("Loading keystore. KeystorePath = {}.", jksPath);
            InputStream jksInputStream = jksDatastore(jksPath);
            SSLContext serverContext = SSLContext.getInstance("TLS");
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(jksInputStream, keyStorePassword.toCharArray());
            LOG.info("Initializing key manager...");
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyManagerPassword.toCharArray());
            TrustManager[] trustManagers = null;
            if (needsClientAuth) {
                LOG.warn(
                        "Client authentication is enabled. "
                        + "The keystore will be used as a truststore. KeystorePath = {}.",
                        jksPath);
                // use keystore as truststore, as server needs to trust certificates signed by the
                // server certificates
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                trustManagers = tmf.getTrustManagers();
            }
            // init sslContext
            LOG.info("Initializing SSL context...");
            serverContext.init(kmf.getKeyManagers(), trustManagers, null);
            LOG.info("The SSL context has been initialized successfully.");

            return serverContext;
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | KeyStoreException
                | KeyManagementException | IOException ex) {
            LOG.error(
                    "Unable to initialize SSL context. Cause = {}, errorMessage = {}.",
                    ex.getCause(),
                    ex.getMessage());
            return null;
        }
    }

    private InputStream jksDatastore(String jksPath) throws FileNotFoundException {
        URL jksUrl = getClass().getClassLoader().getResource(jksPath);
        if (jksUrl != null) {
            LOG.info("Starting with jks at {}, jks normal {}", jksUrl.toExternalForm(), jksUrl);
            return getClass().getClassLoader().getResourceAsStream(jksPath);
        }
        LOG.warn("No keystore has been found in the bundled resources. Scanning filesystem...");
        File jksFile = new File(jksPath);
        if (jksFile.exists()) {
            LOG.info("Loading external keystore. Url = {}.", jksFile.getAbsolutePath());
            return new FileInputStream(jksFile);
        }
        LOG.warn("The keystore file does not exist. Url = {}.", jksFile.getAbsolutePath());
        return null;
    }
}
