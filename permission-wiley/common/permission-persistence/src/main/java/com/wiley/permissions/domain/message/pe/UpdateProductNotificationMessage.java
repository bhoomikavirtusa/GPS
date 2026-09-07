package com.wiley.permissions.domain.message.pe;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.sf.common.xml.bind.DateXmlAdapter;

public class UpdateProductNotificationMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private String externalId;
	private Date lastUpdatedDate;
	private String isbn10;
	private String isbn13;
	private String editor;
	private String pnumber;
	private String commonWork;
	private String dataSource;
	private String businessUnit;
	private String title;
	private String shortAuthorName;
	private List<String> authorCodes;
	private Integer editionNumber;
	private String previousEditionWID;
	private String nextEditionWID;
	private String publicationStatus;
	private Integer copyrightYear;
	private String mediumCode;
	private String productPriority;
	
	public UpdateProductNotificationMessage() {
	}

	@XmlElement
	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@XmlElement
	@XmlJavaTypeAdapter(DateXmlAdapter.class)
	public Date getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(Date lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	@XmlElement
	public String getIsbn10() {
		return isbn10;
	}

	public void setIsbn10(String isbn10) {
		this.isbn10 = isbn10;
	}

	@XmlElement
	public String getIsbn13() {
		return isbn13;
	}

	public void setIsbn13(String isbn13) {
		this.isbn13 = isbn13;
	}
	
	@XmlElement
	public String getProductPriority() {
		return productPriority;
	}

	public void setProductPriority(String productPriority) {
		this.productPriority = productPriority;
	}

	@XmlElement
	public String getEditor() {
		return editor;
	}

	public void setEditor(String editor) {
		this.editor = editor;
	}

	@XmlElement
	public String getPnumber() {
		return pnumber;
	}

	public void setPnumber(String pnumber) {
		this.pnumber = pnumber;
	}

	@XmlElement
	public String getCommonWork() {
		return commonWork;
	}

	public void setCommonWork(String commonWork) {
		this.commonWork = commonWork;
	}

	@XmlElement
	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	@XmlElement
	public String getBusinessUnit() {
		return businessUnit;
	}

	public void setBusinessUnit(String businessUnit) {
		this.businessUnit = businessUnit;
	}

	@XmlElement
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@XmlElement
	public String getShortAuthorName() {
		return shortAuthorName;
	}

	public void setShortAuthorName(String shortAuthorName) {
		this.shortAuthorName = shortAuthorName;
	}

	@XmlElement(name="authorCode")
	public List<String> getAuthorCodes() {
		return authorCodes;
	}

	public void setAuthorCodes(List<String> authorCodes) {
		this.authorCodes = authorCodes;
	}

	@XmlElement
	public Integer getEditionNumber() {
		return editionNumber;
	}

	public void setEditionNumber(Integer i) {
		this.editionNumber = i;
	}

	@XmlElement
	public String getPreviousEditionWID() {
		return previousEditionWID;
	}

	public void setPreviousEditionWID(String previousEditionWID) {
		this.previousEditionWID = previousEditionWID;
	}

	@XmlElement
	public String getNextEditionWID() {
		return nextEditionWID;
	}

	public void setNextEditionWID(String nextEditionWID) {
		this.nextEditionWID = nextEditionWID;
	}

	@XmlElement(name="status")
	public String getPublicationStatus() {
		return publicationStatus;
	}

	public void setPublicationStatus(String publicationStatus) {
		this.publicationStatus = publicationStatus;
	}

	@XmlElement
	public Integer getCopyrightYear() {
		return copyrightYear;
	}

	public void setCopyrightYear(Integer i) {
		this.copyrightYear = i;
	}

	@XmlElement(name = "targetMedium")
	public String getMediumCode() {
		return mediumCode;
	}

	public void setMediumCode(String mediumCode) {
		this.mediumCode = mediumCode;
	}

	/**
	 * Builds a Product object with just the externalId and dataSource.
	 * This is only called by PEMessageService and we don't want the other
	 * identifiers - PE only wants externalId (wid) and dataSource
	 * in a request message for the full product update.
	 */
	public Product buildProductWithExternalIdAndDataSource() {
		Product temp = new Product();
		temp.setExternalId(getExternalId());
		temp.setDataSource(getDataSource());
		return temp;
	}

	@Override
	public String toString() {
		return "externalId = " + externalId + ", isbn13 = " + isbn13 + ", pnumber = " + pnumber
		+ ", commonWork = " + commonWork
		  + ",\r\ntitle = " + title
		  + ",\r\nproductPriority = " + productPriority
		  + ",\r\npreviousEditionWID = " + previousEditionWID + ", nextEditionWID = " + nextEditionWID
		  + ", publicationStatus = " + publicationStatus
		  + ", copyrightYear = " + copyrightYear
		  + ", shortAuthorName = " + shortAuthorName
		  + ",\r\nauthorCodes = " + StringUtils.join(authorCodes, ", ");
	}
}
