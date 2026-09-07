package com.wiley.permissions.domain.persistence.permissions;

import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.DomainObject;


/**
 * Class that combines Enterprise and Source beans.
 * so that the Correct CMS message can be served
 *
 * @author nmedrano
 */
public class ExtendedSourceEnterpriseMessage
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	private Integer id = null;
	private String externalId = null;
	private String name;
	private String creditLine;
	private String phone;
	private String fax;
	private String website;
	private String coreIdOne;
	private String coreIdTwo;
	private String permissionType;
	private DeliveryMethod deliveryMethod;
	private Country country = null;
	private Date lastUpdatedDate = null;

	@XmlAttribute (name="updateTime")
	public Date getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(Date value) {
		lastUpdatedDate = value;
	}

	@XmlAttribute
	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	@XmlID
	@XmlAttribute (name="wid")
	public String getExternalId()
	{
		return externalId;
	}

	public void setExternalId(String externalId)
	{
		this.externalId = externalId;
	}

	@XmlElement
	public String getName()
	{
		return name;
	}

	public void setName(String value)
	{
		name = value;
	}

	@XmlElement
	public Country getCountry()
	{
		return country;
	}

	public void setCountry(Country country)
	{
		this.country = country;
	}

	@XmlElement
	public String getPhone()
	{
		return phone;
	}

	public void setPhone(String value)
	{
		this.phone = value;
	}

	@XmlElement
	public String getFax()
	{
		return fax;
	}

	public void setFax(String value)
	{
		this.fax = value;
	}

	@XmlElement
	public String getWebsite()
	{
		return website;
	}

	public void setWebsite(String value)
	{
		this.website = value;
	}

	@XmlElement
	public String getCoreIdOne()
	{
		return coreIdOne;
	}

	public void setCoreIdOne(String value)
	{
		this.coreIdOne = value;
	}

	@XmlElement
	public String getCoreIdTwo()
	{
		return coreIdTwo;
	}

	public void setCoreIdTwo(String value)
	{
		this.coreIdTwo = value;
	}

	@XmlElement
	public String getPermissionType()
	{
		return permissionType;
	}

	public void setPermissionType(String value)
	{
		this.permissionType = value;
	}

	@XmlElement
	public String getCreditLine()
	{
		return creditLine;
	}

	public void setCreditLine(String creditLine)
	{
		this.creditLine = StringUtils.trimToNull(creditLine);
	}

	@XmlElement
	public DeliveryMethod getDeliveryMethod()
	{
		return deliveryMethod;
	}

	public void setDeliveryMethod(DeliveryMethod deliveryMethod)
	{
		this.deliveryMethod = deliveryMethod;
	}

	@Override
	public String toString() {
		String dvalue = "id [" + id + "] \n" +
		"external id [" + getExternalId() + "] \n" +
		"name [" + name + "] \n" +
		"permissionType [" + permissionType + "] \n" +
		"creditLine [" + creditLine + "] \n" +
		"phone [" + phone + "] \n" +
		"fax [" + fax + "] \n" +
		"website [" + website + "] \n" +
		"coreIdOne [" + coreIdOne + "] \n" +
		"coreIdTwo [" + coreIdTwo + "] \n";

		if (null != deliveryMethod) {
			dvalue = dvalue + "deliveryMethod[" + deliveryMethod.toString() + "]\n";
			dvalue = dvalue + "deliveryMethod code[" + deliveryMethod.getCode() + "]\n";
			dvalue = dvalue + "deliveryMethod description[" + deliveryMethod.getDescription() + "]\n";
		} else {
			dvalue = dvalue + "deliveryMethod[NULL]";
		}

		return dvalue;
	}

}
