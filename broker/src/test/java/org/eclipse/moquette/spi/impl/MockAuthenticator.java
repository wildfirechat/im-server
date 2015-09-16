/*
 * Copyright (c) 2012-2015 The original author or authors
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
package org.eclipse.moquette.spi.impl;

import java.util.Map;
import org.eclipse.moquette.spi.impl.security.IAuthenticator;

/**
 * Test utility to implements authenticator instance.
 * 
 * @author andrea
 */
class MockAuthenticator implements IAuthenticator {
    
    private Map<String, byte[]> m_userPwds;
    
    MockAuthenticator(Map<String, byte[]> userPwds) {
        m_userPwds = userPwds;
    }

    public boolean checkValid(String username, byte[] password) {
        if (!m_userPwds.containsKey(username)) {
            return false;
        }
        return m_userPwds.get(username).equals(password);
    }
    
}
