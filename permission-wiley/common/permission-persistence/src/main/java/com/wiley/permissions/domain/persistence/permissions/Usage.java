package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "USAGE_TYPE")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class Usage
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// These constants should match the database values (code, description) exactly.
	public static final Usage
		BACK_COVER = new Usage("Back Cover", "Back Cover"),
		BOX_FEATURE = new Usage("Box Feature", "Box Feature"),
		CASE_ART = new Usage("Case Art", "Case Art"),
		CDROM = new Usage("CDROM", "CDROM"),
		ENDPAPERS = new Usage("Endpapers", "Endpapers"),
		EPIGRAPH = new Usage("Epigraph", "Epigraph"),
		EXHIBIT = new Usage("Exhibit", "Exhibit"),
		FEATURE = new Usage("Feature", "Feature"),
		FIGURE = new Usage("Figure", "Figure"),
		FLAPS = new Usage("Flaps", "Flaps"),
		FRONT_COVER = new Usage("Front Cover", "Front Cover"),
		FRONT_MATTER = new Usage("Front Matter", "Front Matter"),
		ICON = new Usage("Icon", "Icon"),
		INLINE_TEXT = new Usage("Inline Text", "Inline Text"),
		OPENER = new Usage("Opener", "Opener"),
		SPINE = new Usage("Spine", "Spine"),
		TABLE = new Usage("Table", "Table"),
		UNNUMBERED_FIGURE = new Usage("Unnumbered Figure", "Unnumbered Figure"),

		WRAP_COVER = new Usage("Wrap Cover", "Wrap Cover");

        // deprecated -- from now on, Cover should be mapped to Front Cover
		// and Numbered Figure should be mapped to Figure
		//COVER = new Usage("Cover", "Cover"),
		//NUMBERED_FIGURE = new Usage("Numbered Figure", "Numbered Figure");

	public static final Usage [] ALL_USAGES = {
		BACK_COVER, BOX_FEATURE, CASE_ART, CDROM, ENDPAPERS, EPIGRAPH, EXHIBIT,
		FEATURE, FIGURE, FLAPS, FRONT_COVER, FRONT_MATTER, ICON, INLINE_TEXT, OPENER,
		SPINE, TABLE, UNNUMBERED_FIGURE, WRAP_COVER  // COVER, NUMBERED_FIGURE,
	};

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description = null;

	// smarkoff:
	// Note there is also an abbreviation column defined in the DB, used for
	// a report (see ArtLogSubReport2.jrxml), but we don't need to define it here
	// (can add later if needed).

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;


	public Usage() {

	}

	public Usage(String code, String description) {
		this.code = code;
		this.description = description;
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
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * We automatically correct the code if only the case is wrong.
	 */
	public void validate() throws ValidateException {
		for (Usage u: ALL_USAGES) {
			if (u.getCode().equals(code))  return;
			if (u.getCode().equalsIgnoreCase(code)) {
				code = u.getCode();
				return;
			}
		}

		throw new ValidateException("\"" + code + "\" is not a valid Usage code");
	}

	/**
	 * Returns null if the given description does not match any Usage.
	 */
	public static Usage getByDescription(String description) {
		for (Usage usage : ALL_USAGES) {
			if (StringUtils.equalsIgnoreCase(description, usage.getDescription()))  return usage;
		}
		return null;
	}

	/**
	 * Returns null if the given code does not match any Usage.
	 */
	public static Usage getByCode(String code) {
		for (Usage usage : ALL_USAGES) {
			if (StringUtils.equalsIgnoreCase(code, usage.getCode()))  return usage;
		}
		return null;
	}

	/**
	 * Returns null if the given description does not match any Usage.
	 * @param description  Must be non-null
	 */
	public static Usage getUsageByDescription(String description) {
		description = description.trim();
		// map old usage_type of "Cover" to "Front Cover"
		if (StringUtils.equalsIgnoreCase("cover", description)) {
			return Usage.FRONT_COVER;
		}

		for (Usage usage : Usage.ALL_USAGES) {
			if (StringUtils.equalsIgnoreCase(usage.getDescription(), description))
				return usage;
		}
		return null;
	}

	/**
	 * Base on code.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof Usage)) return false;
		final Usage other = (Usage) obj;
		if (this.code != other.code && (this.code == null || !this.code.equals(other.code)))
		{
			return false;
		}
		return true;
	}

	/**
	 * Base on code.
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 71 * hash + (this.code != null ? this.code.hashCode() : 0);
		return hash;
	}

	@Override
	public String toString() {
		return "code = " + getCode() + ", description = " + getDescription();
	}
}
