package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "COMMON_WORK_STATUS")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class CommonWorkStatus
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// These values match what is in the database.
	public static CommonWorkStatus
		IN_PROGRESS = new CommonWorkStatus("in_progress", "In Progress"),
		NO_PERMISSIONS_REQUIRED = new CommonWorkStatus("no_perm_req", "No Permissions Req."),
	    COMPLETE = new CommonWorkStatus("complete", "Complete"),
		COMPLETE_NO_3RD_PARTY_ASSETS = new CommonWorkStatus("complete_no_3rd_prty", "Complete: No Third-Party Assets");

	public static CommonWorkStatus [] ALL_CW_STATUS_ARRAY = {
		IN_PROGRESS, NO_PERMISSIONS_REQUIRED, COMPLETE, COMPLETE_NO_3RD_PARTY_ASSETS
	};

	/**
	 * Throws an exception if the code is not valid.
	 */
	public static CommonWorkStatus getCommonWorkStatusForCode(String code) {
		for (CommonWorkStatus s : ALL_CW_STATUS_ARRAY) {
			if (s.getCode().equals(code)) return s;
		}

		throw new IllegalArgumentException("Invalid Common Work Status code: " + code);
	}

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description = null;

	public CommonWorkStatus() {
		super();
	}

	private CommonWorkStatus(String code, String description) {
		super();
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "code = " + code + ", description = " + description;
	}

	// base hashCode() and equals() on just code

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;

		// special logic to allow matching with String as well as CommonWorkStatus
		//if (obj instanceof String) {
		//	String s = (String) obj;
		//	return StringUtils.equals(code, s);
		//}

		if (obj instanceof CommonWorkStatus) {
			CommonWorkStatus other = (CommonWorkStatus) obj;
			return StringUtils.equals(code, other.getCode());
		}

		return false;
	}
}
