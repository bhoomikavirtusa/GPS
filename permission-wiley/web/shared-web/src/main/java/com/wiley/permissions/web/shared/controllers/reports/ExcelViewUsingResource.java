package com.wiley.permissions.web.shared.controllers.reports;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.wiley.sf.common.lang.ResourceUtil;

/**
 *
 * @author smarkoff
 */
public class ExcelViewUsingResource extends ExcelView {

	private static final Log log = LogFactory.getLog(ExcelViewUsingResource.class);

	@Override
	protected HSSFWorkbook getTemplateSource(String url, HttpServletRequest request) throws Exception {
		log.debug("getTemplateSource(): entered...");
		
		// In this case we are using the url as a resource path.
		InputStream in = ResourceUtil.getResourceInputStream(getClass(), url);
		    // throws IOException
        HSSFWorkbook workbook = new HSSFWorkbook(in);  // throws IOException
        in.close();  // throws IOException
        
        return workbook;
	}
}
