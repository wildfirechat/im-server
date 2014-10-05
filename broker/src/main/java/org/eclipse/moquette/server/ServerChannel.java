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

/**
 *
 * @author andrea
 */
public interface ServerChannel {
    
    Object getAttribute(Object key);
    
    void setAttribute(Object key, Object value);
    
    void setIdleTime(int idleTime);
    
    void close(boolean immediately);
    
    void write(Object value);
}
