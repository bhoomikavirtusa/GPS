package com.wiley.sf.common.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.StringUtil;

/**
 *
 *
 * @author smarkoff, created 5/17/2007
 * @version $Id: CharsetRecurse.java,v 1.5 2013-02-23 00:12:34 smarkoff Exp $
 */
public class CharsetRecurse {

    // --------------------------- static data ------------------------------

    private static final NumberFormat intFormat = NumberFormat.getIntegerInstance();
    private static final Log log = LogFactory.getLog(CharsetRecurse.class);

    // -------------------------- static methods ----------------------------

    public static void main(String [] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: <file or dir to test>");
            System.exit(1);
        }

        File file = new File(args[0]);
        CharsetRecurse cr = new CharsetRecurse(file);
        cr.recurse();  // throws IOException
        cr.printCounts();
    }


    // --------------------------- instance data ----------------------------

    private final File root;
    private int count = 0;
    private int unknownCount = 0;
    private int asciiCount = 0;
    private int utf8Count = 0;
    private int latin1Count = 0;
    private int windows1252Count = 0;

    // --------------------------- instance methods -------------------------

    /**
     * @param root  Must be non-null
     */
    public CharsetRecurse(File root) {
        ArgUtil.notNull(root, "root");

        if (!root.exists()) {
            throw new RuntimeException("file or dir does not exist: " + root.getAbsolutePath());
        }
        this.root = root;
    }

    public void recurse() throws IOException {
        if (root.isFile()) {
            doFile(root);
        }
        else  doDir(root);
    }

    public void printCounts() {
        System.out.println("count = " + intFormat.format(count));
        System.out.println("unknownCount = " + intFormat.format(unknownCount));
        System.out.println("asciiCount = " + intFormat.format(asciiCount));
        System.out.println("utf8Count = " + intFormat.format(utf8Count));
        System.out.println("latin1Count = " + intFormat.format(latin1Count));
        System.out.println("windows1252Count = " + intFormat.format(windows1252Count));
    }

    private void updateCounts(CharsetType charsetType) {
        count++;

        if (charsetType == CharsetType.UNKNOWN)  unknownCount++;
        else if (charsetType == CharsetType.ASCII)  asciiCount++;
        else if (charsetType == CharsetType.UTF8) utf8Count++;
        else if (charsetType == CharsetType.LATIN1) latin1Count++;
        else if (charsetType == CharsetType.WINDOWS1252) windows1252Count++;
        else throw new RuntimeException("unknown type");  // this should never happen
    }

    private void doDir(File dir) throws IOException {
        File [] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) doFile(files[i]);
            else  doDir(files[i]);
        }
    }

    private void doFile(File file) throws IOException {
        if (StringUtils.endsWithIgnoreCase(file.getName(), ".tar.gz")) {
            doTarGz(file);
        }

        if (isText(file.getName())) {
            CharsetType type = CharsetToolkit.determineCharset(file).getCharsetType();
                // throws IOException
            updateCounts(type);
            //log.debug("type = " + type.getName() + ", path = " + file.getAbsolutePath());
            if (type == CharsetType.UNKNOWN || type == CharsetType.WINDOWS1252) {
                System.out.println("type = " + type.getName() + ", path = " + file.getAbsolutePath());
            }
        }
    }

    private boolean isText(String fileName) {
        String ext = FilenameUtils.getExtension(fileName);
        String [] process = { "htm", "html", "txt", "text" };
        return StringUtil.equalsAnyIgnoreCase(ext, process);
    }

    private void doTarGz(File file) throws IOException {
        FileInputStream fin = new FileInputStream(file);
            // throws IOException
        GZIPInputStream zin = new GZIPInputStream(fin);
            // throws IOException
        TarInputStream tin = new TarInputStream(zin);

        TarEntry entry;

        while ((entry = tin.getNextEntry()) != null) {
            doTarGz(tin, entry.getName(), file.getPath());
        }

        tin.close();  // throws IOException
        zin.close();  // throws IOException
        fin.close();  // throws IOException
    }

    private void doTarGz(InputStream in, String fileName, String source) throws IOException {
        if (isText(fileName)) {
            CharsetType type = CharsetToolkit.determineCharset(in).getCharsetType();
                // throws IOException
            updateCounts(type);
            log.debug("type = " + type.getName() + ", file = " + fileName
                + " inside the file " + source);
        }
    }
}
