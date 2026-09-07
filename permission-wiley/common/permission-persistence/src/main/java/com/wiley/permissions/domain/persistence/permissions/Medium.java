package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
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
@Table(name = "MEDIUM")
@Cache (usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Medium
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// these constants must match the database
	public static Medium
		SERVICE = new Medium("2", "Service"),
		AUDIO = new Medium("A", "Audio"),
		BINDERS = new Medium("B", "Binders, 3- or 5-ring"),
		CLOTH = new Medium("C", "Cloth"),
		DISPLAY = new Medium("D", "Display Stand / Counter P"),
		EBOOK = new Medium("E", "E-books"),
		FILM = new Medium("F", "Film"),
		GAYLORD = new Medium("G", "Gaylord for Hurts"),
		DOWNLOADABLE = new Medium("H", "Downloadable content"),
		SLIDES = new Medium("I", "Slides"),
		JOURNAL = new Medium("J", "Journal"),
		KIT = new Medium("K", "KIT Nonbook/Nonelectronic"),
		LOOSE_LEAF = new Medium("L", "Loose-leaf"),
		MARKETING_EQUIPMENT = new Medium("M", "Marketing Equipment"),
		NEWSLETTER = new Medium("N", "Newsletter"),
		ONLINE = new Medium("O", "Online Products-all types"),
		PAPER = new Medium("P", "Paper"),
		CONTENT_ONLY = new Medium("Q", "Content only"),
		MIXED = new Medium("R", "Mixed with US E-PRoduct"),
		SOFTWARE = new Medium("S", "Software*"),
		TRANSPARENCY = new Medium("T", "Transparency"),
		US_SERVED = new Medium("U", "US served e-Product"),
		VIDEO = new Medium("V", "Video*"),
		WEBSITE = new Medium("W", "Website"),
		VALUE_PACK = new Medium("X", "Value Pack (Set) in Aus*"),
		PAPER_VAT_UK = new Medium("Y", "Paper VATable (UK only)"),
		CLOTH_VAT_UK = new Medium("Z", "Cloth VATable (UK only)"),
		CD = new Medium("CD", "CD"),
		DVD = new Medium("DVD", "DVD"),
		// All Electronic Rights is fictive value that encapsulates multiple of other values
		ALL_ELECTRONIC_RIGHTS = new Medium("ALLER", "All Electronic Rights");

	public static Medium [] ALL_MEDIUMS = { SERVICE, AUDIO, BINDERS, CLOTH, DISPLAY, EBOOK,
		FILM, GAYLORD, DOWNLOADABLE, SLIDES, JOURNAL, KIT, LOOSE_LEAF, MARKETING_EQUIPMENT,
		NEWSLETTER, ONLINE, PAPER, CONTENT_ONLY, MIXED, SOFTWARE, TRANSPARENCY, US_SERVED,
		VIDEO, WEBSITE, VALUE_PACK, PAPER_VAT_UK, CLOTH_VAT_UK, CD, DVD, ALL_ELECTRONIC_RIGHTS
	};
	//As per DM-292 added the below physical and electronic categories.
	public static Medium [] PHYSICAL_MEDIUMS = {
		SERVICE, BINDERS, CLOTH, DISPLAY, KIT, LOOSE_LEAF, MARKETING_EQUIPMENT,
		NEWSLETTER, PAPER, CONTENT_ONLY, VALUE_PACK, PAPER_VAT_UK, CLOTH_VAT_UK
	};

	public static Medium [] ELECTRONIC_MEDIUMS = {
		AUDIO, EBOOK, FILM, DOWNLOADABLE, SLIDES, ONLINE, MIXED, SOFTWARE,
		TRANSPARENCY, US_SERVED, VIDEO, WEBSITE
	};

	/**
	 * Returns "P" for Physical medium and "E" for electronic medium
	 */
	public static String getMediumType(String code) {
		if(null != code) {
			for (Medium m : PHYSICAL_MEDIUMS) {
				if (m.getCode().equals(code)) {
					return "P";
				}
			}
			for (Medium m : ELECTRONIC_MEDIUMS) {
				if (m.getCode().equals(code)) {
					return "E";
				}
			}
		}
		return null;
	}

	/**
	 * Returns null if the code is not recognized.
	 */
	public static Medium getMediumForCode(String code) {
		for (Medium m : ALL_MEDIUMS) {
			if (m.getCode().equals(code)) {
				return m;
			}
		}

		return null;
	}

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String code = null;

	@Column(name = "NAME", nullable = true, length = 100)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String name = null;

	public Medium()
	{
		super();
	}

	public Medium(String code, String name) {
		this.code = code;
		this.name = name;
	}

	@XmlElement(name = "code")
	@XmlID
	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	@XmlElement(name = "name")
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	// base equals and hashCode on the @XmlID which is code

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)  return true;
		if (obj == null)  return false;
		// Important to use "instance of" due to JPA proxies
		if (!(obj instanceof Medium)) return false;
		final Medium other = (Medium) obj;
		if (code == null) {
			if (other.code != null) return false;
		}
		else if (!code.equals(other.code)) {
			return false;
		}
		return true;
	}
}
