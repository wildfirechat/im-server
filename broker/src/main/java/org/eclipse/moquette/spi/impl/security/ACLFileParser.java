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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.moquette.spi.impl.security.Authorization.Permission;

/**
 * @author andrea
 * */
public class ACLFileParser {
    private static final Logger LOG = LoggerFactory.getLogger(ACLFileParser.class);

    /**
     * Parse the configuration from file.
     */
    public static List<Authorization> parse(File file) throws ParseException {
        if (file == null) {
            LOG.warn("parsing NULL file, so fallback on default configuration!");
            return Collections.emptyList();
        }
        if (!file.exists()) {
            LOG.warn(String.format("parsing not existing file %s, so fallback on default configuration!", file.getAbsolutePath()));
            return Collections.emptyList();
        }
        try {
            FileReader reader = new FileReader(file);
            return parse(reader);
        } catch (FileNotFoundException fex) {
            LOG.warn(String.format("parsing not existing file %s, so fallback on default configuration!", file.getAbsolutePath()), fex);
            return Collections.emptyList();
        }
    }

    /**
     * Parse the ACL configuration file
     *
     * @throws java.text.ParseException if the format is not compliant.
     */
    public static List<Authorization> parse(Reader reader) throws ParseException {
        if (reader == null) {
            //just log and return default properties
            LOG.warn("parsing NULL reader, so fallback on default configuration!");
            return Collections.emptyList();
        }

        BufferedReader br = new BufferedReader(reader);
        String line;
        List<Authorization> authorizations = new ArrayList();
        try {
            while ((line = br.readLine()) != null) {
                int commentMarker = line.indexOf('#');
                if (commentMarker != -1) {
                    if (commentMarker == 0) {
                        //skip its a comment
                        continue;
                    } else {
                        //it's a malformed comment
                        throw new ParseException(line, commentMarker);
                    }
                } else {
                    if (line.isEmpty() || line.matches("^\\s*$")) {
                        //skip it's a black line
                        continue;
                    }

                    authorizations.add(parseAuthLine(line));
                }
            }
        } catch (IOException ex) {
            throw new ParseException("Failed to read", 1);
        }
        return authorizations;
    }

    protected static Authorization parseAuthLine(String line) throws ParseException {
        String[] tokens = line.split("\\s+");
        String keyword = tokens[0];
        if ("topic".equalsIgnoreCase(keyword)) {
            if (tokens.length > 2) {
                //if the tokenized lines has 3 token the second must be the permission
                try {
                    Permission permission = Permission.valueOf(tokens[1].toUpperCase());
                    //bring topic with all original spacing
                    String topic = line.substring(line.indexOf(tokens[2]));

                    return new Authorization(topic, permission);
                } catch (IllegalArgumentException iaex) {
                    throw new ParseException("invalid permission token", 1);
                }
            }
            String topic = tokens[1];
            return new Authorization(topic);
        }
        return null;
    }
}
