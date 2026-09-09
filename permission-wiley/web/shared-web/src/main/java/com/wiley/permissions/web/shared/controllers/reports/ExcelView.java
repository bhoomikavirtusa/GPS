package com.wiley.permissions.web.shared.controllers.reports;


import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;




import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFHeader;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;

import com.wiley.sf.common.excel.WriteExcel;

/**
 *
 * @author lnagy
 */
public class ExcelView extends AbstractLegacyExcelView {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ExcelView.class);

	public static final String
		SHEET_NAME = "sheet",
		// the following 3 constants affect the actual header area of the sheet
		// note this area does not show in View - Normal mode, it does show in View - Page Layout mode
		HEADER_LEFT = "headerLeft",
		HEADER_CENTER = "headerCenter",
		HEADER_RIGHT = "headerRight",
		// FIRST_ROW_IS_HEADER does NOT refer to the actual sheet header (as the above constants do)
		FIRST_ROW_IS_HEADER = "firstRowIsHeader",
		AUTO_SIZE_COLS = "autoSizeCols",
		AUTO_SIZE_MAX_WIDTH = "autoSizeMaxWidth",
		AUTO_SIZE_FIRST_ROW = "autoSizeFirstRow",  // 0-based
		START_ROW = "startRow",  // 0-based
		ONE_OFF_CELL_VALUES = "oneOffCellValues",
		FILE_NAME = "fileName";

	@SuppressWarnings("unchecked")
	@Override
	// smarkoff: Spring 3.1 changes the super class (AbstractExcelView) so you can use POI 3.5 which has Workbook
	// as a super class of HSSFWorkbook -- change later
	protected void buildExcelDocument(Map<String, Object> model, HSSFWorkbook workbook,
		HttpServletRequest request, HttpServletResponse response)
	{
		List<Object> rows = (List<Object>) model.get("data");

		// handle situation where workbook is blank or non-blank
		HSSFSheet sheet;
		boolean usingTemplate = false;
		if (workbook.getNumberOfSheets() > 0) {
			sheet = workbook.getSheetAt(0);
			usingTemplate = true;
		}
		else {
            sheet = workbook.createSheet(ExcelView.SHEET_NAME);
		}

		HSSFHeader header = sheet.getHeader();
		String headerLeft = (String) model.get(HEADER_LEFT);
		String headerCenter = (String) model.get(HEADER_CENTER);
		String headerRight = (String) model.get(HEADER_RIGHT);
		if (StringUtils.isNotBlank(headerLeft)) {
			header.setLeft(headerLeft);
		}
		if (StringUtils.isNotBlank(headerCenter)) {
			header.setCenter(headerCenter);
		}
		if (StringUtils.isNotBlank(headerRight)) {
			header.setRight(headerRight);
		}

		// Define Styles (for use when !usingTemplate)
		HSSFCellStyle headerStyle = workbook.createCellStyle();
		HSSFFont headerFont = workbook.createFont();
		headerFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		headerStyle.setFont(headerFont);
		
		//START: Code Addition for Asset Compliance Report Ticket,Added Font style for Title Header
		HSSFCellStyle titleHeaderStyle = workbook.createCellStyle();
		HSSFFont titleHeaderFont = workbook.createFont();
		titleHeaderFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		titleHeaderFont.setFontHeight((short) 250);
		titleHeaderStyle.setFont(titleHeaderFont);
		//END: Code Addition for Asset Compliance Report Ticket
		
		HSSFCellStyle normalStyle = workbook.createCellStyle();
		HSSFFont normalFont = workbook.createFont();
		normalFont.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);
		normalStyle.setFont(normalFont);

		HSSFCellStyle normalWrappedStyle = workbook.createCellStyle();
		normalWrappedStyle.setFont(normalFont);
		normalWrappedStyle.setWrapText(true);

		HSSFCellStyle dateStyle = workbook.createCellStyle();
		CreationHelper createHelper = workbook.getCreationHelper();
		dateStyle.setDataFormat(
		createHelper.createDataFormat().getFormat("mmm-d-yyyy"));
		dateStyle.setFont(normalFont);

		HSSFCellStyle dateStyle2 = workbook.createCellStyle();
		dateStyle2.setDataFormat(
        createHelper.createDataFormat().getFormat("mmm-d-yyyy"));
		dateStyle2.setFont(normalFont);
		short border = 1;
		dateStyle2.setBorderBottom(border);
		dateStyle2.setBorderTop(border);
		dateStyle2.setBorderLeft(border);
		dateStyle2.setBorderRight(border);

		HSSFCellStyle hlinkStyle = workbook.createCellStyle();
		HSSFFont hlinkFont = workbook.createFont();
		hlinkFont.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);
		hlinkFont.setUnderline(HSSFFont.U_SINGLE);
		hlinkFont.setColor(IndexedColors.BLUE.getIndex());
		hlinkStyle.setFont(hlinkFont);
		
		

		Boolean firstRowIsHeader = (Boolean) model.get(FIRST_ROW_IS_HEADER);
		if (firstRowIsHeader == null)  firstRowIsHeader = Boolean.FALSE;

		int nRow = 0;

		Integer startRow = (Integer) model.get(START_ROW);
		if (startRow != null)  nRow = startRow;

		HSSFCellStyle dStyle = normalStyle;
		for (Object row : rows) {
			dStyle = normalStyle;
			List<Object> columns = (List<Object>) row;
			int nColumn = 0;
			boolean doingDate = false;
			for (Object column : columns) {
				HSSFCell cell = getCell(sheet, nRow, nColumn++);
				// handling sub headings
				// subheadings contain the literal "..Subhead" as the first column and it only implies that the
				// a heading style is be applied to the entire row
				if (column != null) {
					if (column instanceof Integer) {
						// ignore
					}
					else {
						//START: Code Addition for Asset Compliance Report Ticket, Added the font style for Title Header
						if (column.toString().equals("..Head")) {
							dStyle = titleHeaderStyle;
							nColumn--;
							continue;
						}
						///END: Code Addition for Asset Compliance Report Ticket, Added the font style for Title Header
						else if (column.toString().equals("..Subhead")) {
							dStyle = headerStyle;
							nColumn--;
							continue;
						}
						else if (column.toString().equals("..WrappedText")) {
							dStyle = normalWrappedStyle;
							nColumn--;
							continue;
						}
						else if (column.toString().equals("..Date")) {
							doingDate = true;
							dStyle = dateStyle;
							nColumn--;
							continue;
						}
						else if (column.toString().equals("..DateBorder")) {
							doingDate = true;
							dStyle = dateStyle2;
							nColumn--;
							continue;
						}
						
						//START: Code Addition for Asset Compliance Report Ticket
						
						else if(column.toString().equals("..Merge")){
							 
							int rowCount=(Integer) request.getAttribute("RowCount");
							log.debug("ROW COUNT= "+rowCount);
							
							
							HSSFRow mergedRow=sheet.createRow(rowCount);
							sheet.addMergedRegion(new CellRangeAddress(rowCount,rowCount,0,4));
							
							HSSFCell mergedCell= mergedRow.createCell(0);
							
							dStyle = headerStyle;
							nColumn--;
							continue;
							
						}
						
						
						
						else if(column.toString().equals("..Merge2")){
							 
							int rowCount=(Integer) request.getAttribute("RowCount2");
							log.debug("ROW COUNT= "+rowCount);
							
							 
							HSSFRow mergedRow=sheet.createRow(rowCount);
							sheet.addMergedRegion(new CellRangeAddress(rowCount,rowCount,0,4));
							
							HSSFCell mergedCell= mergedRow.createCell(0);
							 
							
							dStyle = headerStyle;
							nColumn--;
							continue;
							
						}
						else if(column.toString().equals("..Merge3")){
							 
							int rowCount=(Integer) request.getAttribute("RowCount3");
							log.debug("ROW COUNT= "+rowCount);
							
							HSSFRow mergedRow=sheet.createRow(rowCount);
							
							sheet.addMergedRegion(new CellRangeAddress(rowCount,rowCount,0,4));
							
							HSSFCell mergedCell= mergedRow.createCell(0);
							 
							dStyle = headerStyle;
							nColumn--;
							continue;
							
						}
						
						 

					}
                       
					if (!usingTemplate) {
						if (firstRowIsHeader && nRow == 0) {
							cell.setCellStyle(titleHeaderStyle);
						}
						//END:Code Addition for Asset Compliance Report Ticket
						else {
							cell.setCellStyle(dStyle);
						}
					}

					// Don't call setText for an Integer b/c then in the
					// spreadsheet cell you get a green triangle (warning).
					if (doingDate) {
						if (column instanceof Date) {
							cell.setCellValue((Date) column);
							cell.setCellStyle(dateStyle);
						} else {
							try {
								DateFormat dateFormat = DateFormat.getDateInstance();
								Date dDate = dateFormat.parse(column.toString().trim());
								cell.setCellValue(dDate);
								cell.setCellStyle(dateStyle);
							} catch (Exception ff) {
								setText(cell, column.toString());
								cell.setCellStyle(dStyle);
							}
						}
						doingDate = false;
					} else if (column instanceof Integer) {
						cell.setCellValue((Integer) column);
					}
					else {
						int idx = column.toString().indexOf("http:");
						if (idx > -1 && idx < 4) {
							Hyperlink url_link = createHelper.createHyperlink(HSSFHyperlink.LINK_URL);
							url_link.setAddress(column.toString());
							cell.setCellValue(column.toString());
							cell.setHyperlink(url_link);
							cell.setCellStyle(hlinkStyle);
						} else {
							setText(cell, column.toString());
						}
					}

				} // end if

		  } // end for

			dStyle = normalStyle;
			nRow++;
		} // end for

		ArrayList<OneOffCellValue> oneOffList = (ArrayList<OneOffCellValue>) model.get(ONE_OFF_CELL_VALUES);
		if (oneOffList != null) {
			for (OneOffCellValue oocv: oneOffList) {
		        HSSFCell cell = getCell(sheet, oocv.getRow(), oocv.getColumn());
		        Object value = oocv.getValue();
		        if (value != null) {
		        	if (value instanceof Integer) {
		        		cell.setCellValue((Integer) value);
		        		cell.setCellStyle(normalStyle);
		        	}
		        	else {
		        		cell.setCellValue(String.valueOf(value));
		        	}
		        }
			}
		}

		Integer numCols = (Integer) model.get(AUTO_SIZE_COLS);
		Integer maxWidth = (Integer) model.get(AUTO_SIZE_MAX_WIDTH);
		Integer firstRow = (Integer) model.get(AUTO_SIZE_FIRST_ROW);

		if (numCols != null && numCols > 0) {
			if (firstRow == null)  firstRow = 0;
		    WriteExcel.autoSizeColumns(sheet, numCols, maxWidth, 2, firstRow);
		}

		String fileName = (String) model.get(FILE_NAME);
		if (StringUtils.isNotBlank(fileName)) {
			// default file name that parent AbstractExcelView uses unless
			// either a source file "url" property is specified or we do this is excel.xls
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		}
	}
}
