package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import com.wiley.permissions.domain.MaterializationKey;

/**
 *
 * @author ttidwell
 */
@MappedSuperclass
public abstract class EnumData implements Comparable<EnumData>
{
	@Id
	@Column(name="CODE", nullable=false, insertable=false, updatable=false)
	@MaterializationKey (alwaysTrim=true)
	private String code = null;

	@Column(name="DESCRIPTION", nullable=false, insertable=false, updatable=false)
	private String description = null;

	public EnumData() { }

	/**
	 * @param code  Must be non-blank
	 * @param description
	 */
	public EnumData(String code, String description) {
		this.code = code;
		this.description = description;
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

	public void setDescription(String value) {
		this.description = value;
	}

	@Override
	public String toString() {
		return super.toString() + ", code = " + getCode() + ", description = " + getDescription();
	}

	/**
	 * Base only on code.
	 */
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	/**
	 * Base only on code. This method should generally be overridden so it contains
	 * a check for the correct class instance instead of just this base class of EnumData.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof EnumData)) return false;
		final EnumData other = (EnumData) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		}
		else if (!code.equals(other.code))
			return false;
		return true;
	}

	@Override
	public int compareTo(EnumData ed) {
		return code.compareTo(ed.getCode());
	}
}
