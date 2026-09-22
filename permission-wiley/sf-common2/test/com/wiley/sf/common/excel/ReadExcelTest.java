/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.excel;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;

import com.wiley.sf.common.lang.ResourceUtil;

/**
 *
 * @since JDK 1.6, POI 3.7, JUnit 4.10
 * @author Steve Markoff
 * @version $Id: ReadExcelTest.java,v 1.5 2012-02-25 00:31:25 smarkoff Exp $
 */
public class ReadExcelTest {

    @Test
    public void parse() throws IOException, InvalidFormatException {
	    InputStream inStream = ResourceUtil.getResourceInputStream(
	        ReadExcelTest.class, "ReadExcelTest.xlsx");
	    SimpleExcelData data = ReadExcel.parse(inStream);
	        // throws IOException, InvalidFormatException
	    String [] headers = data.getHeaders();
	    List<String []> main = data.getData();

	    assertTrue("headers.length expected to be 3", headers.length == 3);
	    assertTrue("main.length expected to be 2", main.size() == 2);
	    assertTrue(headers[0].equals("head1"));
	    assertTrue(headers[1].equals("head2"));
	    assertTrue(headers[2].equals("head3"));
	    assertTrue(main.get(0)[0].equals("row1col1"));
	    assertTrue(main.get(1)[2].equals("row2col3"));
    }

}
