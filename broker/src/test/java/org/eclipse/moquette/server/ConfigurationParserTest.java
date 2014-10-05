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
package org.eclipse.moquette.server;

import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Properties;
import org.eclipse.moquette.commons.Constants;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author andrea
 */
public class ConfigurationParserTest {

    ConfigurationParser m_parser;
    
    @Before
    public void setUp() {
        m_parser = new ConfigurationParser();
    }
    
    @Test
    public void checkDefaultOptions() {
        Properties props = m_parser.getProperties();
        
        verifyDefaults(props);
    }
    
    @Test
    public void parseEmpty() throws ParseException {
        Reader conf = new StringReader("  ");
        m_parser.parse(conf);
        
        //Verify
        verifyDefaults(m_parser.getProperties());
    }
    
    @Test
    public void parseValidComment() throws ParseException {
        Reader conf = new StringReader("#simple comment");
        m_parser.parse(conf);
        
        //Verify
        verifyDefaults(m_parser.getProperties());
    }
    
    @Test(expected = ParseException.class)
    public void parseInvalidComment() throws ParseException {
        Reader conf = new StringReader(" #simple comment");
        m_parser.parse(conf);
    } 
    
    @Test
    public void parseSingleVariable() throws ParseException {
        Reader conf = new StringReader("port 1234");
        m_parser.parse(conf);
        
        //Verify
        assertEquals("1234", m_parser.getProperties().getProperty("port"));
    }
    
    @Test
    public void parseCompleteFile() throws ParseException {
        String content = "# This is initial config format \r\n"
                + "  \r\n"
                + "port 1234 \r\n"
                + "host   localhost \r\n"
                + "fake  multi word string property\r\n";
        Reader conf = new StringReader(content);
        m_parser.parse(conf);
        
        //Verify
        Properties props = m_parser.getProperties();
        assertEquals("1234", props.getProperty("port"));
        assertEquals("localhost", props.getProperty("host"));
        assertEquals("multi word string property", props.getProperty("fake"));
    }
    
    /**
     * Helper method to verify default options.
     */
    private void verifyDefaults(Properties props) {
        assertEquals(Constants.PORT, Integer.parseInt(props.getProperty("port")));
        assertEquals(Constants.HOST, props.getProperty("host"));
    }
}