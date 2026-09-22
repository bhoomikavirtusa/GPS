package com.wiley.sf.common.codec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.wiley.sf.common.lang.ArgUtil;

/**
 *
 * @since JDK 1.6
 * @version $Id: MessageDigestUtil.java,v 1.4 2012-06-19 19:34:33 smarkoff Exp $
 * @author  Steve Markoff
 *
 * @see com.wiley.sf.common.security.PasswordUtil
 */
public class MessageDigestUtil {

    public enum Algorithm {
        MD5 ("MD5"),
        SHA_1 ("SHA-1"),
        SHA_256 ("SHA-256"),
        SHA_384 ("SHA-384"),
        SHA_512 ("SHA-512");

        private final String name;

        private Algorithm(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Applies the specified message digest algorithm to the specified string.
     * Returns the resulting bytes converted to hex representation.
     *
     * @param s          Must be non-null
     * @param algorithm  Must be non-null
     */
    public static String getDigest(String s, Algorithm algorithm) {
        ArgUtil.notNull(s, "s");
        ArgUtil.notNull(algorithm, "algorithm");

        try {
            MessageDigest md = MessageDigest.getInstance(algorithm.getName());
                // throws NoSuchAlgorithmException
            md.update(s.getBytes("UTF-8"));
                // throws UnsupportedEncodingException
            byte [] bytes = md.digest();
            return Hex.bytesToHex(bytes);
        }
        // We don't expect these exceptions since we are using
        // a known algorithm and encoding.
        catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Applies the specified message digest algorithm to the specified file.
     * Returns the resulting bytes converted to hex representation.
     *
     * @param file       Must be non-null
     * @param algorithm  Must be non-null
     */
    public static String getDigest(File file, Algorithm algorithm)
        throws IOException
    {
        ArgUtil.notNull(file, "file");
        ArgUtil.notNull(algorithm, "algorithm");
        MessageDigest md;

        // We don't expect a NoSuchAlgorithmException since we are using
        // a known algorithm.
        try {
            md = MessageDigest.getInstance(algorithm.getName());
            // throws NoSuchAlgorithmException
        }
        catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }

        FileInputStream in = new FileInputStream(file);
        byte [] buf = new byte[8 * 1024];
        int amount;
        while ((amount = in.read(buf)) != -1) {
            md.update(buf, 0, amount);
        }
        in.close();

        byte [] bytes = md.digest();
        return Hex.bytesToHex(bytes);
    }

    /** Test program. */
    public static void main(String [] args)
        throws IOException
    {
        if (args.length < 1) {
            System.err.println("usage: <string to encode> [file to encode]");
            System.err.println("If you specify a second parameter, the first will be ignored.");
            System.exit(1);
        }

        Algorithm [] algorithms = {
            MessageDigestUtil.Algorithm.MD5,
            MessageDigestUtil.Algorithm.SHA_1,
            MessageDigestUtil.Algorithm.SHA_256,
            MessageDigestUtil.Algorithm.SHA_384,
            MessageDigestUtil.Algorithm.SHA_512 };

        String string = args[0];
        File file = null;

        if (args.length > 1) {
            file = new File(args[1]);
        }

        for (int i = 0; i < algorithms.length; i++) {
            System.out.println("---- " + algorithms[i]);
            long startTime = System.currentTimeMillis();

            String hex;
            if (file == null) {
                hex = getDigest(string, algorithms[i]);
            }
            else {
                hex = getDigest(file, algorithms[i]);  // throws IOException
            }
            long time = System.currentTimeMillis() - startTime;

            System.out.println("byte size: " + hex.length() / 2);
            System.out.println("hex: " + hex);
            System.out.println("time was " + time + " ms.");
        }
    }
}
