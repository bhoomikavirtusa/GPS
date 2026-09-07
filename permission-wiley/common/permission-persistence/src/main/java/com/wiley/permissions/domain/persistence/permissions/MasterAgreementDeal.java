package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.DomainObject;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "MASTER_AGREEMENT_DEAL")
@Cache (usage = CacheConcurrencyStrategy.READ_WRITE)
public class MasterAgreementDeal
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	@Column(name = "NAME", nullable = false, length = 100)
	private String name;

	@Column(name = "SOURCE_GROUP_ID", nullable = false)
	private int sourceGroupId;

	@Column(name = "FILE_NAME", nullable = false, length = 200)
	private String fileName = null;

	@Column(name = "MIME_TYPE", nullable = false, length = 100)
	private String mimeType = null;

	@Lob
	@Column(name = "FILE_DATA", nullable = false, length = 10111000)
	private byte[] fileData = null;
	
	@Column(name = "NOTES", nullable = true, length = 1000)
	private String notes = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "START_DATE", nullable = false)
	private Date startDate;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "END_DATE", nullable = true)
	private Date endDate;

	@Column(name = "PRICING_INFO", nullable = true, length = 2000)
	private String pricingInfo;

	@Column(name = "GRANT_INFO", nullable = true, length = 2000)
	private String grantInfo;

	@Column(name = "GRANT_DURATION_YEARS", nullable = false)
	private int grantDurationYears = 0;  // 0 means Life of Edition

	@OneToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "MADEAL_2_ULOCATION",
			joinColumns = @JoinColumn(name = "MADEAL_ID"),
			inverseJoinColumns = @JoinColumn(name = "ULOCATION_CODE")
		)
	private List<UserLocation> userLocations;

	@Transient
	private String [] selectedUserLocations;  // for form use

	@OneToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "MADEAL_2_BUSINESSUNIT",
			joinColumns = @JoinColumn(name = "MADEAL_ID"),
			inverseJoinColumns = @JoinColumn(name = "BUSINESSUNIT_CODE")
		)
	private List<BusinessUnit> businessUnits;

	@Transient
	private String [] selectedBusinessUnits;  // for form use

	// Actually this is OneToMany but it's not the usual kind
	@ManyToMany
	@JoinTable(
			name = "MA_DEAL_2_CONDITION",
			joinColumns = @JoinColumn(name = "MA_DEAL_ID"),
			inverseJoinColumns = @JoinColumn(name = "CONDITION_ID"))
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private List<Condition> conditions;


	public MasterAgreementDeal() {
		super();
	}

	public MasterAgreementDeal(int id, String name) {
		this.id = id;
		this.name = name;
	}

	// smarkoff: The attributes marked as XmlElement are the ones I want passed when I do a ObjectToJson transform.
	@XmlElement
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getSourceGroupId() {
		return sourceGroupId;
	}

	public void setSourceGroupId(int id) {
		this.sourceGroupId = id;
	}

	@XmlElement
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public byte[] getFileData() {
		return fileData;
	}

	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}
	
	@XmlElement
	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = StringUtils.trimToNull(notes);
	}
	
	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * Returns true if today's date is between start/end dates
	 * (or after start date and endDate is null).
	 */
	@Transient
	public boolean isActive() {
		Date date = new Date();
		return  getStartDate().getTime() <= date.getTime()
			&& (getEndDate() == null || getEndDate().getTime() >= date.getTime());
	}

	@XmlElement
	public String getPricingInfo() {
		return pricingInfo;
	}

	public void setPricingInfo(String pricingInfo) {
		this.pricingInfo = pricingInfo;
	}

	@XmlElement
	public String getGrantInfo() {
		return grantInfo;
	}

	public void setGrantInfo(String grantInfo) {
		this.grantInfo = grantInfo;
	}

	@XmlElement
	public int getGrantDurationYears() {
		return grantDurationYears;
	}

	public void setGrantDurationYears(int grantDurationYears) {
		this.grantDurationYears = grantDurationYears;
	}

	public List<UserLocation> getUserLocations() {
		return userLocations;
	}

	@Transient
	public String getUserLocationsDisplay() {
		StringBuilder sb = new StringBuilder();
		for (UserLocation ul : getUserLocations()) {
			if (sb.length() > 0) sb.append(", ");
			// use code instead of name since don't have much space
			sb.append(ul.getCode());
		}
		return sb.toString();
	}

	public void setUserLocations(List<UserLocation> userLocations) {
		this.userLocations = userLocations;
	}

	@Transient
	public String [] getSelectedUserLocations() {
		return selectedUserLocations;
	}

	public void setSelectedUserLocations(String [] array) {
		this.selectedUserLocations = array;
	}

	public void setSelectedUserLocations(UserLocation [] array) {
		if (array == null)  selectedUserLocations = null;
		else {
			selectedUserLocations = new String[array.length];
			for (int i = 0; i < array.length; i++) {
				selectedUserLocations[i] = array[i].getCode();
			}
		}
	}

	public void setSelectedUserLocationsFromData() {
		if (userLocations == null)  selectedUserLocations = null;
		else {
			selectedUserLocations = new String[userLocations.size()];
			for (int i = 0; i < userLocations.size(); i++) {
				selectedUserLocations[i] = userLocations.get(i).getCode();
			}
		}
	}

	public void setUserLocationsFromSelected() {
		if (selectedUserLocations == null)  userLocations = null;
		else {
			userLocations = new ArrayList<UserLocation>(selectedUserLocations.length);
			for (String code : selectedUserLocations) {
				userLocations.add(UserLocation.forCode(code));
			}
		}
	}

	public List<BusinessUnit> getBusinessUnits() {
		return businessUnits;
	}

	@Transient
	public String getBusinessUnitsDisplay() {
		StringBuilder sb = new StringBuilder();
		for (BusinessUnit bu : getBusinessUnits()) {
			if (sb.length() > 0) sb.append(", ");
			// currently shortName not in db so check constants to get it
			String shortName = bu.getShortName();
			if (StringUtils.isBlank(shortName)) {
				shortName = BusinessUnit.forCode(bu.getCode()).getShortName();
			}
			sb.append(shortName);
		}
		return sb.toString();
	}

	public void setBusinessUnits(List<BusinessUnit> businessUnits) {
		this.businessUnits = businessUnits;
	}

	@Transient
	public String [] getSelectedBusinessUnits() {
		return selectedBusinessUnits;
	}

	public void setSelectedBusinessUnits(String [] array) {
		this.selectedBusinessUnits = array;
	}

	public void setSelectedBusinessUnits(BusinessUnit [] array) {
		if (array == null)  selectedBusinessUnits = null;
		else {
			selectedBusinessUnits = new String[array.length];
			for (int i = 0; i < array.length; i++) {
				selectedBusinessUnits[i] = array[i].getCode();
			}
		}
	}

	public void setSelectedBusinessUnitsFromData() {
		if (businessUnits == null)  selectedBusinessUnits = null;
		else {
			selectedBusinessUnits = new String[businessUnits.size()];
			for (int i = 0; i < businessUnits.size(); i++) {
				selectedBusinessUnits[i] = businessUnits.get(i).getCode();
			}
		}
	}

	public void setBusinessUnitsFromSelected() {
		if (selectedBusinessUnits == null)  businessUnits = null;
		else {
			businessUnits = new ArrayList<BusinessUnit>(selectedBusinessUnits.length);
			for (String code : selectedBusinessUnits) {
				businessUnits.add(BusinessUnit.forCode(code));
			}
		}
	}

	public List<Condition> getConditions() {
		return conditions;
	}

	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}

	@Transient
	public List<Condition> getConditionsNotNull() {
		if (conditions == null) {
			conditions = new ArrayList<Condition>();
		}
		return conditions;
	}

	/**
	 * We automatically correct the code if only the case is wrong.
	 *
	 * @throws ValidateException
	 */
	public void validate() throws ValidateException {
		return;
	}

	@Override
	public String toString() {
		// use getters due to JPA
		return "id = " + getId()
		    + ", name = " + getName()
		    + ", sourceGroupId = " + getSourceGroupId()
		    + ", startDate = " + getStartDate()
		    + ", endDate = " + getEndDate()
		    ;
	}

	/**
	 * Base on id.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// call getId() since using JPA - not sure if necessary
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());

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
		if (!(obj instanceof MasterAgreementDeal)) return false;
		MasterAgreementDeal other = (MasterAgreementDeal) obj;
		if (getId() == null) {
			if (other.getId() != null) return false;
		}
		else if (!getId().equals(other.getId())) return false;
		return true;
	}
}
