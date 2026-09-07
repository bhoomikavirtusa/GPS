package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.MaterializationKey;

/**
 * @author ttidwell
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "CONTACT")
public class Contact {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@MaterializationKey
	private Integer id = null;

	@Column(name = "FIRST_NAME", nullable = false)
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	private String firstName = null;

	@Column(name = "MIDDLE_NAME", nullable = true)
	private String middleName = null;

	@Column(name = "LAST_NAME", nullable = true)
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	private String lastName = null;

	@Column(name = "JOB_TITLE", nullable = true)
	private String jobTitle = null;

	@Column(name = "GENDER", nullable = true)
	private String gender = null;

	@Column(name = "EMAIL", nullable = true)
	private String email = null;

	@OneToOne (cascade = {
			CascadeType.MERGE,
			CascadeType.PERSIST,
			CascadeType.REFRESH
			})
	@JoinColumn(name = "ADDRESS_ID", nullable = true)
	private Address address = null;

	@OneToOne
	@JoinColumn(name = "SOURCE_ID", nullable = true)
	private Source source = null;

	@Column(name = "PHONE", nullable = true)
	private String phone = null;  // called Office Phone in UI

	@Column(name = "FAX", nullable = true)
	private String fax = null;

	@Column(name = "HOME_PHONE", nullable = true)
	private String homePhoneNumber = null;

	@Column(name = "MOBILE_PHONE", nullable = true)
	private String mobilePhoneNumber = null;

	@Column(name = "NOTE", nullable = true)
	private String note = null;

	@XmlAttribute
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElement
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@XmlElement
	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	@XmlElement
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Transient
	public String getName() {
		return firstName + " " + lastName;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	@XmlElement
	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	@XmlElement
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getFax() {
		return fax;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	@XmlElement
	public String getHomePhoneNumber() {
		return homePhoneNumber;
	}

	public void setHomePhoneNumber(String homePhoneNumber) {
		this.homePhoneNumber = homePhoneNumber;
	}

	@XmlElement
	public String getMobilePhoneNumber() {
		return mobilePhoneNumber;
	}

	public void setMobilePhoneNumber(String mobilePhoneNumber) {
		this.mobilePhoneNumber = mobilePhoneNumber;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	@Transient
	public boolean isBlank() {
		// use getters due to JPA
		return StringUtils.isBlank(getFirstName())
		        && StringUtils.isBlank(getMiddleName())
		        && StringUtils.isBlank(getLastName())
		        && StringUtils.isBlank(getGender())
		        && StringUtils.isBlank(getEmail())
		        && StringUtils.isBlank(getPhone())
		        && StringUtils.isBlank(getHomePhoneNumber())
		        && StringUtils.isBlank(getMobilePhoneNumber());
	}

	@Transient
	public boolean isNotBlank() {
		return !isBlank();
	}
}
