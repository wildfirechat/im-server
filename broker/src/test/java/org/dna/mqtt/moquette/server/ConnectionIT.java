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
package org.dna.mqtt.moquette.server;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;

/**
 * This class runs the robot test scripts to run the TCK for Moquette  broker, before  remember to run
 * mvn -Dmaven.robot.daemon=false robot:start from broker submodule folder
 *
 *
 * @author pkhanal
 * 
 */
public class ConnectionIT {
	
	@Rule
    public RobotRule robot = new RobotRule();
	
	Server m_server;
	
    protected void startServer() throws IOException {
        m_server = new Server();
        m_server.startServer();
    }

    @Before
    public void setUp() throws Exception {
        startServer();
    }

    @After
    public void tearDown() throws Exception {
        m_server.stopServer();
    }
	
	// The test starts the server during setUp
    // The network communication is driven by the script.
    @Robotic(script = "connect.then.close")
    @Test(timeout = 1000)
    public void shouldConnectThenClose() throws Exception {
    	robot.join();
    }

}
