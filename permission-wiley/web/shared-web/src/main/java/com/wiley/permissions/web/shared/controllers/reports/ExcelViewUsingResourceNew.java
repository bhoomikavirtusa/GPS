package com.wiley.permissions.web.shared.controllers.reports;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.wiley.sf.common.lang.ResourceUtil;

/**
 *
 * @author smarkoff
 */
public class ExcelViewUsingResourceNew extends ExcelViewNew {
	
	private static final Log log = LogFactory.getLog(ExcelViewUsingResourceNew.class);
	
	private String url;

	@Override
	protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		log.debug("createWorkbook(): entered...");
		log.debug("URL of the Template------>"+url);
		
		XSSFWorkbook workbook=new XSSFWorkbook();
		
		try {
		// In this case we are using the url as a resource path.
		InputStream in = ResourceUtil.getResourceInputStream(getClass(), url);
		    // throws IOException
		workbook= new XSSFWorkbook(in);  // throws IOException
        in.close();  // throws IOException
		}catch(IOException e) {
			e.printStackTrace();
		}
		log.debug("No of WorkBook Sheets-------->"+workbook.getNumberOfSheets());
		
		return workbook;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
