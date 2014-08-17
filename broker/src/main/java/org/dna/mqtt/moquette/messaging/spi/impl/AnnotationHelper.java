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
package org.dna.mqtt.moquette.messaging.spi.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.server.ServerChannel;

/**
 * Annotation @MQTTMessage helper class.
 * 
 * It has the responsibility to identify the @MQTTMessage method of a given instance 
 * or class and define an easy access to invoke them.
 *  
 * @author andrea
 */
class AnnotationHelper {
    
    private Map<Class, Method> messageClassToMethod = new HashMap<Class, Method>();
    private Object targetInstance;

    void processAnnotations(Object instance) {
        targetInstance = instance;
        Class instanceCls = instance.getClass();
        Method[] allMethods = instanceCls.getDeclaredMethods();
        for (Method m : allMethods) {
            MQTTMessage annotation = m.getAnnotation(MQTTMessage.class);
            if (annotation == null) {
                continue;
            }
            Class mqttMessageClass = annotation.message();
            messageClassToMethod.put(mqttMessageClass, m);
        }
    }
    
    /**
     * Dispatch a call to the wrapped target invoking the method that match the 
     * msg class.
     * 
     * @return true iff the dispatched method was found.
     */
    boolean dispatch(ServerChannel session, AbstractMessage msg) {
        Method targetMethod = this.messageClassToMethod.get(msg.getClass());
        if (targetMethod == null) {
            return false;
        }
        try {
            targetMethod.invoke(targetInstance, session, msg);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } 
        
        return true;
    }

    
}
