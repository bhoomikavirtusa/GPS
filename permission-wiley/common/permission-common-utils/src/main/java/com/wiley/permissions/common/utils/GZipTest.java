package com.wiley.permissions.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.zip.GZIPInputStream;

/**
 *
 * @since JDK 1.6
 * @version 9/7/2011
 * @author Steve Markoff
 */
public class GZipTest {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <gzip file>");
			System.exit(1);
		}

		File file = new File(args[0]);
		String s = ungzip(file);
		System.out.println("text");
		System.out.println(s);
	}

	private static String ungzip(File file) throws Exception {
		long startTime = System.currentTimeMillis();
		if (file.length() > Integer.MAX_VALUE) {
			throw new RuntimeException("Can't handle payload over 2GB.");
		}
		FileInputStream fin = new FileInputStream(file);
		GZIPInputStream gin = new GZIPInputStream(fin);
		InputStreamReader isr = new InputStreamReader(gin, "UTF-8");
		// note the String could end up smaller than the byte array
		// if there are double-byte chars but "size" gives us a good estimate
		// for the char buffer size needed
		int size = (int) file.length();
		StringBuilder sb = new StringBuilder(size);
		int c = isr.read();
		while (c != -1) {
			sb.append((char) c);
			c = isr.read();
		}
		isr.close();
		gin.close();
		fin.close();
		String ret = sb.toString();
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		long time = System.currentTimeMillis() - startTime;
		System.out.println("unzipped payload of " + intFormat.format(size)
			+ " bytes -> " + intFormat.format(ret.length()) + " chars, in " + time + " ms.");
		return ret;
	}
}
