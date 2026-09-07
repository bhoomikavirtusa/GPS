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

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name="CURRENCY")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class Currency
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// constants should match the database exactly
	public static final Currency
		US = new Currency("USD", "USD (US Dollar)"),
		CAD = new Currency("CAD", "CAD (Canadian Dollar)"),
		AUD = new Currency("AUD", "AUD (Australian Dollar)"),
		EUR = new Currency("EUR", "EUR (Euro)"),
		GBP = new Currency("GBP", "GBP (British Pound)"),
		JPY = new Currency("JPY", "JPY (Japanese Yen)"),
		CNY = new Currency("CNY", "CNY (Chinese Yuan Renminbi)"),
		RUB = new Currency("RUB", "RUB (Russian Rouble)"),
		ISPCR = new Currency("ISPCR", "ISPCR (iStockphoto credits)"),
		//Added New Zealand Currency
	    NZD = new Currency("NZD", "NZD (New Zealand Dollar)"),
	    SEK = new Currency("SEK", "SEK (Swedish Krona)"),
	    SGD = new Currency("SGD", "SGD (Singapore Dollar)");

	public static final Currency [] ALL = { US, CAD, AUD, EUR, GBP, JPY, CNY, RUB, ISPCR ,NZD, SEK, SGD};

	public static Currency getByCode(String code) {
		for (Currency c : ALL) {
			if (c.getCode().equalsIgnoreCase(code)) return c;
		}
		return null;
	}

	@Id
	@Column(name = "CODE", nullable = false, unique = true)
	@XmlElement(name="code")
	@XmlID
	@MaterializationKey
	private String code;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	@XmlElement(name="description")
	private String description;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;


	public Currency() {
		super();
	}

	public Currency(String code, String description) {
	    super();
	    this.code = code;
	    this.description = description;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

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
	 * Returns null if the given description does not match any Currency.
	 */
	public static Currency getByDescription(String description) {
		for (Currency currency : ALL) {
			//if (StringUtils.equalsIgnoreCase(description, currency.getDescription()))  return currency;
			if(currency.getDescription().equalsIgnoreCase(description)) {
				return currency;
			}
		}
		return null;
	}

	// base equals and hashCode on the @XmlID which is code

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof Currency)) return false;
		final Currency other = (Currency) obj;
		if (code == null) {
			if (other.code != null) return false;
		}
		else if (!code.equals(other.code)) return false;

		return true;
	}
}
