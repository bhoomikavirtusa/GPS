package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.wiley.permissions.domain.MaterializationKey;

/**
 *
 * @author ttidwell
 */
@Entity
@Table(name = "SOURCE_2_ADDRESS")
@IdClass(value=SourceAddressPK.class)
public class SourceAddress
{
	@Id
	@MaterializationKey(mode=MaterializationKey.Mode.ADDITIVE)
	@ManyToOne
	private Source source = null;

	@Id
	@MaterializationKey(mode=MaterializationKey.Mode.ADDITIVE)
	@ManyToOne
	private Address address = null;

	public Source getSource() {
    	return source;
    }

	public void setSource(Source source) {
    	this.source = source;
    }

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@Transient
	@XmlAttribute
	public Integer getId() {
		return address.getId();
	}

	public void setId(Integer id) {
		address.setId(id);
	}

	@Transient
	@XmlElement
	public Country getCountry() {
		return address.getCountry();
	}

	public void setCountry(Country country) {
		address.setCountry(country);
	}

	@Transient
	@XmlElement
	public String getCity() {
		return address.getCity();
	}

	public void setCity(String city) {
		address.setCity(city);
	}

	@Transient
	@XmlElement(name = "line1")
	public String getLineOne() {
		return address.getLineOne();
	}

	public void setLineOne(String lineOne) {
		address.setLineOne(lineOne);
	}

	@Transient
	@XmlElement(name = "line2")
	public String getLineTwo() {
		return address.getLineTwo();
	}

	public void setLineTwo(String lineTwo) {
		address.setLineTwo(lineTwo);
	}

	@Transient
	@XmlElement(name = "line3")
	public String getLineThree() {
		return address.getLineThree();
	}

	public void setLineThree(String lineThree) {
		address.setLineThree(lineThree);
	}

	@Transient
	@XmlElement
	public String getProvince() {
		return address.getProvince();
	}

	public void setProvince(String province) {
		address.setProvince(province);
	}

	@Transient
	@XmlElement
	public String getPostalCode() {
		return address.getPostalCode();
	}

	public void setPostalCode(String postalCode) {
		address.setPostalCode(postalCode);
	}
}