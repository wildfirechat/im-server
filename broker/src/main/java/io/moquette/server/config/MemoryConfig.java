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

import java.util.Map;
import java.util.Properties;

/**
 * Configuration backed by memory.
 */
public class MemoryConfig extends IConfig {

    private final Properties m_properties = new Properties();

    public MemoryConfig(Properties properties) {
        assignDefaults();
        for (Map.Entry<Object, Object> entrySet : properties.entrySet()) {
            m_properties.put(entrySet.getKey(), entrySet.getValue());
        }
    }

    @Override
    public void setProperty(String name, String value) {
        m_properties.setProperty(name, value);
    }

    @Override
    public String getProperty(String name) {
        return m_properties.getProperty(name);
    }

    @Override
    public String getProperty(String name, String defaultValue) {
        return m_properties.getProperty(name, defaultValue);
    }

    @Override
    public IResourceLoader getResourceLoader() {
        return new FileResourceLoader();
    }

}
