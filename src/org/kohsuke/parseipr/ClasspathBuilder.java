package org.kohsuke.parseipr;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

/**
 * Builds a CLASSPATH string from {@link File}s.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ClasspathBuilder {
    /**
     * Path separator.
     * The default value is platform-dependent.
     */
    private String separator = File.pathSeparator;

    protected final StringBuffer buf = new StringBuffer();

    /**
     * Used for checking duplicates.
     */
    protected final Set files = new HashSet();

    /**
     * Overrides the platform-default separator string.
     */
    public void setSeparator( String sep ) {
        this.separator = sep;
    }

    public void reset() {
        buf.setLength(0);
    }

    /**
     * Adds a new entry
     */
    public void add( File f ) throws IOException {
        f = f.getCanonicalFile();
        if(!files.add(f))
            return; // already added

        if( buf.length()!=0 )
            buf.append(separator);
        buf.append(f.toString());
    }

    /**
     * Returns the string formatted for the CLASSPATH variable.
     */
    public String getResult() {
        return buf.toString();
    }
}
