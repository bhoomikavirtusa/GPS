package com.wiley.permissions.web.shared.controllers.sources;

import com.wiley.permissions.web.shared.controllers.BaseFormBean;



/**
 *
 * @author ttidwell
 */
public class SourceMainForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;
	
	private String selectedSourceExtId = null;

	public SourceMainForm() {

	}

	public String getSelectedSourceExtId() {
		return selectedSourceExtId;
	}

	public void setSelectedSourceExtId(String selectedSourceExtId) {
		this.selectedSourceExtId = selectedSourceExtId;
	}
	
	@Override
	public String toString() {
		return super.toString()
		    + ", selectedSourceExtId = " + selectedSourceExtId;
	}
}
