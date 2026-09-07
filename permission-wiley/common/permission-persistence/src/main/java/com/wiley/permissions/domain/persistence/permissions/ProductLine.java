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
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.permissions.domain.MaterializationKey.Mode;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "PRODUCT_LINE", uniqueConstraints = @UniqueConstraint(columnNames={"CODE", "DATA_SOURCE"}))
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ProductLine
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ProductLine.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey(mode=Mode.ADDITIVE)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String code = null;

	@Column(name = "DATA_SOURCE", nullable = false, length = 20)
	@MaterializationKey(mode=Mode.ADDITIVE)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String dataSource = null;

	@Column(name = "NAME", nullable = true, length = 100)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String name = null;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "BUSINESS_UNIT")
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private BusinessUnit businessUnit = null;


	public ProductLine() {
		super();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElement
	@XmlID
	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	@XmlElement
	//@XmlID - even though code + dataSource is proper unique key, can only have XmlId on one field
	public String getDataSource()
	{
		return dataSource;
	}

	public void setDataSource(String dataSource)
	{
		this.dataSource = dataSource;
	}

	@XmlElement
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@XmlElement
	@XmlIDREF
	public BusinessUnit getBusinessUnit()
	{
		return businessUnit;
	}

	public void setBusinessUnit(BusinessUnit businessUnit)
	{
		this.businessUnit = businessUnit;
	}

	public String toString() {
		// use getters due to JPA
		return "id = " + getId() + ", code = " + getCode() + ", name = " + getName()
			+ ",\r\ndataSource = " + getDataSource()
			+ ",\r\nbusinessUnit = " + getBusinessUnit();
	}
	
	/**
	 * Base on code (XmlID).
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result
				+ ((dataSource == null) ? 0 : dataSource.hashCode());
		return result;
	}

	/**
	 * Base on code (XmlID).
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof ProductLine)) return false;
		ProductLine other = (ProductLine) obj;
		if (code == null) {
			if (other.code != null) return false;
		}
		else if (!code.equals(other.code)) return false;
		if (dataSource == null) {
			if (other.dataSource != null) return false;
		}
		else if (!dataSource.equals(other.dataSource)) return false;
		return true;
	}
}
