package com.wiley.permissions.domain.message.pe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.ComplianceStatus;
import com.wiley.sf.common.io.FixWindows1252Chars;

/**
 *
 * @author ttidwell
 */
public class ProductSearchResult
implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(ProductSearchResult.class);

	// - following fields available in SOAP search and product index (even for products we have not imported)

	@XmlAttribute(required = true)
	private String wid = null;

	@XmlAttribute(name = "businessUnit")
	private String businessUnitCode = null;

	@XmlTransient
	// This is filled in by ProductServiceImpl.formatSearchResults()
	// or by corresponding logic if the result is from the product index.
	private String businessUnitName = null;

	@XmlAttribute
	private String isbn10 = null;

	@XmlAttribute
	private String isbn13 = null;

	@XmlAttribute(name="pnumber")
	private String pnumber = null;

	@XmlElement(name="commonWork")
	private String commonWorkCode = null;

	@XmlAttribute
	private String status = null;

	@XmlAttribute(required = true)
	private String dataSource = null;

	@XmlElement
	private String title = null;

	@XmlElement
	private String shortAuthorName = null;

	@XmlElement(name="author")
	private List<String> authors = new ArrayList<String>();

	@XmlElement
	private String copyrightYear = null;

	@XmlElement
	private String editionNumber = null;
	
	@XmlElement(name="medium")
	private String mediumCode = null;

	// -- following fields available from SOAP search, and from index if it's a product we imported

	@XmlElement(name="productLine")
	private String productLineCode = null;

	@XmlElement
	private String editor = null;

	// -- following fields only available from the product index -- not the SOAP search

	private String previousEditionWID;
	private String nextEditionWID;

	private Date lastUpdatedDate;

	private Date indexDate;  // date/time that document was added (or re-added) to index - for debugging only
	private int indexVersion;

	// -- following fields only in index, and only for products we have in DB

	private Integer cwId;
	private String editorName;
	private Date consolidatedReleaseDate;
	private Date transmittalDate;
	private ComplianceStatus complianceStatus;
	private Date lastWorkedOnDate;
	private Integer auCountNotCanceled;
	private Integer coverCountNotCanceled;
	private Integer statusNotOkCount;
	private Integer photoIllusTotalCount;
	private String productPriority;
	private String photoEditorLastName;
	private String photoEditorFirstName;
	private String msToComp;
	private Double finalCost;
	private Integer totalPhotos;
	private Integer photoCoverPickupCount;
	private Integer photoInternalFreeCount;
	private Integer photoInternalRoyalityFreeCount;

	// Default to false, may be set to true by ProductService.searchProducts()
	// after we get the results back (not part of the actual product results).
	private boolean watched = false;


	public ProductSearchResult() {

	}

	public String getShortAuthorName() {
		return shortAuthorName;
	}

	public void setShortAuthorName(String shortAuthorName) {
		this.shortAuthorName = shortAuthorName;
	}

	public String getWid() {
		return wid;
	}

	public void setWid(String wid) {
		this.wid = wid;
	}

	public String getBusinessUnitCode() {
		return businessUnitCode;
	}

	public void setBusinessUnitCode(String businessUnitCode) {
		this.businessUnitCode = businessUnitCode;
	}

	public String getBusinessUnitName() {
		return businessUnitName;
	}

	public void setBusinessUnitName(String businessUnitName) {
		this.businessUnitName = businessUnitName;
	}

	public String getIsbn10() {
		return isbn10;
	}

	public void setIsbn10(String isbn10) {
		this.isbn10 = isbn10;
	}

	public String getIsbn13() {
		return isbn13;
	}

	public void setIsbn13(String isbn13) {
		this.isbn13 = isbn13;
	}

	/**
	 * Will return ISBN13 if available, else ISBN10 else PNumber
	 * @return String
	 */
	public String getIsbn () {
		if (StringUtils.isNotBlank(getIsbn13())) {
			return getIsbn13();
		} else if (StringUtils.isNotBlank(getIsbn10())) {
			return getIsbn10();
		} else if (StringUtils.isNotBlank(getPnumber())) {
			return getPnumber();
		} else return StringUtils.EMPTY;
	}

	public String getPnumber() {
		return pnumber;
	}

	public void setPnumber(String pnumber) {
		this.pnumber = pnumber;
	}

	public String getCommonWorkCode() {
		return commonWorkCode;
	}

	public void setCommonWorkCode(String commonWorkCode) {
		this.commonWorkCode = commonWorkCode;
	}

	
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = FixWindows1252Chars.fix(title, "title", "setTitle", log);
	}

	public List<String> getAuthors() {
		// add the short name
		if (CollectionUtils.isEmpty(authors) && StringUtils.isNotBlank(shortAuthorName)) {
			if (authors == null)  authors = new ArrayList<String>(1);
			authors.add(shortAuthorName);
		}
		return authors;
	}

	public String getAuthorsAsString() {
		return StringUtils.join(getAuthors(), ", ");
	}

	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}

	public String getCopyrightYear() {
		return copyrightYear;
	}

	public void setCopyrightYear(String copyrightYear) {
		this.copyrightYear = copyrightYear;
	}

	public String getEditionNumber() {
		return editionNumber;
	}

	public void setEditionNumber(String editionNumber) {
		this.editionNumber = editionNumber;
	}
	
	public String getMediumCode() {
		return mediumCode;
	}

	public void setMediumCode(String mediumCode) {
		this.mediumCode = mediumCode;
	}

	public String getProductLineCode() {
		return productLineCode;
	}

	public void setProductLine(String productLineCode) {
		this.productLineCode = productLineCode;
	}

	public String getEditor() {
		return editor;
	}

	public void setEditor(String editor) {
		this.editor = editor;
	}

	public String getPreviousEditionWID() {
		return previousEditionWID;
	}

	public void setPreviousEditionWID(String previousEditionWID) {
		this.previousEditionWID = previousEditionWID;
	}

	public String getNextEditionWID() {
		return nextEditionWID;
	}

	public void setNextEditionWID(String nextEditionWID) {
		this.nextEditionWID = nextEditionWID;
	}

	public Date getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(Date date) {
		lastUpdatedDate = date;
	}

	public Date getIndexDate() {
		return indexDate;
	}

	public void setIndexDate(Date indexDate) {
		this.indexDate = indexDate;
	}

	public int getIndexVersion() {
		return indexVersion;
	}

	public void setIndexVersion(int indexVersion) {
		this.indexVersion = indexVersion;
	}

	public Integer getCwId() {
		return cwId;
	}

	public void setCwId(Integer cwId) {
		this.cwId = cwId;
	}

	public String getEditorName() {
		return editorName;
	}

	public void setEditorName(String s) {
		editorName = s;
	}

	public Date getConsolidatedReleaseDate() {
		return consolidatedReleaseDate;
	}

	public void setConsolidatedReleaseDate(Date date) {
		this.consolidatedReleaseDate = date;
	}
	
	public Date getTransmittalDate() {
		return transmittalDate;
	}

	public void setTransmittalDate(Date date) {
		this.transmittalDate = date;
	}

	public ComplianceStatus getComplianceStatus() {
		return complianceStatus;
	}

	public void setComplianceStatus(ComplianceStatus complianceStatus) {
		this.complianceStatus = complianceStatus;
	}

	public Date getLastWorkedOnDate() {
		return lastWorkedOnDate;
	}

	public void setLastWorkedOnDate(Date lastWorkedOnDate) {
		this.lastWorkedOnDate = lastWorkedOnDate;
	}

	public Integer getAuCountNotCanceled() {
		return auCountNotCanceled;
	}

	public void setAuCountNotCanceled(Integer count) {
		auCountNotCanceled = count;
	}

	public Integer getCoverCountNotCanceled() {
		return coverCountNotCanceled;
	}

	public void setCoverCountNotCanceled(Integer count) {
		coverCountNotCanceled = count;
	}

	public Integer getStatusNotOkCount() {
		return statusNotOkCount;
	}

	public void setStatusNotOkCount(Integer count) {
		statusNotOkCount = count;
	}

	public Integer getPhotoIllusTotalCount() {
		return photoIllusTotalCount;
	}

	public void setPhotoIllusTotalCount(Integer count) {
		photoIllusTotalCount = count;
	}

	public String getPhotoEditorLastName() {
		return photoEditorLastName;
	}

	public void setPhotoEditorLastName(String s) {
		this.photoEditorLastName = s;
	}

	public String getPhotoEditorFirstName() {
		return photoEditorFirstName;
	}

	public void setPhotoEditorFirstName(String s) {
		this.photoEditorFirstName = s;
	}

	public String getMsToComp() {
		return msToComp;
	}

	public void setMsToComp(String s) {
		this.msToComp = s;
	}

	public Double getFinalCost() {
		return finalCost;
	}

	public void setFinalCost(Double d) {
		this.finalCost = d;
	}

	public Integer getTotalPhotos() {
		return totalPhotos;
	}

	public void setTotalPhotos(Integer i) {
		this.totalPhotos = i;
	}

	public Integer getPhotoCoverPickupCount() {
		return photoCoverPickupCount;
	}

	public void setPhotoCoverPickupCount(Integer i) {
		this.photoCoverPickupCount = i;
	}

	public Integer getPhotoInternalFreeCount() {
		return photoInternalFreeCount;
	}

	public void setPhotoInternalFreeCount(Integer i) {
		this.photoInternalFreeCount = i;
	}

	public Integer getPhotoInternalRoyalityFreeCount() {
		return photoInternalRoyalityFreeCount;
	}

	public void setPhotoInternalRoyalityFreeCount(Integer i) {
		this.photoInternalRoyalityFreeCount = i;
	}

	public boolean isWatched() {
		return watched;
	}

	public void setWatched(boolean b) {
		watched = b;
	}
	
	public String getProductPriority() {
		return productPriority;
	}

	public void setProductPriority(String productPriority) {
		this.productPriority = productPriority;
	}

	@Override
	public String toString() {
		return "wid = " + wid + ", commonWorkCode = " + commonWorkCode
			+ ", businessUnitCode = " + businessUnitCode
			+ ", productLineCode = " + productLineCode
			+ ", mediumCode = " + mediumCode
			+ ", title = " + title + ", (other fields not shown)";
	}
}
