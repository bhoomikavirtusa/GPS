package com.wiley.permissions.services.datatypes;


public interface IDataType {
	
	// will validate the data
	public boolean valid (String value);

	// will return the data after is formated
	public String format (String value);
}
