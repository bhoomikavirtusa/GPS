package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.sf.common.io.FixWindows1252Chars;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.StringUtil;

/**
 *
 * @version $Id: Asset.java,v 1.109 2016-12-18 05:54:05 nchandra Exp $
 * @author lnagy, created Aug 30, 2007
 */
@Entity
@PersistenceUnit(unitName="permissions")
@Table(name="ASSET")
@DiscriminatorValue(value="ASSET")
@NamedNativeQueries({
@NamedNativeQuery(	name = "Asset.finalCost",
		 		  	query = "(select coalesce(sum(c2a.price),0) as double_value from contract_2_asset c2a join contract c on c2a.contract_id = c.id " +
		 		  	"where c2a.asset_base_id = ? and c.cw_id = ?)",
		 		  	resultSetMapping="scalarDouble"),

@NamedNativeQuery(	name = "Asset.countAssetUses",
		 		  	query = "select count(*) as count from asset_use where asset_id = ?",
		 		  	resultSetMapping="scalarCount"),

@NamedNativeQuery(	name = "Asset.countAssetUsesInCW",
					query = "select count(*) as count from asset_use where asset_id = ? and cw_id=?",
					resultSetMapping="scalarCount"),

@NamedNativeQuery(	name = "Asset.countSources",
					query = "select count(*) as count  from asset_2_source where asset_id = ? ",
					resultSetMapping="scalarCount"),

@NamedNativeQuery(	name = "Asset.countPurchaseOrdersInCW",
		 		  	query = "select count(*) as count from purchase_order_2_asset p2a join purchase_order po on po.id = p2a.purchase_order_id where p2a.asset_base_id = ? and po.cw_id=? ",
		 		  	resultSetMapping="scalarCount"),

@NamedNativeQuery(	name = "Asset.countContractsInCW",
		 		  	query = "select count(*) as count from contract_2_asset c2a join contract c on c.id = c2a.contract_id where c2a.asset_base_id = ? and c.cw_id=? ",
		 		  	resultSetMapping="scalarCount"),

@NamedNativeQuery(	name = "Asset.countFiles",  // Note AssetUse.countFiles is a different query and located in AssetUse.java
		 		  	query = "select count(*) as count from asset_file where asset_id = ?",
		 		  	resultSetMapping="scalarCount"),

@NamedNativeQuery(	name = "Asset.activeRoyaltyFreeDeals",
					query = "select rf.* from asset_use au join au_source_perm_status asp on au.id = asp.asset_use_id join contract_2_asset c2a on au.asset_id = c2a.asset_base_id and c2a.contract_id = asp.latest_contract_id join royalty_free_deal rf on c2a.rfdeal_id = rf.id where au.id = ?",
					resultClass=RoyaltyFreeDeal.class)

})
public class Asset extends AssetBase
{
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(Asset.class);

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="ASSET_GROUP_ID")
	private AssetGroup assetGroup;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="COPIED_FROM_ID")
	private Asset copiedFrom;

	@NotNull
	@Size (max=1000)
	@Column(name="DESCRIPTION", nullable=false, length=1000)
	private String description;

	@ManyToOne
	@JoinColumn(name="OWNER_TYPE", referencedColumnName="CODE")
	private OwnerType ownerType = null;

	@ManyToOne
	@JoinColumn(name="MEDIA_TYPE", referencedColumnName="CODE")
	private MediaType mediaType = null;

	@Size (max=1000)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE)
	@Column(name = "CREDIT_LINE", length = 1000)
	private String creditLine;



	/**
	 * smarkoff: Note really this might be more appropriate on the Asset_2_Source table.
	 * If an asset has 2 sources might there be a different vendorId for each?
	 */
	@Size (max=150)
	@Column(name="VENDOR_ID", length=150)
	private String vendorId;

	@Size (max=400)
	@Column(name="ARTIST", length=400)
	private String artist;

	@ManyToOne
	@JoinColumn(name="MODEL_RELEASE", referencedColumnName="CODE")
	private ModelRelease modelRelease = ModelRelease.ACQUIRED;

	@Column(name="PROPERTY_RELEASE", nullable=false)
	private boolean isPropertyRelease = false;

	@Column(name="WORK_FOR_HIRE", nullable=false)
	private boolean isWorkForHire = false;

	@Column(name="WILL_BE_WORK_FOR_HIRE", nullable=false)
	private boolean willBeWorkForHire = false;

	@Column(name="OBTAINED_BY_AUTHOR", nullable=false)
	private boolean isObtainedByAuthor = false;

	@Column(name="IS_FEE_REQUIRED", nullable=false)
	private boolean feeRequired = false; // default

	@Column(name="IS_RESTRICTED_USE", nullable=false)
	private boolean restrictedUse;

	@Column(name="IS_STM_GUIDELINES", nullable=false)
	private boolean isStmGuidelines;

	@Column(name="IS_ACTIVE", nullable=false)
	private boolean active = true;

	@Column(name="RESTRICTED_USE_DETAILS", nullable=true)
	private String restrictedUseDetails;

	@Column(name="IS_MANAGED", nullable=false)
	private boolean isManaged = true; // default

	@Column(name="IS_ROYALTY_FREE", nullable=false)
	private boolean isRoyaltyFree = false;

	@Column(name="WILL_BE_ROYALTY_FREE", nullable=false)
	private boolean willBeRoyaltyFree = false;

	@Column(name="MUST_DISPLAY_CREDIT", nullable=false)
	private boolean mustDisplayCredit;

	@Size (max=300)
	@Column(name="ORIGINAL_PUBLICATION_TITLE", length=300)
	private String originalPublicationTitle;

	@Size (max=500)
	@Column(name="ORIGINAL_PUBLICATION_AUTHOR", length=500)
	private String originalPublicationAuthor;

	@Size (max=300)
	@Column(name="ORIGINAL_ARTICLE_TITLE", length=300)
	private String originalArticleTitle;

	@Size (max=300)
	@Column(name="ORIGINAL_ARTICLE_AUTHOR", length=300)
	private String originalArticleAuthor;

	@Size (max=13)
	@Column(name="ORIGINAL_PUBLICATION_ISBN", length=13)
	private String originalPublicationIsbn;

	@Size (max=25)
	// original publication figure number
	@Column(name="ORIGINAL_FIGURE_NUMBER", length=25)
	private String originalFigureNumber;

	@Size (max=25)
	//original publication page number
	@Column(name="ORIGINAL_PAGE_NUMBER", length=25)
	private String originalPageNumber;

	@Size (max=1000)
	@Column(name="CITATION", length=1000)
	private String citation;

	@Size (max=1000)
	@Column(name="BIBLIO", length=1000)
	private String biblio;

	@Size (max=25)
	@Column(name="FROM_1", length=25)
	private String from1;

	@Size (max=25)
	@Column(name="TO_1", length=25)
	private String to1;

	@Size (max=25)
	@Column(name="FROM_2", length=25)
	private String from2;

	@Size (max=25)
	@Column(name="TO_2", length=25)
	private String to2;

	@Size (max=25)
	@Column(name="FROM_3", length=25)
	private String from3;

	@Size (max=25)
	@Column(name="TO_3", length=25)
	private String to3;

	@Column(name="ARCHIVE", nullable=false)
	private boolean archive = false;

	@Size (max=256)
	@Column(name="KEYWORDS", nullable=true, length=256)
	private String keywords;

	@Column(name="TOTAL_SEATS", nullable=false)
	private int totalSeats;

	@Column(name="TOTAL_PRINT_RUN", nullable=false)
	private int totalPrintRun;

	@Column(name="TOTAL_USED_SEATS", nullable=false)
	private int totalUsedSeats;


	//sandhya adding new column

	@Column(name = "IS_SUBLICENSE", nullable = false)
	private boolean isSublicense = false;


	public boolean isSublicense() {
		return isSublicense;
	}

	public void setSublicense(boolean isSublicense) {
		this.isSublicense = isSublicense;
	}
	//sandhya end

	//santhosh adding new column

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "ORIGINAL_PUBLICATION_DATE")
	private Date originalPublicationDate;


	public Date getOriginalPublicationDate() {
		return originalPublicationDate;
	}

	public void setOriginalPublicationDate(Date originalPublicationDate) {
		this.originalPublicationDate = originalPublicationDate;
	}

	//Start: Added a new column for DM-374
	@Column(name="IS_MANAGER_APPROVED", nullable=true)
	private boolean managerApproved = false;

	public boolean isManagerApproved() {
		return managerApproved;
	}

	public void setManagerApproved(boolean managerApproved) {
		this.managerApproved = managerApproved;
		if (managerApproved) {
			setOwnerType(OwnerType.THIRD_PARTY);
			setManaged(true);
		}
	}
	//End: Added a new column for DM-374

	//Added for Paperwork Task B Public Domain Starts
	@Transient
	private boolean isPubDomain = false;

	public boolean isPubDomain() {
		return isPubDomain;
	}

	public void setPubDomain(boolean isPubDomain) {
		this.isPubDomain = isPubDomain;
	}
	//Paperwork Task B Public Domain ends

	@Column(name="FILENAME", nullable=true, length=100)
	private String filename;

	@ManyToOne
	@JoinColumn(name="IMPORT_SOURCE", referencedColumnName="CODE")
	private ImportSource importSource;

	@Column(name = "REVIEWED_UNKNOWN", nullable = false)
	private boolean reviewedUnknown = false;

	@Column(name = "AUTHOR_PROVIDED_UNKNOWN", nullable = false)
	private boolean authorProvidedUnknown = false;

	@ManyToOne
	@JoinColumn(name="COPYRIGHT_TYPE", referencedColumnName="CODE")
	private CopyrightType copyrightType = CopyrightType.NOT_WILEY_OWNED;

	@OneToMany(mappedBy = "asset")
	// Don't use Merge.CollectionHandling.REPLACE because of code in AssetServiceImpl.updateAsset()
	private List<AssetFile> files = null;

	@OneToMany(mappedBy="asset")
	// added NEVER_MERGE so don't get lazy load problem in AssetServiceImpl.updateAsset()
	// which uses BeanUtility.merge()
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private List<AssetUse> assetUses = new ArrayList<AssetUse>();

	@Valid
	@ManyToMany
	@JoinTable(
		name = "ASSET_2_SOURCE",
		joinColumns = @JoinColumn(name = "ASSET_ID"),
		inverseJoinColumns = @JoinColumn(name = "SOURCE_ID")
	)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT,
			collectionHandling=Merge.CollectionHandling.REPLACE)
	// lnagy - needs an empty list for the asset import to work
	private List<Source> sources = new ArrayList<Source>();



	//OneToMany (mappedBy="asset")
	//Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	@Transient
	private List<AssetToSource> assetToSources = new ArrayList<AssetToSource>();

	// Transient
	//  private List<Source> sources = new ArrayList<Source>();

	@Transient
	private String positionsAsString = StringUtils.EMPTY;

	public Asset() {
		super();
	}

	public void validate() throws ValidateException {
		StringBuilder validateError = new StringBuilder();

		if (StringUtils.isBlank(getDescription())) {
			validateError.append("Description must not be blank.");
		}

		if (getMediaType() != null) {
			try {
				getMediaType().validate();
			} catch (ValidateException ve) {
				validateError.append(ve.getMessage());
			}
		}

		if (getOwnerType() != null) {
			try {
				getOwnerType().validate();
			} catch (ValidateException ve) {
				validateError.append(ve.getMessage());
			}
		}

		if (CollectionUtils.isNotEmpty(getSources())) {
			for (Source s: getSources()) {
				try {
					s.validate();
				} catch (ValidateException ve) {
					validateError.append(ve.getMessage());
				}
			}
		}

		if (!isManaged() && !isRoyaltyFree())
			validateError.append("Is not managed and is not royalty free is an invalid combination.");

		String error = validateError.toString();
		if (StringUtils.isNotBlank(error))
			throw new ValidateException (error);
	}

	/**
	 * Use PurchaseOrderService.loadListForAssetSourceProduct() if you want
	 * a list filtered by Source and Product as well as Asset.
	 */
	public Set<PurchaseOrder> getAllPurchaseOrders() {
		Set<PurchaseOrder> output = new HashSet<PurchaseOrder>();

		output.addAll(getPurchaseOrders());

		if (assetGroup != null) {
			output.addAll(assetGroup.getPurchaseOrders());
		}

		return output;
	}

	@Transient
	public int getAllPurchaseOrdersCount() {

		int count = getPurchaseOrders().size();
		if (assetGroup != null) {
			count += assetGroup.getPurchaseOrders().size();
		}
		return count;
	}

	/**
	 * Use PurchaseOrderService.loadListForAssetSourceProduct() if you want
	 * a list filtered by Source and Product as well as Asset.
	 */
	public PurchaseOrderList getPurchaseOrderList() {
		Set<PurchaseOrder> purchaseOrders = getAllPurchaseOrders();

		if (purchaseOrders.size() == 0) {
			return null;
		}
		else {
			PurchaseOrderList purchaseOrderList = new PurchaseOrderList();
			purchaseOrderList.addAll(purchaseOrders);
			return purchaseOrderList;
		}
	}

	/**
	 * Use ContractService.loadListForAssetSourceProduct() if you want
	 * a list filtered by Source and Product as well as Asset.
	 */
	public ContractList getContractList()
	{
		Set<Contract> contracts = getAllContracts();

		if (contracts.size() == 0) {
			return null;
		}
		else {
			ContractList contractList = new ContractList(contracts.size());
			contractList.addAll(contracts);
			return contractList;
		}
	}

	/**
	 * Use ContractService.loadListForAssetSourceCW() if you want
	 * a list filtered by Source and CommonWork as well as Asset.
	 */
	public Set<Contract> getAllContracts() {
		Set<Contract> output = new HashSet<Contract>();

		for (ContractAsset contract : getContracts()) {
			output.add(contract.getContract());
		}

		if (assetGroup != null) {
			for (ContractAsset contract : assetGroup.getContracts()) {
				output.add(contract.getContract());
			}
		}

		return output;
	}

	@Transient
	public int getAllContractsCount() {
		int count = getContracts().size();
		if (assetGroup != null) {
			count += assetGroup.getContracts().size();
		}

		return count;
	}

	@XmlElement
	public AssetGroup getAssetGroup() {
		return assetGroup;
	}

	public void setAssetGroup(AssetGroup assetGroup) {
		this.assetGroup = assetGroup;
	}

	@XmlElement
	public Asset getCopiedFrom() {
		return copiedFrom;
	}

	public void setCopiedFrom(Asset copiedFrom) {
		this.copiedFrom = copiedFrom;
	}

	@Transient
	public String getTruncDescription() {
		if (null == getDescription()) return null;
		if (getDescription().length() > 10) {
			return getDescription().substring(0, 9);
		}
		return getDescription();
	}

	@Override
	@XmlElement
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = StringUtil.removeSpecialControlChars(description, "description", "setDescription", log);
		this.description = FixWindows1252Chars.fix(this.description, "description", "setDescription", log);
	}

	@XmlElement
	public OwnerType getOwnerType() {
		return ownerType;
	}

	/**
	 * Convenience method for jsp/dataTable to use.
	 * (Can get NullPointerException trying to reference "ownerType.description"
	 * when ownerType is null. It's ok if description is null.)
	 */
	@Transient
	public String getOwnerTypeDescription() {
		// call getter because of the way JPA works
		OwnerType ot = getOwnerType();
		return (ot == null) ? null : ot.getDescription();
	}

	public void setOwnerType(OwnerType ownerType) {
		// The ownerType column is nullable but generally this should not be null
		ArgUtil.notNull(ownerType, "ownerType");

		this.ownerType = ownerType;
		if (OwnerType.PUBLIC_DOMAIN.equals(ownerType)) {
			setManaged(false);
			setRoyaltyFree(true);
		} else if (OwnerType.FAIR_USE.equals(ownerType)) {
			setManaged(false);
			setFeeRequired(false);
			setRoyaltyFree(true);
		} else if (OwnerType.WILEY.equals(ownerType)) {
			setManaged(false);
		}
	}

	@XmlElement
	public MediaType getMediaType() {
		return mediaType;
	}

	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
	}

	@XmlElement
	public CopyrightType getCopyrightType() {
		return copyrightType;
	}

	public void setCopyrightType(CopyrightType copyrightType) {
		this.copyrightType = copyrightType;
	}

	@XmlElement
	public String getCreditLine() {
	return creditLine;
	}


	public void setCreditLine(String creditLine) {
		this.creditLine = StringUtils.stripToNull(creditLine);
	}

	@XmlElement
	public String getVendorId() {
		return vendorId;
	}

	public void setVendorId(String vendorId) {
		this.vendorId = StringUtils.stripToNull(vendorId);
	}

	@XmlElement
	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = StringUtils.stripToNull(artist);
	}

	@XmlElement
	public boolean isFeeRequired() {
		return feeRequired;
	}

	public void setFeeRequired(boolean feeRequired) {
		this.feeRequired = feeRequired;
	}

	@XmlElement
	public boolean isRestrictedUse() {
		return restrictedUse;
	}

	public void setRestrictedUse(boolean restrictedUse) {
		this.restrictedUse = restrictedUse;
	}

	@XmlElement
	public boolean isStmGuidelines() {
		return isStmGuidelines;
	}

	public void setStmGuidelines(boolean isStmGuidelines) {
		this.isStmGuidelines = isStmGuidelines;
		if (isStmGuidelines) {
			setOwnerType(OwnerType.THIRD_PARTY);
			setManaged(false);
		}
	}

	@XmlElement
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@XmlElement
	public String getRestrictedUseDetails() {
		return restrictedUseDetails;
	}

	public void setRestrictedUseDetails(String restrictedUseDetails) {
		this.restrictedUseDetails = StringUtils.trimToNull(restrictedUseDetails);
	}

	@XmlElement
	public boolean isMustDisplayCredit() {
		return mustDisplayCredit;
	}

	public void setMustDisplayCredit(boolean mustDisplayCredit)  {
		this.mustDisplayCredit = mustDisplayCredit;
	}

	// Rather than put an XML annotation here, see getOriginalFileXml() below
	public List<AssetFile> getFiles() {
		return files;
	}

	@Transient
	public List<AssetFile> getFilesNotNull() {
		if (files == null) {
			files = new ArrayList<AssetFile>();
		}
		return files;
	}

	@Transient
	public int getFilesCount() {
		if (files == null)  return 0;
		else return getFiles().size();
	}

	public void setFiles(List<AssetFile> files) {
		this.files = files;
	}

	@Transient
	public AssetFile getOriginalFile() {
		return getFileOfRenditionType(RenditionType.ORIGINAL);
	}

	@Transient
	public AssetFile getSmallThumbnail() {
		return getFileOfRenditionType(RenditionType.SMALL_THUMBNAIL);
	}

	@Transient
	public AssetFile getMediumThumbnail() {
		return getFileOfRenditionType(RenditionType.MEDIUM_THUMBNAIL);
	}

	@Transient
	public AssetFile getLargeThumbnail() {
		return getFileOfRenditionType(RenditionType.LARGE_THUMBNAIL);
	}

	/**
	 * If there is more than one of the given RenditionType,
	 * just returns the first one.
	 */
	@Transient
	public AssetFile getFileOfRenditionType(RenditionType rtype) {
		if (files == null)  return null;
		for (AssetFile file: files) {
			if (file.getRenditionType().equals(rtype)) {
				return file;
			}
		}
		return null;
	}

	@Transient
	public void replaceFileOfSameRenditionType(AssetFile af) {
		if (files == null)  files = new ArrayList<AssetFile>();

		for (int i = 0; i < files.size(); i++) {
			AssetFile file = files.get(i);
			if (file.getRenditionType().equals(af.getRenditionType())) {
				files.remove(file);
				// don't break in case there is more than one file of the same
				// rendition type although there should not be
				//break;
				i--;
			}
		}

		files.add(af);
	}

	@Transient
	@XmlElement(name="content")
	public AssetFile getOriginalFileXml() {
		AssetFile original = getOriginalFile();
		// TODO: Add logic to return null unless the file was just uploaded
		// (look at lastUpdatedDate)

		return original;
	}

	// Actually CMS will only send a thumbnail to CMS (not an original file)
	// but this method must be named the same as the getter
	@Transient
	public void setOriginalFileXml(AssetFile file) {
		replaceFileOfSameRenditionType(file);
	}

	public List<AssetUse> getAssetUses() {
		return assetUses;
	}

	public void setAssetUses(List<AssetUse> assetUses) {
		this.assetUses = assetUses;
	}

	@XmlElementWrapper(name = "sources")
    @XmlElement(name = "source")
	public List<Source> getSources() {
		return sources;
	}

	@Transient
	public List<Source> getSourcesNotNull() {
		if (sources == null) {
			sources = new ArrayList<Source>();
		}
		return sources;
	}

	@Transient
	public String getSourcesCommaSeparated() {
		return StringUtils.join(sources, ", ");
	}

	public void setSources(List<Source> sources) {
		if (sources != null && sources.contains(null)) {
	        throw new IllegalArgumentException("'sources' contains null element");
		}
		this.sources = sources;
	}

	@Transient
	public boolean isSourceNofly() {
		List<Source> sources = getSources();
		if (sources == null)  return false;

		for (Source source : sources) {
			if (source.isNofly())
				return true;
		}
		return false;
	}

	//Transient
	//XmlElementWrapper(name = "sources")
    //XmlElement(name = "source")
	//public List<Source> getSources()
//	{
//		List<Source> results =  new ArrayList<Source>();
//
//		for (AssetToSource a2s : assetToSources)
//		{
//			results.add(a2s.getSource());
//		}
//
//		return results;
//	}

	@Transient
	public void setSource(int index, Source source) {
		List<Source> sources = getSources();
		if (null != sources) {
			sources.add(index, source);
		}
	}

	@Transient
	public Source getSource(int index) {
		List<Source> sources = getSources();
		if (null != sources) {
			try {
				return sources.get(index);
			} catch (IndexOutOfBoundsException e) {
				return null;
			}
		}
		return null;
	}

	public String getSourcesAsString() {
		List<Source> sources = this.getSources();
		if (sources == null)  return "";

		StringBuilder sb = new StringBuilder();
	    boolean gotOne = false;

		for (Source source : getSources()) {
			if (gotOne) sb.append(", ");
			if (source.isNofly())
				sb.append(source.getName() + "(Inactive)");
			else
				sb.append(source.getName());
			gotOne = true;
		}

		return sb.toString();
	}

    public String getSourcesAsHTML() {
    	return Asset.sourcesAsHTML(getSources());
    }

	/**
	 * This is a copy of the method in JSTLFunctions - can't use use that
	 * method here since it's in the web package and the service package should
	 * not depend on the web package.
	 */
	public static String sourcesAsHTML(List<Source> sources) {
		if (sources == null)  return "";

		StringBuilder sb = new StringBuilder();
	    boolean gotOne = false;

		for (Source source : sources) {
			if (gotOne) sb.append(", ");

			if (source.isNofly()) {
				sb.append("{" + StringEscapeUtils.escapeHtml4(source.getName()) + "}");
			}
			else {
				sb.append(StringEscapeUtils.escapeHtml4(source.getName()));
			}

			gotOne = true;
		}

		return sb.toString();
	}

	/**
	 * If the Asset has no Source, or no Source with a non-blank
	 * Credit Line, then the empty string is returned.
	 * In the case that the Asset has more than one Source,
	 * the first non-blank credit line will be returned.
	 */

	public String getSourceCreditLine() {
		List<Source> sources = this.getSources();
		if (sources == null)  return "";

		for (Source source: sources) {
			String creditLine = source.getCreditLine();
			if (StringUtils.isNotBlank(creditLine)) {
				return creditLine;
			}
		}

		return "";
	}

	/**
	 * This is a Utility method to find out the given asset is part of any of
	 * the given Contracts
	 *
	 * @param asset
	 * @param contracts
	 * @return boolean
	 */
	@Transient
	public boolean isInContracts (List<Contract> contracts) {
		for (Contract contract : contracts) {

			if (null == contract) {
				continue;
			}

			List<ContractAsset> contractAssets = contract.getAssets();

			for (ContractAsset contractAsset : contractAssets) {
				if (contractAsset.getAssetBase().getId().equals(getId())) {
					return true;
				}
			}
		}

		return false;
	}

	public ModelRelease getModelRelease() {
		return modelRelease;
	}

	public void setModelRelease(ModelRelease modelRelease) {
		this.modelRelease = modelRelease;
	}

	public boolean isObtainedByAuthor() {
		return isObtainedByAuthor;
	}

	public void setObtainedByAuthor(boolean isObtainedByAuthor) {
		this.isObtainedByAuthor = isObtainedByAuthor;
	}

	public boolean isPropertyRelease() {
		return isPropertyRelease;
	}

	public void setPropertyRelease(boolean isPropertyRelease) {
		this.isPropertyRelease = isPropertyRelease;
	}

	@XmlElement
	public boolean isWorkForHire() {
		return isWorkForHire;
	}

	public void setWorkForHire(boolean isWorkForHire) {
		this.isWorkForHire = isWorkForHire;
	}

	public boolean isWillBeWorkForHire() {
		return willBeWorkForHire;
	}

	public void setWillBeWorkForHire(boolean willBeWorkForHire) {
		this.willBeWorkForHire = willBeWorkForHire;
		if (willBeWorkForHire) {
			if ((null == this.getOwnerType()) || !(this.getOwnerType().getCode().equals("Author Owned"))) {
				setOwnerType(OwnerType.THIRD_PARTY);
				setManaged(true);
			}
		}
	}

	public boolean isWillBeRoyaltyFree() {
		return willBeRoyaltyFree;
	}

	public void setWillBeRoyaltyFree(boolean willBeRoyaltyFree) {
		this.willBeRoyaltyFree = willBeRoyaltyFree;
		if (willBeRoyaltyFree) {
			setOwnerType(OwnerType.THIRD_PARTY);
			setManaged(true);
		}
	}

	@Override
	public String toString() {
		// always call getters because of the way JPA works
		StringBuilder sb = new StringBuilder();
		sb.append("id = ");
		sb.append(getId());
		sb.append(", externalId = ");
		sb.append(getExternalId());
		sb.append(", description = ");
		sb.append(getDescription());
		sb.append(", creditLine = ");
		sb.append(getCreditLine());
		sb.append(", vendorId = ");
		sb.append(getVendorId());
		sb.append(", mediaType = [");
		sb.append(getMediaType() + "]");
		sb.append(", ownerType = [");
		sb.append(getOwnerType() + "]");
		sb.append(", isManaged = [");
		sb.append(isManaged() + "]");
		// For some reason if we print out sources, during the handling of an
		// UpdateAssetUse message, we get an exception for not being able to lazy
		// load Roles (which a Sources doesn't even have directly - how is a Source
		// related to Role?)
		/*
		Set<Source> sources = getSources();
		sb.append(", sources (" + (sources == null ? 0 : sources.size()) + ") [\r\n");
		for (Source s: sources) {
			sb.append(s.toString());
			sb.append("\r\n");
		}
		*/
		sb.append(", assetGroup = [");
		sb.append(getAssetGroup() + "]");
		sb.append(", copiedFrom = [");
		sb.append(getCopiedFrom() + "]");
		sb.append(", isFeeRequired = [");
		sb.append(isFeeRequired() + "]");
		sb.append(", isRoyaltyFree = [");
		sb.append(isRoyaltyFree() + "]");
		sb.append(", (other attributes)");

		return sb.toString();
	}

	public String getFrom1() {
		return from1;
	}

	public void setFrom1(String from1) {
		this.from1 = StringUtils.trimToNull(from1);
	}

	public String getFrom2() {
		return from2;
	}

	public void setFrom2(String from2) {
		this.from2 = StringUtils.trimToNull(from2);
	}

	public String getFrom3() {
		return from3;
	}

	public void setFrom3(String from3) {
		this.from3 = StringUtils.trimToNull(from3);
	}

	public String getOriginalPublicationAuthor() {
		return originalPublicationAuthor;
	}

	public void setOriginalPublicationAuthor(String originalPublicationAuthor) {
		this.originalPublicationAuthor = StringUtils.trimToNull(originalPublicationAuthor);
	}

	public String getOriginalPublicationIsbn() {
		return originalPublicationIsbn;
	}

	public void setOriginalPublicationIsbn(String originalPublicationIsbn) {
		this.originalPublicationIsbn = StringUtils.trimToNull(originalPublicationIsbn);
	}

	public String getOriginalPublicationTitle() {
		return originalPublicationTitle;
	}

	public void setOriginalPublicationTitle(String originalPublicationTitle) {
		this.originalPublicationTitle = StringUtils.trimToNull(originalPublicationTitle);
	}

	public String getTo1() {
		return to1;
	}

	public void setTo1(String to1) {
		this.to1 = to1;
	}

	public String getTo2() {
		return to2;
	}

	public void setTo2(String to2) {
		this.to2 = to2;
	}

	public String getTo3() {
		return to3;
	}

	public void setTo3(String to3) {
		this.to3 = to3;
	}

	public String getOriginalArticleTitle() {
		return originalArticleTitle;
	}

	public void setOriginalArticleTitle(String originalArticleTitle) {
		this.originalArticleTitle = StringUtils.trimToNull(originalArticleTitle);
	}

	public String getOriginalArticleAuthor() {
		return originalArticleAuthor;
	}

	public void setOriginalArticleAuthor(String originalArticleAuthor) {
		this.originalArticleAuthor = StringUtils.trimToNull(originalArticleAuthor);
	}

	public String getOriginalFigureNumber() {
		return originalFigureNumber;
	}

	public void setOriginalFigureNumber(String figureNumber) {
		this.originalFigureNumber = StringUtils.trimToNull(figureNumber);
	}

	public String getOriginalPageNumber() {
		return originalPageNumber;
	}

	public void setOriginalPageNumber(String pageNumber) {
		this.originalPageNumber = StringUtils.trimToNull(pageNumber);
	}

	@XmlElement
	public boolean isManaged() {
		return isManaged;
	}

	public void setManaged(boolean isManaged) {
		this.isManaged = isManaged;
		if (isManaged) {
			setRoyaltyFree(false);
		}
	}

	@XmlElement
	public boolean isRoyaltyFree() {
		return isRoyaltyFree;
	}

	public void setRoyaltyFree(boolean isRoyaltyFree) {
		this.isRoyaltyFree = isRoyaltyFree;
		if (isRoyaltyFree) {
			setManaged(false);
		}
	}

	public boolean isArchive() {
		return archive;
	}

	public void setArchive(boolean archive) {
		this.archive = archive;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = StringUtils.trimToNull(keywords);
	}

	public int getTotalSeats() {
		return totalSeats;
	}

	public void setTotalSeats(int i) {
		totalSeats = i;
	}

	public int getTotalPrintRun() {
		return totalPrintRun;
	}

	public void setTotalPrintRun(int i) {
		totalPrintRun = i;
	}

	public int getTotalUsedSeats() {
		return totalUsedSeats;
	}

	public void setTotalUsedSeats(int i) {
		totalUsedSeats = i;
	}

	public String getCitation() {
		return citation;
	}

	public void setCitation(String citation) {
		this.citation = citation;
	}

	public String getBiblio() {
		return biblio;
	}

	public void setBiblio(String biblio) {
		this.biblio = biblio;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = StringUtils.trimToNull(filename);
	}

	public ImportSource getImportSource() {
		return importSource;
	}

	public void setImportSource(ImportSource importSource) {
		this.importSource = importSource;
	}

	public boolean isReviewedUnknown() {
		return reviewedUnknown;
	}

	public void setReviewedUnknown(boolean reviewedUnknown) {
		this.reviewedUnknown = reviewedUnknown;
	}

	public boolean isAuthorProvidedUnknown() {
		return authorProvidedUnknown;
	}

	public void setAuthorProvidedUnknown(boolean authorProvidedUnknown) {
		this.authorProvidedUnknown = authorProvidedUnknown;
	}

	public String getPositionsAsString() {
		if (StringUtils.isBlank(positionsAsString))
			return StringUtils.EMPTY;
		return positionsAsString;
	}

	public void setPositionsAsString(String positionsAsString) {
		this.positionsAsString = positionsAsString;
	}

	@Transient
	public List<AssetToSource> getAssetToSources() {
		if (CollectionUtils.isNotEmpty(assetToSources)) return assetToSources;

		List<AssetToSource> work = new ArrayList<AssetToSource>();

		for (Source source : getSourcesNotNull()) {
			AssetToSource a2s = new AssetToSource();
			a2s.setAsset(this);
			a2s.setAssetId(this.getId());
			a2s.setSource(source);
			a2s.setSourceId(source.getId());
			work.add(a2s);
		}
		this.assetToSources = work;
		return work;
	}

	@Transient
	public void setAssetToSources(List<AssetToSource> assetToSources) {
		this.assetToSources = assetToSources;
	}

	@Transient
	public AssetToSource addSource(Source newSource) {
		// if it already exists return it
		for (AssetToSource test : getAssetToSources()) {
			if (test.getSource().getName().equals(newSource.getName())) {
				return test;
			}
		}

		AssetToSource newAssetToSource = new AssetToSource();
		newAssetToSource.setSourceId(newSource.getId());
		newAssetToSource.setAssetId(this.getId());

		newAssetToSource.setAsset(this);
		newAssetToSource.setSource(newSource);
		getAssetToSources().add(newAssetToSource);
	//	this.sources.add(newSource);
	//	this.sources.add(newSource);
		return newAssetToSource;
	}
}
