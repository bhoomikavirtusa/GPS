package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;
import java.util.Collection;
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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.MaterializationKey;

/**
 * @version $Id: PurchaseOrder.java,v 1.49 2015-06-18 15:09:55 rbongoni Exp $
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "PURCHASE_ORDER")
public class PurchaseOrder extends ExtendedAuditBase implements Comparable<PurchaseOrder> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(PurchaseOrder.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@MaterializationKey
	private Integer id = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "DATE", nullable = false)
	private Date date;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SOURCE_ID")
	private Source source;

	@Column(name = "IS_PERMISSION_REQUEST", nullable = false)
	private boolean permissionRequest = false;

	@Column(name = "IS_OUTSIDE_RECORD", nullable = false)
	private boolean outsideRecord = false;

	@Column(name = "NOTE", nullable = true, length = 500)
	private String note;

	@Column(name = "RETURN_ADDRESS", nullable = true, length = 200)
	private String returnAddress = StringUtils.EMPTY;

	@Column(name = "USER_SIGNATURE", nullable = true, length = 200)
	private String userSignature = StringUtils.EMPTY;

	@Column(name = "ESTIMATED_PRICE", nullable = false)
	private double estimatedPrice = 0;

	@Column(name = "SHOW_ESTIMATED_COST", nullable = false)
	private boolean showEstimatedCost = true;

	@Column(name = "SHOW_ISBN", nullable = false)
	private boolean showIsbn = true;

	@Column(name = "SHOW_COPYRIGHT_YEAR", nullable = false)
	private boolean showCopyrightYear = true;

	@Size(max = 100)
	@Column(name = "SOURCE_CONTACT_INFO", nullable = true, length = 100)
	private String sourceContactInfo;

	@Size(max = 200)
	@Column(name = "SOURCE_ADDRESS_INFO", nullable = true, length = 200)
	private String sourceAddressInfo;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ESTIMATED_CURRENCY")
	private Currency estimatedCurrency = null;

	@Column(name = "LANGUAGE_CODE")
	private String languageCode = EnumLanguage.EN;

	// changed to eager to fix a bug in UI
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "PURCHASE_ORDER_2_ASSET", joinColumns = @JoinColumn(name = "PURCHASE_ORDER_ID"),
		inverseJoinColumns = @JoinColumn(name = "ASSET_BASE_ID"))
	private List<Asset> assets = new ArrayList<Asset>();

	@ManyToOne
	@JoinColumn(name = "CW_ID", nullable = false)
	private CommonWork commonWork = null;

	@OneToMany(mappedBy = "purchaseOrder")
	private List<Contract> contracts = new ArrayList<Contract>();

	// Start : Added for DM-1606
	@Column(name = "IS_APPLIED_ONLINE", nullable = false)
	private boolean appliedOnline = false;

	@Transient
	public int requestAppliedOnline;
	// End : Added for DM-1606

	public PurchaseOrder() {
		super();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public boolean isWileyOrAuthorOwned() {
		return null != assets && assets.size() > 0 &&
		( assets.get(0).getOwnerType().equals(OwnerType.WILEY) ||
		  assets.get(0).getOwnerType().equals(OwnerType.WORK_FOR_HIRE) ||
		  assets.get(0).getOwnerType().equals(OwnerType.WILEY_CREATED) ||
		  assets.get(0).getOwnerType().equals(OwnerType.AUTHOR_OWNED) );
	}

	public boolean isWileyOwned() {
		return null != assets && assets.size() > 0 &&
			(assets.get(0).getOwnerType().equals(OwnerType.WILEY) ||
			assets.get(0).getOwnerType().equals(OwnerType.WILEY_CREATED) ||
			 assets.get(0).getOwnerType().equals(OwnerType.WORK_FOR_HIRE));

	}

	public boolean isAuthorOwned() {
		return null != assets && assets.size() > 0 &&
			assets.get(0).getOwnerType().equals(OwnerType.AUTHOR_OWNED);

	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public boolean isPermissionRequest() {
		return permissionRequest;
	}

	public void setPermissionRequest(boolean permissionRequest) {
		this.permissionRequest = permissionRequest;
	}

	public boolean isOutsideRecord() {
		return outsideRecord;
	}

	public void setOutsideRecord(boolean outsideRecord) {
		this.outsideRecord = outsideRecord;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
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

	public double getEstimatedPrice() {
		return estimatedPrice;
	}

	public void setEstimatedPrice(double estimatedPrice) {
		this.estimatedPrice = estimatedPrice;
	}

	public boolean isShowEstimatedCost() {
		return showEstimatedCost;
	}

	public void setShowEstimatedCost(boolean showEstimatedCost) {
		this.showEstimatedCost = showEstimatedCost;
	}

	public boolean isShowIsbn() {
		return showIsbn;
	}

	public void setShowIsbn(boolean showIsbn) {
		this.showIsbn = showIsbn;
	}

	public boolean isShowCopyrightYear() {
		return showCopyrightYear;
	}

	public void setShowCopyrightYear(boolean showCopyrightYear) {
		this.showCopyrightYear = showCopyrightYear;
	}

	public Currency getEstimatedCurrency() {
		return estimatedCurrency;
	}

	public void setEstimatedCurrency(Currency estimatedCurrency) {
		this.estimatedCurrency = estimatedCurrency;
	}

	public List<Asset> getAssets() {
		return assets;
	}

	public void setAssets(List<Asset> assets) {
		this.assets = assets;
	}

	public CommonWork getCommonWork() {
		return commonWork;
	}

	public void setCommonWork(CommonWork commonWork) {
		this.commonWork = commonWork;
	}

	public List<Contract> getContracts() {
		return contracts;
	}

	public void setContracts(List<Contract> contracts) {
		this.contracts = contracts;
	}

	public String getSourceContactInfo() {
		return sourceContactInfo;
	}

	public void setSourceContactInfo(String sourceContactInfo) {
		this.sourceContactInfo = sourceContactInfo;
	}


	public String getSourceAddressInfo() {
		return sourceAddressInfo;
	}

	public void setSourceAddressInfo(String sourceAddressInfo) {
		this.sourceAddressInfo = sourceAddressInfo;
	}

	public String getLanguageCode() {
		// if empty return English
		if (StringUtils.isBlank(languageCode)) {
			return  EnumLanguage.EN;
		}
		return languageCode;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}

	// Start : Added for DM-1606
	public boolean isAppliedOnline() {
		return appliedOnline;
	}

	public void setAppliedOnline(boolean appliedOnline) {
		this.appliedOnline = appliedOnline;
	}
	public int getRequestAppliedOnline() {
		return requestAppliedOnline;
	}

	public void setRequestAppliedOnline(int requestAppliedOnline) {
		this.requestAppliedOnline = requestAppliedOnline;
	}

	// End : Added for DM-1606
	@Transient
	public List<Asset> getUnusedAssetsInContracts() {
		List<Asset> assets = getAssets();

		List<Asset> removedAssets = new ArrayList<Asset>();

		List<Contract> contracts = getContracts();

		for (Asset asset : assets) {
			if (!asset.isInContracts(contracts)) {
				if (!removedAssets.contains(asset)) {
					Collection<AssetUse> aus = asset.getAssetUses();

					for (AssetUse assetUse : aus) {
						// if at least one usage is not canceled
						if (!assetUse.isCanceled()) {
							removedAssets.add(asset);
							break;
						}
					}
				}
			}
		}

		return removedAssets;
	}

	/**
	 * This method is used by JMesa for table sorting in
	 * listSourcesToContract.jspx.
	 */
	@Transient
	public int getUnusedAssetsInContractsCount() {
		return getUnusedAssetsInContracts().size();
	}

	/**
	 * if the PO has at least one Contract, and all Contracts have Payment
	 * Requests that are paid than we consider the Purchase Order paid, else not
	 *
	 * @return boolean
	 */
	@Transient
	public boolean getIsPaid() {
		// if it has assets that are not part of any contract, it is not paid
		if (CollectionUtils.isNotEmpty(getUnusedAssetsInContracts())) {
			return false;
		}

		// if all assets are part of an contract, and all contracts have
		// PaymentRequest
		// that is paid, then is PAID
		if (CollectionUtils.isNotEmpty(getContracts())) {
			for (Contract contract : contracts) {
				if (null == contract.getPaymentRequest()) {
					return false;
				}
				else if (!contract.getPaymentRequest().isPaid()) {
					return false;
				}
			}
		}
		else {
			return false;
		}
		return true;
	}

	/**
	 * Returns error message if Return Address is not valid.
	 * Possible errors are being null/blank,
	 * more than 7 new lines, or a single line is more than 38 chars.
	 */
	public static String validateReturnAddress(String s) {
		if (StringUtils.isBlank(s)) {
			return "Return Address cannot be blank";
		}

		// check for newline instead of carriage returns since cover both "\r\n" and "\n"
		int newLineCount = StringUtils.countMatches(s, "\n");
		if (newLineCount > 7) {
			// (can either have 7 lines with a newline at the end of each one
			// or 8 lines with no newline at the end but this looks bad)
			return "Return Address cannot have more than 7 lines";
		}

		String [] lines = s.split("\\r?\\n");  // note "\\" may not be necessary
		for (String line : lines) {
			if (line.length() > 38) {
				return "Return Address cannot have a line more than 38 chars (found " + line.length() + ")";
			}
		}

		return null;
	}

	@Transient
	public boolean isNew() {
		return getId() == null;
	}

	@Override
	public String toString() {
		// use getters due to JPA
		return "id = " + getId()
			+ ", date = " + getDate()
			+ ", permissionRequest = " + isPermissionRequest()
			+ ", commonwork = "+ getCommonWork().getId();
	}

	/**
	 * Implements Comparable interface.
	 *
	 * Sort by Source Name and then by PO Date.
	 */
	@Override
	public int compareTo(PurchaseOrder po) {
		if (this == po)  return 0;
		Source s = getSource();
		Source s2 = po.getSource();
		int value = s.compareTo(s2);
		if (value != 0)  return value;

		return getDate().compareTo(po.getDate());
	}
}
