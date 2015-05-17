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

import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.List;
import static org.eclipse.moquette.spi.impl.security.Authorization.*;

import static org.junit.Assert.*;

/**
 * @author andrea
 * */
public class ACLFileParserTest {

    private static final Authorization RW_ANEMOMETER = new Authorization("/weather/italy/anemometer");
    private static final Authorization R_ANEMOMETER = new Authorization("/weather/italy/anemometer", Permission.READ);
    private static final Authorization W_ANEMOMETER = new Authorization("/weather/italy/anemometer", Permission.WRITE);

    @Test
    public void testParseEmpty() throws ParseException {
        Reader conf = new StringReader("  ");
        List<Authorization> authorizations = ACLFileParser.parse(conf);

        //Verify
        assertTrue(authorizations.isEmpty());
    }

    @Test
    public void testParseValidComment() throws ParseException {
        Reader conf = new StringReader("#simple comment");
        List<Authorization> authorizations = ACLFileParser.parse(conf);

        //Verify
        assertTrue(authorizations.isEmpty());
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidComment() throws ParseException {
        Reader conf = new StringReader(" #simple comment");
        ACLFileParser.parse(conf);
    }

    @Test
    public void testParseSingleLineACL() throws ParseException {
        Reader conf = new StringReader("topic /weather/italy/anemometer");
        List<Authorization> authorizations = ACLFileParser.parse(conf);

        //Verify
        assertEquals(1, authorizations.size());
        Authorization anemosAuth = authorizations.iterator().next();
        assertEquals(RW_ANEMOMETER, anemosAuth);
    }

    @Test
    public void testParseAuthLineValid() throws ParseException {
        Authorization authorization = ACLFileParser.parseAuthLine("topic /weather/italy/anemometer");

        //Verify
        assertEquals(RW_ANEMOMETER, authorization);
    }

    @Test
    public void testParseAuthLineValid_read() throws ParseException {
        Authorization authorization = ACLFileParser.parseAuthLine("topic read /weather/italy/anemometer");

        //Verify
        assertEquals(R_ANEMOMETER, authorization);
    }

    @Test
    public void testParseAuthLineValid_write() throws ParseException {
        Authorization authorization = ACLFileParser.parseAuthLine("topic write /weather/italy/anemometer");

        //Verify
        assertEquals(W_ANEMOMETER, authorization);
    }

    @Test
    public void testParseAuthLineValid_readwrite() throws ParseException {
        Authorization authorization = ACLFileParser.parseAuthLine("topic readwrite /weather/italy/anemometer");

        //Verify
        assertEquals(RW_ANEMOMETER, authorization);
    }


    @Test
    public void testParseAuthLineValid_topic_with_space() throws ParseException {
        Authorization expected = new Authorization("/weather/eastern italy/anemometer");
        Authorization authorization = ACLFileParser.parseAuthLine("topic readwrite /weather/eastern italy/anemometer");

        //Verify
        assertEquals(expected, authorization);
    }

    @Test(expected = ParseException.class)
    public void testParseAuthLineValid_invalid() throws ParseException {
        ACLFileParser.parseAuthLine("topic faker /weather/italy/anemometer");
    }
}