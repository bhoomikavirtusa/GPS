package com.wiley.permissions.domain.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.wiley.permissions.domain.persistence.permissions.AddressType;
import com.wiley.permissions.domain.persistence.permissions.Country;
import com.wiley.permissions.domain.persistence.permissions.DeliveryMethod;
import com.wiley.permissions.domain.persistence.permissions.ExtendedSourceEnterpriseMessage;
import com.wiley.permissions.domain.persistence.permissions.MessageErrorOp;
import com.wiley.permissions.domain.persistence.permissions.MessageSuccessOp;

@XmlRootElement(name="wrapper")
public class RestMessage {

	@XmlElements(
		{
			@XmlElement(name="source", type=ExtendedSourceEnterpriseMessage.class),
			@XmlElement(name="country", type=Country.class),
			@XmlElement(name="addressType", type=AddressType.class),
			@XmlElement(name="deliveryMethod", type=DeliveryMethod.class),
			@XmlElement(name="error", type=MessageErrorOp.class),
			@XmlElement(name="success", type=MessageSuccessOp.class)
		}
	)
	private List<Object> itemList = new ArrayList<Object>();


	public List<Object> getItemList() {
		return itemList;
	}

	public void setItemList(List<Object> itemList) {
		this.itemList = itemList;
	}
}
