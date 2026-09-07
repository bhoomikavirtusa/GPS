package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "ROYALTY_FREE_DEAL")
@Cache (usage = CacheConcurrencyStrategy.READ_WRITE)
public class RoyaltyFreeDeal
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false)
	@MaterializationKey
	private Integer id;

	@Column(name = "SOURCE_GROUP_ID", nullable = false)
	private Integer sourceGroupId;


	@Column(name = "SEATS", nullable = false)
	private Integer seats;

	@Column(name = "TOTAL_PRINT_RUN", nullable = false)
	private Integer totalPrintRun;


	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description;


	@Column(name = "DISABLED_FLAG", nullable = false)
	private boolean disabledFlag = false;

	/* Sandhya adding new column*/
	@Column(name = "LICENSE_FLAG", nullable = false)
	private boolean licenseFlag = true;

	public boolean isLicenseFlag() {
		return licenseFlag;
	}

	public void setLicenseFlag(boolean licenseFlag) {
		this.licenseFlag = licenseFlag;
	}

/* Sandhya end */

	@Transient
	private boolean isBeingUsed = false;

	public RoyaltyFreeDeal() {
		super();
	}

	public RoyaltyFreeDeal(Integer id, String description, Integer sourceGroupId) {
		this.id = id;
		this.description = description;
		this.sourceGroupId =  sourceGroupId;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getSourceGroupId() {
		return sourceGroupId;
	}

	public void setSourceGroupId(Integer id) {
		this.sourceGroupId = id;
	}

	public Integer getSeats() {
		return seats;
	}

	public void setSeats(Integer seats) {
		this.seats = seats;
	}

	public Integer getTotalPrintRun() {
		return totalPrintRun;
	}

	public void setTotalPrintRun(Integer totalPrintRun) {
		this.totalPrintRun = totalPrintRun;
	}

	public boolean getDisabledFlag() {
		return disabledFlag;
	}

	public void setDisabledFlag(boolean disabledFlag) {
		this.disabledFlag = disabledFlag;
	}

	public boolean isdisabled() {
		return disabledFlag;
	}

	// this is a transient set by a controller after inspecting SourceRepository
	public boolean getIsBeingUsed() {
		return isBeingUsed;
	}

	// this is a transient set by a controller after inspecting SourceRepository
	public void setIsBeingUsed(boolean isBeingUsed) {
		this.isBeingUsed = isBeingUsed;
	}


	/**
	 * We automatically correct the code if only the case is wrong.
	 * (Actually this method does nothing right now.)
	 *
	 * @throws ValidateException
	 */
	public void validate() throws ValidateException {
		return;
	}

	@Override
	public String toString() {
		return "id = " + id +
			"sourceGrop = " + sourceGroupId +
			", seats = " + seats +
			", totalPrintRun = " + totalPrintRun +
		    ", description = " + description +
		    ", disabled Flag = " + disabledFlag
		    ;
	}

	/**
	 * Base on id.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/**
	 * Base on id.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof RoyaltyFreeDeal)) return false;
		RoyaltyFreeDeal other = (RoyaltyFreeDeal) obj;
		if (id == null) {
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		return true;
	}
}
