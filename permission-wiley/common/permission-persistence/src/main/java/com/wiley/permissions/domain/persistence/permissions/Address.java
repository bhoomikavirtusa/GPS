package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.MaterializationKey;

/**
 * @author ttidwell
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "ADDRESS")
public class Address {
	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(Address.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@MaterializationKey
	private Integer id = null;

	@OneToOne
	@JoinColumn(name = "COUNTRY_CODE", nullable = true)
	private Country country = null;

	@Column(name = "CITY", nullable = true)
	private String city = null;

	@Column(name = "LINE_ONE", nullable = false)
	private String lineOne = null;

	@Column(name = "LINE_TWO", nullable = true)
	private String lineTwo = null;

	@Column(name = "LINE_THREE", nullable = true)
	private String lineThree = null;

	@Column(name = "STATE_PROVINCE", nullable = true)
	private String province = null;

	@Column(name = "POSTAL_CODE", nullable = true)
	private String postalCode = null;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ADDRESS_TYPE_CODE", nullable = false)
	private AddressType type = AddressType.MAIN;

	@XmlAttribute
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElement
	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	@XmlElement
	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@XmlElement(name = "line1")
	public String getLineOne() {
		return lineOne;
	}

	public void setLineOne(String lineOne) {
		this.lineOne = lineOne;
	}

	@XmlElement(name = "line2")
	public String getLineTwo() {
		return lineTwo;
	}

	public void setLineTwo(String lineTwo) {
		this.lineTwo = lineTwo;
	}

	@XmlElement(name = "line3")
	public String getLineThree() {
		return lineThree;
	}

	public void setLineThree(String lineThree) {
		this.lineThree = lineThree;
	}

	@XmlElement
	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	@XmlElement
	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public AddressType getType() {
    	return type;
    }

	public void setType(AddressType type) {
    	this.type = type;
    }

	@Transient
	public String getOneLineDisplay() {
		// use getters due to JPA
		ArrayList<String> list = new ArrayList<String>();

		if (StringUtils.isNotBlank(getLineOne())) {
			list.add(lineOne);
		}
		if (StringUtils.isNotBlank(getLineTwo())) {
			list.add(lineTwo);
		}
		if (StringUtils.isNotBlank(getLineThree())) {
			list.add(lineThree);
		}
		if (StringUtils.isNotBlank(getCity())) {
			list.add(city);
		}
		if (StringUtils.isNotBlank(getProvince())) {
			list.add(province);
		}
		if (StringUtils.isNotBlank(getPostalCode())) {
			list.add(postalCode);
		}
		
		if (null != getCountry() && StringUtils.isNotBlank(getCountry().getDescription())) {
			list.add(getCountry().getDescription());
		}
		
		return StringUtils.join(list, ", ");
	}

	/**
	 * See if can delete this method and use toString() instead.
	 * Probably created this method to call from JSP.
	 */
	@Transient
	public String getAddressDisplay() {
		return toString();
	}

	@Transient
	public boolean isBlank() {
		// use getters due to JPA
		boolean ret =
				// don't include type in this check because pre-filled to Main Address
				//getType() == null &&
				StringUtils.isBlank(getLineOne())
		        && StringUtils.isBlank(getLineTwo())
		        && StringUtils.isBlank(getLineThree())
		        && StringUtils.isBlank(getCity())
		        && StringUtils.isBlank(getProvince())
		        && StringUtils.isBlank(getPostalCode());
				// don't include country in this check because can be pre-filled
		        //&& country == null;
		return ret;
	}

	@Transient
	public boolean isNotBlank() {
		return !isBlank();
	}

	@Override
	public String toString() {
		// use getters due to JPA
		StringBuilder sb = new StringBuilder();

		if (StringUtils.isNotBlank(getLineOne())) {
			sb.append(lineOne).append("\r\n");
		}
		if (StringUtils.isNotBlank(getLineTwo())) {
			sb.append(lineTwo).append("\r\n");
		}
		if (StringUtils.isNotBlank(getLineThree())) {
			sb.append(lineThree).append("\r\n");
		}

		sb.append(getCity()).append(", ").append(getProvince()).append(" ").append(getPostalCode());
		if(null != getCountry()) {
			sb.append("\r\n" + getCountry().getDescription());
		}
		return sb.toString();
	}

	@Transient
	public String toStringOneLine() {
		// use getters due to JPA
		StringBuilder sb = new StringBuilder();

		if (StringUtils.isNotBlank(getLineOne())) {
			sb.append(lineOne).append(", ");
		}
		if (StringUtils.isNotBlank(getLineTwo())) {
			sb.append(lineTwo).append(", ");
		}
		if (StringUtils.isNotBlank(getLineThree())) {
			sb.append(lineThree).append(", ");
		}

		sb.append(getCity()).append(", ").append(getProvince()).append(" ").append(getPostalCode());
		if(null != getCountry()) {
			sb.append(", " + getCountry().getDescription());
		}
		return sb.toString();
	}
}
