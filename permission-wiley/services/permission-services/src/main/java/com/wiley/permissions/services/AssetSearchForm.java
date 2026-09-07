package com.wiley.permissions.services;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.Medium;
import com.wiley.permissions.domain.persistence.permissions.ModelRelease;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.persistence.permissions.Size;
import com.wiley.permissions.domain.persistence.permissions.Usage;

/**
 * This form is in the services package instead of the web package because it
 * needs to be referenced from the services package.
 *
 * @author smarkoff
 */
public class AssetSearchForm {

	// product stuff
	private boolean includeCommonWorkId = true;  // reset to false in controller if no product
	private Integer commonWorkId = null;

	private boolean includeEnteredCwID=false; // Added for DM-280
	private Integer enteredCwID = null;
	private boolean includeAuthor = false;
	private String author = null;
	private boolean includeTitle = false;
	private String title = null;


	// Removed ISBN13 and Isbn10 as Tim suggested not a show-stopper but confusing
	/*private boolean includeIsbn13 = false;
	private String isbn13 = null;
	private boolean includeIsbn10 = false;
	private String isbn10 = null;
	*/

	private boolean includeCopyrightYear = false;
	private String copyrightYear = null;
	private boolean includeUserName = false;
	private String userName = null;
	private boolean includePublicationStatus = false;
	private PublicationStatus publicationStatus = null;

	//Start: Added for DM-280
	private boolean includeProductNumber = false;
	private String productNumber = null;
	private boolean includeBusinessUnit = false;
	private BusinessUnit businessUnit = null;
	private Integer bunitCode = null;
	private boolean includeBunitCode = false;
	private boolean includeEditionNumber = false;
	private String editionNumber = null;
	private boolean includeProductPriority = false;
	private String productPriority = null;
	private boolean includeMedium = false;
	private Medium medium = null;
	private boolean includeDataSource = false;
	private String dataSource = null;
	private boolean  includeShortAuthorName= false;
	private String shortAuthorName = null;
	private boolean includeComplianceStatus = false;
	private String complianceStatus = null;
	private boolean includeEditorCode = false;
	private String editorCode = null;
	private boolean includeEditorName = false;
	private String editorName = null;
	private boolean includeAUNotCancelCount = false;
	private String auCountNotCanceledCnt = null;
	private boolean includeCoverCountNotCanceled = false;
	private String coverCountNotCanceled = null;
	private boolean includeStatusNotOkCount = false;
	private String statusNotOkCount = null;
	private boolean includePhotoIllusTotalCount = false;
	private String photoIllusTotalCount = null;
	private boolean includePhotoEditorLastName = false;
	private String photoEditorLastName = null;
	private boolean includePhotoEditorFirstName = false;
	private String photoEditorFirstName = null;
	private boolean includeLastUpdatedDate = false;
	private String lastUpdatedDate = null;
	private boolean includeConsolidatedReleaseDate = false;
	private String consolidatedReleaseDate = null;
	private boolean includeTransmittalDate = false;
	private String transmittalDate = null;


	public boolean isIncludeProductNumber() {
		return includeProductNumber;
	}

	public void setIncludeProductNumber(boolean includeProductNumber) {
		this.includeProductNumber = includeProductNumber;
	}

	public String getProductNumber() {
		return productNumber;
	}

	public void setProductNumber(String productNumber) {
		this.productNumber = productNumber;
	}

	public boolean isIncludeBusinessUnit() {
		return includeBusinessUnit;
	}

	public void setIncludeBusinessUnit(boolean includeBusinessUnit) {
		this.includeBusinessUnit = includeBusinessUnit;
	}

	public BusinessUnit getBusinessUnit() {
		return businessUnit;
	}

	public void setBusinessUnit(BusinessUnit businessUnit) {
		this.businessUnit = businessUnit;
	}

	public Integer getBunitCode() {
		return bunitCode;
	}

	public void setBunitCode(Integer bunitCode) {
		this.bunitCode = bunitCode;
	}

	public boolean isIncludeBunitCode() {
		return includeBunitCode;
	}

	public void setIncludeBunitCode(boolean includeBunitCode) {
		this.includeBunitCode = includeBunitCode;
	}

	public boolean isIncludeEditionNumber() {
		return includeEditionNumber;
	}

	public void setIncludeEditionNumber(boolean includeEditionNumber) {
		this.includeEditionNumber = includeEditionNumber;
	}

	public String getEditionNumber() {
		return editionNumber;
	}

	public void setEditionNumber(String editionNumber) {
		this.editionNumber = editionNumber;
	}

	public boolean isIncludeProductPriority() {
		return includeProductPriority;
	}

	public void setIncludeProductPriority(boolean includeProductPriority) {
		this.includeProductPriority = includeProductPriority;
	}

	public String getProductPriority() {
		return productPriority;
	}

	public void setProductPriority(String productPriority) {
		this.productPriority = productPriority;
	}

	public boolean isIncludeMedium() {
		return includeMedium;
	}

	public void setIncludeMedium(boolean includeMedium) {
		this.includeMedium = includeMedium;
	}

	public Medium getMedium() {
		return medium;
	}

	public void setMedium(Medium medium) {
		this.medium = medium;
	}

	public boolean isIncludeDataSource() {
		return includeDataSource;
	}

	public void setIncludeDataSource(boolean includeDataSource) {
		this.includeDataSource = includeDataSource;
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public boolean isIncludeShortAuthorName() {
		return includeShortAuthorName;
	}

	public void setIncludeShortAuthorName(boolean includeShortAuthorName) {
		this.includeShortAuthorName = includeShortAuthorName;
	}

	public String getShortAuthorName() {
		return shortAuthorName;
	}

	public void setShortAuthorName(String shortAuthorName) {
		this.shortAuthorName = shortAuthorName;
	}

	public boolean isIncludeComplianceStatus() {
		return includeComplianceStatus;
	}

	public void setIncludeComplianceStatus(boolean includeComplianceStatus) {
		this.includeComplianceStatus = includeComplianceStatus;
	}

	public String getComplianceStatus() {
		return complianceStatus;
	}

	public void setComplianceStatus(String complianceStatus) {
		this.complianceStatus = complianceStatus;
	}

	public boolean isIncludeEditorCode() {
		return includeEditorCode;
	}

	public void setIncludeEditorCode(boolean includeEditorCode) {
		this.includeEditorCode = includeEditorCode;
	}

	public String getEditorCode() {
		return editorCode;
	}

	public void setEditorCode(String editorCode) {
		this.editorCode = editorCode;
	}

	public boolean isIncludeEditorName() {
		return includeEditorName;
	}

	public void setIncludeEditorName(boolean includeEditorName) {
		this.includeEditorName = includeEditorName;
	}

	public String getEditorName() {
		return editorName;
	}

	public void setEditorName(String editorName) {
		this.editorName = editorName;
	}

	public boolean isIncludeAUNotCancelCount() {
		return includeAUNotCancelCount;
	}

	public void setIncludeAUNotCancelCount(boolean includeAUNotCancelCount) {
		this.includeAUNotCancelCount = includeAUNotCancelCount;
	}

	public String getAuCountNotCanceledCnt() {
		return auCountNotCanceledCnt;
	}

	public void setAuCountNotCanceledCnt(String auCountNotCanceledCnt) {
		this.auCountNotCanceledCnt = auCountNotCanceledCnt;
	}

	public boolean isIncludeCoverCountNotCanceled() {
		return includeCoverCountNotCanceled;
	}

	public void setIncludeCoverCountNotCanceled(boolean includeCoverCountNotCanceled) {
		this.includeCoverCountNotCanceled = includeCoverCountNotCanceled;
	}

	public String getCoverCountNotCanceled() {
		return coverCountNotCanceled;
	}

	public void setCoverCountNotCanceled(String coverCountNotCanceled) {
		this.coverCountNotCanceled = coverCountNotCanceled;
	}

	public boolean isIncludeStatusNotOkCount() {
		return includeStatusNotOkCount;
	}

	public void setIncludeStatusNotOkCount(boolean includeStatusNotOkCount) {
		this.includeStatusNotOkCount = includeStatusNotOkCount;
	}

	public String getStatusNotOkCount() {
		return statusNotOkCount;
	}

	public void setStatusNotOkCount(String statusNotOkCount) {
		this.statusNotOkCount = statusNotOkCount;
	}

	public boolean isIncludePhotoIllusTotalCount() {
		return includePhotoIllusTotalCount;
	}

	public void setIncludePhotoIllusTotalCount(boolean includePhotoIllusTotalCount) {
		this.includePhotoIllusTotalCount = includePhotoIllusTotalCount;
	}

	public String getPhotoIllusTotalCount() {
		return photoIllusTotalCount;
	}

	public void setPhotoIllusTotalCount(String photoIllusTotalCount) {
		this.photoIllusTotalCount = photoIllusTotalCount;
	}

	public boolean isIncludePhotoEditorLastName() {
		return includePhotoEditorLastName;
	}

	public void setIncludePhotoEditorLastName(boolean includePhotoEditorLastName) {
		this.includePhotoEditorLastName = includePhotoEditorLastName;
	}

	public String getPhotoEditorLastName() {
		return photoEditorLastName;
	}

	public void setPhotoEditorLastName(String photoEditorLastName) {
		this.photoEditorLastName = photoEditorLastName;
	}

	public boolean isIncludePhotoEditorFirstName() {
		return includePhotoEditorFirstName;
	}

	public void setIncludePhotoEditorFirstName(boolean includePhotoEditorFirstName) {
		this.includePhotoEditorFirstName = includePhotoEditorFirstName;
	}

	public String getPhotoEditorFirstName() {
		return photoEditorFirstName;
	}

	public void setPhotoEditorFirstName(String photoEditorFirstName) {
		this.photoEditorFirstName = photoEditorFirstName;
	}

	public boolean isIncludeLastUpdatedDate() {
		return includeLastUpdatedDate;
	}

	public void setIncludeLastUpdatedDate(boolean includeLastUpdatedDate) {
		this.includeLastUpdatedDate = includeLastUpdatedDate;
	}

	public String getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(String lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	public boolean isIncludeConsolidatedReleaseDate() {
		return includeConsolidatedReleaseDate;
	}

	public void setIncludeConsolidatedReleaseDate(
			boolean includeConsolidatedReleaseDate) {
		this.includeConsolidatedReleaseDate = includeConsolidatedReleaseDate;
	}

	public String getConsolidatedReleaseDate() {
		return consolidatedReleaseDate;
	}

	public void setConsolidatedReleaseDate(String consolidatedReleaseDate) {
		this.consolidatedReleaseDate = consolidatedReleaseDate;
	}

	public boolean isIncludeTransmittalDate() {
		return includeTransmittalDate;
	}

	public void setIncludeTransmittalDate(boolean includeTransmittalDate) {
		this.includeTransmittalDate = includeTransmittalDate;
	}

	public String getTransmittalDate() {
		return transmittalDate;
	}

	public void setTransmittalDate(String transmittalDate) {
		this.transmittalDate = transmittalDate;
	}
	//End: Added for DM-280

	// asset stuff
    private boolean includeSourceName = false;
    private String sourceName = null;
	private boolean includeMediaType = false;
	private MediaType mediaType = null;
	private boolean includeDescription = false;
    private String description = null;
	private boolean includeKeywords = false;
    private String keywords = null;
    private boolean includeWorkForHire = false;
    private boolean workForHire = false;
	private boolean includeOwnerType = false;
	private OwnerType ownerType = null;
	private boolean includeSourceRef = false;
    private String sourceRef = null;
    private boolean includeArtist = false;
    private String artist = null;
    private boolean includeManaged = false;
    private boolean managed = false;
    private boolean includeFeeRequired = false;
    private boolean feeRequired = false;
    private boolean includeRoyaltyFree = false;
    private boolean royaltyFree = false;
    private boolean includeRestrictedUse = false;
    private boolean restrictedUse = false;
    private boolean includeActive = false;
    private boolean active = true;
    private boolean includeMustDisplayCredit = false;
    private boolean mustDisplayCredit = false;
    private boolean includeObtainedByAuthor = false;
    private boolean obtainedByAuthor = false;
    private boolean includeModelRelease = false;
    private ModelRelease modelRelease = null;
    private boolean includePropertyRelease = false;
    private boolean propertyRelease = false;
    private boolean includeArchive = false;
    private boolean archive = false;

    // asset_use stuff
	private boolean includePermissionStatus = false;
	private PermissionStatus permissionStatus = null;
    private boolean includeCanceled = false;
    private boolean canceled = false;
    private boolean includeReplaced = false;
    private boolean replaced = false;
    private boolean includeCreditLine = false;
    private String creditLine = null;
	private boolean includeUsage = false;
	private Usage usage = null;
    private boolean includePosition = false;
    private String position = null;
    private boolean includeFoundOn = false;
    private String foundOn = null;
	private boolean includeComponentName = false;
	private String componentName = null;
	private boolean includeManuscriptPage = false;
	private String manuscriptPage = null;
	private boolean includeFinalPage = false;
	private String finalPage = null;
    private boolean includeColor = false;
    private boolean color = false;
	private boolean includeSize = false;
	private Size size = null;
    private boolean includeCaption = false;
    private String caption = null;
    private boolean includePermissionComment = false;
    private String permissionComment = null;
    private boolean includeReuse = false;
    private boolean reuse = false;
    private boolean includePickup = false;
    private boolean pickup = false;
    private boolean includeMediaManager = false;
    private boolean mediaManager = false;
    private boolean includeCameraCopyToCome = false;
    private boolean cameraCopyToCome = false;
    private boolean includeSentToProduction = false;
    private boolean sentToProduction = false;
    private boolean includeMediaReturnRequest = false;
    private boolean mediaReturnRequest = false;

    public AssetSearchForm() {

	}

    public boolean isSearchByCurrentCommonWork() {
    	return (includeCommonWorkId && commonWorkId != null);
    }

    public boolean isSomethingSpecified() {
    	// product stuff
    	if (includeCommonWorkId && commonWorkId != null)  return true;
    	if (includeEnteredCwID  && enteredCwID != null)  return true;
    	if (includeAuthor && StringUtils.isNotBlank(author))  return true;
    	if (includeTitle && StringUtils.isNotBlank(title))  return true;

    	//Removed ISBN13 and Isbn10 as Tim suggested not a show-stopper but confusing

    	// if (includeIsbn13 && StringUtils.isNotBlank(isbn13))  return true;
    	// if (includeIsbn10 && StringUtils.isNotBlank(isbn10))  return true;

    	if (includeCopyrightYear && StringUtils.isNotBlank(copyrightYear))  return true;
    	if (includeUserName && StringUtils.isNotBlank(userName))  return true;
    	if (includePublicationStatus && publicationStatus != null)  return true;
    	//Start: Added for DM-280
    	if (includeProductNumber && StringUtils.isNotBlank(productNumber))  return true;
    	if (includeEditionNumber && StringUtils.isNotBlank(editionNumber))  return true;
    	if (includeBusinessUnit && businessUnit != null) return true;
    	if (includeBunitCode && bunitCode != null) return true;
    	if (includeProductPriority && StringUtils.isNotBlank(productPriority))  return true;
    	if (includeMedium && medium != null)  return true;
    	if (includeDataSource && StringUtils.isNotBlank(dataSource))  return true;
    	if (includeShortAuthorName && StringUtils.isNotBlank(shortAuthorName))  return true;
    	if (includeComplianceStatus && StringUtils.isNotBlank(complianceStatus))  return true;
    	if (includeEditorCode && StringUtils.isNotBlank(editorCode))  return true;
    	if (includeEditorName && StringUtils.isNotBlank(editorName))  return true;
    	if (includeAUNotCancelCount && StringUtils.isNotBlank(auCountNotCanceledCnt))  return true;
    	if (includeCoverCountNotCanceled && StringUtils.isNotBlank(coverCountNotCanceled))  return true;
    	if (includeStatusNotOkCount && StringUtils.isNotBlank(statusNotOkCount))  return true;
    	if (includePhotoIllusTotalCount && StringUtils.isNotBlank(photoIllusTotalCount))  return true;
    	if (includePhotoEditorLastName && StringUtils.isNotBlank(photoEditorLastName))  return true;
    	if (includePhotoEditorFirstName && StringUtils.isNotBlank(photoEditorFirstName))  return true;
    	if (includeLastUpdatedDate && StringUtils.isNotBlank(lastUpdatedDate))  return true;
    	if (includeConsolidatedReleaseDate && StringUtils.isNotBlank(consolidatedReleaseDate))  return true;
    	if (includeTransmittalDate && StringUtils.isNotBlank(transmittalDate))  return true;
    	//End: Added for DM-280

    	// asset stuff
    	if (includeSourceName && StringUtils.isNotBlank(sourceName))  return true;
    	if (includeMediaType && mediaType != null)  return true;
    	if (includeDescription && StringUtils.isNotBlank(description))  return true;
    	if (includeKeywords && StringUtils.isNotBlank(keywords))  return true;
    	if (includeWorkForHire)  return true;
    	if (includeOwnerType && ownerType != null)  return true;
    	if (includeSourceRef && StringUtils.isNotBlank(sourceRef))  return true;
    	if (includeArtist && StringUtils.isNotBlank(artist))  return true;
    	if (includeManaged)  return true;
    	if (includeFeeRequired)  return true;
    	if (includeRoyaltyFree)  return true;
    	if (includeRestrictedUse)  return true;
    	if (includeActive)  return true;
    	if (includeMustDisplayCredit)  return true;
    	if (includeObtainedByAuthor)  return true;
    	if (includeModelRelease && modelRelease != null)  return true;
    	if (includePropertyRelease)  return true;
    	if (includeArchive)  return true;

    	// asset_use stuff
    	if (includePermissionStatus && permissionStatus != null)  return true;
    	if (includeCanceled)  return true;
    	if (includeReplaced)  return true;
    	if (includeCreditLine && StringUtils.isNotBlank(creditLine))  return true;
    	if (includeUsage && usage != null)  return true;
    	if (includePosition && StringUtils.isNotBlank(position))  return true;
    	if (includeFoundOn && StringUtils.isNotBlank(foundOn))  return true;
    	if (includeComponentName && StringUtils.isNotBlank(componentName))  return true;
    	if (includeManuscriptPage && StringUtils.isNotBlank(manuscriptPage))  return true;
    	if (includeFinalPage && StringUtils.isNotBlank(finalPage))  return true;
    	if (includeColor)  return true;
    	if (includeSize && size != null)  return true;
    	if (includeCaption && StringUtils.isNotBlank(caption))  return true;
    	if (includePermissionComment && StringUtils.isNotBlank(permissionComment))  return true;
    	if (includeReuse)  return true;
    	if (includePickup)  return true;
    	if (includeMediaManager)  return true;
    	if (includeCameraCopyToCome)  return true;
    	if (includeSentToProduction)  return true;
    	if (includeMediaReturnRequest)  return true;

    	return false;
    }


    // ---------- product stuff ---------

	public boolean getIncludeCommonWorkId() {
		return includeCommonWorkId;
	}

	public void setIncludeCommonWorkId(boolean b) {
		includeCommonWorkId = b;
	}

    public Integer getCommonWorkId() {
    	return commonWorkId;
    }

    public void setCommonWorkId(Integer commonWorkId) {
    	this.commonWorkId = commonWorkId;
    }

	public boolean getIncludeAuthor() {
		return includeAuthor;
	}

	public void setIncludeAuthor(boolean b) {
		includeAuthor = b;
	}

	//Start: Added for DM-280
	public boolean getIncludeEnteredCwID() {
		return includeEnteredCwID;
	}

	public void setIncludeEnteredCwID(boolean includeEnteredCwID) {
		this.includeEnteredCwID = includeEnteredCwID;
	}

	public Integer getEnteredCwID() {
		return enteredCwID;
	}

	public void setEnteredCwID(Integer enteredCwID) {
		this.enteredCwID = enteredCwID;
	}

	//End: Added for DM-280

    public String getAuthor() {
    	return author;
    }

    public void setAuthor(String author) {
    	this.author = StringUtils.trimToNull(author);
    }

	public boolean getIncludeTitle() {
		return includeTitle;
	}

	public void setIncludeTitle(boolean b) {
		includeTitle = b;
	}

    public String getTitle() {
    	return title;
    }

    public void setTitle(String title) {
    	this.title = StringUtils.trimToNull(title);
    }

  // Removed ISBN13 and Isbn10 as Tim suggested not a show-stopper but confusing
	/*public boolean getIncludeIsbn13() {
		return includeIsbn13;
	}

	public void setIncludeIsbn13(boolean b) {
		includeIsbn13 = b;
	}

    public String getIsbn13() {
    	return isbn13;
    }

    public void setIsbn13(String isbn13) {
    	this.isbn13 = StringUtils.trimToNull(isbn13);
    }

    public boolean getIncludeIsbn10() {
		return includeIsbn10;
	}

	public void setIncludeIsbn10(boolean b) {
		includeIsbn10 = b;
	}

    public String getIsbn10() {
    	return isbn10;
    }

    public void setIsbn10(String isbn10) {
    	this.isbn10 = StringUtils.trimToNull(isbn10);
    }*/

	public boolean getIncludeCopyrightYear() {
		return includeCopyrightYear;
	}

	public void setIncludeCopyrightYear(boolean b) {
		includeCopyrightYear = b;
	}

    public String getCopyrightYear() {
    	return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear) {
    	this.copyrightYear = StringUtils.trimToNull(copyrightYear);
    }

	public boolean getIncludeUserName() {
		return includeUserName;
	}

	public void setIncludeUserName(boolean b) {
		includeUserName = b;
	}

    public String getUserName() {
    	return userName;
    }

    public void setUserName(String userName) {
    	this.userName = StringUtils.trimToNull(userName);
    }

	public boolean getIncludePublicationStatus() {
		return includePublicationStatus;
	}

	public void setIncludePublicationStatus(boolean b) {
		includePublicationStatus = b;
	}

	public PublicationStatus getPublicationStatus() {
		return publicationStatus;
	}

	public void setPublicationStatus(PublicationStatus publicationStatus) {
		this.publicationStatus = publicationStatus;
	}


	// ---------- asset stuff ---------

	public void setIncludeSourceName(boolean includeSourceName) {
		this.includeSourceName = includeSourceName;
	}

	public boolean getIncludeSourceName() {
		return includeSourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = StringUtils.trimToNull(sourceName);
	}

	public String getSourceName() {
		return sourceName;
	}

	public boolean getIncludeMediaType() {
		return includeMediaType;
	}

	public void setIncludeMediaType(boolean b) {
		includeMediaType = b;
	}

	public MediaType getMediaType() {
		return mediaType;
	}

	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
	}

	public boolean getIncludeDescription() {
		return includeDescription;
	}

	public void setIncludeDescription(boolean b) {
		includeDescription = b;
	}

    public String getDescription() {
    	return description;
    }

    public void setDescription(String description) {
    	this.description = StringUtils.trimToNull(description);
    }

	public boolean getIncludeKeywords() {
		return includeKeywords;
	}

	public void setIncludeKeywords(boolean b) {
		includeKeywords = b;
	}

    public String getKeywords() {
    	return keywords;
    }

    public void setKeywords(String keywords) {
    	this.keywords = StringUtils.trimToNull(keywords);
    }

	public boolean getIncludeOwnerType() {
		return includeOwnerType;
	}

	public void setIncludeOwnerType(boolean b) {
		includeOwnerType = b;
	}

	public OwnerType getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(OwnerType ownerType) {
		this.ownerType = ownerType;
	}

	public boolean getIncludeSourceRef() {
		return includeSourceRef;
	}

	public void setIncludeSourceRef(boolean b) {
		includeSourceRef = b;
	}

    public String getSourceRef() {
    	return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
    	this.sourceRef = StringUtils.trimToNull(sourceRef);
    }

	public void setIncludeArtist(boolean includeArtist) {
		this.includeArtist = includeArtist;
	}

	public boolean getIncludeArtist() {
		return includeArtist;
	}

	public void setArtist(String artist) {
		this.artist = StringUtils.trimToNull(artist);
	}

	public String getArtist() {
		return artist;
	}

    public boolean getIncludeWorkForHire() {
    	return includeWorkForHire;
    }

    public void setIncludeWorkForHire(boolean includeWorkForHire) {
    	this.includeWorkForHire = includeWorkForHire;
    }

    public boolean isWorkForHire() {
    	return workForHire;
    }

    public void setWorkForHire(boolean workForHire) {
    	this.workForHire = workForHire;
    }

    public boolean getIncludeManaged() {
    	return includeManaged;
    }

    public void setIncludeManaged(boolean b) {
    	this.includeManaged = b;
    }

    public boolean isManaged() {
    	return managed;
    }

    public void setManaged(boolean b) {
    	this.managed = b;
    }

    public boolean getIncludeFeeRequired() {
    	return includeFeeRequired;
    }

    public void setIncludeFeeRequired(boolean b) {
    	this.includeFeeRequired = b;
    }

    public boolean isFeeRequired() {
    	return feeRequired;
    }

    public void setFeeRequired(boolean feeRequired) {
    	this.feeRequired = feeRequired;
    }

    public boolean getIncludeRoyaltyFree() {
    	return includeRoyaltyFree;
    }

    public void setIncludeRoyaltyFree(boolean b) {
    	this.includeRoyaltyFree = b;
    }

    public boolean isRoyaltyFree() {
    	return royaltyFree;
    }

    public void setRoyaltyFree(boolean b) {
    	this.royaltyFree = b;
    }

    public boolean getIncludeRestrictedUse() {
    	return includeRestrictedUse;
    }

    public void setIncludeRestrictedUse(boolean b) {
    	this.includeRestrictedUse = b;
    }

    public boolean getIncludeActive() {
    	return includeActive;
    }

    public void setIncludeActive(boolean b) {
    	this.includeActive = b;
    }

    public boolean isRestrictedUse() {
    	return restrictedUse;
    }

    public void setRestrictedUse(boolean b) {
    	this.restrictedUse = b;
    }

    public boolean isActive() {
    	return active;
    }

    public void setActive(boolean b) {
    	this.active = b;
    }

    public boolean getIncludeMustDisplayCredit() {
    	return includeMustDisplayCredit;
    }

    public void setIncludeMustDisplayCredit(boolean b) {
    	this.includeMustDisplayCredit = b;
    }

    public boolean isMustDisplayCredit() {
    	return mustDisplayCredit;
    }

    public void setMustDisplayCredit(boolean b) {
    	this.mustDisplayCredit = b;
    }

    public boolean getIncludeObtainedByAuthor() {
    	return includeObtainedByAuthor;
    }

    public void setIncludeObtainedByAuthor(boolean b) {
    	this.includeObtainedByAuthor = b;
    }

    public boolean isObtainedByAuthor() {
    	return obtainedByAuthor;
    }

    public void setObtainedByAuthor(boolean b) {
    	this.obtainedByAuthor = b;
    }

    public boolean getIncludeModelRelease() {
    	return includeModelRelease;
    }

    public void setIncludeModelRelease(boolean b) {
    	this.includeModelRelease = b;
    }

    public ModelRelease getModelRelease() {
    	return modelRelease;
    }

    public void setModelRelease(ModelRelease b) {
    	this.modelRelease = b;
    }

    public boolean getIncludePropertyRelease() {
    	return includePropertyRelease;
    }

    public void setIncludePropertyRelease(boolean b) {
    	this.includePropertyRelease = b;
    }

    public boolean isPropertyRelease() {
    	return propertyRelease;
    }

    public void setPropertyRelease(boolean b) {
    	this.propertyRelease = b;
    }

    public boolean getIncludeArchive() {
    	return includeArchive;
    }

    public void setIncludeArchive(boolean b) {
    	this.includeArchive = b;
    }

    public boolean isArchive() {
    	return archive;
    }

    public void setArchive(boolean b) {
    	this.archive = b;
    }


	// ---------- asset_use stuff ---------

	public void setIncludePermissionStatus(boolean includePermissionStatus) {
		this.includePermissionStatus = includePermissionStatus;
	}

	public boolean getIncludePermissionStatus() {
		return includePermissionStatus;
	}

	public void setPermissionStatus(PermissionStatus permissionStatus) {
		this.permissionStatus = permissionStatus;
	}

	public PermissionStatus getPermissionStatus() {
		return permissionStatus;
	}

	public void setIncludeCanceled(boolean b) {
		this.includeCanceled = b;
	}

	public boolean getIncludeCanceled() {
		return includeCanceled;
	}

	public void setCanceled(boolean b) {
		this.canceled = b;
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setIncludeReplaced(boolean b) {
		this.includeReplaced = b;
	}

	public boolean getIncludeReplaced() {
		return includeReplaced;
	}

	public void setReplaced(boolean b) {
		this.replaced = b;
	}

	public boolean isReplaced() {
		return replaced;
	}

	public void setIncludeCreditLine(boolean b) {
		this.includeCreditLine = b;
	}

	public boolean getIncludeCreditLine() {
		return includeCreditLine;
	}

	public void setCreditLine(String creditLine) {
		this.creditLine = StringUtils.trimToNull(creditLine);
	}

	public String getCreditLine() {
		return creditLine;
	}

	public void setIncludeUsage(boolean includeUsage) {
		this.includeUsage = includeUsage;
	}

	public boolean getIncludeUsage() {
		return includeUsage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}

	public Usage getUsage() {
		return usage;
	}

	public boolean getIncludePosition() {
		return includePosition;
	}

	public void setIncludePosition(boolean b) {
		includePosition = b;
	}

    public String getPosition() {
    	return position;
    }

    public void setPosition(String position) {
    	this.position = StringUtils.trimToNull(position);
    }

	public boolean getIncludeFoundOn() {
		return includeFoundOn;
	}

	public void setIncludeFoundOn(boolean b) {
		includeFoundOn = b;
	}

    public String getFoundOn() {
    	return foundOn;
    }

    public void setFoundOn(String foundOn) {
    	this.foundOn = StringUtils.trimToNull(foundOn);
    }

	public boolean getIncludeComponentName() {
		return includeComponentName;
	}

	public void setIncludeComponentName(boolean b) {
		includeComponentName = b;
	}

    public String getComponentName() {
    	return componentName;
    }

    public void setComponentName(String componentName) {
    	this.componentName = StringUtils.trimToNull(componentName);
    }

	public boolean getIncludeManuscriptPage() {
		return includeManuscriptPage;
	}

	public void setIncludeManuscriptPage(boolean b) {
		includeManuscriptPage = b;
	}

    public String getManuscriptPage() {
    	return manuscriptPage;
    }

    public void setManuscriptPage(String manuscriptPage) {
    	this.manuscriptPage = StringUtils.trimToNull(manuscriptPage);
    }

	public boolean getIncludeFinalPage() {
		return includeFinalPage;
	}

	public void setIncludeFinalPage(boolean b) {
		includeFinalPage = b;
	}

    public String getFinalPage() {
    	return finalPage;
    }

    public void setFinalPage(String finalPage) {
    	this.finalPage = StringUtils.trimToNull(finalPage);
    }

	public void setIncludeColor(boolean b) {
		this.includeColor = b;
	}

	public boolean getIncludeColor() {
		return includeColor;
	}

	public void setColor(boolean b) {
		this.color = b;
	}

	public boolean isColor() {
		return color;
	}

	public void setIncludeSize(boolean b) {
		this.includeSize = b;
	}

	public boolean getIncludeSize() {
		return includeSize;
	}

	public void setSize(Size size) {
		this.size = size;
	}

	public Size getSize() {
		return size;
	}

	public boolean getIncludeCaption() {
		return includeCaption;
	}

	public void setIncludeCaption(boolean b) {
		includeCaption = b;
	}

    public String getCaption() {
    	return caption;
    }

    public void setCaption(String s) {
    	this.caption = StringUtils.trimToNull(s);
    }

	public boolean getIncludePermissionComment() {
		return includePermissionComment;
	}

	public void setIncludePermissionComment(boolean b) {
		includePermissionComment = b;
	}

    public String getPermissionComment() {
    	return permissionComment;
    }

    public void setPermissionComment(String s) {
    	this.permissionComment = StringUtils.trimToNull(s);
    }

	public void setIncludeReuse(boolean b) {
		this.includeReuse = b;
	}

	public boolean getIncludeReuse() {
		return includeReuse;
	}

	public void setReuse(boolean b) {
		this.reuse = b;
	}

	public boolean isReuse() {
		return reuse;
	}

	public void setIncludePickup(boolean b) {
		this.includePickup = b;
	}

	public boolean getIncludePickup() {
		return includePickup;
	}
	
	public void setIncludeMediaManager(boolean b) {
		this.includeMediaManager = b;
	}

	public boolean getIncludeMediaManager() {
		return includeMediaManager;
	}

	public void setPickup(boolean b) {
		this.pickup = b;
	}

	public boolean isPickup() {
		return pickup;
	}
	
	public void setMediaManager(boolean b) {
		this.mediaManager = b;
	}

	public boolean isMediaManager() {
		return mediaManager;
	}


	public void setIncludeCameraCopyToCome(boolean b) {
		this.includeCameraCopyToCome = b;
	}

	public boolean getIncludeCameraCopyToCome() {
		return includeCameraCopyToCome;
	}

	public void setCameraCopyToCome(boolean b) {
		this.cameraCopyToCome = b;
	}

	public boolean isCameraCopyToCome() {
		return cameraCopyToCome;
	}

	public void setIncludeMediaReturnRequest(boolean b) {
		this.includeMediaReturnRequest = b;
	}

	public boolean getIncludeMediaReturnRequest() {
		return includeMediaReturnRequest;
	}

	public void setMediaReturnRequest(boolean b) {
		this.mediaReturnRequest = b;
	}

	public boolean isMediaReturnRequest() {
		return mediaReturnRequest;
	}

	public boolean getIncludeSentToProduction() {
		return includeSentToProduction;
	}

	public void setIncludeSentToProduction(boolean includeSentToProduction) {
		this.includeSentToProduction = includeSentToProduction;
	}

	public boolean isSentToProduction() {
		return sentToProduction;
	}

	public void setSentToProduction(boolean sentToProduction) {
		this.sentToProduction = sentToProduction;
	}
}
