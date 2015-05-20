/*
 * Copyright (c) 2012-2014 The original author or authors
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
package org.eclipse.moquette.spi.impl.security;

import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;

import static java.util.Arrays.asList;
import static org.eclipse.moquette.spi.impl.security.Authorization.Permission.READ;
import static org.eclipse.moquette.spi.impl.security.Authorization.Permission.WRITE;
import static org.eclipse.moquette.spi.impl.security.AuthorizationsCollector.*;
import static org.junit.Assert.*;

/**
 * @author andrea
 */
public class AuthorizationsCollectorTest {

    private static final Authorization RW_ANEMOMETER = new Authorization("/weather/italy/anemometer");
    private static final Authorization R_ANEMOMETER = new Authorization("/weather/italy/anemometer", Authorization.Permission.READ);
    private static final Authorization W_ANEMOMETER = new Authorization("/weather/italy/anemometer", Authorization.Permission.WRITE);

    private AuthorizationsCollector authorizator;

    @Before
    public void setUp() {
        authorizator = new AuthorizationsCollector();
    }

    @Test
    public void testParseAuthLineValid() throws ParseException {
        Authorization authorization = parseAuthLine("topic /weather/italy/anemometer");

        //Verify
        assertEquals(RW_ANEMOMETER, authorization);
    }

    @Test
    public void testParseAuthLineValid_read() throws ParseException {
        Authorization authorization = parseAuthLine("topic read /weather/italy/anemometer");

        //Verify
        assertEquals(R_ANEMOMETER, authorization);
    }

    @Test
    public void testParseAuthLineValid_write() throws ParseException {
        Authorization authorization = parseAuthLine("topic write /weather/italy/anemometer");

        //Verify
        assertEquals(W_ANEMOMETER, authorization);
    }

    @Test
    public void testParseAuthLineValid_readwrite() throws ParseException {
        Authorization authorization = parseAuthLine("topic readwrite /weather/italy/anemometer");

        //Verify
        assertEquals(RW_ANEMOMETER, authorization);
    }


    @Test
    public void testParseAuthLineValid_topic_with_space() throws ParseException {
        Authorization expected = new Authorization("/weather/eastern italy/anemometer");
        Authorization authorization = parseAuthLine("topic readwrite /weather/eastern italy/anemometer");

        //Verify
        assertEquals(expected, authorization);
    }

    @Test(expected = ParseException.class)
    public void testParseAuthLineValid_invalid() throws ParseException {
        parseAuthLine("topic faker /weather/italy/anemometer");
    }

    @Test
    public void testCanWriteSimpleTopic() {
        authorizator.parse("topic write /sensors");

        //verify
        assertTrue(authorizator.canWrite("/sensors"));
    }

    @Test
    public void testCanReadSimpleTopic() {
        authorizator.parse("topic read /sensors");

        //verify
        assertTrue(authorizator.canRead("/sensors"));
    }

    @Test
    public void testCanReadWriteMixedSimpleTopic() {
        authorizator.parse("topic write /sensors");
        authorizator.parse("topic read /sensors/anemometer");

        //verify
        assertTrue(authorizator.canWrite("/sensors"));
        assertFalse(authorizator.canRead("/sensors"));
    }

    @Test
    public void testCanWriteMultiMatherTopic() {
        authorizator.parse("topic write /sensors/#");

        //verify
        assertTrue(authorizator.canWrite("/sensors/anemometer/wind"));
    }

    @Test
    public void testCanWriteSingleMatherTopic() {
        authorizator.parse("topic write /sensors/+");

        //verify
        assertTrue(authorizator.canWrite("/sensors/anemometer"));
    }
}