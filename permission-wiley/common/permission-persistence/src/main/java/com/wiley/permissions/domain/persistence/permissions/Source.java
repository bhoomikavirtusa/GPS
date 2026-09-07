package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.PrePersist;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "SOURCE")
@SqlResultSetMappings({
// scalarExternalId is for SourceService.loadSourceListWithDisabledFlag()
@SqlResultSetMapping(name = "scalarExternalId", columns = @ColumnResult(name = "external_id")) })
@NamedNativeQueries({
@NamedNativeQuery(	name = "Source.findSourceByNameOrExternalId",
					query = "select s.* from source s where s.name = ? or s.external_id=?",
					resultClass=Source.class),
@NamedNativeQuery(	name = "Source.findSourcesInSameSourceGroup",
					query = "select * from source where id=? union select * from source where source_group = (select source_group from source where id=?)",
					resultClass=Source.class)

})
public class Source
        extends DomainObject
        implements Comparable<Source> {
	private static final long serialVersionUID = 1L;

	public static final int MAX_NAME_LENGTH = 200;

	public static final int WILEY_ID = 1;
	public static final String WILEY_EXT_ID = "perm.source.1";

	private final static Log log = LogFactory.getLog(Source.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@MaterializationKey
	private Integer id;

	@Column(name = "EXTERNAL_ID", nullable = false, unique = true)
	@MaterializationKey
	private String externalId;

	@Column(name = "NOFLY", nullable = false)
	private boolean nofly = false;

	@Column(name = "PERMISSION_REQUEST_URL", nullable = true)
	private String permissionRequestUrl;

	// lnagy - I added a Transient method getExternalName but the column is actually mapped to "name"
	// field, the method is just a help method to return the truncated value
	@NotNull
	@Size(max = 200)
	@Column(name = "NAME", nullable = false, length = 200)
	@MaterializationKey
	private String name;

	@NotNull
	@Size(max = 200)
	@Column(name = "DISPLAY_NAME", nullable = false, length = 200)
	private String displayName;

	@Column(name = "CREDIT_LINE", nullable = true, length = 1000)
	private String creditLine;

	@Column(name = "NOFLY_PHOTOGRAPHER_LAST_NAME", nullable = true, length = 50)
	private String noFlyPhotographerLastName;

	@Column(name = "COMMENT", length = 1000)
	private String comment;

	@Column(name = "SUED_WILEY")
	private boolean suedWiley = false;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "COUNTRY_CODE", nullable = true)
	private Country country;

	@Column(name = "PHONE", nullable = true)
	private String phoneNumber;

	@Column(name = "FAX", nullable = true)
	private String faxNumber;

	@Column(name = "WEBSITE", nullable = true, length = 200)
	private String website;

	@Column(name = "JDE_VENDOR_NUMBER", nullable = true, length = 8)
	private String jdeVendorNumber;
	
	//new column added for REQ0376879 by Santhosh
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "LAST_UPDATED_DATE")
	private Date lastUpdatedDate = null;

	@Column(name = "PERMISSION_TYPE", nullable = false)
	// lnagy - default to Permission Form (based on what James said)
	private String permissionType = PermissionType.REUSE_FORM.getCode();

	// @Column(name = "SOURCE_GROUP", nullable = true, unique = false)
	@OneToOne
	@JoinColumn(name = "SOURCE_GROUP")
	private SourceGroup sourceGroup;

	// @ManyToOne(fetch = FetchType.LAZY) <-- does not work well with the CMS services part
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "DELIVERY_METHOD")
	private DeliveryMethod deliveryMethod;

	@ManyToMany
	@JoinTable(name = "ASSET_2_SOURCE",
	        joinColumns = @JoinColumn(name = "SOURCE_ID"),
	        inverseJoinColumns = @JoinColumn(name = "ASSET_ID"))
	private Set<Asset> assets = new HashSet<Asset>();

	@OneToMany(cascade = { CascadeType.REFRESH, CascadeType.DETACH }, fetch = FetchType.LAZY, mappedBy = "source")
	private List<SourceFile> files = new ArrayList<SourceFile>();

	@OneToMany(mappedBy = "source")
	private List<Contract> contracts = new ArrayList<Contract>();

	@OneToMany(mappedBy = "source")
	private List<PurchaseOrder> purchaseOrders = new ArrayList<PurchaseOrder>();

	@OneToMany(mappedBy = "source")
	private List<Contact> contacts = new ArrayList<Contact>();

	@OneToMany(mappedBy = "source")
	private List<SourceAddress> addresses = new ArrayList<SourceAddress>();

	// this member will be used just to store assets of the Source associated with a specific product
	@Transient
	private List<Asset> assetsUsedInCW = new ArrayList<Asset>();

	// only loaded by SourceResource.java service and only used in auto complete logic
	@Transient
	public String sourceGroupName;

	public Source() {
		super();
	}

	public Source(int id) {
		super();

		setId(id);
	}

	@PrePersist
	public void prePersist() {
		log.debug("prePersist() called");

		if (StringUtils.isBlank(getExternalId())) {
			setExternalId(UniqueIdentifierGenerator.getNextIdentifier("perm.source."));
		}
	}

	@XmlElement
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlID
	@XmlElement
	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@XmlElement
	public String getCreditLine() {
		return creditLine;
	}

	public void setCreditLine(String creditLine) {
		this.creditLine = StringUtils.trimToNull(creditLine);
	}

	public SourceGroup getSourceGroup() {
		return sourceGroup;
	}

	public void setSourceGroup(SourceGroup sourceGroup) {
		this.sourceGroup = sourceGroup;
	}

	@Transient
	public boolean isCoveredByMasterAgreement() {
		if (null != getSourceGroup())
			return getSourceGroup().hasMasterAgreementActive();
		return false;
	}

	public String getSourceGroupName() {
		return sourceGroupName;
	}

	public void setSourceGroupName(String sourceGroupName) {
		this.sourceGroupName = sourceGroupName;
	}

	@XmlElement
	public DeliveryMethod getDeliveryMethod() {
		return deliveryMethod;
	}

	public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
		this.deliveryMethod = deliveryMethod;
	}

	public Set<Asset> getAssets() {
		return assets;
	}

	public void setAssets(Set<Asset> assets) {
		this.assets = assets;
	}

	public List<SourceFile> getFiles() {
		return files;
	}

	public void setFiles(List<SourceFile> files) {
		this.files = files;
	}

	@XmlElement
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Rip this out at some point and fix any jsp errors by using nofly instead.
	 * 
	 * @deprecated use isNofly instead
	 */
	@Deprecated
	public boolean isDisabled() {
		return nofly;
	}

	@Transient
	public String getNoFlyText() {
		return isNofly() ? "NO FLY" : "ACTIVE";
	}

	@XmlElement
	public boolean isNofly() {
    	return nofly;
    }

	public void setNofly(boolean nofly) {
    	this.nofly = nofly;
    }

	@XmlElement
	public String getNoFlyPhotographerLastName() {
		return noFlyPhotographerLastName;
	}

	public void setNoFlyPhotographerLastName(String noFlyPhotographerLastName) {
		this.noFlyPhotographerLastName = noFlyPhotographerLastName;
	}

	@XmlElement
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = StringUtils.trimToNull(comment);
	}

	@XmlElement
	public boolean isSuedWiley() {
		return suedWiley;
	}

	public void setSuedWiley(boolean suedWiley) {
		this.suedWiley = suedWiley;
	}

	public String getPermissionRequestUrl() {
		return permissionRequestUrl;
	}

	public void setPermissionRequestUrl(String permissionRequestUrl) {
		this.permissionRequestUrl = permissionRequestUrl;
	}

	@Transient
	public String getExternalName() {
		String name = getName();

		if (StringUtils.isNotBlank(name))
			return name.substring(0, Math.min(name.length(), Source.MAX_NAME_LENGTH));
		return null;
	}

	public List<Contract> getContracts() {
		return contracts;
	}

	public void setContracts(List<Contract> contracts) {
		this.contracts = contracts;
	}

	public List<PurchaseOrder> getPurchaseOrders() {
		return purchaseOrders;
	}

	public void setPurchaseOrders(List<PurchaseOrder> purchaseOrders) {
		this.purchaseOrders = purchaseOrders;
	}

	public List<Asset> getAssetsUsedInCW() {
		return assetsUsedInCW;
	}

	public void setAssetsUsedInCW(List<Asset> assetsUsedInCW) {
		this.assetsUsedInCW = assetsUsedInCW;
	}

	public Country getCountry() {
    	return country;
    }

	public void setCountry(Country country) {
    	this.country = country;
    }

	public String getPhoneNumber() {
    	return phoneNumber;
    }

	public void setPhoneNumber(String phoneNumber) {
    	this.phoneNumber = StringUtils.trimToNull(phoneNumber);
    }

	public String getFaxNumber() {
    	return faxNumber;
    }

	public void setFaxNumber(String faxNumber) {
    	this.faxNumber = StringUtils.trimToNull(faxNumber);
    }

	public String getWebsite() {
    	return website;
    }

	public void setWebsite(String website) {
    	this.website = StringUtils.trimToNull(website);
    }

	public String getJdeVendorNumber() {
    	return jdeVendorNumber;
    }

	public void setJdeVendorNumber(String jdeVendorNumber) {
    	this.jdeVendorNumber = StringUtils.trimToNull(jdeVendorNumber);
    }
	
	public Date getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(Date lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	public String getPermissionType() {
    	return permissionType;
    }

	public void setPermissionType(String permissionType) {
    	this.permissionType = permissionType;
    }

	public List<Contact> getContacts() {
    	return contacts;
    }

	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}

	public List<SourceAddress> getAddresses() {
    	return addresses;
    }

	public void setAddresses(List<SourceAddress> addresses) {
    	this.addresses = addresses;
    }

	/**
	 * Returns the first main address (there should be only one) or
	 * null if there is no main address.
	 */
	@Transient
	public Address getMainAddress() {
		List<SourceAddress> addresses = getAddresses();
		for (SourceAddress address : addresses) {
			if (address.getAddress().getType().equals(AddressType.MAIN)) {
				return address.getAddress();
			}
		}

		return null;
	}

	@Transient
	public boolean isValid() {
		boolean hasMainAddress = getMainAddress() != null;
		log.debug ("isValid(): hasMainAddress " + hasMainAddress);
		if (hasMainAddress) {
			return true;
		}

		List<Contact> contacts = getContacts();
		boolean hasEmail = false;
		for (Contact contact : contacts) {
			if (StringUtils.isNotBlank(contact.getEmail())) {
				hasEmail = true;
				break;
			}
		}
		log.debug ("isValid(): hasEmail " + hasEmail);
		if (hasEmail) {
			return true;
		}

		return false;
	}

	public void validate() throws ValidateException {
		// use getters due to JPA behavior

		// don't validate id and externalId as being non-null because
		// if this is a new object they will be null before db insert
	}

	@Override
	public String toString() {
		// always call get methods because of the way JPA works
		return "id = " + getId()
		        + ", externalId = " + getExternalId()
		        + ", name = " + getName()
		        + ", externalName = " + getExternalName()
		        + ", deliveryMethod = " + getDeliveryMethod()
		        + ", creditLine = " + getCreditLine()
		        + ", (other fields not displayed)";
	}

	/**
	 * Based on id, externalId, name - maybe change to just externalId(?).
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		// if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof Source))
			return false;

		// Use getter methods (getId(), etc) since using JPA - not sure if necessary

		final Source other = (Source) obj;
		if (this.getId() != other.getId() && (this.getId() == null || !this.getId().equals(other.getId()))) {
			return false;
		}
		if (this.getExternalId() != other.getExternalId() && (this.getExternalId() == null || !this.getExternalId().equals(other.getExternalId()))) {
			return false;
		}
		if (this.getName() != other.getName() && (this.getName() == null || !this.getName().equals(other.getName()))) {
			return false;
		}
		return true;
	}

	/**
	 * Based on id, externalId, name - maybe change to just externalId(?).
	 */
	@Override
	public int hashCode() {
		// Use getter methods (getId(), etc) since using JPA - not sure if necessary
		int hash = 3;
		hash = 67 * hash + (this.getId() != null ? this.getId().hashCode() : 0);
		hash = 67 * hash + (this.getExternalId() != null ? this.getExternalId().hashCode() : 0);
		hash = 67 * hash + (this.getName() != null ? this.getName().hashCode() : 0);
		return hash;
	}

	/**
	 * Implements Comparable interface. Compare based on name.
	 */
	@Override
	public int compareTo(Source s) {
		if (s == this)
			return 0;
		return getName().compareTo(s.getName());
	}
}
