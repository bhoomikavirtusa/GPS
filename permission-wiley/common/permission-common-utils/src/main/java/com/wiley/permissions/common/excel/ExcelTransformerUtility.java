package com.wiley.permissions.common.excel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import net.sf.jxls.reader.ReaderBuilder;
import net.sf.jxls.reader.ReaderConfig;
import net.sf.jxls.reader.XLSReadMessage;
import net.sf.jxls.reader.XLSReadStatus;
import net.sf.jxls.reader.XLSReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.xml.sax.SAXException;

import com.wiley.permissions.common.utils.ValidateException;

public class ExcelTransformerUtility {

	private static final Log log = LogFactory.getLog(ExcelTransformerUtility.class);

	/**
	 * The list of beans that will be used to read the data in, is part of the MashMap
	 * ex: map.put("assets", new ListArray<Asset> ())
	 * map.get("inputMapping") is the XML that defines the mapping between the excel and bean definitions
	 * map.get("inputStream") is the excel file to be mapped
	 * @param map
	 * @return the status of reading the excel file (will have all errors)
	 * @throws IOException
	 * @throws SAXException
	 */
	public XLSReadStatus transformExcelToBeans(Map<String, Object> map) throws IOException, SAXException
	{
		InputStream inputXls = null;
		try {
			ReaderConfig.getInstance().setSkipErrors(true);

			InputStream inputXml = new ByteArrayInputStream((byte[]) map.get("inputMapping"));
			XLSReader mainReader = ReaderBuilder.buildFromXML(inputXml);


			inputXls = new ByteArrayInputStream((byte[]) map.get("inputStream"));
			XLSReadStatus readStatus = null;
			try {
				readStatus = mainReader.read(inputXls, map);
			} catch (InvalidFormatException e) {
				log.debug("transformExcelToBeans(): error " + e.getMessage());
			}

			for (XLSReadMessage message : (List<XLSReadMessage>) readStatus.getReadMessages()) {
				log.debug("transformExcelToBeans(): error " + message.getMessage());
			}

			return readStatus;
		} finally {
			try {
				inputXls.close();
			} catch (Exception e) {}
		}
	}

	//public static String[] validate (byte[] input) throws ValidateException {
	public static void validate (byte[] input) throws ValidateException {
		InputStream validateXls = null;
		try {
			validateXls = new ByteArrayInputStream(input);
			// test if name is Sheet1
			boolean found = false;
			Workbook workbook = WorkbookFactory.create(validateXls);
			//Sheet hssfSheet = null;
			for (int sheetNo = 0; sheetNo < workbook.getNumberOfSheets(); sheetNo++) {
				String sheetName = workbook.getSheetName(sheetNo);
				//if (sheetName.equalsIgnoreCase("sheet1")) {
				if (sheetName.equalsIgnoreCase("Asset List")) { //Changes made for the new spreadsheet upload requirement DM-289
					//hssfSheet = workbook.getSheetAt(sheetNo);
					found = true;
					break;
				}
			}
			if (!found) {
				//log.debug ("validate: throw exception ... no sheet1 found ");
				log.debug ("validate: throw exception ... no worksheet 'Asset List' - found ");
				throw new ValidateException("There is no \"Asset List\" worksheet. Please make sure first worksheet is named \"Asset List\".");
			}
			//Commented below code for the new spreadsheet upload requirement DM-289
			/*Iterator<Row> rowIterator = hssfSheet.rowIterator();
			List<String> columns = new ArrayList<String>();
			try {
				Row hssfRow = rowIterator.next();
				Iterator<Cell> iterator = hssfRow.cellIterator();
				while (iterator.hasNext()) {
					Cell hssfCell = iterator.next();
					switch (hssfCell.getCellType ())
					{
						case Cell.CELL_TYPE_NUMERIC :
						{
							// cell type numeric.
							columns.add("" + (int)hssfCell.getNumericCellValue());
							break;
						}
						case Cell.CELL_TYPE_STRING :
						{
							// cell type string.
							columns.add(hssfCell.getRichStringCellValue().toString());
							break;
						}
					}
				}
				String[] strarray = new String[columns.size()];
				return columns.toArray(strarray);
			} catch (NoSuchElementException ex) {
				log.debug ("validate(): no rows found");
				throw new ValidateException("There are no rows in \"Sheet1\" worksheet.");
			}*/
		}
		catch (IOException e) {
			throw new ValidateException (e.getMessage());
		}
		catch (InvalidFormatException e)
		{
			throw new ValidateException (e.getMessage());
		} finally {
			try {
				validateXls.close();
			} catch (Exception e) {}
		}
	}
}
