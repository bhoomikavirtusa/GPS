package com.wiley.sf.common.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Provides methods to help read Excel documents.
 * (Builds upon the Apache POI library.)
 *
 * @since   JDK 1.6, Apache POI 3.7
 * @version $Id: ReadExcel.java,v 1.8 2012-02-24 00:15:58 smarkoff Exp $
 * @author  Steve Markoff, created 4/11/2008
 */
public class ReadExcel {

    /**
     * Parse the given file, which must be an Excel document
     * (either xls or xlsx format), and return the
     * data encapsulated in the first sheet (any other sheets are ignored).
     *
     * @param file  Must be non-null
     */
    public static SimpleExcelData parse(File file)
        throws IOException, InvalidFormatException
    {
        ArgUtil.notNull(file, "file");
        FileInputStream fin = new FileInputStream(file);  // throws IOException

        try {
            return parse(fin);  // throws IOException, InvalidFormatException
        }
        finally {
            IOUtils.closeQuietly(fin);
        }
    }

    /**
     * Parse the given InputStream, which must contain an Excel document
     * (either xls or xlsx format), and
     * return the data encapsulated in the first sheet (any other sheets
     * are ignored). The calling method is responsible for closing the
     * InputStream.
     *
     * @param inStream  Must be non-null
     */
    public static SimpleExcelData parse(InputStream inStream)
        throws IOException, InvalidFormatException
    {
        ArgUtil.notNull(inStream, "inStream");
        Workbook workbook = WorkbookFactory.create(inStream);
            // throws IOException, InvalidFormatException

        Sheet sheet = workbook.getSheetAt(0);

        // Find the first row where the first cell is not blank
        // and assume this is the header row.
        Row row = getFirstNonBlankRow(sheet, 4, 3);
        String [] headers = rowToStringArray(row, 0);

        // Get all the data.
        List<String []> data = rowsToList(sheet, row.getRowNum() + 1,
            headers.length);

        return new SimpleExcelData(headers, data);
    }

    /**
     * Returns the first row that is non-blank. Tests for non-blank by looking
     * only at the first [numCells] of a row. [minNotBlank] cells must be
     * non-blank for the row to be considered non-blank (this logic helps to
     * skip over a title for example).
     * If the last row of the sheet is found without finding a non-blank row,
     * throws a RuntimeException.
     *
     * @param sheet        Must be non-null
     * @param numCells     Must be at least 1
     * @param minNotBlank  Should be at least 0 (but anything under 1 is treated
     *                     the same as 1).
     */
    public static Row getFirstNonBlankRow(Sheet sheet, int numCells,
        int minNotBlank)
    {
        ArgUtil.notNull(sheet, "sheet");
        ArgUtil.notLess1(numCells, "numCells");

        int lastRowNum = sheet.getLastRowNum();

        for (int rowNum = 0; rowNum <= lastRowNum; rowNum++) {
            //System.out.println("getFirstNonBlankRow(): rowNum = " + rowNum);
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                continue;
            }

            int notBlank = 0;

            for (int cellNum = 0; cellNum < numCells; cellNum++) {
                Cell cell = row.getCell(cellNum);
                if (cell != null) {
                    String value = getCellValueAsString(cell);
                    if (value.length() > 0) {
                        notBlank++;
                        if (notBlank >= minNotBlank)  return row;
                    }
                }
            }
        }

        throw new RuntimeException("No non-blank row found.");
    }

    /**
     * Converts each cell in the row to a simple String.
     * If numCells is 0, stops reading the row at the first cell that is
     * null or blank. Otherwise stops reading at numCells.
     *
     * @param row  Must be non-null
     * @param numCells  Should be 0 or greater (but anything under 0 is treated
     *                  the same as 0).
     */
    public static String [] rowToStringArray(Row row, int numCells) {
        ArgUtil.notNull(row, "row");

        int cellNum = 0;
        ArrayList<String> list = new ArrayList<String>();

        while (true) {
            Cell cell = row.getCell(cellNum);

            if (cell == null) {
                if (numCells <= 0)  break;
                else  list.add("");
            }
            else {
                String value = getCellValueAsString(cell);
                if (value.length() > 0) {
                    list.add(value);
                }
                else {
                    if (numCells <= 0)  break;
                    else  list.add("");
                }
            }

            cellNum++;
            if (numCells > 0 && cellNum == numCells)  break;
        }

        String [] sa = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            sa[i] = list.get(i);
        }

        return sa;
    }

    /**
     * Converts a grid of rows and columns to a two dimensional string array.
     * The grid reading is stopped at the first row that is completely blank.
     *
     * @param sheet     Must be non-null
     * @param startRow  Must be at least 0
     * @param numCells  Must be at least 1
     */
    public static ArrayList<String []> rowsToList(Sheet sheet, int startRow,
        int numCells)
    {
        ArgUtil.notNull(sheet, "sheet");
        ArgUtil.notLess0(startRow, "startRow");
        ArgUtil.notLess1(numCells, "numCells");

        int lastRowNum = sheet.getLastRowNum();
        ArrayList<String []> list = new ArrayList<String []>();

        for (int i = startRow; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row == null)  break;
            String [] sa = rowToStringArray(row, numCells);
            if (isBlank(sa))  break;
            else  list.add(sa);
        }

        return list;
    }

    /**
     * Returns true if all the strings of the given String array have
     * length 0. (But all strings must be non-null, and the array itself
     * must be non-null.)
     */
    public static boolean isBlank(String [] stringArray) {
        ArgUtil.notNull(stringArray, "stringArray");

        for (int i = 0; i < stringArray.length; i++) {
            if (stringArray[i].length() > 0)  return false;
        }

        return true;
    }

    /**
     * Convert the given cell's value to a string (whether it is already
     * a string or not) and if it's already a string, trim the value before
     * returning. It's surprising there is not already an API like this in
     * POI but I can't find one.
     *
     * @param cell  Must be non-null
     */
    public static String getCellValueAsString(Cell cell) {
        ArgUtil.notNull(cell, "cell");

        // There are some issues with knowing how to format dates and currency,
        // but we do the best we can.

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        DecimalFormat decFormat = new DecimalFormat("#,###.##");

        int type = cell.getCellType();

        switch (type) {
            case Cell.CELL_TYPE_STRING:
                return cell.getRichStringCellValue().getString().trim();
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return dateFormat.format(date);
                }
                else {
                    double d = cell.getNumericCellValue();
                    return decFormat.format(d);
                }
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_BLANK:
                return "";
            case Cell.CELL_TYPE_FORMULA:
                return cell.getCellFormula();
            case Cell.CELL_TYPE_ERROR:
                return String.valueOf(cell.getErrorCellValue());
            default:
                return "(unknown cell type)";
        }
    }

    /**
     * There is no reason to create an instance of this class since all methods
     * are static.
     */
    private ReadExcel() { }
}
