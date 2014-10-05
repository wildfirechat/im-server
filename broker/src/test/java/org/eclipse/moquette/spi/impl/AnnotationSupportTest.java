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
package org.eclipse.moquette.spi.impl;

import org.eclipse.moquette.proto.messages.ConnAckMessage;
import org.eclipse.moquette.proto.messages.ConnectMessage;
import org.eclipse.moquette.server.ServerChannel;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class AnnotationSupportTest {
    
    class SimplyAnnotatedFixture {
        
        @MQTTMessage(message = ConnectMessage.class)
        void methodToBeDispatched(ServerChannel session, ConnectMessage msg) {
            System.out.println("Just a sample method name");
            methodInvoked = true;
        }
    }
    
    SimplyAnnotatedFixture annotationTarget = new SimplyAnnotatedFixture();
    boolean methodInvoked;
    
    @Before
    public void setUp() {
        methodInvoked = false;
    }
    
    @Test
    public void testAnnotationDiscovery() {
        AnnotationSupport annHelper = new AnnotationSupport();
        annHelper.processAnnotations(annotationTarget);
        annHelper.dispatch(null, new ConnectMessage());
        
        //Verify
        assertTrue(methodInvoked);
    }
    
    @Test(expected = RuntimeException.class)
    public void testAnnotationFailsDiscovery() {
        AnnotationSupport annHelper = new AnnotationSupport();
        annHelper.processAnnotations(annotationTarget);
        annHelper.dispatch(null, new ConnAckMessage());
    }
}
