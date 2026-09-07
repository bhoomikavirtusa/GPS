package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name="PRIVILEGE")
@SqlResultSetMappings({
	// scalarCode is for SecurityService.getAuthorPrivileges()
	@SqlResultSetMapping(name="scalarCode", columns = @ColumnResult(name = "code") )
})
public class Privilege
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(Privilege.class);

	// constants - these should match the database exactly
	public static final Privilege
		EDIT_CHAPTERS = new Privilege("edit_chapters", "Edit/Enter assets for all chapters", false, true),
		COVER_ASSETS = new Privilege("cover_assets", "View/Enter cover assets", false, true),
		IN_PRODUCTION_ACTIVE = new Privilege("in_production_active", "See In Production products as Active", true, true);

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description = null;

	@Column(name="IS_GLOBAL", nullable=false)
	private boolean global = false;

	@Column(name="AUTHOR_OK", nullable=false)
	private boolean authorOk = false;

	public Privilege() {
		super();
	}

	public Privilege(String code, String description, boolean global, boolean authorOk) {
		this.code = code;
		this.description = description;
		this.global = global;
		this.authorOk = authorOk;
	}

	@XmlID
	@XmlElement
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@XmlElement
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	public boolean isAuthorOk() {
		return authorOk;
	}

	public void setAuthorOk(boolean authorOk) {
		this.authorOk = authorOk;
	}

	/**
	 * Base just on code.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof Privilege)) return false;
		final Privilege other = (Privilege) obj;
		if (this.code != other.code && (this.code == null || !this.code.equals(other.code)))
		{
			return false;
		}
		return true;
	}

	/**
	 * Base just on code.
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 37 * hash + (this.code != null ? this.code.hashCode() : 0);
		return hash;
	}
}
