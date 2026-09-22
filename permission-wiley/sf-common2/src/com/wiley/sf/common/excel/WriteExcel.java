package com.wiley.sf.common.excel;

import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Provides methods to help write/edit Excel documents.
 * (Builds upon the Apache POI library.)
 *
 * @since   JDK 1.6, Apache POI 3.9
 * @version $Id: WriteExcel.java,v 1.13 2013-11-07 23:05:08 smarkoff Exp $
 * @author  Steve Markoff, created 10/17/2006
 */
public class WriteExcel {

    /**
     * Create a Workbook (in xlsx format) with a single Sheet having the given name
     * and fill it with the given data. The header row will be in bold
     * (if there is header data). If there is no header data, then the
     * first row will be left blank.
     *
     * @param data          Must be non-null
     * @param sheetName     Must be non-blank
     * @param columnWidths  May be null (in which case all columns are default width)
     */
    public static Workbook writeWorkbook(SimpleExcelData data,
        String sheetName, int [] columnWidths)
    {
        return writeWorkbook(data, sheetName, columnWidths, true);
    }

    /**
     * Create a Workbook with a single Sheet having the given name
     * and fill it with the given data. The header row will be in bold
     * (if there is header data). If there is no header data, then the
     * first row will be left blank.
     *
     * @param data          Must be non-null
     * @param sheetName     Must be non-blank
     * @param columnWidths  May be null (in which case all columns are default width)
     * @param xlsxFormat    false means use the older xls (1997-2003) format
     */
    public static Workbook writeWorkbook(SimpleExcelData data,
        String sheetName, int [] columnWidths, boolean xlsxFormat)
    {
        ArgUtil.notNull(data, "data");
        ArgUtil.notBlank(sheetName, "sheetName");

        Workbook wb = xlsxFormat ? new XSSFWorkbook() : new HSSFWorkbook();
        Sheet sheet = wb.createSheet(sheetName);
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setFont(font);
        writeSheet(data, sheet, style, columnWidths);
        return wb;
    }

    /**
     * The input sheet is assumed to be blank. It will be filled with the
     * given data.
     * The given style will be used for the header cells (if there is a
     * header row), otherwise no particular styles will be applied.
     * If there is no header data, then the first row will be left blank.
     *
     * @param excelData     Must be non-null
     * @param sheet         Must be non-null
     * @param headerStyle   If null, defaults to no particular style
     * @param columnWidths  May be null (in which case all columns are default width)
     */
    public static void writeSheet(SimpleExcelData excelData,
        Sheet sheet, CellStyle headerStyle, int [] columnWidths)
    {
        ArgUtil.notNull(excelData, "excelData");
        ArgUtil.notNull(sheet, "sheet");

        if (columnWidths != null) {
            setColumnWidths(sheet, columnWidths);
        }

        // Do the header row
        String [] headerArray = excelData.getHeaders();
        if (headerArray != null && headerArray.length > 0) {
            doHeaderRow(headerArray, sheet, headerStyle);
        }

        // need CreationHelper to create RichTextString below
        CreationHelper creationHelper = sheet.getWorkbook().getCreationHelper();

        // Do the other rows
        List<String []> data = excelData.getData();
        for (int i = 0; i < data.size(); i++) {
            String [] rowData = data.get(i);
            Row row = sheet.createRow(i + 1);
            for (int c = 0; c < rowData.length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(creationHelper.createRichTextString(rowData[c]));
            }
        }
    }

    private static void doHeaderRow(String [] headerArray,
        Sheet sheet, CellStyle headerStyle)
    {
        Row row = sheet.createRow(0);
        // need CreationHelper to create RichTextString below
        CreationHelper creationHelper = sheet.getWorkbook().getCreationHelper();

        for (int i = 0; i < headerArray.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(creationHelper.createRichTextString(headerArray[i]));
            if (headerStyle != null)  cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Set columns widths to fixed values.
     * (For autoSize, use Sheet.autoSizeColumn().)
     *
     * @param sheet   The sheet to set column widths for
     * @param widths  The widths to use, specified as number of characters
     */
    public static void setColumnWidths(Sheet sheet, int [] widths) {
        ArgUtil.notNull(sheet, "sheet");
        ArgUtil.notNull(widths, "widths");

        for (int i = 0; i < widths.length; i++) {
            // column width unit is 1/256th of character
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    /**
     * Auto-size the first [numCols] of the sheet, providing a maximum width.
     * Minimum width is 2.
     *
     * @param sheet  The sheet to auto-size the columns for
     * @param numCols  The number of columns (starting from the left) to auto-size
     * @param maxWidth  Don't go wider than this number of characters, 0 means unlimited
     */
    public static void autoSizeColumns(Sheet sheet, int numCols, int maxWidth) {
        ArgUtil.notNull(sheet, "sheet");
        ArgUtil.notLess1(numCols, "numCols");
        ArgUtil.notLess0(maxWidth, "maxWidth");

        autoSizeColumns(sheet, numCols, maxWidth, 2);
    }

    /**
     * Auto-size the first [numCols] of the sheet, providing a maximum and minimum width.
     *
     * @param sheet  The sheet to auto-size the columns for
     * @param numCols  The number of columns (starting from the left) to auto-size
     * @param maxWidth  Don't go wider than this number of characters, 0 means unlimited
     * @param minWidth  Don't go narrower than this number of characters (0 smallest value allowed)
     */
    public static void autoSizeColumns(Sheet sheet, int numCols, int maxWidth, int minWidth) {
        ArgUtil.notNull(sheet, "sheet");
        ArgUtil.notLess1(numCols, "numCols");
        ArgUtil.notLess0(maxWidth, "maxWidth");
        ArgUtil.notLess0(minWidth, "minWidth");

        autoSizeColumns(sheet, numCols, maxWidth, minWidth, 0);
    }

    /**
     * Auto-size the first [numCols] of the sheet, providing a maximum and minimum width,
     * and first row to look at.
     *
     * @param sheet  The sheet to auto-size the columns for
     * @param numCols  The number of columns (starting from the left) to auto-size
     * @param maxWidth  Don't go wider than this number of characters, 0 means unlimited
     * @param minWidth  Don't go narrower than this number of characters (0 smallest value allowed)
     * @param firstRow  0-based index of the first row to consider (inclusive)
     */
    public static void autoSizeColumns(Sheet sheet, int numCols, int maxWidth, int minWidth, int firstRow) {
        ArgUtil.notNull(sheet, "sheet");
        ArgUtil.notLess1(numCols, "numCols");
        ArgUtil.notLess0(maxWidth, "maxWidth");
        ArgUtil.notLess0(minWidth, "minWidth");
        ArgUtil.notLess0(firstRow, "firstRow");

        final int maxWidthUnits = maxWidth * 256;
        final int minWidthUnits = minWidth * 256;

        for (int i = 0; i < numCols; i++) {
            //sheet.autoSizeColumn(i);
            autoSizeColumn(sheet, i, false, firstRow, sheet.getLastRowNum());
            int size = sheet.getColumnWidth(i);
            if (size > maxWidthUnits && maxWidth > 0) {
                sheet.setColumnWidth(i, maxWidthUnits);
            }
            else if (size < minWidthUnits) {
                sheet.setColumnWidth(i, minWidthUnits);
            }
        }
    }

    /**
     * This is similar to Sheet.autoSizeColumn but the POI library doesn't have a version of
     * this method that takes firstRow and lastRow parameters.
     *
     * @param sheet  the sheet to calculate
     * @param column  0-based index of the column
     * @param useMergedCells  whether to use merged cells
     * @param firstRow  0-based index of the first row to consider (inclusive)
     * @param lastRow  0-based index of the last row to consider (inclusive)
     */
    public static void autoSizeColumn(Sheet sheet, int column, boolean useMergedCells, int firstRow, int lastRow) {
        // This is a source code copy of Sheet.autoSizeColumn(column, useMergedCells) from POI 3.9
        // but I added the firstRow/lastRow parameters (which SheetUtil.getColumnWidth can take).
        double width = SheetUtil.getColumnWidth(sheet, column, useMergedCells, firstRow, lastRow);

        if (width != -1) {
            width *= 256;
            int maxColumnWidth = 255 * 256; // The maximum column width for an individual cell is 255 characters
            if (width > maxColumnWidth) {
                width = maxColumnWidth;
            }
            sheet.setColumnWidth(column, (int) (width));
        }
    }

    /**
     * There is no reason to create an instance of this class since all
     * methods are static.
     */
    private WriteExcel() { }
}
