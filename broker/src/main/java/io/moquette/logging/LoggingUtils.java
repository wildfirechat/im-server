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

package io.moquette.logging;

import java.util.ArrayList;
import java.util.Collection;
import io.moquette.interception.InterceptHandler;

public final class LoggingUtils {

    public static <T extends InterceptHandler> Collection<String> getInterceptorIds(Collection<T> handlers) {
        Collection<String> result = new ArrayList<>(handlers.size());
        for (T handler : handlers) {
            result.add(handler.getID());
        }
        return result;
    }

    private LoggingUtils() {
    }
}
