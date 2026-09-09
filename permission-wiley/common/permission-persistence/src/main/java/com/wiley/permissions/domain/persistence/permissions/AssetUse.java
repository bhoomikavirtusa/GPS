package com.wiley.permissions.domain.persistence.permissions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.PrePersist;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;
import com.wiley.permissions.security.web.ThreadLocalUser;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.sf.common.lang.StringUtil;

@Entity
@PersistenceUnit(unitName = "permissions")
@NamedQueries({
	@NamedQuery(name = "AssetUse.loadUsesWithSamePositionInCW",
		 		query = "select au from AssetUse au join fetch au.asset a left join fetch a.sources " +
	 	  						" where au.commonWork.id = ?1 and au.id != ?2 and upper(au.position) = upper (?3) and au.canceled = false")
})
@NamedNativeQueries({
	@NamedNativeQuery(name = "AssetUse.loadUsesWithSameAssetInCW",
		 		  	query = "(select id from asset_use au join (select asset_id, cw_id from asset_use where id=?) a on " +
		 		  			" a.asset_id = au.asset_id and a.cw_id = au.cw_id)",
		 		  	resultSetMapping="scalarId"),
	@NamedNativeQuery(name = "AssetUse.countFiles",  // Note Asset.countFiles is a different query and located in Asset.java
		 		  	query = "select count(*) as count from asset_use_file where au_id = ?",
		 		  	resultSetMapping="scalarCount")
})

@SqlResultSetMappings({
	// scalarCount is for AssetUseService.countAssetUseHavingComponent()
	@SqlResultSetMapping(name="scalarCount", columns = @ColumnResult(name = "count") ),
	// scalarId is for CommonWorkService.loadCWIds()
	// and ProductService.lookupIdForExternalId()
	// and AssetUseService.getNextAssetUseId()
	// (and other places)
	@SqlResultSetMapping(name="scalarId", columns = @ColumnResult(name = "id") ),
	// scalarDate used by CommonWorkServiceImpl.getLastProductUpdate()
	// and UserRepository.getAuthorDueDate()
	@SqlResultSetMapping(name="scalarInt", columns = @ColumnResult(name = "int_col") ),
	// scalarInt used by ContractRepository.getSourceIdForContractId()
	// smarkoff: I would ideally go back and always use scalarInt instead of scalarId
	@SqlResultSetMapping (name="scalarBoolean", columns = @ColumnResult(name = "boolean_col") ),
	// scalarBoolean currently not used but created for future use
	@SqlResultSetMapping(name="scalarDate", columns = @ColumnResult(name = "date") ),
	// scalarGroup is for AssetUseRepository.loadAssetPermissionInfo()
	@SqlResultSetMapping (name="scalarGroup", columns = @ColumnResult(name = "cgroup") ),
	// scalarString is for ProductRepository.loadExternalIds() and other methods
	@SqlResultSetMapping (name="scalarString", columns = @ColumnResult(name = "string") )
})
@Table(name = "ASSET_USE")
public class AssetUse extends ExtendedAuditBase {
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(AssetUse.class);

	private static final String WILEY_OWNED_CREDIT_LINE = "Reprinted with permission of John Wiley & Sons, Inc.";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@Size (max=64)
	@Column(name = "EXTERNAL_ID", nullable = false, length = 64)
	@MaterializationKey
	private String externalId = null;

	@Valid
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ASSET_ID", nullable = false)
	private Asset asset = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CW_ID", nullable = false)
	private CommonWork commonWork = null;

	@Size (max=50)
	@Column(name = "POSITION", nullable = true, length = 50)
	private String position = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "COMPONENT_ID")
	private Component component = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USAGE_TYPE")
	private Usage usage = null;

	@Size (max=8)
	@Column(name = "MANUSCRIPT_PAGE", nullable = true, length = 8)
	private String manuscriptPage = null;

	@Size (max=30)
	@Column(name = "FINAL_PAGE", nullable = true, length = 30)
	private String finalPage = null;

	@Column(name = "IS_COLOR", nullable = false)
	private boolean color = false;

	@Column(name = "IS_PAGE_PROOF", nullable = false)
	private boolean pageProofRequired = false;

	@Column(name = "IS_CUSTOM", nullable = false)
	private boolean custom = false;

	@Column(name = "IMAGE_POSTED_FTP", nullable = false)
	private boolean imagePostedFtp = false;

	@Column(name = "ESTIMATED_COST", nullable = false)
	private double estimatedCost = 0;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ESTIMATED_CURRENCY")
	private Currency estimatedCurrency = null;

	@Column(name = "PREVIOUS_WILEYPUB_AU_ID", nullable = true)
	private Integer previousWileypubAssetUseId = null;

	@Column(name = "GBPM_CATEGORY", nullable = true)
	private Integer gbpmCategory = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SIZE")
	private com.wiley.permissions.domain.persistence.permissions.Size size =
		com.wiley.permissions.domain.persistence.permissions.Size.NA;
	// init here to N/A for default but in most cases this is overridden by UserDefaults.size
	// - see AssetResource.saveAssetUseForm() and CustomAssetController.dbload() for overrides

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PERMISSION_STATUS")
	private PermissionStatus status = null;

	@OneToMany(mappedBy = "assetUse")
	private List<AuSourcePermStatus> sourceStatuses;

	@Column(name = "STATUS_EXPLANATION")
	private String statusExplanation = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "LAST_UPDATED_STATUS", nullable=false)
	private Date lastUpdatedStatus = null;

	// should be true if true for any source (can be multiple)
	// - breakdown by source in AuSourcePermStatus
	@Column(name = "NEED_PAYMENT_REQUEST", nullable = false)
	private boolean needPaymentRequest = false;

	// should be true if true for any source (can be multiple)
	// - breakdown by source in AuSourcePermStatus
	@Column(name = "PAID", nullable = false)
	private boolean paid = false;

	@Size (max=5000)
	@Column(name = "PERMISSION_COMMENT", nullable = true, length = 5000)
	private String permissionComment = null;

	@Size (max=5000)
	@Column(name = "PRODUCTION_COMMENT", nullable = true, length = 5000)
	private String productionComment = null;

	@Size (max=2048)
	@Column(name = "REQUEST_COMMENT", nullable = true, length = 2048)
	private String requestComment = null;

	@Size (max=500)
	@Column(name = "CAPTION", nullable = true, length = 500)
	private String caption = null;

	@Column(name = "REUSED_FROM_PREV_ED", nullable = false)
	private boolean reusedFromPreviousEdition = false;

	@Column(name = "IS_PICKUP", nullable = false)
	private boolean isPickup = false;
	
	@Column(name = "IS_MEDIAMANAGER", nullable = false)
	private boolean isMediaManager = false;

	@Column(name = "CAMERA_COPY_TO_COME", nullable = false)
	private boolean cameraCopyToCome = false;

	@Column(name = "IS_CANCELED", nullable = false)
	private boolean canceled = false;

	// Removed is a special version of canceled - when removed is true, canceled should also be true
	// - Important since we have many queries that check the canceled flag but do not look at removed
	@Column(name = "IS_REMOVED", nullable = false)
	private boolean removed = false;

	@Column(name = "MEDIA_RETURN_REQ", nullable = false)
	private boolean mediaReturnRequested = false;

	@Size (max=13)
	@Column(name = "PICKUP_ISBN", nullable = true, length = 13)
	private String pickupISBN = null;

	@Size (max=500)
	@Column(name = "PICKUP_COMMENT", nullable = true, length = 500)
	private String pickupComment = null;

	@Size (max=50)
	@Column(name = "PICKUP_POSITION", nullable = true, length = 50)
	private String pickupPosition;

	@Size (max=8)
	@Column(name = "PICKUP_PAGE", nullable = true, length = 8)
	private String pickupPage = null;

	@Column(name = "PICKUP_ISSUE_NUMBER", nullable = true)
	private String pickupIssueNumber;

	@Column(name = "PICKUP_EDITION_NUMBER", nullable = true)
	private Integer pickupEditionNumber = null;

	@Size (max=300)
	@Column(name = "PICKUP_TITLE", nullable = true, length = 300)
	private String pickupTitle = null;

	@Size (max=300)
	@Column(name = "PICKUP_AUTHOR", nullable = true, length = 300)
	private String pickupAuthor = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "PICKUP_ISSUE_DATE")
	private Date pickupIssueDate;

	@Size (max=13)
	@Column(name = "REUSED_ISBN", nullable = true, length = 13)
	private String reusedISBN = null;

	@Size (max=500)
	@Column(name = "REUSED_COMMENT", nullable = true, length = 500)
	private String reusedComment = null;

	@Size (max=50)
	@Column(name = "REUSED_POSITION", nullable = true, length = 50)
	private String reusedPosition;

	@Size (max=8)
	@Column(name = "REUSED_PAGE", nullable = true, length = 8)
	private String reusedPage = null;

	@Size (max=200)
	@Column(name = "FOUND_ON", nullable = true, length = 200)
	private String foundOn;

	@Column(name = "IS_R_USE_APPROVED", nullable = false)
	private boolean restrictedUseApproved = false;

	@Column(name = "NEW", nullable = false)
	private boolean isNew = false;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CANCEL_TIMESTAMP")
	private Date cancelTimestamp = null;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "CANCEL_USER_ID")
	private User cancelUser;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "CANCEL_REPLACEMENT_ID")
	private AssetUse cancelReplacement;

	@Size (max=1000)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE)
	@Column(name = "CANCEL_COMMENT", length = 1000)
	private String cancelComment = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PAGE_POS")
	private PagePosition pagePosition = null;

	@ManyToOne
	@JoinColumn(name = "R_USE_APPROVED_USER_ID")
	private User restrictedUseApprovedUser;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "R_USE_APPROVED_DATE")
	private Date restrictedUseApprovedDate;

	@Column(name = "PRINT_RUN_WARNING", nullable = false)
	private boolean printRunWarning = false;

	@ManyToOne
	@JoinColumn(name="IMPORT_SOURCE", referencedColumnName="CODE")
	private ImportSource importSource;

	@Size (max=30)
	@Column(name = "WIZARD_OWNER_TYPE", nullable = true, length = 30)
	private String wizardOwnerType;

	@Column(name = "SENT_TO_PRODUCTION", nullable = false)
	private boolean sentToProduction = false;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "DATE_TO_PROD")
	private Date dateToProduction;

	@Column(name="SORT_ORDER", length=10)
	private String sortOrder = null;

	/**
	 * This is the created user group. If the user changes group after creating
	 * an AssetUse then this field will NOT change.
	 */
	@ManyToOne
	@JoinColumn(name="USER_GROUP_ID")
	private UserGroup userGroup = null;

	@Column(name="NOFLY_MATCH_APPROVED", nullable=true)
	private boolean noFlyMatchApproved = false;

	public boolean isNoFlyMatchApproved() {
		return noFlyMatchApproved;
	}

	public void setNoFlyMatchApproved(boolean noFlyMatchApproved) {
		this.noFlyMatchApproved = noFlyMatchApproved;
	}

	@Transient
	private boolean isStatusChanged = false;  // currently not used

	/* nmedrano - only used by the tag as replace option has no other use */
	@Transient
	private boolean isJustAdded = false;

	@Transient
	private boolean isNeedToConfirmCancels = true;

	@Transient
	private Integer copyAssetsCwId = null;

	public AssetUse() {
		super();
	}

	public AssetUse (int id, int cwId) {
		this.setId(id);
		CommonWork cw = new CommonWork();
		cw.setId(cwId);
		this.setCommonWork(cw);
	}

	public AssetUse(AssetUse au) {
		// could use some sort of BeanUtils.copy() method here but would need to exclude
		// the mentioned attributes

		// copy everything except id, externalId, createdDate, createdUser,
		// lastUpdatedDate, lastUpdateUser, and status
		setAsset(au.getAsset());
		setCameraCopyToCome(au.isCameraCopyToCome());
		setCanceled(au.isCanceled());
		setRemoved(au.isRemoved());
		setCaption(au.getCaption());
		setColor(au.isColor());
		setCommonWork(au.getCommonWork());
		setComponent(au.getComponent());
		//setCreatedDate
		//setCreatedUser
		//setExternalId
		setFinalPage(au.getFinalPage());
		setFoundOn(au.getFoundOn());
		//setId
		//setLastUpdatedDate
		//setLastUpdatedUser
		setManuscriptPage(au.getManuscriptPage());
		setMediaReturnRequested(au.isMediaReturnRequested());
		setNeedPaymentRequest(au.isNeedPaymentRequest());
		setNeedToConfirmCancels(au.isNeedToConfirmCancels());
		setNew(au.isNew());
		setPagePosition(au.getPagePosition());
		setPermissionComment(au.getPermissionComment());
		setPickup(au.isPickup());
		setMediaManager(au.isMediaManager()); // SR_301213
		setPickupAuthor(au.getPickupAuthor());
		setPickupComment(au.getPickupComment());
		setPickupISBN(au.getPickupISBN());
		setPickupIssueDate(au.getPickupIssueDate());
		setPickupIssueNumber(au.getPickupIssueNumber());
		setPickupPage(au.getPickupPage());
		setPickupPosition(au.getPickupPosition());
		setPickupTitle(au.getPickupTitle());
		setPosition(au.getPosition());
		setPrintRunWarning(au.isPrintRunWarning());
		setProductionComment(au.getProductionComment());
		setRequestComment(au.getRequestComment());
		setRestrictedUseApproved(au.isRestrictedUseApproved());
		setRestrictedUseApprovedDate(au.getRestrictedUseApprovedDate());
		setRestrictedUseApprovedUser(au.getRestrictedUseApprovedUser());
		setReusedComment(au.getReusedComment());
		setReusedFromPreviousEdition(au.isReusedFromPreviousEdition());
		setReusedISBN(au.getReusedISBN());
		setReusedPage(au.getReusedPage());
		setReusedPosition(au.getReusedPosition());
		setSize(au.getSize());
		//setStatus
		//setStatusChanged
		//setStatusExplanation
		setUsage(au.getUsage());
		setWizardOwnerType(au.getWizardOwnerType());
	}

	@Override
	@PrePersist
	public void prePersist() {
		log.debug("prePersist() called");
		super.prePersist();

		if (StringUtils.isBlank(getExternalId())) {
			setExternalId(UniqueIdentifierGenerator.getNextIdentifier("perm.asset_use."));
		}

		// note the following allows for asset.getMediaType() to be null
		if (MediaType.PHOTO.equals(asset.getMediaType())) {
			setColor(true);
		}

		if (getLastUpdatedStatus() == null) {
			// I believe when an Asset is first created using the "regular" UI,
			// the status is just set to NoSource without calling the rules engine
			// so we want the date to be current in this situation.
			if (getStatus() != null) {
				setLastUpdatedStatus(new Date());
			}
			else {
				try {
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					Date date = dateFormat.parse("1999-01-01");
					setLastUpdatedStatus(date);
				}
				catch (ParseException ex) {
					// this should never happen
				}
			}
		}

		updateUserGroup();
	}

	/**
	 * For a while we did this with a trigger instead but had some weird
	 * intermittent problems with that (see asset_use_insert trigger for notes).
	 */
	public void updateUserGroup() {
		if (userGroup == null) {
			try {
				UserGroup ug = new UserGroup();
				UserPrincipal userPrincipal = ThreadLocalUser.get();
				Integer groupId = userPrincipal.getGroupId();
				log.debug("updateUserGroup(): ThreadLocalUser.groupId() = " + groupId);

				if (groupId == null) {
					ug.setId(1);  // GE Photo Editors
				}
				else {
					ug.setId(groupId);
				}

				setUserGroup(ug);
			}
			catch (Exception e) {
				log.error("updateUserGroup(): Error reconciling user ", e);
			}
		}
	}

	@XmlElement
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElement
	@XmlID
	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@XmlElement
	public Asset getAsset() {
		return asset;
	}

	public void setAsset(Asset asset) {
		this.asset = asset;
	}

	@XmlElement
	public CommonWork getCommonWork() {
		return commonWork;
	}

	public void setCommonWork(CommonWork commonWork) {
		this.commonWork = commonWork;
	}

	@XmlElement
	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = StringUtils.stripToNull(position);
	}

	@XmlElement
	public Component getComponent() {
		return component;
	}

	public void setComponent(Component component) {
		this.component = component;
	}

	/**
	 * Convenience method for jsp/JMesa to use.
	 * (Can get NullPointerException trying to reference "component.name"
	 * when component is null. It's ok if name is null.)
	 */
	@Transient
	public String getComponentName() {
		// call getter because of the way JPA works
		Component c = getComponent();
		return (c == null) ? StringUtils.EMPTY : c.getName();
	}

	@XmlElement
	public Usage getUsage() {
		return usage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}

	@XmlElement
	public String getManuscriptPage() {
		return manuscriptPage;
	}

	public void setManuscriptPage(String manuscriptPage) {
		this.manuscriptPage = StringUtils.stripToNull(manuscriptPage);
	}

	@XmlElement
	public String getFinalPage() {
		return finalPage;
	}

	public void setFinalPage(String finalPage) {
		this.finalPage = StringUtils.stripToNull(finalPage);
	}

	@XmlElement(name = "isColor")
	public boolean isColor() {
		return color;
	}

	public void setColor(boolean isColor) {
		this.color = isColor;
	}

	public boolean isPageProofRequired() {
		return pageProofRequired;
	}

	public void setPageProofRequired(boolean pageProofRequired) {
		this.pageProofRequired = pageProofRequired;
	}

	public boolean isCustom() {
		return custom;
	}

	public void setCustom(boolean custom) {
		this.custom = custom;
	}

	public boolean isImagePostedFtp() {
		return imagePostedFtp;
	}

	public void setImagePostedFtp(boolean imagePostedFtp) {
		this.imagePostedFtp = imagePostedFtp;
	}

	public double getEstimatedCost() {
		return estimatedCost;
	}

	public void setEstimatedCost(double estimatedCost) {
		this.estimatedCost = estimatedCost;
	}

	public Currency getEstimatedCurrency() {
		return estimatedCurrency;
	}

	public void setEstimatedCurrency(Currency estimatedCurrency) {
		this.estimatedCurrency = estimatedCurrency;
	}

	@XmlElement
	public com.wiley.permissions.domain.persistence.permissions.Size getSize() {
		return size;
	}

	public void setSize(com.wiley.permissions.domain.persistence.permissions.Size size) {
		this.size = size;
	}

	@XmlElement(name = "permissionStatus")
	public PermissionStatus getStatus() {
		return status;
	}

	public void setStatus(PermissionStatus status) {
		this.status = status;
	}

	public List<AuSourcePermStatus> getSourceStatuses() {
		return sourceStatuses;
	}

	public String getStatusExplanation() {
		return statusExplanation;
	}

	public void setStatusExplanation(String statusExplanation) {
		// 1000 matches the database column size
		this.statusExplanation = StringUtil.truncate(statusExplanation, 1000, true);
	}

	public Date getLastUpdatedStatus() {
		return lastUpdatedStatus;
	}

	public void setLastUpdatedStatus(Date lastUpdatedStatus) {
		this.lastUpdatedStatus = lastUpdatedStatus;
	}

	public boolean isNeedPaymentRequest() {
		return needPaymentRequest;
	}

	public void setNeedPaymentRequest(boolean needPaymentRequest) {
		this.needPaymentRequest = needPaymentRequest;
	}

	public boolean isPaid() {
		return paid;
	}

	public void setPaid(boolean paid) {
		this.paid = paid;
	}

	@XmlElement
	public String getPermissionComment() {
		return permissionComment;
	}

	public void setPermissionComment(String permissionComment) {
		this.permissionComment = StringUtils.stripToNull(permissionComment);
	}

	public String getProductionComment() {
		return productionComment;
	}

	public void setProductionComment(String productionComment) {
		this.productionComment = StringUtils.stripToNull(productionComment);
	}

	public String getRequestComment() {
		return requestComment;
	}

	public void setRequestComment(String requestComment) {
		this.requestComment = StringUtils.stripToNull(requestComment);
	}

	public String getWizardOwnerType() {
		return wizardOwnerType;
	}

	public void setWizardOwnerType(String wizardOwnerType) {
		this.wizardOwnerType = wizardOwnerType;
	}

	/**
	 * 1st take from contract conditions (asset or global)
	 * 2nd take from Asset
	 * 3rd take from Source
	 * 4th use Wiley Owned credit line if appropriate
	 * 5th leave blank
	 *
	 * Default credit line is currently from Source default.
	 * New default credit line should be "Photographer name\Source default credit"
	 * or just Source default credit if there is no photographer.
	 *
	 * Note because contract condition takes precedence over asset, if a user
	 * edits the credit line on the landing page and the contract condition is
	 * set, they will not see any change, which I'm sure will be confusing.
	 * However James said that it's more important that the credit line be
	 * correct in terms of precedence.
	 */
	public String getCreditLine() {
		// ---- 1st priority
		String credit = "";
		String prefix = "";

		if (StringUtils.isNotBlank(getAsset().getArtist())) {
			prefix = getAsset().getArtist() + "\\";
		}

		List <AuSourcePermStatus> sourceStatuses = getSourceStatuses();
		for (AuSourcePermStatus sourceStatus : sourceStatuses) {
			if (StringUtils.isNotBlank(sourceStatus.getContractCreditLine())) {
				credit = sourceStatus.getContractCreditLine();
			}
		}

		if (StringUtils.isNotBlank(credit)) {
			log.debug("1st: sourcestatuses");
			//Commenting the existing logic as per James Russiello - Start
			//return prefix + credit;
			return credit;
			//Commenting the existing logic as per James Russiello - End
		}

		// ---- 2nd priority
		if (StringUtils.isNotBlank(asset.getCreditLine())) {
			log.debug("2nd: asset");
			return asset.getCreditLine();
		}

		// ---- 3rd priority
		List<Source> sources = asset.getSources();
		credit = "";

		boolean gotOne = false;
		for (Source source : sources) {
			if (StringUtils.isNotBlank(source.getCreditLine())) {
				if (gotOne)
					credit += ", ";
				credit += source.getCreditLine();
				gotOne = true;
			}
		}
		if (StringUtils.isNotBlank(credit)){
			log.debug("3rd: credit");
			return prefix + credit;
		}

		// ---- 4th priority
		OwnerType ot = getAsset().getOwnerType();
		if (ot != null && isPickup()) {
			if (ot.equals(OwnerType.WILEY) || ot.equals(OwnerType.WORK_FOR_HIRE)
					|| ot.equals(OwnerType.WILEY_CREATED) || ot.equals(OwnerType.AUTHOR_OWNED)) {
				log.debug("4th: Wiley Owned");
				return WILEY_OWNED_CREDIT_LINE;
			}
		}

		// ---- 5th priority
		log.debug("5th: empty");
		return StringUtils.EMPTY;
	}

	@XmlElement
	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = StringUtils.stripToNull(caption);
	}

	@XmlElement(name = "reuse")
	public boolean isReusedFromPreviousEdition() {
		return reusedFromPreviousEdition;
	}

	public void setReusedFromPreviousEdition(boolean reusedFromPreviousEdition) {
		this.reusedFromPreviousEdition = reusedFromPreviousEdition;
	}

	@XmlElement(name = "cameraCopyToCome")
	public boolean isCameraCopyToCome() {
		return cameraCopyToCome;
	}

	public void setCameraCopyToCome(boolean cameraCopyToCome) {
		this.cameraCopyToCome = cameraCopyToCome;
	}

	@XmlElement(name = "canceled")
	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	public boolean isRemoved() {
		return removed;
	}

	public void setRemoved(boolean removed) {
		this.removed = removed;
	}

	public boolean isMediaReturnRequested() {
		return mediaReturnRequested;
	}

	public void setMediaReturnRequested(boolean mediaReturnRequested) {
		this.mediaReturnRequested = mediaReturnRequested;
	}

	@XmlElement(name = "pickupISBN")
	public String getPickupISBN() {
		return pickupISBN;
	}

	public void setPickupISBN(String pickupISBN) {
		this.pickupISBN = StringUtils.stripToNull(pickupISBN);
	}

	public String getPickupTitle() {
		return pickupTitle;
	}

	public void setPickupTitle(String pickupTitle) {
		this.pickupTitle = pickupTitle;
	}

	public String getPickupAuthor() {
		return pickupAuthor;
	}

	public void setPickupAuthor(String pickupAuthor) {
		this.pickupAuthor = pickupAuthor;
	}

	public String getPickupComment() {
		return pickupComment;
	}

	public void setPickupComment(String pickupComment) {
		this.pickupComment = StringUtils.stripToNull(pickupComment);
	}

	public String getPickupPosition() {
		return pickupPosition;
	}

	public void setPickupPosition(String pickupPosition) {
		this.pickupPosition = StringUtils.stripToNull(pickupPosition);
	}

	public String getPickupPage() {
		return pickupPage;
	}

	public void setPickupPage(String pickupPage) {
		this.pickupPage = StringUtils.stripToNull(pickupPage);
	}

	public String getPickupIssueNumber() {
		return pickupIssueNumber;
	}

	public void setPickupIssueNumber(String pickupIssueNumber) {
		this.pickupIssueNumber = pickupIssueNumber;
	}

	public Date getPickupIssueDate() {
		return pickupIssueDate;
	}

	public void setPickupIssueDate(Date pickupIssueDate) {
		this.pickupIssueDate = pickupIssueDate;
	}


	@XmlElement
	public Integer getPickupEditionNumber() {
		return pickupEditionNumber;
	}

	public void setPickupEditionNumber(Integer pickupEditionNumber) {
		this.pickupEditionNumber = pickupEditionNumber;
	}

	@XmlElement(name = "isPickup")
	public boolean isPickup() {
		return isPickup;
	}

	public void setPickup(boolean isPickup) {
		this.isPickup = isPickup;
	}

	@XmlElement(name = "isMediaManager")
	public boolean isMediaManager() {
		return isMediaManager;
	}

	public void setMediaManager(boolean isMediaManager) {
		this.isMediaManager = isMediaManager;
	}
	
	public void setPrintRunWarning(boolean value) {
		this.printRunWarning = value;
	}

	public boolean isPrintRunWarning() {
		return printRunWarning;
	}

	public Date getDateToProduction() {
		return dateToProduction;
	}

	public void setDateToProduction(Date dateToProduction) {
		this.dateToProduction = dateToProduction;
	}

	public Date getCancelTimestamp() {
		return cancelTimestamp;
	}

	public void setCancelTimestamp(Date cancelTimestamp) {
		this.cancelTimestamp = cancelTimestamp;
	}

	public User getCancelUser() {
		return cancelUser;
	}

	public void setCancelUser(User cancelUser) {
		this.cancelUser = cancelUser;
	}

	public AssetUse getCancelReplacement() {
		return cancelReplacement;
	}

	public void setCancelReplacement(AssetUse cancelReplacement) {
		this.cancelReplacement = cancelReplacement;
	}

	public String getCancelComment() {
		return cancelComment;
	}

	public void setCancelComment(String cancelComment) {
		this.cancelComment = cancelComment;
	}


	/**
	 * Will deal with any condition that an asset to product might have that will not make it
	 * usable for PO generation
	 * @return boolean
	 */
	@Transient
	public boolean exclude() {
		// if permission is GRANTED, not required anymore
		if (PermissionStatus.isComplete(getStatus())) {
			return true;
		}

		// if no source
		if (CollectionUtils.isEmpty(getAsset().getSources())) {
			return true;
		}

		return false;
	}

	public String getReusedComment() {
		return reusedComment;
	}

	public void setReusedComment(String reusedComment) {
		this.reusedComment = StringUtils.stripToNull(reusedComment);
	}

	@XmlElement(name = "reuseISBN")
	public String getReusedISBN() {
		return reusedISBN;
	}

	public void setReusedISBN(String reusedISBN) {
		this.reusedISBN = StringUtils.stripToNull(reusedISBN);
	}

	public String getReusedPage() {
		return reusedPage;
	}

	public void setReusedPage(String reusedPage) {
		this.reusedPage = StringUtils.stripToNull(reusedPage);
	}

	public String getReusedPosition() {
		return reusedPosition;
	}

	public void setReusedPosition(String reusedPosition) {
		this.reusedPosition = StringUtils.stripToNull(reusedPosition);
	}

	public String getFoundOn() {
		return foundOn;
	}

	public void setFoundOn(String foundOn) {
		this.foundOn = StringUtils.stripToNull(foundOn);
	}

	public boolean isRestrictedUseApproved() {
		return restrictedUseApproved;
	}

	public void setRestrictedUseApproved(boolean restrictedUseApproved) {
		this.restrictedUseApproved = restrictedUseApproved;
	}

	public void setRestrictedUseApprovedUser(User restrictedUseApprovedUser) {
		this.restrictedUseApprovedUser = restrictedUseApprovedUser;
	}

	public User getRestrictedUseApprovedUser() {
		return restrictedUseApprovedUser;
	}

	public void setRestrictedUseApprovedDate(Date restrictedUseApprovedDate) {
		this.restrictedUseApprovedDate = restrictedUseApprovedDate;
	}

	public Date getRestrictedUseApprovedDate() {
		return restrictedUseApprovedDate;
	}

	public boolean isSentToProduction() {
		return sentToProduction;
	}

	public void setSentToProduction(boolean sentToProduction) {
		this.sentToProduction = sentToProduction;
	}


	@XmlElement
	public Integer getGbpmCategory() {
		return gbpmCategory;
	}

	public void setGbpmCategory(Integer gbpmCategory) {

			this.gbpmCategory = gbpmCategory;
	}
	@Override
	public String toString() {
		return super.toString()
		    + ", id = " + id
		    + ", externalId = " + externalId
		    + ", permissionStatus = [" + status + "]\r\n"
		    + ", permissionComment = [" + permissionComment + "]\r\n"
		    + ", asset = [" + asset + "]"
		    + ", (other fields not displayed)";
	}

	@Transient
	public void validate() throws ValidateException {
		StringBuilder validateError = new StringBuilder();

		if (null == getAsset()) {
			validateError.append("Asset is required");
		}
		else {
			try {
				getAsset().validate();
			} catch (ValidateException ve) {
				validateError.append(ve.getMessage());
			}
		}

		if (null == getUsage()) {
			validateError.append("Usage is required");
		}
		else {
			try {
				getUsage().validate();
			} catch (ValidateException ve) {
				validateError.append(ve.getMessage());
			}
		}
		// validate PagePosition
		if (null != getPagePosition()) {
			try {
				getPagePosition().validate();
			} catch (ValidateException ve) {
				validateError.append(ve.getMessage());
			}
		}

		String error = validateError.toString();
		if (StringUtils.isNotBlank(error))
			throw new ValidateException (error);
	}

	@Transient
	public boolean isUnrequested() {
		// important that getStatus is 2nd in case it might be null (which is unusual)
		return PermissionStatus.isUnrequested(getStatus());
	}

	@Transient
	public boolean isUnrequested(Source s) {
		List<AuSourcePermStatus> list = getSourceStatuses();
		for (AuSourcePermStatus asps: list) {
			if (asps.getSource().getId().equals(s.getId())) {
				return PermissionStatus.isUnrequested(getStatus());
			}
		}
		return false;
	}

	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

	public PagePosition getPagePosition() {
		return pagePosition;
	}

	public void setPagePosition(PagePosition pagePosition) {
		this.pagePosition = pagePosition;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = StringUtils.trimToNull(sortOrder);
	}

	public ImportSource getImportSource() {
		return importSource;
	}

	public void setImportSource(ImportSource importSource) {
		this.importSource = importSource;
	}

	public UserGroup getUserGroup() {
		return userGroup;
	}

	public void setUserGroup(UserGroup userGroup) {
		this.userGroup = userGroup;
	}

	public Integer getPreviousWileypubAssetUseId() {
		return previousWileypubAssetUseId;
	}

	public void setPreviousWileypubAssetUseId(Integer previousWileypubAssetUseId) {
		this.previousWileypubAssetUseId = previousWileypubAssetUseId;
	}

	/* lnagy - currently not used */
	@Transient
	public boolean isStatusChanged() {
		return isStatusChanged;
	}

	@Transient
	public void setStatusChanged(boolean isStatusChanged) {
		this.isStatusChanged = isStatusChanged;
	}

	/* nmedrano - only used by the tag as replace option has no other use */
	@Transient
	public boolean isJustAdded() {
		return isJustAdded;
	}
	/* nmedrano - only used by the tag as replace option has no other use */
	@Transient
	public void setIsJustAdded(boolean isJustAdded) {
		this.isJustAdded = isJustAdded;
	}

	//Added for Paperwork Granted starts
	public Integer getCopyAssetsCwId() {
		return copyAssetsCwId;
	}

	public void setCopyAssetsCwId(Integer copyAssetsCwId) {
		this.copyAssetsCwId = copyAssetsCwId;
	}
	//Paperwork Granted Ends

	@Transient
	public boolean isNeedToConfirmCancels() {
		return this.isNeedToConfirmCancels;
	}

	@Transient
	public void setNeedToConfirmCancels(boolean value) {
		this.isNeedToConfirmCancels = value;
	}

	@Transient
	public boolean isNeedToIncludePhotographer() {
		MediaType mediaType = getAsset().getMediaType();
		boolean matches = (MediaType.TEXT.equals(mediaType) ||
				MediaType.DATA_SET.equals(mediaType) ||
				MediaType.LIST.equals(mediaType) ||
				MediaType.QUESTION.equals(mediaType) ||
				MediaType.REALIA.equals(mediaType) ||
				MediaType.LECTURE_PRESENTATION.equals(mediaType)||
				MediaType.INTERACTIVITY.equals(mediaType) ||
				MediaType.TABLE.equals(mediaType)
				);
		return !matches;
	}
}
