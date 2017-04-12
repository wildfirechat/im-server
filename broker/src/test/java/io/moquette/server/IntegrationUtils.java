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

import java.io.File;
import java.util.Properties;
import static io.moquette.BrokerConstants.DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME;
import static io.moquette.BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;

/**
 * Used to carry integration configurations.
 */
public final class IntegrationUtils {

    static String localMapDBPath() {
        String currentDir = System.getProperty("user.dir");
        return currentDir + File.separator + "target" + File.separator + DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME;
    }

    static String localClusterMapDBPath(int port) {
        String currentDir = System.getProperty("user.dir");
        return currentDir + File.separator + "target" + File.separator + port + DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME;
    }

    public static Properties prepareTestProperties() {
        Properties testProperties = new Properties();
        testProperties.put(PERSISTENT_STORE_PROPERTY_NAME, IntegrationUtils.localMapDBPath());
        testProperties.put(PORT_PROPERTY_NAME, "1883");
        return testProperties;
    }

    public static Properties prepareTestClusterProperties(int port) {
        Properties testProperties = new Properties();
        testProperties.put(PERSISTENT_STORE_PROPERTY_NAME, IntegrationUtils.localClusterMapDBPath(port));
        testProperties.put(PORT_PROPERTY_NAME, Integer.toString(port));
        return testProperties;
    }

    /*public static void cleanPersistenceFile(IConfig config) {
        String fileName = config.getProperty(PERSISTENT_STORE_PROPERTY_NAME);
        cleanPersistenceFile(fileName);
    }

    public static void cleanPersistenceFile(String fileName) {
        File dbFile = new File(fileName);
        if (dbFile.exists()) {
            dbFile.delete();
            new File(fileName + ".p").delete();
            new File(fileName + ".t").delete();
        }
        assertFalse(dbFile.exists());
    }*/

    private IntegrationUtils() {
    }
}
