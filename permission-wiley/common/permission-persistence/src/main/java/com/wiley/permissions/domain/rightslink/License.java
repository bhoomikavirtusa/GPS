package com.wiley.permissions.domain.rightslink;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.persistence.oxm.annotations.XmlPath;
import org.w3c.dom.Element;

import com.wiley.permissions.common.bean.BeanProperty;
import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.ContractAsset;
import com.wiley.permissions.domain.persistence.permissions.ContractType;
import com.wiley.permissions.domain.persistence.permissions.Currency;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.MediaType;

public class License implements Serializable {

	private static final long serialVersionUID = 1L;

	private final static Log log = LogFactory.getLog(License.class);
	
	public static final Map<String, String> specialMapping = new HashMap<String, String>() {
		{
			put("Rights for", "derivativeWorks");
			put("Duration of use", "offerDuration");
			put("For distribution to", "salesTeritory");
			put("In the following language(s)", "languageLimitation");
			put("Specific languages", "languagesLimitation");
			put("Number of languages", "numLanguages");
			put("With incidental promotional use", "hasIncidentalPromotionalUse");
			put("The lifetime unit quantity of new product", "printRun");
		}
	};

	@XmlElement(name="portionUsed")
	private String contentMediaType;

	@XmlAttribute(name="number")
	private String invoiceNumber;

	@XmlAttribute(name="createDate")
	private Date invoiceDate = new Date();

	@XmlElement(name="publication")
	private String originalPublicationTitle;

	//@XmlPath("details/detail[@key='volumeNum'|@key='portionVolume']/text()")
	//@XmlElement(name="portionVolume")
	private String volumeNum;

	//@XmlPath("details/detail[@key='issueNum'|@key='portionIssue']/text()")
	//@XmlElement(name="author")
	private String issueNum;

	@XmlElement(name="title")
	//@XmlPath("details/detail[@key='articleChapterTitle']/text()")
	private String originalArticleTitle;

	@XmlElement(name="author")
	private String originalArticleAuthor;

	@XmlElement(name="startPage")
	private String originalArticleStart;

	@XmlElement(name="endPage")
	private String originalArticlelEnd;

	@XmlPath("details/detail[@key='numFigures']/text()")
	private String numFigures;

	@XmlPath("details/detail[@key='numImages']/text()")
	private String numImages;

	@XmlPath("details/detail[@key='figureNames'|@key='portionTitle']/text()")
	private String originalFigureNumber;

	@XmlPath("details/detail[@key='portionPageRange']/text()")
	private String originalPageNumber;

	@XmlElement(name="publisher")
	private String sourceName;

	@XmlElement(name="currency")
	private String currency;

	@XmlElement(name="total")
	private double totalAmount;

	@XmlAttribute(name="createDate")
	private Date grantStartDate = new Date();

	private Date grantEndDate;

	private String creditLine;

	private String usage;

	@XmlPath("details/detail[@key='geoRights']/text()")
	private String salesTeritory;

	@XmlPath("details/detail[@key='language']/text()") // English plus translations
	private String languageLimitation;

	@XmlPath("details/detail[@key='languages']/text()") 
	private String languagesLimitation;

	@XmlPath("details/detail[@key='numLanguages']/text()") 
	private String numLanguages;

	@XmlPath("details/detail[@key='hasIncidentalPromotionalUse']/text()") 
	private String hasIncidentalPromotionalUse;

	private String editionGrant;

	@XmlPath("details/detail[@key='rightsForRelatedProducts']/text()")
	private String derivativeWorks;

	@XmlPath("details/detail[@key='offerDuration']/text()")
	private String offerDuration;

	@XmlPath("details/detail[@key='numCopies'|@key='numSets'|@key='distributionQuantity'|@key='circulationRange']/text()")
	private String printRun;

	@XmlElement(name="format")
	private String medium;

	@XmlElement(name="right")
	private String contractType;

	@XmlElement(name="requesterType")
	private String marketUse;

	@XmlAnyElement
	private List<Element> elements;

	@XmlPath("details/detail")
	private List<Detail> details = new ArrayList<Detail>();

	private String xml;

	/**
	 * Use getMediaType() instead to get the MediaType calculated for the contentMediaType.
	 */
	public String getContentMediaType() {
		return contentMediaType;
	}

	public void setContentMediaType(String contentMediaType) {
		this.contentMediaType = contentMediaType;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public Date getInvoiceDate() {
		return invoiceDate;
	}

	public String getOriginalArticleStart() {
		return originalArticleStart;
	}

	public void setOriginalArticleStart(String originalArticleStart) {
		this.originalArticleStart = originalArticleStart;
	}

	public String getOriginalArticlelEnd() {
		return originalArticlelEnd;
	}

	public void setOriginalArticlelEnd(String originalArticlelEnd) {
		this.originalArticlelEnd = originalArticlelEnd;
	}

	public String getNumFigures() {
		return numFigures;
	}

	public void setNumFigures(String numFigures) {
		this.numFigures = numFigures;
	}

	public String getNumImages() {
		return numImages;
	}

	public void setNumImages(String numImages) {
		this.numImages = numImages;
	}

	public String getOriginalFigureNumber() {
		return originalFigureNumber;
	}

	public void setOriginalFigureNumber(String originalFigureNumber) {
		this.originalFigureNumber = originalFigureNumber;
	}

	public String getOriginalPageNumber() {
		return originalPageNumber;
	}

	public void setOriginalPageNumber(String originalPageNumber) {
		this.originalPageNumber = originalPageNumber;
	}

	public String getLanguagesLimitation() {
		return languagesLimitation;
	}

	public void setLanguagesLimitation(String languagesLimitation) {
		this.languagesLimitation = languagesLimitation;
	}

	public String getNumLanguages() {
		return numLanguages;
	}

	public void setNumLanguages(String numLanguages) {
		this.numLanguages = numLanguages;
	}

	public String getHasIncidentalPromotionalUse() {
		return hasIncidentalPromotionalUse;
	}

	public void setHasIncidentalPromotionalUse(String hasIncidentalPromotionalUse) {
		this.hasIncidentalPromotionalUse = hasIncidentalPromotionalUse;
	}

	public String getOfferDuration() {
		return offerDuration;
	}

	public void setOfferDuration(String offerDuration) {
		this.offerDuration = offerDuration;
	}

	public void setInvoiceDate(Date invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public String getOriginalPublicationTitle() {
		return originalPublicationTitle;
	}

	public void setOriginalPublicationTitle(String s) {
		this.originalPublicationTitle = s;
	}

	public String getVolumeNum() {
		return volumeNum;
	}

	public void setVolumeNum(String s) {
		this.volumeNum = s;
	}

	public String getIssueNum() {
		return issueNum;
	}

	public void setIssueNum(String s) {
		this.issueNum = s;
	}

	public String getOriginalArticleTitle() {
		return originalArticleTitle;
	}

	public void setOriginalArticleTitle(String s) {
		this.originalArticleTitle = s;
	}

	public String getOriginalArticleAuthor() {
		return originalArticleAuthor;
	}

	public void setOriginalArticleAuthor(String s) {
		this.originalArticleAuthor = s;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	/**
	 * Use getCurrencyCalc() to get value for contract.
	 */
	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}

	/**
	 * Use getGrantStartDateCalc() to get value for contract.
	 */
	public Date getGrantStartDate() {
		return grantStartDate;
	}

	public void setGrantStartDate(Date grantStartDate) {
		this.grantStartDate = grantStartDate;
	}

	public Date getGrantEndDate() {
		return grantEndDate;
	}

	public void setGrantEndDate(Date grantEndDate) {
		this.grantEndDate = grantEndDate;
	}

	public String getCreditLine() {
		return creditLine;
	}

	public void setCreditLine(String creditLine) {
		this.creditLine = creditLine;
	}

	public String getUsage() {
		return usage;
	}

	public void setUsage(String usage) {
		this.usage = usage;
	}

	public String getSalesTeritory() {
		return salesTeritory;
	}

	public void setSalesTeritory(String salesTeritory) {
		this.salesTeritory = salesTeritory;
	}

	public String getLanguageLimitation() {
		return languageLimitation;
	}

	public void setLanguageLimitation(String languageLimitation) {
		this.languageLimitation = languageLimitation;
	}

	public String getEditionGrant() {
		return editionGrant;
	}

	public void setEditionGrant(String editionGrant) {
		this.editionGrant = editionGrant;
	}

	public String getDerivativeWorks() {
		return derivativeWorks;
	}

	public void setDerivativeWorks(String derivativeWorks) {
		this.derivativeWorks = derivativeWorks;
	}

	public String getMarketUse() {
		return marketUse;
	}

	public void setMarketUse(String marketUse) {
		this.marketUse = marketUse;
	}

	public List<Element> getElements() {
		return elements;
	}

	public void setElements(List<Element> elements) {
		this.elements = elements;
	}

	public List<Detail> getDetails() {
		return details;
	}

	public void setDetails(List<Detail> details) {
		this.details = details;
	}

	public String getPrintRun() {
		return printRun;
	}

	public void setPrintRun(String printRun) {
		this.printRun = printRun;
	}

	public String getMedium() {
		return medium;
	}

	public void setMedium(String medium) {
		this.medium = medium;
	}

	/**
	 * Use getContractTypeCalc() to set value for contract.
	 */
	public String getContractType() {
		return contractType;
	}

	public void setContractType(String contractType) {
		this.contractType = contractType;
	}

	public String getXml() {
		log.debug ("getXml: " + xml);
		return xml;
	}

	public void setXml(String xml) {
		log.debug ("setXml: " + xml);
		this.xml = xml;
	}

	// computed or combinations of fields
	public String getAssetDescription() {
		return StringUtils.left(getOriginalPublicationTitleExtended(), 50) + " - " + StringUtils.left(originalArticleTitle, 50) + " excerpt";
	}

	public String getOriginalPublicationTitleExtended() {
		StringBuilder sb = new StringBuilder(originalPublicationTitle);
		if (StringUtils.isNotBlank(volumeNum)) {
			sb.append(", vol. " + volumeNum);
		}
		if (StringUtils.isNotBlank(issueNum)) {
			sb.append(", issue " + issueNum);
		}
		return sb.toString();
	}

	public MediaType getMediaType() {
		// Want to get mediaType from license.getContentMediaType,
		// but samples values are all null or "figures/tables/illustrations".
		// James says to default to Text for null and Illustration for the other.
		String contentMediaType = getContentMediaType();
		if (StringUtils.containsIgnoreCase(contentMediaType, "illustration")) {
			return MediaType.ILLUSTRATION;
		}
		else {
			return MediaType.TEXT;
		}
	}

	public int getAssetsCount() {
		int numFigures = 0, numImages = 0;
		try {
			numFigures = new Integer(getNumFigures ());
		} catch (Exception e) {			
		}
		try {
			numImages = new Integer(getNumImages ());
		} catch (Exception e) {			
		}
		return numFigures + numImages;
	}

	public ContractType getContractTypeCalc() {
		ContractType cType = ContractType.getByCode(contractType);
		if (cType == null)
				cType = ContractType.FRONTLIST;
		return cType;
	}

	public Currency getCurrencyCalc() {
		// Sample data has currently in this format: USD, GBP, etc (which matches our Currency codes)
		return Currency.getByCode(currency);
	}

	/**
	 * returns a string containing the other elements that are not mapped
	 * @return String
	 */
	public String getOtherElements() {
		StringBuilder sb = new StringBuilder();
		for (Element element : elements) {
			//sb.append(XMLUtil.nodeToString(element));
			sb.append(element.getNodeName() + " = " + element.getTextContent());
			sb.append("\r\n");
		}

		sb.append("other details = \r\n");
		for (Detail detail : details) {
			//sb.append(detail);
		/*	if(detail.getKey().equalsIgnoreCase("volumeNum")){
				setVolumeNum(detail.getValue());
			}
			if(detail.getKey().equalsIgnoreCase("issueNum")){
				setIssueNum(detail.getValue());
			}
		*/
			sb.append(detail.getKey() + " = " + detail.getValue());
			sb.append("\r\n");
		}
		return sb.toString();
	}

	/**
	 * loads data in a contract object from a license
	 * @param contract
	 */
	public void merge (Contract contract) {
		contract.setImportSource(ImportSource.FROM_RIGHTS_LINK);
		contract.setNumber(getInvoiceNumber());
		contract.setContractType(getContractTypeCalc());

		contract.setDate(getInvoiceDate());
		contract.setStartDate(getGrantStartDate());
		contract.setEndDate(getGrantEndDate());

		contract.setCurrency(getCurrencyCalc());
		contract.setPrice(getTotalAmount());
		contract.setLicenseXml(getXml());
		List<ContractAsset> assets = contract.getAssets();
		for (ContractAsset asset : assets) {
			asset.setCreditLine(getCreditLine());
		}
	}

	/**
	 * loads data in a license object from a contract
	 * @param contract
	 */
	public void load (Contract contract) {
		setContractType(contract.getContractType().getCode());
		if (CollectionUtils.isNotEmpty(contract.getAssets()))
			setCreditLine(contract.getAssets().get(0).getCreditLine());
		setCurrency(contract.getCurrency().getCode());
		setGrantEndDate(contract.getEndDate());
		setGrantStartDate(contract.getStartDate());
		setInvoiceDate(contract.getDate());
		setTotalAmount(contract.getPrice());
		setXml (contract.getLicenseXml());
	}

	public Map<String,String> getSpecialMapping () {
		Map<String, String> mapping = new HashMap<String, String> ();
		for (Map.Entry<String, String> entry : specialMapping.entrySet()) {
			String returnVal = null;
			try {
				BeanProperty prop = BeanUtility.getPropertyFromClass(License.class, entry.getValue());
				Method readMethod = prop.getReadMethod();
				returnVal = (String)readMethod.invoke(this, new Object[] {});
			} catch (Exception e) {
				log.debug(e);
			}
			mapping.put(entry.getKey(), StringUtils.isEmpty(returnVal) ? "n/a" : returnVal);
		}
		return mapping;
	}

	
	@Override
	public String toString() {
		return "contentMediaType = " + contentMediaType
			+ ",\r\ninvoiceNumber = " + invoiceNumber + ", invoiceDate = " + invoiceDate
			+ ",\r\noriginalPublicationTitle = " + originalPublicationTitle
	//		+ ",\r\nvolumeNum = " + volumeNum + ", issueNum = " + issueNum
			+ ",\r\noriginalArticleTitle = " + originalArticleTitle
			+ ",\r\noriginalArticleAuthor = " + originalArticleAuthor
			+ ",\r\nsourceName = " + sourceName
			+ ",\r\ncurrency = " + currency + ", totalAmount = " + totalAmount
			+ ",\r\ngrantStartDate = " + grantStartDate + ", grantEndDate = " + grantEndDate
			+ ",\r\ncreditLine = " + creditLine
			+ ",\r\nusage = " + usage
			+ ",\r\nsalesTeritory = " + salesTeritory
			+ ",\r\nlanguageLimitation = " + languageLimitation
			+ ",\r\neditionGrant = " + editionGrant
			+ ",\r\nderivativeWorks = " + derivativeWorks
			+ ",\r\nprintRun = " + printRun
			+ ",\r\nmedium = " + medium
			+ ",\r\ncontractType = " + contractType
			+ ",\r\nmarketUse = " + marketUse
			+ ",\r\nother = " + getOtherElements();
	}

	public static class Detail  implements Serializable {
		private static final long serialVersionUID = 1L;

		@XmlAttribute(name="key")
		private String key;

		@XmlValue
		private String value;

		public Detail() {
			super();
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "Detail [key=" + key + ", value=" + value + "]";
		}
	}
}