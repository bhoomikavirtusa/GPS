package com.wiley.permissions.services.view;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.sf.common.lang.StringUtil;

@XmlRootElement
public class ProductSummaryView {

	private static final Log log = LogFactory.getLog(ProductSummaryView.class);

	private Integer id;
	private String externalId;
	private String isbn10;
	private String isbn13;
	private String editor;
	private String pnumber;
	private String title;
	private Date lastUpdatedDate;
	private String authorNames;
	private int assetsCount;
	private Integer commonWorkId;
	private Date boundBookDate;
	private Date manuscriptProductionDate;
	private String notes;
	private Integer photoIllusTotalCount;
	private String mediumCode;
	private Integer editionNumber;
	private String  productPriority;


	public ProductSummaryView() {
		super();
	}

	public ProductSummaryView(Product product) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
	{
		super();
		PropertyUtils.copyProperties(this, product);
		try {
			this.setCommonWorkId(product.getCommonWork().getId());
			this.setAuthorNames(product.getAuthorsAsString());
			notes = product.getCommonWork().getNotes();
		} catch (Exception e) {
			log.debug("ProductSummaryView()(ctor): caught exception: ", e);
		}
	}

	public String getMediumCode() {
		return mediumCode;
	}

	public void setMediumCode(String mediumCode) {
		this.mediumCode = mediumCode;
	}
	
	public Integer getEditionNumber() {
		return editionNumber;
	}

	public void setEditionNumber(Integer editionNumber) {
		this.editionNumber = editionNumber;
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
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

	public String getEditor() {
		return editor;
	}

	public void setEditor(String editor) {
		this.editor = editor;
	}

	public String getPnumber() {
		return pnumber;
	}

	public void setPnumber(String pnumber) {
		this.pnumber = pnumber;
	}

	public String getProductPriority() {
		return productPriority;
	}

	public void setProductPriority(String productPriority) {
		this.productPriority = productPriority;
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(Date lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	public String getAuthorNames() {
		return authorNames;
	}

	public void setAuthorNames(String authors) {
		this.authorNames = authors;
	}

	public int getAssetsCount() {
		return assetsCount;
	}

	public void setAssetsCount(int assetsCount) {
		this.assetsCount = assetsCount;
	}

	public Integer getCommonWorkId() {
		return commonWorkId;
	}

	public void setCommonWorkId(Integer commonWorkId) {
		this.commonWorkId = commonWorkId;
	}

	public Date getBoundBookDate() {
		return boundBookDate;
	}

	public void setBoundBookDate(Date boundBookDate) {
		this.boundBookDate = boundBookDate;
	}

	public Date getManuscriptProductionDate() {
		return manuscriptProductionDate;
	}

	public void setManuscriptProductionDate(Date manuscriptProductionDate) {
		this.manuscriptProductionDate = manuscriptProductionDate;
	}

	public String getNotes() {
		return notes;
	}

	public String getNotesTruncated() {
		return StringUtil.truncate(notes, 40, true);
	}

	public String getNotesEscaped() {
		return StringEscapeUtils.escapeHtml4(notes);
	}

	public Integer getPhotoIllusTotalCount() {
		return photoIllusTotalCount;
	}

	public void setPhotoIllusTotalCount(Integer i) {
		this.photoIllusTotalCount = i;
	}
}
