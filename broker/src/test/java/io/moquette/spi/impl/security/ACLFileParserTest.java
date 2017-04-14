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

import org.junit.Test;
import io.moquette.spi.impl.subscriptions.Topic;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import static org.junit.Assert.*;

public class ACLFileParserTest {

    @Test
    public void testParseEmpty() throws ParseException {
        Reader conf = new StringReader("  ");
        AuthorizationsCollector authorizations = ACLFileParser.parse(conf);

        // Verify
        assertTrue(authorizations.isEmpty());
    }

    @Test
    public void testParseValidComment() throws ParseException {
        Reader conf = new StringReader("#simple comment");
        AuthorizationsCollector authorizations = ACLFileParser.parse(conf);

        // Verify
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
        AuthorizationsCollector authorizations = ACLFileParser.parse(conf);

        // Verify
        assertTrue(authorizations.canRead(new Topic("/weather/italy/anemometer"), "", ""));
        assertTrue(authorizations.canWrite(new Topic("/weather/italy/anemometer"), "", ""));
    }

}
