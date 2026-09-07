package com.wiley.permissions.domain.persistence.permissions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @version $Id: Contract.java,v 1.102 2014-07-28 18:27:26 jmanyam Exp $
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "CONTRACT")
@SqlResultSetMappings({
	// scalarPrice is for AssetUseRepository.loadAssetPermissionInfo()
	@SqlResultSetMapping(name="scalarPrice", columns = @ColumnResult(name = "dprice") )
})
@NamedNativeQueries({
@NamedNativeQuery(name = "Contract.countContractsWithRightsLinkLicense",
		 		  query = "select count(*) as count from contract where import_source = 4 and number = ?",
		 		  resultSetMapping="scalarCount")
})
public class Contract extends ExtendedAuditBase implements Comparable<Contract> {
	private static final long serialVersionUID = 1L;

	public enum PaymentType {
	    PAYMENT, AUTHOR, WILEY;
	}

	private final static Log log = LogFactory.getLog(Contract.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 200)
	private String description;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "START_DATE")
	private Date startDate = new Date();

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "END_DATE")
	private Date endDate;

	// Actually this is OneToMany but it's not the usual kind
	// Changed to eager to fix this scenario:
	// Try to add a Edition Number condition to a Contract with a letter (invalid) instead of digit.
	// - Get a lazy-load exception when the controller tries to reload the Contract.
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "CONTRACT_2_CONDITION",
		joinColumns = @JoinColumn(name = "CONTRACT_ID"),
		inverseJoinColumns = @JoinColumn(name = "CONDITION_ID")
	)
	private List<Condition> conditions;

	@OneToMany(mappedBy = "contract")
	private List<ContractAsset> assets = new ArrayList<ContractAsset>();

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "contract")
	private List<ContractFile> files = new ArrayList<ContractFile>();

	//Start: Added for DM-532
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "contract")
	private List<ContractFileName> fileNames = new ArrayList<ContractFileName>();

	public List<ContractFileName> getFileNames() {
		return fileNames;
	}

	public void setFileNames(List<ContractFileName> fileNames) {
		this.fileNames = fileNames;
	}

	@Transient
	private String fileNameString = null;

	public String getFileNameString() {
		StringBuilder sb = null;
		if (null != fileNames && !fileNames.isEmpty()) {
			sb = new StringBuilder();
			for(ContractFileName fileName : fileNames) {
				if(!fileName.isFileUploaded()) {
					sb.append(fileName.getFileName().trim()).append("; ");
				}
			}
			return (sb.toString().trim().endsWith(";") ? sb.substring(0, sb.length() - 2).toString() : sb.toString()); // "-2" to remove semicolon and space at the end
			//return sb.toString();
		}
		return fileNameString;
	}

	public void setFileNameString(String fileNameString) {
		this.fileNameString = fileNameString;
	}
	//End: Added for DM-532

	@ManyToOne
	@JoinColumn(name = "CW_ID", nullable = false)
	private CommonWork commonWork = null;

	@Column(name = "NUMBER", nullable = true, length = 50)
	private String number;

	@Column(name = "IS_PERMISSION_FORM")
	private boolean permissionForm;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "DATE", nullable = false)
	private Date date;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SOURCE_ID")
	private Source source;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CURRENCY")
	private Currency currency = Currency.US; // default currency

	@Column(name = "PRICE", nullable = false)
	private double price = 0;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CONTRACT_TYPE")
	private ContractType contractType = ContractType.FRONTLIST; // default

	@OneToOne(mappedBy = "contract")
	private PaymentRequest paymentRequest = null;

	@OneToOne(cascade = CascadeType.ALL, mappedBy = "contract")
	private Amendment amendment = null;

	@ManyToOne
	@JoinColumn(name = "PURCHASE_ORDER_ID")
	private PurchaseOrder purchaseOrder = null;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "contract")
	private List<CompCopy> compCopies = new ArrayList<CompCopy>();

	@Column(name = "PAYMENT_TYPE")
	@Enumerated(EnumType.STRING)
	private PaymentType paymentType;

	@Column(name = "PAYMENT_DATE")
	private Date paymentDate;
	
	//Added for SS Task
	@Column(name = "CHECK_NUMBER")
	private Integer checkNumber = null;	

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MA_DEAL_ID")
	private MasterAgreementDeal maDeal;

	@ManyToOne
	@JoinColumn(name="IMPORT_SOURCE", referencedColumnName="CODE")
	private ImportSource importSource;

	@ManyToOne
	@JoinColumn(name="COPYRIGHT_TYPE", referencedColumnName="CODE")
	private CopyrightType copyrightType = CopyrightType.NOT_WILEY_OWNED;

	@Column(name = "LICENSE_XML")
	private String licenseXml;

	@Transient
	private List<ConditionNode> conditionNodeTree = null;

	// Added for SS Task 12 - Start
	@Transient
	private String approverName;

	@Transient
	private String approverRemarks;
	// Added for SS Task 12 - End

	public Contract() {
		super();
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

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = StringUtils.trimToNull(number);
	}

	public boolean isPermissionForm() {
		return permissionForm;
	}

	public void setPermissionForm(boolean permissionForm) {
		this.permissionForm = permissionForm;
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

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public ContractType getContractType() {
		return contractType;
	}

	public void setContractType(ContractType contractType) {
		this.contractType = contractType;
	}

	public PaymentType getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(PaymentType paymentType) {
		this.paymentType = paymentType;
	}

	// This exists for use by the form on customdetails.jspx which has a checkbox for paidByAuthor (old flag)
	@Transient
	public boolean isPaidByAuthor() {
		return paymentType == PaymentType.AUTHOR;
	}

	// This exists for use by the form on customdetails.jspx which has a checkbox for paidByAuthor (old flag)
	@Transient
	public void setPaidByAuthor(boolean value) {
		// smarkoff: I added this logic but I think James should review
		if (value) {
			if (paymentType != PaymentType.PAYMENT) {
				paymentType = PaymentType.AUTHOR;
			}
		}
		else {
			if (paymentType == PaymentType.AUTHOR) {
				paymentType = null;
			}
		}
	}

	public Date getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(Date paymentDate) {
		this.paymentDate = paymentDate;
	}
	
	//Added for SS Task
	public Integer getCheckNumber() {
		return checkNumber;
	}

	public void setCheckNumber(Integer checkNumber) {
		this.checkNumber = checkNumber;
	}
	//Ends

	public PaymentRequest getPaymentRequest() {
		return paymentRequest;
	}

	public void setPaymentRequest(PaymentRequest paymentRequest) {
		this.paymentRequest = paymentRequest;
	}

	// Called by drools
	@Transient
	public boolean isNeedPaymentRequest() {
	    // not sure if checking for PaymentType is really necessary but in any case this should be fine
	    // - can PaymentType not be null AND contract.isPaidByAuthor be false AND paymentRequest be null?
		return getPrice() > 0 && !isPaidByAuthor() && getPaymentRequest() == null && getPaymentType() == null;
	}

	// Called by drools
	@Transient
	public boolean isPaid() {
		return isPaidByAuthor() || getPaymentRequest() != null || getPaymentType() != null;
	}

	public PurchaseOrder getPurchaseOrder() {
		return purchaseOrder;
	}

	public Amendment getAmendment() {
		return amendment;
	}

	public void setAmendment(Amendment amendment) {
		this.amendment = amendment;
	}

	@Transient
	public boolean getLimitationOfLiability() {
		return (null != amendment);
	}

	public void setLimitationOfLiability(boolean limitation) {
		if (null == amendment && limitation == true) {
			amendment = new Amendment();
			amendment.setContract(this);
		}
	}

	public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
		this.purchaseOrder = purchaseOrder;
	}

	public List<CompCopy> getCompCopies() {
		return compCopies;
	}

	public void setCompCopies(List<CompCopy> compCopies) {
		this.compCopies = compCopies;
	}

	public MasterAgreementDeal getMasterAgreementDeal() {
		return maDeal;
	}

	public void setMasterAgreementDeal(MasterAgreementDeal maDeal) {
		this.maDeal = maDeal;
	}

	public ImportSource getImportSource() {
		return importSource;
	}

	public void setImportSource(ImportSource importSource) {
		this.importSource = importSource;
	}

	public CopyrightType getCopyrightType() {
		return copyrightType;
	}

	public void setCopyrightType(CopyrightType copyrightType) {
		this.copyrightType = copyrightType;
	}

	public String getLicenseXml() {
		return licenseXml;
	}

	public void setLicenseXml(String licenseXml) {
		this.licenseXml = licenseXml;
	}

	public List<ConditionNode> getConditionNodeTree() {
		return conditionNodeTree;
	}

	public void setConditionNodeTree(List<ConditionNode> conditionNodeTree) {
		this.conditionNodeTree = conditionNodeTree;
	}

	/**
	 * Will return true even the if the amendment does not have a file yet.
	 * Use hasAmendmentFile() if you want to know if the file has been created.
	 */
	@Transient
	public boolean hasAmendment() {
		return getAmendment() != null;
	}

	@Transient
	public boolean hasAmendmentFile() {
		// check fileName instead of data because may be faster (if data is large may be slower to load into memory)
		return getAmendment() != null && getAmendment().getFileName() != null;
	}

	public boolean isDateExpired() {
		Date today = new Date();
		Date endDate = getEndDate();
		Date startDate = getStartDate();

		if (startDate != null && startDate.after(today)) {
			// return false even though the startDate is in the future
			return false;
		}
		if (endDate != null && endDate.before(today)) {
			return true;
		}

		return false;
	}

	/**
	 * Does the end date meet a CW grant years minimum requirement?
	 * This method takes a cw because sometimes the contract covers
	 * another cw besides the contract cw (royaltyFree, another edition).
	 */
	@Transient
	public boolean startEndDateMeetMinGrantYears(CommonWork cw) {
		if (endDate == null) return true;
		// 0 means unlimited
		if (cw.getMinGrantYears() == 0) return false;

		Calendar minEndCal = Calendar.getInstance();
		minEndCal.setTime(startDate);
		minEndCal.add(Calendar.YEAR, cw.getMinGrantYears());
		Calendar endCal = Calendar.getInstance();
		endCal.setTime(endDate);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");
		log.debug("startEndDateMeetMinGrantYears(): minEndDate ["
				+ dateFormat.format(minEndCal.getTime())
				+ "] endDate [" + dateFormat.format(endCal.getTime()) + "]");

		// there is no beforeOrEquals so use !after
		return !minEndCal.after(endCal);
	}

	public List<ContractAsset> getAssets() {
		return assets;
	}

	public void setAssets(List<ContractAsset> assets) {
		this.assets = assets;
	}

	public CommonWork getCommonWork() {
		return commonWork;
	}

	public void setCommonWork(CommonWork commonWork) {
		this.commonWork = commonWork;
	}

	public List<ContractFile> getFiles() {
		return files;
	}

	public void setFiles(List<ContractFile> files) {
		this.files = files;
	}

	@Transient
	public double getAssetsTotalPrice() {
		double total = 0;
		for (ContractAsset ca : getAssets()) {
			if (null != ca.getPrice()) {
				total += ca.getPrice();
			}
		}

		return total;
	}

	public List<Condition> getConditions() {
		return conditions;
	}

	@Transient
	public List<Condition> getConditionsNotNull() {
		if (conditions == null) {
			conditions = new ArrayList<Condition>();
		}
		return conditions;
	}

	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}

	@Transient
	public Condition getConditionByType(ConditionType type) {
		if (getConditions() == null)  return null;

		for (Condition c : getConditions()) {
			if (c.getType().equals(type)) {
				return c;
			}
		}
		return null;
	}

	@Transient
	public boolean isUnlimitedPrintRun() {
		Condition c = getConditionByType(ConditionType.PRINT_RUN_UNLIMITED);
		if (c != null && c.isValueTrue())  return true;

		c = getConditionByType(ConditionType.PRINT_RUN_NO_MENTION);
		if (c != null && c.isValueTrue())  return true;

		return false;
	}

	@Transient
	public boolean isAllLanguages() {
		Condition c = getConditionByType(ConditionType.LANGUAGE_NO_MENTION);
		if (c != null && c.isValueTrue())  return true;

		c = getConditionByType(ConditionType.LANGUAGE_ALL_ALIAS);
		if (c != null && c.isValueTrue())  return true;

		c = getConditionByType(ConditionType.LANGUAGE_ALL);
		if (c != null && c.isValueTrue())  return true;

		return false;
	}

	@Transient
	public boolean isRightToSublicense() {
		Condition c = getConditionByType(ConditionType.SUBLICENSE_RIGHT);
		if (c != null && c.isValueTrue())  return true;

		return false;
	}

	/**
	 * Used by rules.
	 * Returns empty string if the contract is has no limits that we care about,
	 * otherwise returns a string with limited explanation.
	 */
	@Transient
	public String getLimitedExplanation() {
		StringBuilder sb = new StringBuilder();

		if (!isUnlimitedPrintRun()) {
			sb.append("limited print run");
		}

		if (getEndDate() != null) {
			if (sb.length() > 0)  sb.append(", ");
			sb.append("has end date");
		}

		if (!isAllLanguages()) {
			if (sb.length() > 0)  sb.append(", ");
			sb.append("not all languages included");
		}

		if (!isRightToSublicense()) {
			if (sb.length() > 0)  sb.append(", ");
			sb.append("right to sublicense not included");
		}

		return sb.toString();
	}

	/**
	 * Returns null or else the condition of the specified type from the list.
	 *
	 * @param list  May be null (in which case null is returned)
	 * @param type  Should not be null (but still works)
	 */
	@Transient
	public static Condition getConditionByType(List<Condition> list, ConditionType type) {
		if (list == null)  return null;

		for (Condition c: list) {
			if (c.getType().equals(type)) {
				return c;
			}
		}

		return null;
	}

	@Transient
	public void setContractAssets (List<Asset> assets) {
		setAssets(new ArrayList<ContractAsset>());

		for (Asset asset : assets) {
			ContractAsset ca = new ContractAsset(asset, this, null);
			getAssets().add(ca);
		}
	}

	@Transient
	public boolean isNew() {
		return getId() == null;
	}

	@Transient
	public List<Integer> getAssetIds() {
		List<Integer> aids = new ArrayList<Integer>();
		List<ContractAsset> assets = getAssets();
		for (ContractAsset asset : assets) {
			if (!aids.contains(asset.getAssetBaseId()))
				aids.add(asset.getAssetBaseId());
		}
		return aids;
	}

	@Transient
	public ContractAsset addAssetId(Integer aid) {
		log.debug("addAssetId() aid = " + aid);
		List<ContractAsset> assets = getAssets();
		ContractAsset asset = getContractAssetById (aid);
		// if not found
		if (null == asset) {
			AssetBase ab = new AssetBase();
			ab.setId(aid);
			asset = new ContractAsset(ab, this, null);
			assets.add(asset);
			log.debug("addAssetId(): added asset: " + asset);
		}
		return asset;
	}

	@Transient
	public ContractAsset getContractAssetById(Integer aid) {
		for (ContractAsset asset : getAssets()) {
			log.debug("getContractAssetById(): get contract asset " + asset.getAssetBaseId());
			if (asset.getAssetBaseId().intValue() == aid.intValue())
				return asset;
		}
		return null;
	}

	public void addAsset(ContractAsset contractAsset) {
		if (assets == null) {
			assets = new ArrayList<ContractAsset>();
		}
		contractAsset.setContract(this);

		assets.add (contractAsset);
	}

	public void removeAsset(ContractAsset contractAsset) {
		assets.remove(contractAsset);
	}

	@Override
	public String toString() {
		// use getters due to JPA
		return "id = " + getId()
			+ ", number = " + getNumber()
			+ ", date = " + getDate()
		    + ", description = " + getDescription()
			+ ", start date = " + getStartDate()
			+ ", end date = " + getEndDate();
	}

	/**
	 * Implements Comparable<Contract> interface.
	 *
	 * Sort by Source Name and then by Contract Number.
	 */
	@Override
	public int compareTo(Contract c) {
		if (this == c)  return 0;
		Source s = getSource();
		Source s2 = c.getSource();
		int value = s.compareTo(s2);
		if (value != 0)  return value;

		String num1 = getNumber();
		String num2 = c.getNumber();

		// compareTo throws NullPointerException if parameter is null
		if (num1 == null && num2 == null)
			return 0;
		if ((num1 == null && num2 != null) || (num1 != null && num2 == null))
			return -1;
		return num1.compareTo(num2);
	}
	
	// Added for SS Task 12 - Start
	public String getApproverName() {
		return approverName;
	}

	public void setApproverName(String approverName) {
		this.approverName = approverName;
	}

	public String getApproverRemarks() {
		return approverRemarks;
	}

	public void setApproverRemarks(String approverRemarks) {
		this.approverRemarks = approverRemarks;
	}
	// Added for SS Task 12 - End
	
}
