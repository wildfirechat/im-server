
package io.moquette.server.config;

import static org.junit.Assert.*;
import org.junit.Test;
import io.moquette.BrokerConstants;

public class ClasspathResourceLoaderTest {

    @Test
    public void testSetProperties() {
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        final IConfig classPathConfig = new ResourceLoaderConfig(classpathLoader);
        assertEquals("" + BrokerConstants.PORT, classPathConfig.getProperty(BrokerConstants.PORT_PROPERTY_NAME));
        classPathConfig.setProperty(BrokerConstants.PORT_PROPERTY_NAME, "9999");
        assertEquals("9999", classPathConfig.getProperty(BrokerConstants.PORT_PROPERTY_NAME));
    }

}
