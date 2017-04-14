/*
 * Copyright (c) 2016-2017 The original author or authors
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

package io.moquette.test.osgi;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

/**
 * Run a set of unit tests inside of an OSGi container to make sure
 * Moquette works in the OSGi container,
 */
@RunWith(PaxExam.class)
public class MoquetteOsgiTest {
    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {

        return options(
            junitBundles(),
            mavenBundle("io.moquette", "moquette-broker")
        );
    }

    /**
     * Look for a bundle with moquette in the name.
     *
     * <p>
     * If the OSGi headers are not in the moquette jar, this will fail.
     */
    @Test
    public void testCanFindOsgiBundle() throws Exception {
        for (Bundle b : bundleContext.getBundles()) {
            String symbolicName = b.getSymbolicName();
            System.out.format("%s %d\n", symbolicName, b.getState());
            if (symbolicName != null && symbolicName.indexOf("moquette") != -1) {
                return;
            }
        }

        // If found no bundles with moquette in the name, fail the test.
	    fail();
    }
}
