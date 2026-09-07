package com.wiley.permissions.domain.persistence.permissions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Transient;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.utils.ValidateException;

/**
 * We need this extension so we can get data from spreadsheet, and tries to map
 * the data to "correct" fields in AssetUse and Asset object
 *
 * @author lnagy
 */
public class ExtendedAssetUse extends AssetUse {

	private static final Log log = LogFactory.getLog(ExtendedAssetUse.class);

	private static final int DESCRIPTION_LENGTH = 500;

	private static final long serialVersionUID = 1L;

	public static final Map<String, Size> sizeMapping = new HashMap<String, Size>() {{
		put("1 1/2 Page", Size.ONE_AND_12_PAGE);
		put("1 1/4 Page", Size.ONE_AND_14_PAGE);
		put("1 3/4 Page", Size.ONE_AND_34_PAGE);
		put("1/2 Page", Size.HALF_PAGE);
		put("1/4 Page", Size.QUARTER_PAGE);
		put("3/4 Page", Size.THREE_QUARTER_PAGE);
		put("1/8 Page", Size.ONE_EIGHTH);
		put("Double Page", Size.DOUBLE_PAGE);
		put("Full Page", Size.FULL_PAGE);
		put("Spot", Size.SPOT);
		put("1/4 -One Quarter", Size.QUARTER_PAGE);
		put("1/4-One Quarter", Size.QUARTER_PAGE);
		put("1/2-One Half", Size.HALF_PAGE);
		put("2-Two Pages", Size.DOUBLE_PAGE);
		put("1-Full Page", Size.FULL_PAGE);
		put("3/4-Three Quarter", Size.THREE_QUARTER_PAGE);
		put("1 1/2-One and a Half", Size.ONE_AND_12_PAGE);
		put("1 3/4-One and Three Quarter", Size.ONE_AND_34_PAGE);
		put("1 1/4-One and a Quarter", Size.ONE_AND_14_PAGE);
		put("N/A", Size.NA);
		put("1/3 Page", Size.THIRD_PAGE);
	}};

	@Transient
	private String isb10;
	@Transient
	private String isb13;

	@Transient
	private Contract contract;

	@Transient
	private String blackAndWhite;

	@Transient
	private String inColor;

	@Transient
	private String cameraCopy;

	@Transient
	private String royaltyFree;

	@Transient
	private String reuse;

	@Transient
	private String isExtPickup;

	@Transient
	private String modelRelease;

	@Transient
	private String obtainedByAuthor;

	@Transient
	private String photoSize;

	@Transient
	private String workForHire;

	@Transient
	private String chapterOpener;

	@Transient
	private String free;

	@Transient
	private String isExtNew;

	@Transient
	private String isExtArchive;

	@Transient
	private String extSortOrder;

	@Transient
	private String typeFigure;

	@Transient
	private String typeSection;

	@Transient
	private String canceled;

	@Transient
	private String permissionForm;

	@Transient
	private boolean isPermissionFormComplete;

	@Transient
	private String isExtManaged;

	@Transient
	private String isExtRoyaltyFree;

	@Transient
	private String permissionType;


	// import related fields for conditions
	@Transient
	private String printRunCondition;
	@Transient
	private String ebookPrintRunCondition;
	@Transient
	private String printRunIncludeEbooks;
	@Transient
	private String languageCondition;
	@Transient
	private String salesTerritoryCondition;
	@Transient
	private String unlimitedPrintRunCondition;
	@Transient
	private String territoryComments;
	@Transient
	private Integer dealId;
	@Transient
	private Double price;
	@Transient
	private Integer count;

	//Start: Changes made for the new spreadsheet upload requirement DM-289
	@Transient
	private String batchNumber;
	@Transient
	private String strSortOrder;
	@Transient
	private String componentNameString;
	public String getComponentNameString() {
		return componentNameString;
	}

	public void setComponentNameString(String componentNameString) {
		this.componentNameString = componentNameString;
	}

	@Transient
	private String usageDesc;
	@Transient
	private String mediaTypeDesc;
	@Transient
	private String strPosition;
	@Transient
	private String assetDescription;
	@Transient
	private String assetArtist;
	@Transient
	private String getSourceName;
	@Transient
	private String assetSourceVendorId;
	@Transient
	private String assetOriginalPubIsbn;
	@Transient
	private String assetOriginalPubTitle;
	@Transient
	private String assetOriginalArticleTitle;
	@Transient
	private String assetOriginalPubAuthor;
	@Transient
	private String assetOriginalPubPageNo;
	@Transient
	private String assetOriginalPubDate;
	@Transient
	private String assetCreditLine;
	@Transient
	private String assetOwnerTypeCode;
	@Transient
	private String assetModelRelease;
	@Transient
	private String willBeRoyaltyFree;
	@Transient
	private String willBeWorkForHire;
	@Transient
	private String publicDomain;
	@Transient
	private String fairUse;
	//Complete Contract file names string with semicolon separator
	@Transient
	private String contractFileNamesStr; //Added for DM-532
	@Transient
	private List<ContractFileName> contractFileList = null; //Added for DM-532
	@Transient
	private String contractNoStr;
	@Transient
	private String contractCurrencyDesc;
	@Transient
	private String contractPrice;
	@Transient
	private String rightsLinkLicenseNumber;
	@Transient
	private String agreedPrintRun;
	@Transient
	private String doesAgreedPrintRunIncludeEBooks;
	@Transient
	private String eBookPrintRun;
	@Transient
	private String isSizeLimited;
	@Transient
	private String sizeVal;
	@Transient
	private String allLanguagesGranted;
	@Transient
	private String languageLimitation;
	@Transient
	private String grantedWorldSalesTerritory;
	@Transient
	private String salesTerritoryLimitation;
	@Transient
	private String doesDerivativeCusRightsGranted;
	@Transient
	private String customOrDerivative;
	@Transient
	private String clearedForAllMediaTypes;
	@Transient
	private String mediaLimitations;
	@Transient
	private String clearedForAllFutureEditions;
	@Transient
	private String editionLimitation;
	@Transient
	private String doesSublicensingGranted;
	@Transient
	private String permissionCommentStr;
	@Transient
	private String productionCommentStr;
	@Transient
	private String numberOfCompCopies;
	@Transient
	private String compRecipient;
	@Transient
	private Address address;
	@Transient
	private String continuedUse;
	@Transient
	private String continuedISBN;
	@Transient
	private String continuedPosition;
	@Transient
	private String continuedPage;
	@Transient
	private String continuedComment;
	@Transient
	private String pplQAEcolumns;
	/*@Transient
	private String originalPublicationDate;*/
	@Transient
	private String date;
	@Transient
	private String contractStartDate;
	@Transient
	private String contractEndDate;


	public String getBatchNumber() {
		return batchNumber;
	}

	public void setBatchNumber(String batchNumber) {
		this.batchNumber = batchNumber;
	}

	public String getStrSortOrder() {
		return strSortOrder;
	}

	public void setStrSortOrder(String strSortOrder) {
		this.strSortOrder = strSortOrder;
	}

	/*public String getComponentNameStr() {
		return componentNameString;
	}

	public void setComponentName(String componentNameStr) {
		this.componentNameString = componentNameStr;
	}*/

	public String getUsageDesc() {
		return usageDesc;
	}

	public void setUsageDesc(String usageDesc) {
		this.usageDesc = usageDesc;
	}

	public String getMediaTypeDesc() {
		return mediaTypeDesc;
	}

	public void setMediaTypeDesc(String mediaTypeDesc) {
		this.mediaTypeDesc = mediaTypeDesc;
	}

	public String getStrPosition() {
		return strPosition;
	}

	public void setStrPosition(String strPosition) {
		this.strPosition = strPosition;
	}

	public String getAssetDescription() {
		return assetDescription;
	}

	public void setAssetDescription(String assetDescription) {
		this.assetDescription = assetDescription;
	}

	public String getAssetArtist() {
		return assetArtist;
	}

	public void setAssetArtist(String assetArtist) {
		this.assetArtist = assetArtist;
	}

	public String getGetSourceName() {
		return getSourceName;
	}

	public void setGetSourceName(String getSourceName) {
		this.getSourceName = getSourceName;
	}

	public String getAssetSourceVendorId() {
		return assetSourceVendorId;
	}

	public void setAssetSourceVendorId(String assetSourceVendorId) {
		this.assetSourceVendorId = assetSourceVendorId;
	}

	public String getAssetOriginalPubIsbn() {
		return assetOriginalPubIsbn;
	}

	public void setAssetOriginalPubIsbn(String assetOriginalPubIsbn) {
		this.assetOriginalPubIsbn = assetOriginalPubIsbn;
	}

	public String getAssetOriginalPubTitle() {
		return assetOriginalPubTitle;
	}

	public void setAssetOriginalPubTitle(String assetOriginalPubTitle) {
		this.assetOriginalPubTitle = assetOriginalPubTitle;
	}

	public String getAssetOriginalArticleTitle() {
		return assetOriginalArticleTitle;
	}

	public void setAssetOriginalArticleTitle(String assetOriginalArticleTitle) {
		this.assetOriginalArticleTitle = assetOriginalArticleTitle;
	}

	public String getAssetOriginalPubAuthor() {
		return assetOriginalPubAuthor;
	}

	public void setAssetOriginalPubAuthor(String assetOriginalPubAuthor) {
		this.assetOriginalPubAuthor = assetOriginalPubAuthor;
	}

	public String getAssetOriginalPubPageNo() {
		return assetOriginalPubPageNo;
	}

	public void setAssetOriginalPubPageNo(String assetOriginalPubPageNo) {
		this.assetOriginalPubPageNo = assetOriginalPubPageNo;
	}

	public String getAssetOriginalPubDate() {
		return assetOriginalPubDate;
	}

	public void setAssetOriginalPubDate(String assetOriginalPubDate) {
		this.assetOriginalPubDate = assetOriginalPubDate;
	}

	public String getAssetCreditLine() {
		return assetCreditLine;
	}

	public void setAssetCreditLine(String assetCreditLine) {
		this.assetCreditLine = assetCreditLine;
	}

	public String getAssetOwnerTypeCode() {
		return assetOwnerTypeCode;
	}

	public void setAssetOwnerTypeCode(String assetOwnerTypeCode) {
		this.assetOwnerTypeCode = assetOwnerTypeCode;
	}

	public String getAssetModelRelease() {
		return assetModelRelease;
	}

	public void setAssetModelRelease(String assetModelRelease) {
		this.assetModelRelease = assetModelRelease;
	}

	public String getWillBeRoyaltyFree() {
		return willBeRoyaltyFree;
	}

	public void setWillBeRoyaltyFree(String willBeRoyaltyFree) {
		this.willBeRoyaltyFree = willBeRoyaltyFree;
	}

	public String getWillBeWorkForHire() {
		return willBeWorkForHire;
	}

	public void setWillBeWorkForHire(String willBeWorkForHire) {
		this.willBeWorkForHire = willBeWorkForHire;
	}

	public String getPublicDomain() {
		return publicDomain;
	}

	public void setPublicDomain(String publicDomain) {
		this.publicDomain = publicDomain;
	}

	public String getFairUse() {
		return fairUse;
	}

	public void setFairUse(String fairUse) {
		this.fairUse = fairUse;
	}

	//Start: Added for DM-532
	public String getContractFileNamesStr() {
		return contractFileNamesStr;
	}

	public void setContractFileNamesStr(String contractFileNamesStr) {
		this.contractFileNamesStr = contractFileNamesStr;
	}

	public List<ContractFileName> getContractFileList() {
		return contractFileList;
	}

	public void setContractFileList(List<ContractFileName> contractFileList) {
		this.contractFileList = contractFileList;
	}
	//End: Added for DM-532

	public String getContractNoStr() {
		return contractNoStr;
	}

	public void setContractNoStr(String contractNoStr) {
		this.contractNoStr = contractNoStr;
	}

	public String getContractCurrencyDesc() {
		return contractCurrencyDesc;
	}

	public void setContractCurrencyDesc(String contractCurrencyDesc) {
		this.contractCurrencyDesc = contractCurrencyDesc;
	}

	public String getContractPrice() {
		return contractPrice;
	}

	public void setContractPrice(String contractPrice) {
		this.contractPrice = contractPrice;
	}

	public String getRightsLinkLicenseNumber() {
		return rightsLinkLicenseNumber;
	}

	public void setRightsLinkLicenseNumber(String rightsLinkLicenseNumber) {
		this.rightsLinkLicenseNumber = rightsLinkLicenseNumber;
	}

	public String getAgreedPrintRun() {
		return agreedPrintRun;
	}

	public void setAgreedPrintRun(String agreedPrintRun) {
		this.agreedPrintRun = agreedPrintRun;
	}

	public String getDoesAgreedPrintRunIncludeEBooks() {
		return doesAgreedPrintRunIncludeEBooks;
	}

	public void setDoesAgreedPrintRunIncludeEBooks(
			String doesAgreedPrintRunIncludeEBooks) {
		this.doesAgreedPrintRunIncludeEBooks = doesAgreedPrintRunIncludeEBooks;
	}

	public String geteBookPrintRun() {
		return eBookPrintRun;
	}

	public void seteBookPrintRun(String eBookPrintRun) {
		this.eBookPrintRun = eBookPrintRun;
	}

	public String getIsSizeLimited() {
		return isSizeLimited;
	}

	public void setIsSizeLimited(String isSizeLimited) {
		this.isSizeLimited = isSizeLimited;
	}

	public String getSizeVal() {
		return sizeVal;
	}

	public void setSizeVal(String sizeVal) {
		this.sizeVal = sizeVal;
	}

	public String getAllLanguagesGranted() {
		return allLanguagesGranted;
	}

	public void setAllLanguagesGranted(String allLanguagesGranted) {
		this.allLanguagesGranted = allLanguagesGranted;
	}

	public String getLanguageLimitation() {
		return languageLimitation;
	}

	public void setLanguageLimitation(String languageLimitation) {
		this.languageLimitation = languageLimitation;
	}

	public String getGrantedWorldSalesTerritory() {
		return grantedWorldSalesTerritory;
	}

	public void setGrantedWorldSalesTerritory(String grantedWorldSalesTerritory) {
		this.grantedWorldSalesTerritory = grantedWorldSalesTerritory;
	}

	public String getSalesTerritoryLimitation() {
		return salesTerritoryLimitation;
	}

	public void setSalesTerritoryLimitation(String salesTerritoryLimitation) {
		this.salesTerritoryLimitation = salesTerritoryLimitation;
	}

	public String getDoesDerivativeCusRightsGranted() {
		return doesDerivativeCusRightsGranted;
	}

	public void setDoesDerivativeCusRightsGranted(
			String doesDerivativeCusRightsGranted) {
		this.doesDerivativeCusRightsGranted = doesDerivativeCusRightsGranted;
	}

	public String getCustomOrDerivative() {
		return customOrDerivative;
	}

	public void setCustomOrDerivative(String customOrDerivative) {
		this.customOrDerivative = customOrDerivative;
	}

	public String getClearedForAllMediaTypes() {
		return clearedForAllMediaTypes;
	}

	public void setClearedForAllMediaTypes(String clearedForAllMediaTypes) {
		this.clearedForAllMediaTypes = clearedForAllMediaTypes;
	}

	public String getMediaLimitations() {
		return mediaLimitations;
	}

	public void setMediaLimitations(String mediaLimitations) {
		this.mediaLimitations = mediaLimitations;
	}

	public String getClearedForAllFutureEditions() {
		return clearedForAllFutureEditions;
	}

	public void setClearedForAllFutureEditions(String clearedForAllFutureEditions) {
		this.clearedForAllFutureEditions = clearedForAllFutureEditions;
	}

	public String getEditionLimitation() {
		return editionLimitation;
	}

	public void setEditionLimitation(String editionLimitation) {
		this.editionLimitation = editionLimitation;
	}

	public String getDoesSublicensingGranted() {
		return doesSublicensingGranted;
	}

	public void setDoesSublicensingGranted(String doesSublicensingGranted) {
		this.doesSublicensingGranted = doesSublicensingGranted;
	}

	public String getPermissionCommentStr() {
		return permissionCommentStr;
	}

	public void setPermissionCommentStr(String permissionCommentStr) {
		this.permissionCommentStr = permissionCommentStr;
	}

	public String getProductionCommentStr() {
		return productionCommentStr;
	}

	public void setProductionCommentStr(String productionCommentStr) {
		this.productionCommentStr = productionCommentStr;
	}

	public String getNumberOfCompCopies() {
		return numberOfCompCopies;
	}

	public void setNumberOfCompCopies(String numberOfCompCopies) {
		this.numberOfCompCopies = numberOfCompCopies;
	}

	public String getCompRecipient() {
		return compRecipient;
	}

	public void setCompRecipient(String compRecipient) {
		this.compRecipient = compRecipient;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public String getContinuedUse() {
		return continuedUse;
	}

	public void setContinuedUse(String continuedUse) {
		this.continuedUse = continuedUse;
	}

	public String getContinuedISBN() {
		return continuedISBN;
	}

	public void setContinuedISBN(String continuedISBN) {
		this.continuedISBN = continuedISBN;
	}

	public String getContinuedPosition() {
		return continuedPosition;
	}

	public void setContinuedPosition(String continuedPosition) {
		this.continuedPosition = continuedPosition;
	}

	public String getContinuedPage() {
		return continuedPage;
	}

	public void setContinuedPage(String continuedPage) {
		this.continuedPage = continuedPage;
	}

	public String getContinuedComment() {
		return continuedComment;
	}

	public void setContinuedComment(String continuedComment) {
		this.continuedComment = continuedComment;
	}

	public String getPplQAEcolumns() {
		return pplQAEcolumns;
	}

	public void setPplQAEcolumns(String pplQAEcolumns) {
		this.pplQAEcolumns = pplQAEcolumns;
	}

	/*public String getOriginalPublicationDate() {
		return originalPublicationDate;
	}

	public void setOriginalPublicationDate(String originalPublicationDate) {
		this.originalPublicationDate = originalPublicationDate;
	}*/

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getContractStartDate() {
		return contractStartDate;
	}

	public void setContractStartDate(String contractStartDate) {
		this.contractStartDate = contractStartDate;
	}

	public String getContractEndDate() {
		return contractEndDate;
	}

	public void setContractEndDate(String contractEndDate) {
		this.contractEndDate = contractEndDate;
	}

	//End: Changes made for the new spreadsheet upload requirement DM-289

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public ExtendedAssetUse() {
	}

	public ExtendedAssetUse(AssetUse assetUse, Contract contract) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		PropertyUtils.copyProperties(this, assetUse);
		if (null != contract) {
			contract.setCommonWork(assetUse.getCommonWork());
			contract.setPurchaseOrder(null);
		}
		this.contract = contract;
	}

	public ExtendedAssetUse(AssetUse assetUse) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		PropertyUtils.copyProperties(this, assetUse);
	}

	@Transient
	public String getTerritoryComments() {
		return territoryComments;
	}

	@Transient
	public void setTerritoryComments(String territoryComments) {
		this.territoryComments = territoryComments;
	}

	@Transient
	public String getPrintRunCondition() {
		return printRunCondition;
	}

	@Transient
	public void setPrintRunCondition(String printRunCondition) {
		this.printRunCondition = printRunCondition;
	}

	@Transient
	public String getEbookPrintRunCondition() {
		return ebookPrintRunCondition;
	}

	@Transient
	public void setEbookPrintRunCondition(String ebookPrintRunCondition) {
		this.ebookPrintRunCondition = ebookPrintRunCondition;
	}

	@Transient
	public String getPrintRunIncludeEbooks() {
		return printRunIncludeEbooks;
	}

	@Transient
	public void setPrintRunIncludeEbooks(String printRunIncludeEbooks) {
		this.printRunIncludeEbooks = printRunIncludeEbooks;
	}

	@Transient
	public String getLanguageCondition() {
		return languageCondition;
	}

	@Transient
	public void setLanguageCondition(String languageCondition) {
		this.languageCondition = languageCondition;
	}

	@Transient
	public String getSalesTerritoryCondition() {
		return salesTerritoryCondition;
	}

	@Transient
	public void setSalesTerritoryCondition(String salesTerritoryCondition) {
		this.salesTerritoryCondition = salesTerritoryCondition;
	}

	@Transient
	public String getUnlimitedPrintRunCondition() {
		return unlimitedPrintRunCondition;
	}

	@Transient
	public void setUnlimitedPrintRunCondition(String unlimitedPrintRunCondition) {
		this.unlimitedPrintRunCondition = unlimitedPrintRunCondition;
	}

	public String getIsExtManaged() {
		return isExtManaged;
	}

	public void setIsExtManaged(String isExtManaged) {
		this.isExtManaged = isExtManaged;
	}

	public String getIsExtRoyaltyFree() {
		return isExtRoyaltyFree;
	}

	public void setIsExtRoyaltyFree(String isExtRoyaltyFree) {
		this.isExtRoyaltyFree = isExtRoyaltyFree;
	}

	public boolean isPermissionFormComplete() {
		return isPermissionFormComplete;
	}

	public void isPermissionFormComplete(boolean isPermissionFormComplete) {
		this.isPermissionFormComplete = isPermissionFormComplete;
		if (isPermissionFormComplete) {
			if (null == contract)
				contract = createPermissionForm();
			else {
				contract.setPermissionForm(true);
				contract.setEndDate(null);
				contract.setConditions(null);
				contract.setConditionNodeTree(null);
			}
		}
	}

	public String getPermissionForm() {
		return permissionForm;
	}

	public void setPermissionForm(String permissionForm) {
		this.permissionForm = permissionForm;
	}

	public String getBlackAndWhite() {
		return blackAndWhite;
	}

	public void setBlackAndWhite(String blackAndWhite) {
		this.blackAndWhite = blackAndWhite;
	}

	public String getCameraCopy() {
		return cameraCopy;
	}

	public void setCameraCopy(String cameraCopy) {
		this.cameraCopy = cameraCopy;
	}

	public String getInColor() {
		return inColor;
	}

	public void setInColor(String color) {
		this.inColor = color;
	}

	public String getModelRelease() {
		return modelRelease;
	}

	public void setModelRelease(String modelRelease) {
		this.modelRelease = modelRelease;
	}

	public String getObtainedByAuthor() {
		return obtainedByAuthor;
	}

	public void setObtainedByAuthor(String obtainedByAuthor) {
		this.obtainedByAuthor = obtainedByAuthor;
	}

	public String getPhotoSize() {
		return photoSize;
	}

	public void setPhotoSize(String photoSize) {
		this.photoSize = photoSize;
	}

	public String getReuse() {
		return reuse;
	}

	public void setReuse(String reuse) {
		this.reuse = reuse;
	}

	public String getRoyaltyFree() {
		return royaltyFree;
	}

	public void setRoyaltyFree(String royaltyFree) {
		this.royaltyFree = royaltyFree;
	}

	public String getWorkForHire() {
		return workForHire;
	}

	public void setWorkForHire(String workForHire) {
		this.workForHire = workForHire;
	}

	public String getChapterOpener() {
		return chapterOpener;
	}

	public void setChapterOpener(String chapterOpener) {
		this.chapterOpener = chapterOpener;
	}

	public String getFree() {
		return free;
	}

	public void setFree(String free) {
		this.free = free;
	}

	public String getTypeFigure() {
		return typeFigure;
	}

	public void setTypeFigure(String typeFigure) {
		this.typeFigure = typeFigure;
	}

	/**
	 * will take all the string attributes from spread sheet template and populate the correct
	 * AssetUse fields
	 */
	@Transient
	public void mapExcelData() {
		Component component = null;
		ComponentCategory cc = null;
		Asset asset = new Asset();
		Source source = null;
		List<Source> sources = null;
		Contract contract = null;
		CompCopy compCopy = null;
		List<CompCopy> compCopyList = null;
		setAsset(asset);
		String dateStr = null;
		String sizeStr = null;
		String permComment = null;
		String continuedUse = null;
		String componentStr = null;
		String ownerTypeText = "";
		Date date = null;
		if(StringUtils.isNotBlank(getStrSortOrder())) {
			setSortOrder(getStrSortOrder().trim());
		}
		if(StringUtils.isNotBlank(getComponentNameString())) {
			componentStr = getComponentNameString().trim();
			component = new Component();
			setComponent(component);
			component.setName(componentStr);
			component.setCommonWork(this.getCommonWork());
			cc = ComponentCategory.getByMatchingDescription(componentStr);
			component.setCategory(cc);
		}
		if(StringUtils.isNotBlank(getUsageDesc())) {
			setUsage(Usage.getByDescription(getUsageDesc().trim()));
		} else {
			setUsage(Usage.FIGURE);
		}
		if(StringUtils.isNotBlank(getMediaTypeDesc())) {
			getAsset().setMediaType(MediaType.getByDescription(getMediaTypeDesc().trim()));
		} else {
			getAsset().setMediaType(MediaType.PHOTO);
		}
		if(StringUtils.isNotBlank(getStrPosition())) {
			setPosition(getStrPosition().trim());
		}
		if(StringUtils.isNotBlank(getAssetDescription())) {
			getAsset().setDescription(getAssetDescription().trim());
		}
		if(StringUtils.isNotBlank(getAssetArtist())) {
			getAsset().setArtist(getAssetArtist().trim());
		}
		if(StringUtils.isNotBlank(getGetSourceName())) {
			source = new Source();
			source.setName(getGetSourceName().trim());
			sources = new ArrayList<Source>();
			sources.add(source);
			getAsset().setSources(sources);
		}
		if(StringUtils.isNotBlank(getAssetSourceVendorId())) {
			getAsset().setVendorId(getAssetSourceVendorId().trim());
		}
		if(StringUtils.isNotBlank(getAssetOriginalPubIsbn())) {
			getAsset().setOriginalPublicationIsbn(getAssetOriginalPubIsbn().trim());
		}
		if(StringUtils.isNotBlank(getAssetOriginalPubTitle())) {
			getAsset().setOriginalPublicationTitle(getAssetOriginalPubTitle().trim());
		}
		if(StringUtils.isNotBlank(getAssetOriginalArticleTitle())) {
			getAsset().setOriginalArticleTitle(getAssetOriginalArticleTitle().trim());
		}
		if(StringUtils.isNotBlank(getAssetOriginalPubAuthor())) {
			getAsset().setOriginalPublicationAuthor(getAssetOriginalPubAuthor().trim());
		}
		if(StringUtils.isNotBlank(getAssetOriginalPubPageNo())) {
			getAsset().setOriginalPageNumber(getAssetOriginalPubPageNo().trim());
		}
		if(StringUtils.isNotBlank(getAssetOriginalPubDate())) {
			dateStr = getAssetOriginalPubDate().trim();
			date = getDate(dateStr);
			getAsset().setOriginalPublicationDate(date);
		}
		if(StringUtils.isNotBlank(getAssetCreditLine())) {
			getAsset().setCreditLine(getAssetCreditLine().trim());
		}
		if(StringUtils.isNotBlank(getAssetOwnerTypeCode())) {
			ownerTypeText = getAssetOwnerTypeCode().trim();
			if(ownerTypeText.equalsIgnoreCase("3rd Party")) {
				getAsset().setOwnerType(OwnerType.THIRD_PARTY);
			}
			else if(ownerTypeText.equalsIgnoreCase("Photo Request") || ownerTypeText.equalsIgnoreCase("Illustration Request")) {
				getAsset().setOwnerType(OwnerType.PHOTO_REQUEST);
			}
			else if(ownerTypeText.equalsIgnoreCase("Wiley Created/Owned")) {
				getAsset().setOwnerType(OwnerType.WILEY_CREATED);
			}
			else if(ownerTypeText.equalsIgnoreCase("Author Owned")) {
				getAsset().setOwnerType(OwnerType.AUTHOR_OWNED);
			}
			else if(ownerTypeText.equalsIgnoreCase("Manager Approved")) {
				getAsset().setManagerApproved(true);
			}
			else {
				getAsset().setOwnerType(OwnerType.THIRD_PARTY);
			}
		}
		if(StringUtils.isNotBlank(getAssetModelRelease())) {
			getAsset().setModelRelease(ModelRelease.getByDescription(getAssetModelRelease().trim()));
		}
		if(!ownerTypeText.equalsIgnoreCase("Manager Approved")) {
			//If OwnerType is "Manager Approved", then we can ignore the below 4 properties. Hence they are moved inside this if condition.
			getAsset().setRoyaltyFree(getBoolean(getWillBeRoyaltyFree()));
			if(getBoolean(getPublicDomain())) {
				getAsset().setManaged(false);
				getAsset().setRoyaltyFree(true);
			}
			getAsset().setWillBeWorkForHire(getBoolean(getWillBeWorkForHire()));
			if(getBoolean(getWillBeWorkForHire())) {
				getAsset().setOwnerType(OwnerType.WORK_FOR_HIRE);
			}
			if(getBoolean(getFairUse())) {
				getAsset().setManaged(false);
				getAsset().setFeeRequired(false);
				getAsset().setRoyaltyFree(true);
			}
		}

		if(StringUtils.isNotBlank(getContractNoStr()) || StringUtils.isNotBlank(getDate()) || StringUtils.isNotBlank(getContractCurrencyDesc()) ||
				StringUtils.isNotBlank(getContractFileNamesStr()) || StringUtils.isNotBlank(getContractPrice()) ||
				StringUtils.isNotBlank(getContractStartDate()) || StringUtils.isNotBlank(getContractEndDate())) {
			contract = new Contract();
		}
		if(StringUtils.isNotBlank(getContractNoStr())) {
			contract.setNumber(getContractNoStr().trim());
		}
		//Start: Added for DM-532
		if(StringUtils.isNotBlank(getContractFileNamesStr())) {
			String contractFileName = getContractFileNamesStr();
			String[] fileNames = contractFileName.trim().split(";");
			List<String> namesList = new ArrayList<String>(fileNames.length);
			List<ContractFileName> fileNamesList = new ArrayList<ContractFileName>();
			boolean isDistinct;
			for  (int i = 0; i < fileNames.length; i++) {
				isDistinct = false;
				for (int j = 0; j < i; j++) {
					if(fileNames[i].trim().equalsIgnoreCase(fileNames[j].trim())) {
						isDistinct = true;
						break;
					}
				}
				if(!isDistinct) {
					namesList.add(fileNames[i]);
				}
			}
			if(namesList.size()>0) {
				for (String fileName : namesList) {
					ContractFileName cfName = new ContractFileName();
					fileName = fileName.trim();
					cfName.setFileName(fileName);
					fileNamesList.add(cfName);
				}
			}
			//contract.setFileNames(fileNamesList);
			this.setContractFileList(fileNamesList);
		}
		//End: Added for DM-532
		if(StringUtils.isNotBlank(getDate())) {
			dateStr = getDate().trim();
			date = getDate(dateStr);
			contract.setDate(date);
		}
		if(StringUtils.isNotBlank(getContractCurrencyDesc())) {
			contract.setCurrency(Currency.getByDescription(getContractCurrencyDesc().trim()));
		}
		else {
			if(null != contract) {
				contract.setCurrency(Currency.US);
			}
		}
		if(StringUtils.isNotBlank(getContractPrice())) {
			contract.setPrice(Double.parseDouble(getContractPrice().trim()));
		}
		if(StringUtils.isNotBlank(getContractStartDate())) {
			dateStr = getContractStartDate().trim();
			date = getDate(dateStr);
			contract.setStartDate(date);
		}
		if(StringUtils.isNotBlank(getContractEndDate())) {
			dateStr = getContractEndDate().trim();
			date = getDate(dateStr);
			contract.setEndDate(date);
		}
		setContract(contract);
		if(StringUtils.isNotBlank(getRightsLinkLicenseNumber())) {
			this.setRightsLinkLicenseNumber(getRightsLinkLicenseNumber().trim());
		}
		if(StringUtils.isNotBlank(getAgreedPrintRun())) {
			this.setAgreedPrintRun(getAgreedPrintRun().trim());
		}
		if(StringUtils.isNotBlank(geteBookPrintRun())) {
			this.seteBookPrintRun(geteBookPrintRun().trim());
		}
		if(StringUtils.isNotBlank(getSizeVal())) {
			sizeStr = getSizeVal().trim();
			setSize(getSizeObject(sizeStr));
		}
		if(StringUtils.isNotBlank(getLanguageLimitation())) {
			this.setLanguageLimitation(getLanguageLimitation().trim());
		}
		if(StringUtils.isNotBlank(getSalesTerritoryLimitation())) {
			this.setSalesTerritoryLimitation(getSalesTerritoryLimitation().trim());
		}
		if(StringUtils.isNotBlank(getCustomOrDerivative())) {
			this.setCustomOrDerivative(getCustomOrDerivative().trim());
		}
		if(StringUtils.isNotBlank(getMediaLimitations())) {
			this.setMediaLimitations(getMediaLimitations().trim());
		}
		if(StringUtils.isNotBlank(getEditionLimitation())) {
			this.setEditionLimitation(getEditionLimitation().trim());
		}
		if(StringUtils.isNotBlank(getPermissionCommentStr())) {
			permComment = getPermissionCommentStr().trim();
			if(StringUtils.isNotBlank(getRightsLinkLicenseNumber())) {
				permComment = permComment + " -- Note: " + getRightsLinkLicenseNumber();
			}
			setPermissionComment(permComment);
		}
		if(StringUtils.isNotBlank(getProductionCommentStr())) {
			setProductionComment(getProductionCommentStr().trim());
		}
		if(null != getContract()) {
			if(StringUtils.isNotBlank(getNumberOfCompCopies())) {
				int noOfCompCopy = Integer.parseInt(getNumberOfCompCopies().trim());
				if(noOfCompCopy > 0) {
					compCopy = new CompCopy();
					compCopy.setNumberOfCopies(noOfCompCopy);
					if(StringUtils.isNotBlank(getCompRecipient())) {
						//not sure to which compcopy db field to map
					}
					if(null != getAddress()) {
						compCopy.setAddress(getAddress());
					}
				}
			}
			if(null != compCopy) {
				compCopyList = new ArrayList<CompCopy>();
				compCopyList.add(compCopy);
				getContract().setCompCopies(compCopyList);
			}
		}
		if(StringUtils.isNotBlank(getContinuedUse())) {
			continuedUse = getContinuedUse().trim();
			if("Pickup".equalsIgnoreCase(continuedUse)) {
				setPickup(true);
				setPickupISBN(StringUtils.trimToNull(getContinuedISBN()));
				setPickupPosition(StringUtils.trimToNull(getContinuedPosition()));
				setPickupPage(StringUtils.trimToNull(getContinuedPage()));
				setPickupComment(StringUtils.trimToNull(getContinuedComment()));
			} else if ("Reused".equalsIgnoreCase(continuedUse)) {
				setReusedFromPreviousEdition(true);
				setReusedISBN(StringUtils.trimToNull(getContinuedISBN()));
				setReusedPosition(StringUtils.trimToNull(getContinuedPosition()));
				setReusedPage(StringUtils.trimToNull(getContinuedPage()));
				setReusedComment(StringUtils.trimToNull(getContinuedComment()));
			}
		}
		if(StringUtils.isNotBlank(getPplQAEcolumns())) {
			this.setPplQAEcolumns(getPplQAEcolumns().trim());
		}
	}

	private Date getDate(String excelDateString) {
		String [] tokens = excelDateString.split("\\.");
		Calendar cal = null;
		Date date = null;
		if(tokens.length == 3) {
			cal = Calendar.getInstance();
			cal.set(Integer.parseInt(tokens[2]), Integer.parseInt(tokens[1])-1, Integer.parseInt(tokens[0]));
			date = cal.getTime();
		}
		log.debug("date value : "+date);
		return date;
	}

	private Size getSizeObject(String size) {
		/*switch (size) {//right now not supported, so chosen below if.. else approach
		case "0.25":
			return Size.QUARTER_PAGE;
		case "0.5":
			return Size.HALF_PAGE;
		case "0.75":
			return Size.THREE_QUARTER_PAGE;
		case "FULL":
			return Size.FULL_PAGE;
		case "SPOT":
			return Size.SPOT;
		}*/
		if ("0.25".equalsIgnoreCase(size)) {
			return Size.QUARTER_PAGE;
		} else if ("0.5".equalsIgnoreCase(size)) {
			return Size.HALF_PAGE;
		} else if ("0.75".equalsIgnoreCase(size)) {
			return Size.THREE_QUARTER_PAGE;
		} else if ("FULL".equalsIgnoreCase(size)) {
			return Size.FULL_PAGE;
		} else if ("SPOT".equalsIgnoreCase(size)) {
			return Size.SPOT;
		}
		return Size.NA;
	}

	/**
	 * will take all the string attributes and populate the correct
	 * AssetUse fields
	 */
	@Transient
	public void mapData() {
		if (null == getUsage())
			this.setUsage(Usage.FIGURE);
		setSortOrder(getExtSortOrder());  // will convert blank to null
		// with file maker import component name comes as a float value ex: 7.0
		// so in this case convert to integer (ex "7")
		if(this.getImportSource() != null)
		if(! this.getImportSource().equals(ImportSource.FROM_CS_SPREADSHEET))
			if (NumberUtils.isNumber(getComponentName()))
				if(NumberUtils.isNumber(getComponent().getName()))
					getComponent().setName(String.valueOf(new Float(getComponent().getName()).intValue()));
		if (StringUtils.isNotBlank(getIsExtNew()))
			setNew(true);
		getAsset().setManaged(getBoolean (getIsExtManaged()));
		getAsset().setRoyaltyFree(getBoolean (getIsExtRoyaltyFree()));

		if (StringUtils.isNotBlank(getIsExtArchive()))
			getAsset().setArchive(true);
		if (StringUtils.isNotBlank(getBlackAndWhite()))
			setColor(false);
		if (StringUtils.isNotBlank(getInColor()))
			setColor(true);
		if (StringUtils.isNotBlank(getCameraCopy()))
			setCameraCopyToCome(getBoolean (getCameraCopy()));
		if (StringUtils.isNotBlank(getModelRelease()))
			getAsset().setModelRelease(ModelRelease.ACQUIRED);
			//following for v3r11 label
			// getAsset().setModelRelease(true);
		if (StringUtils.isNotBlank(getObtainedByAuthor()))
			getAsset().setObtainedByAuthor(true);

		setReusedFromPreviousEdition(getBoolean (getReuse()));
		setPickup(getBoolean (getIsExtPickup()));
		// order is important - first set pickup if field exists, second based on PickupComment
		if (StringUtils.isNotBlank(getPickupComment()))
			setPickup(true);

		if (StringUtils.isNotBlank(getRoyaltyFree()))
			getAsset().setRoyaltyFree(true);
		if (StringUtils.isNotBlank(getWorkForHire())) {
			getAsset().setOwnerType(OwnerType.WORK_FOR_HIRE);
			getAsset().setManaged(false);
			getAsset().setFeeRequired(false);
			getAsset().setWorkForHire(true);
		}
		if (StringUtils.isNotBlank(getChapterOpener()))
			this.setUsage(Usage.OPENER);
		if (StringUtils.isNotBlank(getFree())) {
			getAsset().setFeeRequired(false);
			getAsset().setRoyaltyFree(true);
		}
		if (StringUtils.isNotBlank(getTypeFigure()))
			this.setUsage(Usage.FIGURE);
		if (StringUtils.isNotBlank(getCanceled()))
			this.setCanceled(true);

		// no field TakenFromAnotherPublication
		/*
		 * if (StringUtils.isNotBlank(getAsset().getOriginalFigureNumber()) ||
		 * StringUtils.isNotBlank(getAsset().getOriginalPageNumber()) ||
		 * StringUtils.isNotBlank(getAsset().getOriginalPublicationAuthor()) ||
		 * StringUtils.isNotBlank(getAsset().getOriginalPublicationIsbn()) ||
		 * StringUtils.isNotBlank(getAsset().getOriginalPublicationTitle())) {
		 * this.getAsset().setTakenFromAnotherPublication(true); }
		 */
		if (StringUtils.isNotBlank(getPhotoSize())) {
			Size size = sizeMapping.get(getPhotoSize());
			if (size != null) {
				this.setSize(size);
			}
		}

		// page position validation - if not valid, reset it to null
		if (null == getPagePosition() || StringUtils.isBlank(getPagePosition().getCode()))
			setPagePosition(null);

		if (null != getPagePosition() && StringUtils.isNotBlank(getPagePosition().getCode())) {
			// removes "(,)"
			getPagePosition().setCode(getPagePosition().getCode().replaceAll("\\(", ""));
			getPagePosition().setCode(getPagePosition().getCode().replaceAll("\\)", ""));
			setPagePosition (getPagePosition().instanceForCode());
		}

		// we trim here because it is very possible larger descriptions are coming
		// from filemaker
		if (StringUtils.isNotBlank(getAsset().getDescription()) && getAsset().getDescription().length() > DESCRIPTION_LENGTH)
			getAsset().setDescription(getAsset().getDescription().substring(0, DESCRIPTION_LENGTH));

		isPermissionFormComplete (getBoolean (getPermissionForm()));

	//if(! this.getImportSource().equals(ImportSource.FROM_CS_SPREADSHEET)) {
	if(this.getImportSource() == null || (this.getImportSource() != null && ! this.getImportSource().equals(ImportSource.FROM_CS_SPREADSHEET))) {
		// If there is no component field in the import try to derive a
		// chapter from the position: format xx-xx or xx.xx
		Component comp = getComponent();
		if (null == comp || StringUtils.isBlank(comp.getName())) {
			String position = getPosition();
			comp = new Component();
			if (StringUtils.isNotBlank(position) && position.matches("[0-9]*\\.[0-9]*"))
				comp.setName(position.substring(0, position.indexOf(".")));
			if (StringUtils.isNotBlank(position) && position.matches("[0-9]*\\-[0-9]*"))
				comp.setName(position.substring(0, position.indexOf("-")));
			setComponent(comp);
		}
	}
		if (null != getAsset().getOwnerType()) {
			try {
				// try to validate the value - if valid, do nothing, otherwise try to map based on value
				getAsset().getOwnerType().validate();
			} catch (ValidateException ve) {
				getAsset().setOwnerType(OwnerType.getOwnerType(getAsset().getOwnerType().getCode()));
			}
		}
		if (StringUtils.isNotBlank(getAsset().getOriginalPublicationIsbn())) {
			getAsset().setOriginalPublicationIsbn(getAsset().getOriginalPublicationIsbn().replaceAll("-", ""));
		}
		if ( null != getContract() &&
			(null == getContract().getStartDate() || DateUtils.isSameDay(new Date(), getContract().getStartDate()))) {
			getContract().setStartDate(getContract().getDate());
		}

		if (null != getContract() &&
			null != getContract().getNumber() &&
			(getContract().getNumber().toLowerCase().equals("n/a") || StringUtils.isBlank(getContract().getNumber().trim()))) {
			getContract().setNumber(null);
		}
	}

	/**
	 * will take all the string attributes and populate the correct
	 * AssetUse fields
	 */
	@Transient
	public void mapAusData() {
		if (null == getUsage())
			this.setUsage(Usage.FIGURE);
		setSortOrder(getExtSortOrder());  // will convert blank to null
		// with file maker import component name comes as a float value ex: 7.0
		// so in this case convert to integer (ex "7")
		if (NumberUtils.isNumber(getComponentName()))
			getComponent().setName(String.valueOf(new Float(getComponent().getName()).intValue()));
		if (StringUtils.isNotBlank(getIsExtNew()))
			setNew(true);
		getAsset().setManaged(getBoolean (getIsExtManaged()));
		getAsset().setRoyaltyFree(getBoolean (getIsExtRoyaltyFree()));

		if (StringUtils.isNotBlank(getIsExtArchive()))
			getAsset().setArchive(true);
		if (StringUtils.isNotBlank(getBlackAndWhite()))
			setColor(false);
		if (StringUtils.isNotBlank(getInColor()))
			setColor(true);
		if (StringUtils.isNotBlank(getCameraCopy()))
			setCameraCopyToCome(getBoolean (getCameraCopy()));
		if (StringUtils.isNotBlank(getModelRelease()))
			getAsset().setModelRelease(ModelRelease.ACQUIRED);
			//following for v3r11 label
			// getAsset().setModelRelease(true);
		if (StringUtils.isNotBlank(getObtainedByAuthor()))
			getAsset().setObtainedByAuthor(true);

		setReusedFromPreviousEdition(getBoolean (getReuse()));
		setPickup(getBoolean (getIsExtPickup()));
		// order is important - first set pickup if field exists, second based on PickupComment
		if (StringUtils.isNotBlank(getPickupComment()))
			setPickup(true);

		if (StringUtils.isNotBlank(getRoyaltyFree()))
			getAsset().setRoyaltyFree(true);
		if (StringUtils.isNotBlank(getWorkForHire())) {
			getAsset().setOwnerType(OwnerType.WORK_FOR_HIRE);
			getAsset().setManaged(false);
			getAsset().setFeeRequired(false);
			getAsset().setWorkForHire(true);
		}
		if (StringUtils.isNotBlank(getChapterOpener()))
			this.setUsage(Usage.OPENER);
		if (StringUtils.isNotBlank(getFree())) {
			getAsset().setFeeRequired(false);
			getAsset().setRoyaltyFree(true);
		}
		if (StringUtils.isNotBlank(getTypeFigure()))
			this.setUsage(Usage.FIGURE);
		if (StringUtils.isNotBlank(getCanceled()))
			this.setCanceled(true);

		// no field TakenFromAnotherPublication
		/*
		 * if (StringUtils.isNotBlank(getAsset().getOriginalFigureNumber()) ||
		 * StringUtils.isNotBlank(getAsset().getOriginalPageNumber()) ||
		 * StringUtils.isNotBlank(getAsset().getOriginalPublicationAuthor()) ||
		 * StringUtils.isNotBlank(getAsset().getOriginalPublicationIsbn()) ||
		 * StringUtils.isNotBlank(getAsset().getOriginalPublicationTitle())) {
		 * this.getAsset().setTakenFromAnotherPublication(true); }
		 */
		if (StringUtils.isNotBlank(getPhotoSize())) {
			Size size = sizeMapping.get(getPhotoSize());
			if (size != null) {
				this.setSize(size);
			}
		}

		// page position validation - if not valid, reset it to null
		if (null == getPagePosition() || StringUtils.isBlank(getPagePosition().getCode()))
			setPagePosition(null);

		if (null != getPagePosition() && StringUtils.isNotBlank(getPagePosition().getCode())) {
			// removes "(,)"
			getPagePosition().setCode(getPagePosition().getCode().replaceAll("\\(", ""));
			getPagePosition().setCode(getPagePosition().getCode().replaceAll("\\)", ""));
			setPagePosition (getPagePosition().instanceForCode());
		}

		// we trim here because it is very possible larger descriptions are coming
		// from Australia
		if (StringUtils.isNotBlank(getAsset().getDescription()) && getAsset().getDescription().length() > DESCRIPTION_LENGTH)
			getAsset().setDescription(getAsset().getDescription().substring(0, DESCRIPTION_LENGTH));

		isPermissionFormComplete (getBoolean (getPermissionForm()));

		// If there is no component field in the import try to derive a
		// chapter from the position: format xx-xx or xx.xx
		Component comp = getComponent();
		if (null == comp || StringUtils.isBlank(comp.getName())) {
			String position = getPosition();
			comp = new Component();
			if (StringUtils.isNotBlank(position) && position.matches("[0-9]*\\.[0-9]*"))
				comp.setName(position.substring(0, position.indexOf(".")));
			if (StringUtils.isNotBlank(position) && position.matches("[0-9]*\\-[0-9]*"))
				comp.setName(position.substring(0, position.indexOf("-")));
			setComponent(comp);
		}
		if (null != getAsset().getOwnerType()) {
			try {
				// try to validate the value - if valid, do nothing, otherwise try to map based on value
				getAsset().getOwnerType().validate();
			} catch (ValidateException ve) {
				getAsset().setOwnerType(OwnerType.getOwnerType(getAsset().getOwnerType().getCode()));
			}
		}
		if (StringUtils.isNotBlank(getAsset().getOriginalPublicationIsbn())) {
			getAsset().setOriginalPublicationIsbn(getAsset().getOriginalPublicationIsbn().replaceAll("-", ""));
		}
		if ( null != getContract() &&
			(null == getContract().getStartDate() || DateUtils.isSameDay(new Date(), getContract().getStartDate()))) {
			getContract().setStartDate(getContract().getDate());
		}

		if (null != getContract() &&
			null != getContract().getNumber() &&
			(getContract().getNumber().toLowerCase().equals("n/a") || StringUtils.isBlank(getContract().getNumber().trim()))) {
			getContract().setNumber(null);
		}
	}

	public static boolean getBoolean(String value) {
		if (StringUtils.isNotBlank(value)) {
			value = value.trim();
			if (value.equalsIgnoreCase("Y") || value.equalsIgnoreCase("Yes"))
				return true;
			if (value.equalsIgnoreCase("N") || value.equalsIgnoreCase("No"))
				return false;
			// if just a value (can be "Reuse", "New" texts from filemaker)
			return true;
		}
		return false;
	}

	public String getTypeSection() {
		return typeSection;
	}

	public void setTypeSection(String typeSection) {
		this.typeSection = typeSection;
	}

	public String getCanceled() {
		return canceled;
	}

	public void setCanceled(String canceled) {
		this.canceled = canceled;
	}

	public String getIsExtArchive() {
		return isExtArchive;
	}

	public void setIsExtArchive(String isExtArchive) {
		this.isExtArchive = isExtArchive;
	}

	public String getIsExtNew() {
		return isExtNew;
	}

	public void setIsExtNew(String isExtNew) {
		this.isExtNew = isExtNew;
	}

	public String getIsExtPickup() {
		return isExtPickup;
	}

	public void setIsExtPickup(String isExtPickup) {
		this.isExtPickup = isExtPickup;
	}

	public String getExtSortOrder() {
		return extSortOrder;
	}

	public void setExtSortOrder(String extSortOrder) {
		this.extSortOrder = extSortOrder;
	}

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}

	public String getPermissionType() {
		return permissionType;
	}

	public void setPermissionType(String permissionType) {
		this.permissionType = permissionType;
	}

	public Integer getDealId() {
    	return dealId;
    }

	public void setDealId(Integer dealId) {
    	this.dealId = dealId;
    }

	public Double getPrice() {
    	return price;
    }

	public void setPrice(Double price) {
    	this.price = price;
    }


	public void setIsb10(String isb10) {
		this.isb10 = isb10;
	}

	public String getIsb10() {
    	return isb10;
    }

	public void setIsb13(String isb13) {
		this.isb13 = isb13;
	}

	public String getIsb13() {
    	return isb13;
    }


	@Transient
	public boolean isSourceRequired() {
		if (getPermissionType().equals("Author Supplied Art") ||
			getPermissionType().equals("Wiley Created"))
			return false;
		else
			return true;
	}

	/**
	 * 	Rights Managed = 3rd Party, managed = Y
	 *	Public Domain = 3rd Party, managed = N, Public Domain = Y
	 *	Work for Hire = Wiley Owned, managed = N
	 *	Author Supplied Art = Author Owned, managed = N
	 *	Courtesy/Gratis = 3rd Party, managed = Y, signed permission form exists with no conditions, give user option to upload signed form
	 *	Wiley Created = Wiley Owned, managed = N
	 * @param permissionType
	 */
	public void mapCustomBean() {
		log.debug ("---->>>> mapCustomBean " + permissionType);
		if (permissionType.equalsIgnoreCase("Rights Managed")) {
			this.getAsset().setOwnerType(OwnerType.THIRD_PARTY);
			this.getAsset().setManaged(true);
		} else if (permissionType.equalsIgnoreCase("Public Domain")) {
			this.getAsset().setOwnerType(OwnerType.PUBLIC_DOMAIN);
			this.getAsset().setManaged(false);
			this.getAsset().setRoyaltyFree(true);
			this.setContract(null);
		} else if (permissionType.equalsIgnoreCase("Work for Hire")) {
			this.getAsset().setOwnerType(OwnerType.WORK_FOR_HIRE);
			this.getAsset().setManaged(false);
			this.getAsset().setRoyaltyFree(true);
			this.getAsset().setFeeRequired(false);
			this.setContract(null);
		} else if (permissionType.equalsIgnoreCase("Author Supplied Art")) { //x
			log.debug ("Author");
			this.getAsset().setOwnerType(OwnerType.AUTHOR_OWNED);
			this.getAsset().setManaged(false);
			this.getAsset().setRoyaltyFree(true);
			this.setContract(null);
			this.getAsset().setSources(null);
		} else if (permissionType.equalsIgnoreCase("STM guidelines")) {
			this.getAsset().setStmGuidelines(true);
		} else if (permissionType.equalsIgnoreCase("Will be royalty free")) {
			this.getAsset().setWillBeRoyaltyFree(true);
		} else if (permissionType.equalsIgnoreCase("Will be work for hire")) {
			this.getAsset().setWillBeWorkForHire(true);
		} else if (permissionType.equalsIgnoreCase("Wiley Created")) { //x
			this.getAsset().setOwnerType(OwnerType.WILEY_CREATED);
			this.getAsset().setManaged(false);
			this.setContract(null);
			this.getAsset().setSources(null);
		} else if(permissionType.equalsIgnoreCase("Royalty Free")) {
			this.getAsset().setManaged(false);
			this.getAsset().setRoyaltyFree(true);
		}
	}

	@Transient
	public String mapCustomForm() {
		if (null == this.getAsset().getOwnerType())
			return null;

		if (this.getAsset().isStmGuidelines()) {
			permissionType = "STM guidelines";
		} else if (this.getAsset().isWillBeRoyaltyFree()) {
			permissionType = "Will be royalty free";
		} else if (this.getAsset().isWillBeWorkForHire()) {
			permissionType = "Will be work for hire";
		} else if (this.getAsset().getOwnerType().equals(OwnerType.THIRD_PARTY) &&
			this.getAsset().isManaged() == true && this.getContract() != null && !this.getContract().isPermissionForm()) {
			permissionType = "Rights Managed";
		} else if (	this.getAsset().getOwnerType().equals(OwnerType.PUBLIC_DOMAIN) &&
					this.getAsset().isManaged() == false &&
					this.getAsset().isRoyaltyFree()) {
			permissionType = "Public Domain";
		} else if (	this.getAsset().getOwnerType().equals(OwnerType.WORK_FOR_HIRE)) {
			permissionType = "Work for Hire";
		} else if (	this.getAsset().getOwnerType().equals(OwnerType.AUTHOR_OWNED)) {
			permissionType = "Author Supplied Art";
		} else if (	this.getAsset().getOwnerType().equals(OwnerType.WILEY_CREATED)) {
			permissionType = "Wiley Created";
		}

		return permissionType;
	}

	/**
	 * Creates a Permission Form for the asset/source
	 *
	 * @param assetUse
	 * @param source
	 */
	private Contract createPermissionForm() {
		Contract contract = new Contract();

		contract.setCommonWork(this.getCommonWork());
		contract.setStartDate(new Date());

		contract.setPermissionForm(true);
		contract.setDate(new Date());
		contract.setPurchaseOrder(null); // may be null
		contract.setCurrency(Currency.US);
		contract.setPrice(0.0);
		return contract;
	}
}
