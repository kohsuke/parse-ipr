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

        File projectFile;
        if(args.length==0)
            projectFile = parseArgument(".");
        else
            projectFile = parseArgument(args[0]);

        projectFile = projectFile.getAbsoluteFile();

        Parser p = parse(projectFile);
        for (int i = 0; i < p.modules.size(); i++) {
            File module = (File) p.modules.get(i);
            p.moduleDir = module.getParentFile();
            createParser().parse(module,p);
        }

        System.out.println(buildClasspath(p).getResult());

        System.exit(0);
    }

    private static ClasspathBuilder buildClasspath(Parser p) {
        ClasspathBuilder builder = new ClasspathBuilder();
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
        return builder;
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
        SAXParser sp = spf.newSAXParser();
        return sp;
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
