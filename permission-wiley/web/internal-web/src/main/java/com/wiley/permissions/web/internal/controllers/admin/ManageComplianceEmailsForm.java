package com.wiley.permissions.web.internal.controllers.admin;

import java.util.List;

import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author nmedrano
 */
public class ManageComplianceEmailsForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	private String selectedCode = null;
	List<BusinessUnit> businessUnits;
	BusinessUnit currentBusinessUnit;

	// Drop down information
	List<Role> roles;

	public ManageComplianceEmailsForm() {

	}

	public String getSelectedCode() {
		return selectedCode;
	}

	public void setSelectedCode(String selectedCode) {
		this.selectedCode = selectedCode;
	}

	public BusinessUnit getCurrentBusinessUnit() {
		return currentBusinessUnit;
	}

	public void setCurrentBusinessUnit(BusinessUnit currentBusinessUnit) {
		this.currentBusinessUnit = currentBusinessUnit;
	}

	public List<BusinessUnit> getBusinessUnits() {
		return businessUnits;
	}

	public void setBusinessUnits(List<BusinessUnit> businessUnits) {
		this.businessUnits = businessUnits;
	}

	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}
}
