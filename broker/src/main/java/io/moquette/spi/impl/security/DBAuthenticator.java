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

import com.zaxxer.hikari.HikariDataSource;
import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.spi.security.IAuthenticator;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Load user credentials from a SQL database. sql driver must be provided at runtime
 */
public class DBAuthenticator implements IAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(DBAuthenticator.class);

    private final MessageDigest messageDigest;
    private HikariDataSource dataSource;
    private String sqlQuery;

    public DBAuthenticator(IConfig conf) {
        this(conf.getProperty(BrokerConstants.DB_AUTHENTICATOR_DRIVER, ""),
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
        this.sqlQuery = sqlQuery;
        this.dataSource = new HikariDataSource();
        this.dataSource.setJdbcUrl(jdbcUrl);

        try {
            this.messageDigest = MessageDigest.getInstance(digestMethod);
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

        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        Connection conn = null;
        try {
            conn = this.dataSource.getConnection();

            preparedStatement = conn.prepareStatement(this.sqlQuery);
            preparedStatement.setString(1, username);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                final String foundPwq = resultSet.getString(1);
                messageDigest.update(password);
                byte[] digest = messageDigest.digest();
                String encodedPasswd = new String(Hex.encodeHex(digest));
                return foundPwq.equals(encodedPasswd);
            }
        } catch (SQLException sqlex) {
            LOG.error("Error quering DB for username: {}", username, sqlex);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOG.error("Error releasing connection to the datasource", username, e);
            }
        }
        return false;
    }
}
