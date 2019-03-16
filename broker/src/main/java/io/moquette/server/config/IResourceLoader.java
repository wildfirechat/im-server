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

import java.io.Reader;

public interface IResourceLoader {

    Reader loadDefaultResource();

    Reader loadResource(String relativePath);

    String getName();

    class ResourceIsDirectoryException extends RuntimeException {

        private static final long serialVersionUID = -6969292229582764176L;

        public ResourceIsDirectoryException(String message) {
            super(message);
        }
    }

}
