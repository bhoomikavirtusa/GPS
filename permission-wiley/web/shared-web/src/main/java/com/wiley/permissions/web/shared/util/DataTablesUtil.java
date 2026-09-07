package com.wiley.permissions.web.shared.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class helps to use the server-side API of the DataTables library
 * by providing generic search/filter, sorting, and pagination.
 *
 * TODO: Eventually move to sf-common2 library.
 *
 * @author smarkoff
 */
public class DataTablesUtil {
	private static final Log log = LogFactory.getLog(DataTablesUtil.class);

	/**
	 * Note specifying false for contains is faster.
	 *
	 * @param contains  true means do a "contains" search, false means do a "startsWith" search (when search criteria is involved)
	 *
	 * @return A copy of the JSON data sent (for debugging)
	 */
	public static String sendJSONData(HttpServletRequest request,
			HttpServletResponse response, List<TableCell[]> list, boolean contains) throws IOException
	{
		String output = DataTablesUtil.buildJSONData(request, list, contains);

		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");
		// This would not give the right byte-length if there are any double-byte chars.
		// - works ok without specifying the length so just skip it - could do if needed
		// be creating a byte buffer and then reporting the length of that.
        //response.setContentLength(output.length());
        PrintWriter writer = response.getWriter();  // throws IOException
        writer.write(output);
        return output;
	}

	/**
	 * Note this method currently assumes that the list parameter includes ALL the data
	 * so that filtering and sorting can be done by this method in-memory.
	 * This may NOT be practical for all situations (especially with large data sets)
	 * - this method/class will need to be enhanced (or perhaps an alternative method created)
	 * for situations where the data might be filtered and sorted directly via SQL.
	 * Actually it's more likely that this class cannot handle such a broad situation - will
	 * have to probably defer this to other (less generic) classes calling this one.
	 *
	 * Note specifying false for contains is faster.
	 *
	 * @param contains  true means do a "contains" search, false means do a "startsWith" search (when search criteria is involved)
	 */
	public static String buildJSONData(HttpServletRequest request, List<TableCell[]> list, boolean contains) {
		//log.debug("buildJSONData(): request params: " + ServletUtil.getAllParameters(request));
		String search = request.getParameter("sSearch");
		int totalBeforeFilter = list.size();
		list = filter(request, list, search, contains);

		// If we are using sAjaxSource but not bServerSide then iSortingCols and all
		// other parameters will not be passed - so check for null for all these.

		String iSortingColsString = request.getParameter("iSortingCols");
		if (iSortingColsString != null) {
			int sortColCount = Integer.parseInt(iSortingColsString);
			List<TableSortCriteria> sortList = new ArrayList<TableSortCriteria>(sortColCount);
			for (int i = 0; i < sortColCount; i++) {
				int columnIndex = Integer.parseInt(request.getParameter("iSortCol_" + i));
				boolean ascending = "asc".equals(request.getParameter("sSortDir_" + i));
				sortList.add(new TableSortCriteria(columnIndex, ascending));
			}

			TableRowComparator comparator = new TableRowComparator(sortList);
			Collections.sort(list, comparator);
		}

		// assume that either both iDisplayStart/Length will be set or neither
		String iDisplayStartString = request.getParameter("iDisplayStart");
		String iDisplayLengthString = request.getParameter("iDisplayLength");
		log.debug("buildJSONData(): iDisplayStart/iDisplayLength = " + iDisplayStartString + "/" + iDisplayLengthString);
		int displayStart = 0;
		int displayLength = list.size();
		if (iDisplayStartString != null) {
			displayStart = Integer.parseInt(iDisplayStartString);
			displayLength = Integer.parseInt(iDisplayLengthString);
		}
		String echo = request.getParameter("sEcho");

		StringBuilder sb = new StringBuilder();
		sb.append("{ \"iTotalRecords\" : " + totalBeforeFilter);
		sb.append(",\n\"iTotalDisplayRecords\" : " + list.size());  // filtered total
		if (echo != null) {
			sb.append(",\n\"sEcho\" : " + echo);
		}
		sb.append(",\n\"aaData\" : [\n");
		int stopIndex = displayStart + displayLength;
		if (stopIndex > list.size())  stopIndex = list.size();

		for (int i = displayStart; i < stopIndex; i++) {
			TableCell [] array = list.get(i);
			sb.append("[\"");
			for (int c = 0; c < array.length; c++) {
				TableCell cell = array[c];
				sb.append(cell.getHtml());
				if (c < array.length - 1) sb.append("\", \"");
			}
			sb.append("\"]");
			if (i < stopIndex - 1) sb.append(",\n");
		}
		sb.append("\n]}");

		return sb.toString();
	}

	/**
	 * Search algorithm: All words in the search String must be found
	 * in some searchable cell in a row for the row to be kept.
	 * (If the search string is "big hat" then "big" could be found in one
	 * searchable cell and "hat" could be found in another - this would count
	 * as a row that is included in the search results.
	 */
	private static List<TableCell[]> filter(HttpServletRequest request,
			List<TableCell[]> list, String search, boolean contains)
	{
		if (StringUtils.isBlank(search))  return list;

		//long startTime = System.currentTimeMillis();

		int numColumns = Integer.parseInt(request.getParameter("iColumns"));
		boolean [] searchable = new boolean[numColumns];

		for (int i = 0; i < numColumns; i++) {
			String s = request.getParameter("bSearchable_" + i);
			// If the parameter is absent (null), default to true
			searchable[i] = !("false".equals(s));
		}

		List<TableCell []> newList = new ArrayList<TableCell []>(list.size());
		String [] searchWords = search.split("\\s");
		String [] lowerSearch = new String[searchWords.length];
		for (int i = 0; i < searchWords.length; i++) {
			lowerSearch[i] = searchWords[i].toLowerCase();
		}

		for (TableCell [] array : list) {
			boolean [] found = new boolean [searchWords.length];

			for (int c = 0; c < array.length; c++) {
				TableCell cell = array[c];
				if (searchable[c] && StringUtils.isNotBlank(cell.getData())) {
					String lowerData = cell.getData().toLowerCase();
					for (int i = 0; i < lowerSearch.length; i++) {
						if ((contains && lowerData.contains(lowerSearch[i]))
							|| (!contains && lowerData.startsWith(lowerSearch[i]))) {
							found[i] = true;
						}
					}
				}
			}

			boolean allFound = true;
			for (boolean f: found) {
				if (!f) {
					allFound = false;
					break;
				}
			}
			if (allFound) newList.add(array);
		}

		//long time = System.currentTimeMillis() - startTime;
		//log.debug("filter(): time was " + time + " ms.");

		return newList;
	}
}

class TableSortCriteria {
	private final int columnIndex;
	private final boolean ascending;

	public TableSortCriteria(int columnIndex, boolean ascending) {
		this.columnIndex = columnIndex;
		this.ascending = ascending;
	}

	public int getColumnIndex() { return columnIndex; }
	public boolean isAscending() { return ascending; }
}

class TableRowComparator implements Comparator<TableCell []> {
	private final List<TableSortCriteria> list;

	public TableRowComparator(List<TableSortCriteria> list) {
		this.list = list;
	}

	@Override
	public int compare(TableCell[] row1, TableCell[] row2) {
		for (TableSortCriteria criteria: list) {
			String data1 = row1[criteria.getColumnIndex()].getData();
			String data2 = row2[criteria.getColumnIndex()].getData();
			boolean asc = criteria.isAscending();

			// Will sort nulls to be at the bottom
			if (data1 != null || data2 != null) {
				if (data1 == null)  return asc ? 1 : -1;
				if (data2 == null)  return asc ? -1 : 1;
				int ret = data1.toLowerCase().compareTo(data2.toLowerCase());
				if (ret != 0) return asc ? ret : -ret;
			}
		}

		return 0;
	}
}
