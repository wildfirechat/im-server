
package io.moquette.server.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClasspathResourceLoader implements IResourceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ClasspathResourceLoader.class);

    private final String defaultResource;
    private final ClassLoader classLoader;

    public ClasspathResourceLoader() {
        this(IConfig.DEFAULT_CONFIG);
    }

    public ClasspathResourceLoader(String defaultResource) {
        this(defaultResource, Thread.currentThread().getContextClassLoader());
    }

    public ClasspathResourceLoader(String defaultResource, ClassLoader classLoader) {
        this.defaultResource = defaultResource;
        this.classLoader = classLoader;
    }

    @Override
    public Reader loadDefaultResource() {
        return loadResource(defaultResource);
    }

    @Override
    public Reader loadResource(String relativePath) {
        LOG.info("Loading resource. RelativePath = {}.", relativePath);
        InputStream is = this.classLoader.getResourceAsStream(relativePath);
        return is != null ? new InputStreamReader(is) : null;
    }

    @Override
    public String getName() {
        return "classpath resource";
    }

}
