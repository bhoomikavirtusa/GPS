package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "user_defaults")
public class UserDefaults
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	public static final String NO_FILTER = null;
	public static final String CUSTOM_FILTER = "Custom";
	public static final String INTERNAL_FILTER = "Internal";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@MaterializationKey
	private Integer id = null;

	@OneToOne
	@JoinColumn(name="USER_ID")
	@MaterializationKey
	private User user;

	@Column(name = "RETURN_ADDRESS", nullable = true, length = 200)
	private String returnAddress = null;

	@Column(name = "USER_SIGNATURE", nullable = true, length = 200)
	private String userSignature = null;

	@Column(name = "COST_CENTER", nullable = true, length = 20)
	private String costCenter = null;

	@OneToOne
	@JoinColumn(name="WILEY_ENTITY_CODE")
	private WileyEntity wileyEntity = WileyEntity.WILEY_US;

	@OneToOne
	@JoinColumn(name="CURRENCY_CODE")
	private Currency currency = Currency.US;

	// cannot link to Country entity as they are from different persistence units
	@Column(name = "COUNTRY_CODE", nullable = false)
	private String countryCode = Country.USA;

	@Transient
	private Country country;

	@Column(name = "SHOW_REQUEST", nullable = false)
	private boolean showRequest = true;

	@Column(name = "SHOW_ESTIMATED_COST", nullable = false)
	private boolean showEstimatedCost = true;

	@Column(name = "CUSTOM_MODE", nullable = false)
	private boolean customMode = true;

	@Column(name = "CUSTOM_FILTER")
	private String customFilter;

	@Column(name = "DATE_FORMAT", nullable = false)
	private String dateFormat = "mm/dd/yyyy";  // default

	@ManyToOne
	@JoinColumn(name="ACCOUNT_ID")
	private Account account = null;

	@ManyToOne
	@JoinColumn(name="COMPONENT_CATEGORY")
	private ComponentCategory componentCategory = null;

	@ManyToOne
	@JoinColumn(name="BUSINESS_UNIT_CODE")
	private BusinessUnit businessUnit = null;

	@ManyToOne
	@JoinColumn(name="USER_LOCATION_CODE")
	private UserLocation userLocation = UserLocation.US;

	@ManyToOne
	@JoinColumn(name="SIZE")
	private Size size = Size.NA;



	public UserDefaults() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getReturnAddress() {
		return returnAddress;
	}

	public void setReturnAddress(String returnAddress) {
		this.returnAddress = returnAddress;
	}

	public String getUserSignature() {
		return userSignature;
	}

	public void setUserSignature(String userSignature) {
		this.userSignature = userSignature;
	}

	public String getCostCenter() {
		return costCenter;
	}

	public void setCostCenter(String costCenter) {
		this.costCenter = costCenter;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public ComponentCategory getComponentCategory() {
		return componentCategory;
	}

	public void setComponentCategory(ComponentCategory componentCategory) {
		this.componentCategory = componentCategory;
	}

	public boolean isShowRequest() {
		return showRequest;
	}

	public void setShowRequest(boolean showRequest) {
		this.showRequest = showRequest;
	}

	public boolean isShowEstimatedCost() {
		return showEstimatedCost;
	}

	public void setShowEstimatedCost(boolean showEstimatedCost) {
		this.showEstimatedCost = showEstimatedCost;
	}

	public WileyEntity getWileyEntity() {
		return wileyEntity;
	}

	public void setWileyEntity(WileyEntity wileyEntity) {
		this.wileyEntity = wileyEntity;
	}

	public BusinessUnit getBusinessUnit() {
		return businessUnit;
	}

	public void setBusinessUnit(BusinessUnit businessUnit) {
		this.businessUnit = businessUnit;
	}

	public UserLocation getUserLocation() {
		return userLocation;
	}

	public void setUserLocation(UserLocation userLocation) {
		this.userLocation = userLocation;
	}

	public Size getSize() {
		return size;
	}

	public void setSize(Size size) {
		this.size = size;
	}

	public boolean isCustomMode() {
		return customMode;
	}

	public void setCustomMode(boolean customMode) {
		this.customMode = customMode;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	@Transient
	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
		setCountryCode(country.getCode());
	}

	public String getCustomFilter() {
		return (StringUtils.isBlank(customFilter) ? StringUtils.EMPTY : customFilter);
	}

	@Transient
	public String getCustomFilterDescription() {
		return StringUtils.isBlank(customFilter) ? "No Filter" : customFilter + " only";
	}

	public void setCustomFilter(String customFilter) {
		if (StringUtils.isBlank(customFilter))
			customFilter = null;
		this.customFilter = customFilter;
	}
	
	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}
	
	public String getDatePickerDateFormat() {
		return dateFormat.replace("yyyy", "yy");
	}
}
