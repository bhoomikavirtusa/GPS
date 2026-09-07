package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "PUBLICATION_STATUS")
@Cache (usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class PublicationStatus
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// constants should match database exactly (however this is not a complete list)
	public static final PublicationStatus
		MAY_OR_MAY_NOT_REPRINT = new PublicationStatus("A", "MAY/MAY NOT REPRINT"),
		EDITORIAL     = new PublicationStatus("E", "Editorial"),
	    IN_PRODUCTION = new PublicationStatus("I", "In Production"),
	    PUBLISHED = new PublicationStatus("N", "Published"),
	    OUT_OF_PRINT = new PublicationStatus("O", "Out of Print"),
	    PRE_CONTRACT = new PublicationStatus("P", "Pre-contract"),
	    NEW_EDITION_PENDING = new PublicationStatus("W", "New edition pending"),
	    NO_REPRINT = new PublicationStatus("X", "No reprint");

	public static final int PUBLISHED_START_YEAR = 2012;

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String description = null;

	public PublicationStatus() {
		super();
	}

	public PublicationStatus(String code, String description) {
		this.code = code;
		this.description = description;
	}

	@XmlElement(name = "code")
	@XmlID
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Transient
	public String getCodeAndDescription() {
		return this.code + " - " + this.description;
	}

	@XmlElement(name = "description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	// This method used by:
	// PhotoSummaryView.isPreProduction()
	// CwLandingController.main()
	// AuthorAccessController.viewEdit()
	/**
	 * @param pubStatus  Null returns false
	 *
	 * If includeInProduction is true,
	 * this still does NOT include PUBLISHED(N) as for the compliance reports.
	 */
	static public boolean isPreProduction(String pubStatus, boolean includeInProduction) {
		return pubStatus != null && (pubStatus.equals(PublicationStatus.PRE_CONTRACT.getCode())
				|| pubStatus.equals(PublicationStatus.EDITORIAL.getCode())
				|| (includeInProduction && pubStatus.equals(PublicationStatus.IN_PRODUCTION.getCode())));
	}

	// used by: PEMessageService.doWeCareAboutProduct()
	/**
	 * Unlike the above method, this includes PUBLISHED(N).
	 */
	static public boolean doWeCareAboutProduct(String pubStatus, Integer copyrightYear) {
		return pubStatus != null && pubStatus.equals(PublicationStatus.PRE_CONTRACT.getCode())
			|| pubStatus.equals(PublicationStatus.EDITORIAL.getCode())
			|| pubStatus.equals(PublicationStatus.IN_PRODUCTION.getCode())
			|| (pubStatus.equals(PublicationStatus.PUBLISHED.getCode())
					&& (copyrightYear == null || copyrightYear >= PUBLISHED_START_YEAR));
	}

	// This method used by:
	// UserRepository.getCurrentProductsForAuthor()
	// UserRepository.getArchivedProductsForAuthor()
	/**
	 * Returns IN clause.
	 *
	 * If includeInProduction is true,
	 * this still does NOT include PUBLISHED(N) as for the compliance reports.
	 */
	static public String getPreProductionSQL(boolean includeInProduction) {
		StringBuilder sb = new StringBuilder("('");
		sb.append(PublicationStatus.PRE_CONTRACT.getCode());
		sb.append("', '");
		sb.append(PublicationStatus.EDITORIAL.getCode());
		if (includeInProduction) {
			sb.append("', '");
			sb.append(PublicationStatus.IN_PRODUCTION.getCode());
		}
		sb.append("')");

		return sb.toString();
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
		if (!(obj instanceof PublicationStatus)) return false;
		final PublicationStatus other = (PublicationStatus) obj;
		if (this.code != other.code && (this.code == null || !this.code.equals(other.code)))
		{
			return false;
		}
		return true;
	}

	/**
	 * Base on code (XmlID).
	 */
	@Override
	public int hashCode() {
		int hash = 5;
		hash = 53 * hash + (this.code != null ? this.code.hashCode() : 0);
		return hash;
	}
}
