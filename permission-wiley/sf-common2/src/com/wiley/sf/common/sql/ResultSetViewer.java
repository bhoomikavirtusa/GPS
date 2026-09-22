package com.wiley.sf.common.sql;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import com.wiley.sf.common.io.FileUtil;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.StringUtil;

/**
 * Provides methods to help view result sets.
 *
 * @since   JDK 1.6
 * @version 4/4/2011
 * @author  Steve Markoff
 */
public class ResultSetViewer {

    private final ColumnMetaData[] colMetaDataArray;
    private final ArrayList<String[]> dataList = new ArrayList<String[]>();

    /**
     * Reads the given ResultSet and stores the data internally. Note that if
     * you want to read the result set again after calling this method, you will
     * need to call first() on the ResultSet (and the ResultSet type must be
     * something other than TYPE_FORWARD_ONLY).
     *
     * @param resultSet Must be non-null
     */
    public ResultSetViewer(ResultSet resultSet) throws SQLException {
        ArgUtil.notNull(resultSet, "resultSet");

        ResultSetMetaData meta = resultSet.getMetaData();
        int numCols = meta.getColumnCount();
        colMetaDataArray = new ColumnMetaData[numCols];

        for (int i = 0; i < numCols; i++) {
            String name = meta.getColumnName(i + 1);
            String type = meta.getColumnTypeName(i + 1);
            String nullable = "(unknown)";

            switch (meta.isNullable(i + 1)) {
                case ResultSetMetaData.columnNoNulls:
                    nullable = "false";
                    break;
                case ResultSetMetaData.columnNullable:
                    nullable = "true";
                    break;
                default:
                    // leave as "(unknown)"
            }

            switch (meta.getColumnType(i + 1)) {
                case Types.VARCHAR:
                case Types.LONGVARCHAR:
                case Types.INTEGER:
                case Types.BIGINT:
                case Types.SMALLINT:
                case Types.TINYINT:
                    type += "(" + meta.getPrecision(i + 1) + ")";
                    break;

                case Types.FLOAT:
                case Types.DOUBLE:
                case Types.NUMERIC:
                    type += "(" + meta.getPrecision(i + 1) + ","
                            + meta.getScale(i + 1) + ")";
                    break;

                default:
                    // leave type alone
            }

            colMetaDataArray[i] = new ColumnMetaData(name, type, nullable);
        } // end for

        while (resultSet.next()) {
            String[] data = new String[numCols];

            for (int i = 0; i < numCols; i++) {
                data[i] = resultSet.getString(i + 1);
            }

            dataList.add(data);
        }
    }

    public void createTabDelimitedView(File file, boolean includeColumnType) throws IOException {
        String s = createTabDelimitedView(includeColumnType);
        FileUtil.stringToFile(s, file);  // throws IOException
    }

    public String createTabDelimitedView(boolean includeColumnType) {
        StringBuilder sb = new StringBuilder();
        final String newLine = StringUtil.getLineSeparator();

        for (int m = 0; m < colMetaDataArray.length; m++) {
            ColumnMetaData meta = colMetaDataArray[m];
            sb.append(meta.getName());
            if (includeColumnType) {
                sb.append(':');
                sb.append(meta.getType());
            }
            if (m < colMetaDataArray.length - 1) {
                sb.append('\t');
            }
        }
        sb.append(newLine);

        for (int i = 0; i < dataList.size(); i++) {
            String [] data = dataList.get(i);

            for (int c = 0; c < data.length; c++) {
                sb.append(data[c]);
                if (c < data.length - 1) {
                    sb.append('\t');
                }
            }

            sb.append(newLine);
        }

        return sb.toString();
    }

    public String createHtmlTableView(boolean includeColumnType) {
        StringBuilder sb = new StringBuilder();

        final String newLine = StringUtil.getLineSeparator();

        sb.append("<table>");
        sb.append(newLine);
        sb.append("<tr>");
        sb.append(newLine);

        sb.append("<th>Row</th>");
        sb.append(newLine);

        for (int i = 0; i < colMetaDataArray.length; i++) {
            sb.append("<th>");
            sb.append(colMetaDataArray[i].getName());
            if (includeColumnType) {
                sb.append(" : ");
                sb.append(colMetaDataArray[i].getType());
            }
            sb.append("</th>");
            sb.append(newLine);
        }

        sb.append("</tr>");
        sb.append(newLine);

        for (int i = 0; i < dataList.size(); i++) {
            sb.append("<tr>");
            sb.append(newLine);

            sb.append("<td>");
            sb.append(i + 1);
            sb.append("</td>");
            sb.append(newLine);

            String[] data = dataList.get(i);

            for (int c = 0; c < colMetaDataArray.length; c++) {
                sb.append("<td>");
                sb.append(data[c]);
                sb.append("</td>");
                sb.append(newLine);
            }

            sb.append("</tr>");
            sb.append(newLine);
        }

        sb.append("</table>");

        return sb.toString();
    }

    /**
     *
    * @param file             Must be non-null
    * @param htmlBodyContent  Should be non-null
    * @param title            Should be non-null
    * @param cssStyle         Null means use the default
    */
    public static void createHtmlShell(File file, String htmlBodyContent, String title, String cssStyle) throws IOException {
        String s = createHtmlShell(htmlBodyContent, title, cssStyle);
        FileUtil.stringToFile(s, file);  // throws IOException
    }

    /**
     *
     * @param htmlBodyContent  Should be non-null
     * @param title            Should be non-null
     * @param cssStyle         Null means use the default
     */
    public static String createHtmlShell(String htmlBodyContent, String title, String cssStyle) {
        final String newLine = StringUtil.getLineSeparator();
        final String defaultStyle = "body {" + newLine
            + "font-family: sans-serif;" + newLine
            + "}" + newLine;
        if (cssStyle == null) cssStyle = defaultStyle;

        StringBuilder sb = new StringBuilder(htmlBodyContent.length());
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
                + newLine + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" + newLine);
        sb.append("<html>" + newLine);
        sb.append("<head>" + newLine);
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />" + newLine);
        sb.append("<title>" + title + "</title>" + newLine);
        sb.append("<style type=\"text/css\">" + newLine);
        sb.append(cssStyle);
        sb.append("</style>" + newLine);
        sb.append("</head>" + newLine);
        sb.append("<body>" + newLine);
        sb.append("<h1 class=\"title\">" + title + "</h1>" + newLine);
        sb.append(htmlBodyContent);
        sb.append("</body>" + newLine);
        sb.append("</html>" + newLine);
        return sb.toString();
    }
}

class ColumnMetaData {
    private final String name;
    private final String type;
    private final String nullable;

    /**
     * @param name  Must be non-null and non-blank
     * @param type  Must be non-null and non-blank
     * @param nullable  Must be non-null and non-blank
     */
    public ColumnMetaData(String name, String type, String nullable) {
        ArgUtil.notBlank(name, "name");
        ArgUtil.notBlank(type, "type");
        ArgUtil.notBlank(nullable, "nullable");

        this.name = name;
        this.type = type;
        this.nullable = nullable;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public String getNullable() { return nullable; }
}
