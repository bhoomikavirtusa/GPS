package com.wiley.permissions.web.shared.controllers.reports;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * To be used for the OneOffCellValues feature of ExcelView.
 * 
 * @author smarkoff
 */
public class OneOffCellValue {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(OneOffCellValue.class);

    private int column;
    private int row;
    private Object value;
    
    public OneOffCellValue(int column, int row, Object value) {
    	this.column = column;
    	this.row = row;
    	this.value = value;
    }
    
    public int getColumn() {
    	return column;
    }
    
    public int getRow() {
    	return row;
    }
    
    public Object getValue() {
    	return value;
    }
}
