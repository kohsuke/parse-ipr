package org.kohsuke.parseipr;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;

import java.util.*;
import java.io.File;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Parser extends DefaultHandler {
    /** Active {@link DefaultHandler}s. */
    private final Set handlers = new HashSet();

    private final File projectDir;
    public File moduleDir;

    /** List of {@link File}s that represent .iml files. */
    public final List modules = new ArrayList();

    /** project/module {@link Library}s. */
    public final List libraries = new ArrayList();

    /** List of {@link File}s that represent output folders. */
    public final List outputs = new ArrayList();


    public Parser(File projectDir) {
        this.projectDir = projectDir;
        addHandler(new ModuleFinder());
        addHandler(new LibraryFinder());
        addHandler(new ModuleScanner());
    }


    private void addHandler(ContentHandler h) {
        handlers.add(h);
    }

    private void removeHandler(ContentHandler h) {
        handlers.remove(h);
    }

    private ContentHandler[] getHandlers() {
        return (ContentHandler[]) handlers.toArray(new ContentHandler[handlers.size()]);
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        ContentHandler[] h = getHandlers();
        for(int i=0; i<h.length; i++ )
            h[i].startElement(uri,localName,qName,attributes);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        ContentHandler[] h = getHandlers();
        for(int i=0; i<h.length; i++ )
            h[i].endElement(uri, localName, qName);
    }


    /**
     * Parses a string that possibly starts from $PROJECT_DIR$ into a {@link File} object.
     */
    private File parseFile(String filepath) {
        File f;
        if(filepath.equals("$PROJECT_DIR$")) {
            f = projectDir;
        } else
        if(filepath.startsWith("$PROJECT_DIR$/")) {
            f = new File(projectDir,filepath.substring("$PROJECT_DIR$/".length()));
        } else
        if(filepath.equals("$MODULE_DIR$")) {
            f = moduleDir;
        } else
        if(filepath.startsWith("$MODULE_DIR$/")) {
            f = new File(moduleDir,filepath.substring("$MODULE_DIR$/".length()));
        } else {
            f = new File(filepath);
        }

        if(!f.exists()) {
            System.err.println("Warning: non-existent file "+f);
            return null;
        }
        return f;
    }

    private File parseUrl(String url) {
        if(url.startsWith("file://")) {
            return parseFile(url.substring(7));
        } else
        if(url.startsWith("jar://")) {
            return parseFile(url.substring(6,url.length()-2));
        } else
            throw new IllegalStateException("Unknown classpath element: "+url);
    }


    public class ModuleFinder extends DefaultHandler {

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if(!localName.equals("module"))
                return;

            String filepath = attributes.getValue("filepath");
            if(filepath==null)
                return;

            File module = parseFile(filepath);
            if(module!=null)
                modules.add(module);
        }
    }

    public class ModuleScanner extends DefaultHandler {
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if(localName.equals("output") || localName.equals("output-test") || localName.equals("sourceFolder")) {
                String url = attributes.getValue("url");
                if(url==null)
                    throw new IllegalStateException("@url missing in <output>");

                File path = parseUrl(url);

                if(path!=null) {
                    if(localName.equals("sourceFolder"))
                        outputs.add(0,path);
                    else
                        outputs.add(path);
                }
            }
        }
    }

    /**
     * Looks for "library"tag and launchs {@link LibraryScanner}
     */
    public class LibraryFinder extends DefaultHandler {
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if(!localName.equals("library"))
                return;

            String name = attributes.getValue("name");
            if(name==null)
                name="##local";
            addHandler(new LibraryScanner(name));
        }
    }

    public class LibraryScanner extends DefaultHandler {
        private final String name;
        private boolean inClass;
        private final List paths = new ArrayList();

        public LibraryScanner(String name) {
            this.name = name;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if(localName.equals("CLASSES"))
                inClass=true;

            if(localName.equals("root") && inClass) {
                String url = attributes.getValue("url");
                if(url==null)
                    throw new IllegalStateException("@url missing in <ROOT>");
                File path = parseUrl(url);
                if(path!=null)
                    paths.add(path);
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if(localName.equals("CLASSES")) {
                inClass=false;
                return;
            }
            if(localName.equals("library"))
                done();
        }

        private void done() {
            removeHandler(this);    // done
            libraries.add(new Library(name,paths));
        }
    }
}
