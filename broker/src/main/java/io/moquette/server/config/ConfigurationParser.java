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

package io.moquette.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Properties;

/**
 * Mosquitto configuration parser.
 *
 * A line that at the very first has # is a comment Each line has key value format, where the
 * separator used it the space.
 */
class ConfigurationParser {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationParser.class);

    private Properties m_properties = new Properties();

    /**
     * Parse the configuration from file.
     */
    void parse(File file) throws ParseException {
        if (file == null) {
            LOG.warn("parsing NULL file, so fallback on default configuration!");
            return;
        }
        if (!file.exists()) {
            LOG.warn(
                    String.format(
                            "parsing not existing file %s, so fallback on default configuration!",
                            file.getAbsolutePath()));
            return;
        }
        try {
            FileReader reader = new FileReader(file);
            parse(reader);
        } catch (FileNotFoundException fex) {
            LOG.warn(
                    String.format(
                            "parsing not existing file %s, so fallback on default configuration!",
                            file.getAbsolutePath()),
                    fex);
            return;
        }
    }

    /**
     * Parse the configuration
     *
     * @throws ParseException
     *             if the format is not compliant.
     */
    void parse(Reader reader) throws ParseException {
        if (reader == null) {
            // just log and return default properties
            LOG.warn("parsing NULL reader, so fallback on default configuration!");
            return;
        }

        BufferedReader br = new BufferedReader(reader);
        String line;
        try {
            while ((line = br.readLine()) != null) {
                line = line.trim();
                int commentMarker = line.indexOf('#');
                if (commentMarker != -1) {
                    if (commentMarker == 0) {
                        // skip its a comment
                        continue;
                    } else {
                        String[] ss = line.split(" ");
                        if(ss.length != 2) {
                            throw new ParseException(line, commentMarker);
                        }
                    }
                } else {
                    if (line.isEmpty() || line.matches("^\\s*$")) {
                        // skip it's a black line
                        continue;
                    }
                }

                // split till the first space
                int delimiterIdx = line.indexOf(' ');
                String key;
                String value = "";
                if (delimiterIdx > 0) {
                    key = line.substring(0, delimiterIdx).trim();
                    value = line.substring(delimiterIdx).trim();
                } else {
                    key = line.trim();
                }

                m_properties.put(key, value);
            }
        } catch (IOException ex) {
            throw new ParseException("Failed to read", 1);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    Properties getProperties() {
        return m_properties;
    }
}
