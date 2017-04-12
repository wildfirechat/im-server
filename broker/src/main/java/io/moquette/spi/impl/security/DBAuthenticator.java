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

package io.moquette.spi.impl.security;

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.spi.security.IAuthenticator;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

/**
 * Load user credentials from a SQL database. sql driver must be provided at runtime
 */
public class DBAuthenticator implements IAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(DBAuthenticator.class);

    private final MessageDigest messageDigest;
    private final PreparedStatement preparedStatement;

    public DBAuthenticator(IConfig conf) {
        this(
                conf.getProperty(BrokerConstants.DB_AUTHENTICATOR_DRIVER, ""),
                conf.getProperty(BrokerConstants.DB_AUTHENTICATOR_URL, ""),
                conf.getProperty(BrokerConstants.DB_AUTHENTICATOR_QUERY, ""),
                conf.getProperty(BrokerConstants.DB_AUTHENTICATOR_DIGEST, ""));
    }

    /**
     * provide authenticator from SQL database
     *
     * @param driver
     *            : jdbc driver class like : "org.postgresql.Driver"
     * @param jdbcUrl
     *            : jdbc url like : "jdbc:postgresql://host:port/dbname"
     * @param sqlQuery
     *            : sql query like : "SELECT PASSWORD FROM USER WHERE LOGIN=?"
     * @param digestMethod
     *            : password encoding algorithm : "MD5", "SHA-1", "SHA-256"
     */
    public DBAuthenticator(String driver, String jdbcUrl, String sqlQuery, String digestMethod) {

        try {
            Class.forName(driver);
            final Connection connection = DriverManager.getConnection(jdbcUrl);
            this.messageDigest = MessageDigest.getInstance(digestMethod);
            this.preparedStatement = connection.prepareStatement(sqlQuery);
        } catch (ClassNotFoundException cnfe) {
            LOG.error(String.format("Can't find driver %s", driver), cnfe);
            throw new RuntimeException(cnfe);
        } catch (SQLException sqle) {
            LOG.error(String.format("Can't connect to %s", jdbcUrl), sqle);
            throw new RuntimeException(sqle);
        } catch (NoSuchAlgorithmException nsaex) {
            LOG.error(String.format("Can't find %s for password encoding", digestMethod), nsaex);
            throw new RuntimeException(nsaex);
        }
    }

    @Override
    public synchronized boolean checkValid(String clientId, String username, byte[] password) {
        // Check Username / Password in DB using sqlQuery
        if (username == null || password == null) {
            LOG.info("username or password was null");
            return false;
        }
        ResultSet r = null;
        try {
            this.preparedStatement.setString(1, username);
            r = this.preparedStatement.executeQuery();
            if (r.next()) {
                final String foundPwq = r.getString(1);
                messageDigest.update(password);
                byte[] digest = messageDigest.digest();
                String encodedPasswd = new String(Hex.encodeHex(digest));
                return foundPwq.equals(encodedPasswd);
            }
            r.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        this.preparedStatement.close();
        this.preparedStatement.getConnection().close();
        super.finalize();
    }
}
