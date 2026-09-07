package com.wiley.permissions.web.internal.controllers.admin;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.SystemNotification;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author nmedrano
 */
public class ManageNotificationForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(ManageNotificationForm.class);

	private SystemNotification systemNotification = null;
	
	private List<SystemNotification> notelist = null;
	
	private Integer selectedId = null;
	
	private String selectedAction = null;

	public ManageNotificationForm() {
	}

	public SystemNotification getSystemNotification() {
		return systemNotification;
	}

	public void setSystemNotification(SystemNotification systemNotification) {
		this.systemNotification = systemNotification;
	}
	
	
	public List<SystemNotification> getNotelist() {
		return notelist;
	}

	public void setNotelist( List<SystemNotification> notelist) {
		this.notelist = notelist;
	}
	
	public Integer getSelectedId() {
		return selectedId;
	}

	public void setSelectedId( Integer selectedId) {
		this.selectedId = selectedId;
	}
	
	public String getSelectedAction() {
		return selectedAction;
	}

	public void setSelectedAction( String selectedAction) {
		this.selectedAction = selectedAction;
	}
	
}
