package com.wiley.permissions.common.utils;

/**
 * 
 * @author smarkoff
 */
public class DuplicateAddressTypeException extends PermBaseException {

	private static final long serialVersionUID = 1L;

	private Integer existingId = null;

	
	public DuplicateAddressTypeException(Integer id, boolean justShowExceptionMessage)
	{
		super("Cannot create/update the address because there is an existing address [id: "
		    + id + "] having the same type.", justShowExceptionMessage);
		this.existingId = id;
	}
	
	public Integer getExistingId() {
		return existingId;
	}
	
	public void setExsistingId(Integer existingId) {
		this.existingId = existingId;
	}
}