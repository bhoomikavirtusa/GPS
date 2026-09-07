package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.permissions.domain.MaterializationKey.Mode;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "EDITOR", uniqueConstraints = @UniqueConstraint(columnNames={"CODE", "DATA_SOURCE"}))
public class Editor
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// smarkoff: The only reason to have an id column at all is
	// in case in the future we want to reference this table
	// and using a single column.
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

	@Column(name = "NAME", nullable = true, length = 200)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String name = null;

	public Editor() {
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
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@XmlElement
	//@XmlID - even though code + dataSource is proper unique key can only have one XmlID
	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	@XmlElement(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// base equals and hashCode on code + dataSource since this is unique
	// - does not quite match XmlID but ok (won't mess up CustomIDResolver)

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result
				+ ((dataSource == null) ? 0 : dataSource.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof Editor)) return false;
		Editor other = (Editor) obj;
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
