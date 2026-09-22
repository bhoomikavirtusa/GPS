package com.wiley.sf.common.excel;

import java.util.List;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Encapsulates data from a simple Excel spreadsheet.
 *
 * @version $Id: SimpleExcelData.java,v 1.3 2010-02-25 01:10:24 smarkoff Exp $
 * @author  Steve Markoff, created 10/17/2006
 */
public class SimpleExcelData {

    private final String [] headers;
    private final List<String []> data;

    /**
     * @param headers  May be null or zero-length
     * @param data     Must be non-null
     */
    public SimpleExcelData(String [] headers, List<String []> data) {
        ArgUtil.notNull(data, "data");
        this.headers = headers;
        this.data = data;
    }

    public String [] getHeaders() { return headers; }
    public List<String []> getData() { return data; }
}
