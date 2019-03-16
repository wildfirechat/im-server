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

import io.moquette.server.config.FileResourceLoader;

/**
 * Load user credentials from a text file. Each line of the file is formatted as
 * "[username]:[sha256(password)]". The username mustn't contains : char.
 *
 * To encode your password from command line on Linux systems, you could use:
 *
 * <pre>
 *     echo -n "yourpassword" | sha256sum
 * </pre>
 *
 * NB -n is important because echo append a newline by default at the of string. -n avoid this
 * behaviour.
 *
 * @deprecated user {@link ResourceAuthenticator} instead
 */
public class FileAuthenticator extends ResourceAuthenticator {

    public FileAuthenticator(String parent, String filePath) {
        super(new FileResourceLoader(parent), filePath);
    }
}
