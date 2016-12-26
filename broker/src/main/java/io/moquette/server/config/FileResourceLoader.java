package io.moquette.server.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

public class FileResourceLoader implements IResourceLoader {

    private final File defaultFile;
    private final String parentPath;

    public FileResourceLoader() {
        this((File) null);
    }

    public FileResourceLoader(File defaultFile) {
        this(defaultFile, System.getProperty("moquette.path", null));
    }

    public FileResourceLoader(String parentPath) {
        this(null, parentPath);
    }

    public FileResourceLoader(File defaultFile, String parentPath) {
        this.defaultFile = defaultFile;
        this.parentPath = parentPath;
    }

    @Override
    public Reader loadDefaultResource() {
        if (defaultFile != null) {
            return loadResource(defaultFile);
        } else {
            throw new IllegalArgumentException("Default file not set!");
        }
    }

    @Override
    public Reader loadResource(String relativePath) {
        return loadResource(new File(parentPath, relativePath));
    }

    public Reader loadResource(File f) {
        if (f.isDirectory()) {
            throw new ResourceIsDirectoryException("File \"" + f + "\" is a directory!");
        }
        try {
            return new FileReader(f);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public String getName() {
        return "file";
    }

}
