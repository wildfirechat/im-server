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
import java.io.Reader;
import java.text.ParseException;
import java.util.Properties;

/**
 * Configuration that loads config stream from a {@link IResourceLoader} instance.
 */
public class ResourceLoaderConfig extends IConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceLoaderConfig.class);

    private final Properties m_properties;
    private final IResourceLoader resourceLoader;

    public ResourceLoaderConfig(IResourceLoader resourceLoader) {
        this(resourceLoader, null);
    }

    public ResourceLoaderConfig(IResourceLoader resourceLoader, String configName) {
        LOG.info("Loading configuration. ResourceLoader = {}, configName = {}.", resourceLoader.getName(), configName);
        this.resourceLoader = resourceLoader;

        /*
         * If we use a conditional operator, the loadResource() and the loadDefaultResource()
         * methods will be always called. This makes the log traces confusing.
         */

        Reader configReader;
        if (configName != null) {
            configReader = resourceLoader.loadResource(configName);
        } else {
            configReader = resourceLoader.loadDefaultResource();
        }

        if (configReader == null) {
            LOG.error(
                    "The resource loader returned no configuration reader. ResourceLoader = {}, configName = {}.",
                    resourceLoader.getName(),
                    configName);
            throw new IllegalArgumentException("Can't locate " + resourceLoader.getName() + " \"" + configName + "\"");
        }

        LOG.info(
                "Parsing configuration properties. ResourceLoader = {}, configName = {}.",
                resourceLoader.getName(),
                configName);
        ConfigurationParser confParser = new ConfigurationParser();
        m_properties = confParser.getProperties();
        assignDefaults();
        try {
            confParser.parse(configReader);
        } catch (ParseException pex) {
            LOG.warn(
                    "Unable to parse configuration properties. Using default configuration. "
                    + "ResourceLoader = {}, configName = {}, cause = {}, errorMessage = {}.",
                    resourceLoader.getName(),
                    configName,
                    pex.getCause(),
                    pex.getMessage());
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
        return resourceLoader;
    }

}
