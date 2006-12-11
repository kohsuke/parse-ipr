package org.kohsuke.parseipr;

import java.io.File;
import java.io.IOException;

/**
 * {@link ClasspathBuilder} for cygwin.
 *
 * @author Kohsuke Kawaguchi
 */
public class CygwinClasspathBuilder extends ClasspathBuilder {
    public CygwinClasspathBuilder() {
        setSeparator(":");
    }

    public void add(File f) throws IOException {
        f = f.getCanonicalFile();
        if(!files.add(f))
            return; // already added
        
        if( buf.length()!=0 )
            buf.append(":");

        String path = f.getAbsolutePath();
        path = path.replace('\\','/');

        if(path.substring(1).startsWith(":/"))
            path = "/cygdrive/"+path.charAt(0)+path.substring(2);

        buf.append(path);
    }
}
