package org.kohsuke.parseipr;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.io.File;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Library {
    private final String name;
    private final List paths;

    public Library(String name,List paths) {
        this.name = name;
        this.paths = Collections.unmodifiableList(paths);
    }

    public void addTo(ClasspathBuilder builder) {
        for (int i = 0; i < paths.size(); i++) {
            File jar = (File)paths.get(i);
            builder.add(jar);
        }
    }
}
