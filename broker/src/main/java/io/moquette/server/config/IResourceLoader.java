package io.moquette.server.config;

import java.io.Reader;

public interface IResourceLoader {

    Reader loadDefaultResource();

    Reader loadResource(String relativePath);

    String getName();

    class ResourceIsDirectoryException extends RuntimeException {
        public ResourceIsDirectoryException(String message) {
            super(message);
        }
    }

}
