/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.excel;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.SystemUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;

import com.wiley.sf.common.lang.StringUtil;

/**
 *
 * @since JDK 1.6, POI 3.7, JUnit 4.10
 * @author Steve Markoff
 * @version $Id: WriteExcelTest.java,v 1.5 2013-02-23 00:03:10 smarkoff Exp $
 */
public class WriteExcelTest {

    @Test
    public void writeWorkbook() throws IOException, InvalidFormatException {
        String [] headers = { "col1", "col2", "col3" };

        ArrayList<String []> data = new ArrayList<String []>();
        data.add(new String [] { "row1col1", "row1col2", "row1col3" });
        data.add(new String [] { "row2col1", "row2col2", "row2col3" });
        data.add(new String [] { "\u00b1\u00b6\u00a2", "\u2105\u211E\u2122", "A\u00b1\u2105" });

        SimpleExcelData sed = new SimpleExcelData(headers, data);
        Workbook wb = WriteExcel.writeWorkbook(sed, "sheet1", null);
	    File tempDir = SystemUtils.getJavaIoTmpDir();
        File file = new File(tempDir, "test.xlsx");
        FileOutputStream out = new FileOutputStream(file);
            // throws IOException
        wb.write(out);  // throws IOException
        out.close();

        // Read file back in and compare to original data
	    SimpleExcelData sed2 = ReadExcel.parse(file);
	        // throws IOException, InvalidFormatException
	    assertTrue(StringUtil.equals(sed2.getHeaders(), headers));
	    assertTrue(StringUtil.equals(sed2.getData(), data));

	    file.delete();
    }

}
