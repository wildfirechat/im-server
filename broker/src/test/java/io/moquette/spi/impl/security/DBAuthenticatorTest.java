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

import org.apache.commons.codec.binary.Hex;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DBAuthenticatorTest {

    private static final Logger LOG = LoggerFactory.getLogger(DBAuthenticatorTest.class);

    public static final String ORG_H2_DRIVER = "org.h2.Driver";
    public static final String JDBC_H2_MEM_TEST = "jdbc:h2:mem:test";
    public static final String SHA_256 = "SHA-256";
    private Connection connection;

    @Before
    public void setup() throws ClassNotFoundException, SQLException, NoSuchAlgorithmException {
        Class.forName(ORG_H2_DRIVER);
        this.connection = DriverManager.getConnection(JDBC_H2_MEM_TEST);
        Statement statement = this.connection.createStatement();
        try {
            statement.execute("DROP TABLE ACCOUNT");
        } catch (SQLException sqle) {
            LOG.info("Table not found, not dropping", sqle);
        }
        MessageDigest digest = MessageDigest.getInstance(SHA_256);
        String hash = new String(Hex.encodeHex(digest.digest("password".getBytes(StandardCharsets.UTF_8))));
        try {
            if (statement.execute("CREATE TABLE ACCOUNT ( LOGIN VARCHAR(64), PASSWORD VARCHAR(256))")) {
                throw new SQLException("can't create USER table");
            }
            if (statement.execute("INSERT INTO ACCOUNT ( LOGIN , PASSWORD ) VALUES ('dbuser', '" + hash + "')")) {
                throw new SQLException("can't insert in USER table");
            }
        } catch (SQLException sqle) {
            LOG.error("Table not created, not inserted", sqle);
            return;
        }
        LOG.info("Table User created");
        statement.close();
    }

    @Test
    public void Db_verifyValid() {
        final DBAuthenticator dbAuthenticator = new DBAuthenticator(
                ORG_H2_DRIVER,
                JDBC_H2_MEM_TEST,
                "SELECT PASSWORD FROM ACCOUNT WHERE LOGIN=?",
                SHA_256);
        assertTrue(dbAuthenticator.checkValid(null, "dbuser", "password".getBytes()));
    }

    @Test
    public void Db_verifyInvalidLogin() {
        final DBAuthenticator dbAuthenticator = new DBAuthenticator(
                ORG_H2_DRIVER,
                JDBC_H2_MEM_TEST,
                "SELECT PASSWORD FROM ACCOUNT WHERE LOGIN=?",
                SHA_256);
        assertFalse(dbAuthenticator.checkValid(null, "dbuser2", "password".getBytes()));
    }

    @Test
    public void Db_verifyInvalidPassword() {
        final DBAuthenticator dbAuthenticator = new DBAuthenticator(
                ORG_H2_DRIVER,
                JDBC_H2_MEM_TEST,
                "SELECT PASSWORD FROM ACCOUNT WHERE LOGIN=?",
                SHA_256);
        assertFalse(dbAuthenticator.checkValid(null, "dbuser", "wrongPassword".getBytes()));
    }

    @After
    public void teardown() {
        try {
            this.connection.close();
        } catch (SQLException e) {
            LOG.error("can't close connection", e);
        }
    }
}
