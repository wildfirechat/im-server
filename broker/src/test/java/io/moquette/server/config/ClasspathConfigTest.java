package io.moquette.server.config;

import static org.junit.Assert.*;

import org.junit.Test;

import io.moquette.BrokerConstants;

public class ClasspathConfigTest {

	@Test
	public void testSetProperties() {
		final IConfig classPathConfig = new ClasspathConfig();
        assertEquals(""+BrokerConstants.PORT,classPathConfig.getProperty(BrokerConstants.PORT_PROPERTY_NAME));
        classPathConfig.setProperty(BrokerConstants.PORT_PROPERTY_NAME, "9999");
        assertEquals("9999",classPathConfig.getProperty(BrokerConstants.PORT_PROPERTY_NAME));
	}

}
