package com.aurea.testgenerator.plugin.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import sun.misc.CompoundEnumeration;

public class CompositeClassLoader extends ClassLoader {

    private final ClassLoader current;

    public CompositeClassLoader(final ClassLoader parent, ClassLoader current) {
        super(parent);
        this.current = current;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        try {
            return current.loadClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name);
        }
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        Enumeration<URL> resources = current.getResources(name);
        Enumeration<URL> parent = super.getResources(name);
        return new CompoundEnumeration<URL>(new Enumeration[]{resources, parent});
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        InputStream stream = current.getResourceAsStream(name);
        return stream != null ? stream : super.getResourceAsStream(name);
    }
}
