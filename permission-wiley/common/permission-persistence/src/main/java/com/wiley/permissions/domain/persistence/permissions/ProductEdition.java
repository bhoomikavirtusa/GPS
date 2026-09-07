package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "PRODUCT_EDITION")
@NamedQueries
(
	{
		@NamedQuery(name="ProductEdition.findByExternalId", query="from ProductEdition pe where pe.externalId = :externalId")
	}
)
public class ProductEdition
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private Integer id = null;

	@Column(name = "EXTERNAL_ID", nullable = false)
	@MaterializationKey(ignoreCase=true, alwaysTrim=true)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String externalId;

	@Column(name = "NAME", nullable=false, length=100)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String name = null;

	@Column(name="EDITION_NUMBER", nullable=false)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private Integer editionNumber = 0;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name="PRODUCT_FAMILY")
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private ProductFamily productFamily = null;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name="PRODUCT_LINE_ID")
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private ProductLine productLine = null;

	public ProductEdition() {
		super();
	}

	@XmlElement(name="id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElement(name="externalId")
	@XmlID
	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@XmlElement(name="name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name="editionNumber")
	public Integer getEditionNumber() {
		return editionNumber;
	}

	public void setEditionNumber(Integer editionNumber) {
		this.editionNumber = editionNumber;
	}

	@XmlIDREF
	public ProductFamily getProductFamily() {
		return productFamily;
	}

	public void setProductFamily(ProductFamily productFamily) {
		this.productFamily = productFamily;
	}

	@XmlIDREF
	public ProductLine getProductLine() {
		return productLine;
	}

	public void setProductLine(ProductLine productLine) {
		this.productLine = productLine;
	}

	@Override
	public String toString() {
		// use getters due to JPA
		return "id = " + getId()
			+ ", externalId = " + getExternalId()
			+ ", name = " + getName()
			+ ", editionNumber = " + getEditionNumber()
			+ ",\r\nproductFamily = " + getProductFamily()
			+ ",\r\nproductLine = " + getProductLine();
	}

	/**
	 * Base on externalId (XmlID).
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((externalId == null) ? 0 : externalId.hashCode());
		return result;
	}

	/**
	 * Base on externalId (XmlID).
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof ProductEdition)) return false;
		ProductEdition other = (ProductEdition) obj;
		if (externalId == null) {
			if (other.externalId != null) return false;
		}
		else if (!externalId.equals(other.externalId)) return false;
		return true;
	}
}
