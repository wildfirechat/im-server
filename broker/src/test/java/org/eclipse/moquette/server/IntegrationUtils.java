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

import java.io.File;
import java.util.Properties;

import static org.eclipse.moquette.commons.Constants.DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME;
import static org.eclipse.moquette.commons.Constants.PERSISTENT_STORE_PROPERTY_NAME;

/**
 * Used to carry integration configurations.
 *
 * Created by andrea on 4/7/15.
 */
class IntegrationUtils {
    static String localMapDBPath() {
        String currentDir = System.getProperty("user.dir");
        return currentDir + File.separator + DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME;
    }

    public static Properties prepareTestPropeties() {
        Properties testProperties = new Properties();
        testProperties.put(PERSISTENT_STORE_PROPERTY_NAME, IntegrationUtils.localMapDBPath());
        return testProperties;
    }
}
