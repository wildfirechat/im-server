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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileResourceLoader implements IResourceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(FileResourceLoader.class);

    private final File defaultFile;
    private final String parentPath;

    public FileResourceLoader() {
        this((File) null);
    }

    public FileResourceLoader(File defaultFile) {
        this(defaultFile, System.getProperty("wildfirechat.path", null));
    }

    public FileResourceLoader(String parentPath) {
        this(null, parentPath);
    }

    public FileResourceLoader(File defaultFile, String parentPath) {
        this.defaultFile = defaultFile;
        this.parentPath = parentPath;
    }

    @Override
    public Reader loadDefaultResource() {
        if (defaultFile != null) {
            return loadResource(defaultFile);
        } else {
            throw new IllegalArgumentException("Default file not set!");
        }
    }

    @Override
    public Reader loadResource(String relativePath) {
        return loadResource(new File(parentPath, relativePath));
    }

    public Reader loadResource(File f) {
        LOG.info("Loading file. Path = {}.", f.getAbsolutePath());
        if (f.isDirectory()) {
            LOG.error("The given file is a directory. Path = {}.", f.getAbsolutePath());
            throw new ResourceIsDirectoryException("File \"" + f + "\" is a directory!");
        }
        try {
            return new FileReader(f);
        } catch (FileNotFoundException e) {
            LOG.error("The file does not exist. Path = {}.", f.getAbsolutePath());
            return null;
        }
    }

    @Override
    public String getName() {
        return "file";
    }

}
