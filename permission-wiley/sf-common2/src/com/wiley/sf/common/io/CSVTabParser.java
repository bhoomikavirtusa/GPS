/**
 * Parse comma-separated values (CSV), a common Windows file format.
 * Sample input: "LU",86.25,"11/4/1998","2:19PM",+4.0625
 * <p>
 * Inner logic adapted from a C++ original that was
 * Copyright (C) 1999 Lucent Technologies
 * Excerpted from 'The Practice of Programming'
 * by Brian W. Kernighan and Rob Pike.
 * <p>
 * Included by permission of the http://tpop.awl.com/ web site,
 * which says:
 * "You may use this code for any purpose, as long as you leave
 * the copyright notice and book citation attached." I have done so.
 * @author Brian W. Kernighan and Rob Pike (C++ original)
 * @author Ian F. Darwin (translation into Java and removal of I/O)
 * @author Ben Ballard (rewrote advQuoted to handle '""' and for readability)
 *
 * smarkoff: Removed empty ctor and added 2 factory methods (getCSVInstance() and getTabInstance()).
 * Renamed parse(String) to parseLine(String)
 * Added parseAll() methods.
 * Renamed parse(List) to generateLine(List).
 * Moved attribute list into parseLine(String) method (only used by that method).
 */
package com.wiley.sf.common.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 * This class is for single-thread use only.
 *
 * There are two ways of using this class. Either you can create an instance
 * using the getCSVInstance() or getTabInstance() methods and call the
 * parseLine() / generateLine() methods as needed,
 * or you can create an instance using one of the constructors that takes
 * a Reader or InputStream add call parseNextLine() as needed.
 * Using parseNextLine(), it will return null when the end of the stream
 * has been reached and close streams opened internally (but not the stream
 * passed into the constructor).
 * If you finish using this class before reaching the end of the stream,
 * you should call close() yourself.
 */
public class CSVTabParser {

	/** the separator char for this parser */
	private final char fieldSep;

	private Reader reader = null;
	private BufferedReader br = null;
	private InputStreamReader isr = null;


	public static CSVTabParser getCSVInstance() {
		return new CSVTabParser(',');
	}

	public static CSVTabParser getTabInstance() {
		return new CSVTabParser('\t');
	}

	/**
	 * Construct a CSV parser with a given separator.
	 */
	public CSVTabParser(char sep) {
		fieldSep = sep;
	}

	public CSVTabParser(char sep, Reader reader, boolean skipFirstLine) throws IOException {
		fieldSep = sep;
		this.reader = reader;
		br = new BufferedReader(reader);
		try {
			if (skipFirstLine) {
				br.readLine();  // throws IOException
			}
		}
		catch (IOException ex) {
			IOUtils.closeQuietly(br);
			throw ex;
		}
	}

	public CSVTabParser(char sep, InputStream is, boolean skipFirstLine) throws IOException {
		fieldSep = sep;
		isr = new InputStreamReader(is, "UTF-8");
		reader = isr;

		br = new BufferedReader(reader);
		try {
			if (skipFirstLine) {
				br.readLine();  // throws IOException
			}
		}
		catch (IOException ex){
			IOUtils.closeQuietly(br);
			throw ex;
		}
	}

	public List<String> parseNextLine() throws IOException {
		try {
			String line = br.readLine();

			if (line == null) {
				close();
				return null;
			}
			else return parseLine(line);
		}
		catch (IOException ex) {
			close();
			throw ex;
		}
	}

	public void close() {
		IOUtils.closeQuietly(isr);
		IOUtils.closeQuietly(br);
	}

	/**
	 * Break the input String into fields
	 * @return List<String> containing each field
	 * from the original as a String, in order.
	 */
	public List<String> parseLine(String line) {
		final StringBuilder sb = new StringBuilder();
		final ArrayList<String> list = new ArrayList<String>();
		int i = 0;

		if (line.length() == 0) {
			list.add(line);
			return list;
		}

		do {
			sb.setLength(0);
			if (i < line.length() && line.charAt(i) == '"')
				i = advQuoted(line, sb, ++i);	// skip quote
			else
				i = advPlain(line, sb, i);
			list.add(sb.toString());
			i++;
		} while (i < line.length());

		return list;
	}

	/**
	 * generates a line from a list of objects
	 * @return String
	 */
	public String generateLine(List<?> line) {
		StringBuilder sb = new StringBuilder();

		for (int x = 0; x < line.size(); x++) {
			Object tempo = line.get(x);
			String value = "";
			String dClass = "";
			// if the object is empty, simply generate an empty entry
			// in the CSV line.  This is in case a processor will need
			// to base the imports in the position of the columns
			if (null != tempo) {
				value = tempo.toString();
				dClass = "";
				if (null != tempo.getClass().getName()) {
				  dClass = tempo.getClass().getName();
				}
			}
		//	System.out.println("class=" + dClass);
			String delimiter = "";
			if (dClass.compareTo("java.lang.String") == 0) delimiter = "\"";
			sb.append(delimiter + value + delimiter);
			if (x < line.size() -1) sb.append(String.valueOf(fieldSep));
		  }

		return sb.toString() + "\n";
	}

	/** advQuoted: quoted field; return index of next separator */
	protected int advQuoted(String s, StringBuilder sb, int i) {
		int j;
		int len= s.length();
		for (j=i; j<len; j++) {
			if (s.charAt(j) == '"' && j+1 < len) {
				if (s.charAt(j+1) == '"' && s.charAt(j+2) != fieldSep) {
					j++; // skip escape char
				} else if (s.charAt(j+1) == '"' && (s.charAt(j+2) == fieldSep || j+2 == len)) {
					// it is a "", situation, that means it is not escape
					sb.append(s.charAt(j));
					j++;
					j++;
					break;
				} else if (s.charAt(j+1) == fieldSep) { //next delimiter
					j++; // skip end quotes
					break;
				}
			} else if (s.charAt(j) == '"' && j+1 == len) { // end quotes at end of line
				break; //done
			}
			sb.append(s.charAt(j));	// regular character.
		}
		return j;
	}

	/** advPlain: unquoted field; return index of next separator */
	protected int advPlain(String s, StringBuilder sb, int i) {
		int j;

		j = s.indexOf(fieldSep, i); // look for separator
		if (j == -1) {               	// none found
			sb.append(s.substring(i));
			return s.length();
		} else {
			sb.append(s.substring(i, j));
			return j;
		}
	}

}
