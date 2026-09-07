package com.wiley.permissions.web.shared.controllers;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author ttidwell
 */
public abstract class BaseFormBean
implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private final static Log log = LogFactory.getLog(BaseFormBean.class);

	/**
	 * Some simple mode constants for forms to use, if needed.
	 * 
	 * Consider all these constants deprecated - we used this stuff before
	 * we had annotated controllers - with the annotated controllers there is
	 * no need for FormMode.
	 */
	public enum FormMode {
		VIEW,
		SEARCH,
		ADD,
		MODIFY,
		REMOVE,
		CANCEL,
		ADD_CHILD,
		MODIFY_CHILD,
		REMOVE_CHILD,
		CANCEL_CHILD,
		ADD_ALT_CHILD,
		EDIT_ALT_CHILD,
		MODIFY_ALT_CHILD,
		REMOVE_ALT_CHILD,
		DISABLE_ALT_CHILD,
		CANCEL_ALT_CHILD,
		ADD_FILE,
		REMOVE_FILE,
		CREATE_CHILD,
		APPLY_TO_MEMBERS
	}

	private FormMode mode = null;
	private boolean change = false;
	private String cameFrom = null;

	public FormMode getMode() {
		return mode;
	}

	public void setMode(FormMode mode) {
		this.mode = mode;
	}

	public boolean isChange() {
		return change;
	}

	public void setChange(boolean change) {
		this.change = change;
	}
	
	public String getCameFrom() {
		return cameFrom;
	}
	
	public void setCameFrom(String cameFrom) {
		// TODO: Figure out why sometimes get "createAsset,createAsset"
		// and remove this terrible hack
		if (cameFrom != null) {
		    int index = cameFrom.indexOf(',');
		    if (index != -1) {
		    	log.debug("cameFrom = " + cameFrom + " -- removing comma onwards");
		    	cameFrom = cameFrom.substring(0, index);
		    }
		}
		this.cameFrom = cameFrom;
	}
	
	@Override
	public String toString() {
		return "mode = " + mode
		    + ", change = " + change
		    + ", cameFrom = " + cameFrom;
	}
}
