package org.kohsuke.parseipr;

import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main {
    public static void main(String[] args) throws Exception {
        System.exit(new Main().run(args));
    }

    public int mode = MODE_PATH;

    /** Just print out the path. */
    public static final int MODE_PATH = 0;

    /** Print out a Windows batch file. */
    public static final int MODE_WINDOWS = 1;

    /** Print out a sh shell script file. */
    public static final int MODE_BOURNE_SHELL = 2;

    /** Print out a csh shell script file. */
    public static final int MODE_C_SHELL = 3;

    public boolean isCygwin = false;

    /** @noinspection StringEquality*/
    public int run(String[] args) throws IOException, ParserConfigurationException, SAXException {
        File projectFile=null;

        for( int i=0; i<args.length; i++ ) {
            String arg = args[i].intern();
            if(arg.startsWith("-")) {
                if(arg=="-batch") {
                    mode = MODE_WINDOWS;
                    continue;
                }
                if(arg=="-sh") {
                    mode = MODE_BOURNE_SHELL;
                    continue;
                }
                if(arg=="-csh") {
                    mode = MODE_C_SHELL;
                    continue;
                }
                if(arg=="-cygwin") {
                    isCygwin = true;
                    continue;
                }
                printUsage("Unrecognized option "+arg);
                return -1;
            } else {
                if(projectFile==null)
                    projectFile = parseArgument(arg);
                else {
                    printUsage(null);
                    return -1;
                }
            }
        }

        if(projectFile==null)
            projectFile = parseArgument(".");

        projectFile = projectFile.getCanonicalFile();

        Parser p = parse(projectFile);
        for (int i = 0; i < p.modules.size(); i++) {
            File module = (File) p.modules.get(i);
            p.moduleDir = module.getParentFile();
            createParser().parse(module,p);
        }

        ClasspathBuilder cpb = isCygwin ? new CygwinClasspathBuilder() : new ClasspathBuilder();
        buildClasspath(cpb,p);

        String path = cpb.getResult();

        switch(mode) {
        case MODE_PATH:
            System.out.print(path);
            return 0;
        case MODE_WINDOWS:
            System.out.println("SET CLASSPATH=\""+path+"\"");
            return 0;
        case MODE_BOURNE_SHELL:
            // when running on Windows for cygwin, println puts CR LF, but we only want LF
            System.out.print("CLASSPATH=\""+path+"\"\n");
            System.out.print("export CLASSPATH\n");
            return 0;
        case MODE_C_SHELL:
            System.out.print("setenv CLASSPATH \""+path+"\"\n");
            return 0;
        default:
            printUsage("Unknown operation mode: "+mode);
            return -1;
        }
    }

    private void printUsage(String msg) {
        if(msg!=null)
            System.err.println(msg);
        System.err.println(
            "Usage: parse-ipr [options...] <path>\n"+
            "Reads IntelliJ IDEA project file and build CLASSPATH value\n" +
            "\n" +
            "Path can be:\n" +
            "  - a directory that has an .ipr file\n" +
            "  - an .ipr file\n" +
            "\n" +
            "Options:\n" +
            "  -batch   : print out Windows batch file that sets CLASSPATH"
        );
    }

    private static void buildClasspath(ClasspathBuilder builder, Parser p) throws IOException {
        // add output dirs
        for (int i = 0; i < p.outputs.size(); i++) {
            File output = (File) p.outputs.get(i);
            builder.add(output);
        }
        // then libraries
        for (int i = 0; i < p.libraries.size(); i++) {
            Library lib = (Library) p.libraries.get(i);
            lib.addTo(builder);
        }
    }

    private static Parser parse(File projectFile) throws ParserConfigurationException, SAXException, IOException {
        File projectDir = projectFile.getParentFile();
        Parser pp = new Parser(projectDir);

        createParser().parse(projectFile,pp);

        return pp;
    }

    private static SAXParser createParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        return spf.newSAXParser();
    }

    private static File parseArgument(String arg) {
        File f = new File(arg);
        if(f.exists()) {
            if(!f.isDirectory() && f.getName().endsWith(".ipr"))
                return f;

            if(f.isDirectory()) {
                // try to locate the sole ipr file in the directory
                File[] children = f.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".ipr");
                    }
                });
                if(children.length==1)
                    return children[0];
                throw new IllegalArgumentException("no .ipr file in "+arg);
            }
        }
        throw new IllegalArgumentException("unrecognized input "+arg);
    }
}
