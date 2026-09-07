package com.wiley.permissions.domain.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;

import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.domain.message.pe.UpdateProductNotificationMessage;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentList;
import com.wiley.permissions.domain.persistence.permissions.MessageErrorOp;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.ProductError;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.persistence.permissions.Source;

public class MessageOperation
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@XmlAttribute
	OperationType operationType;

	@XmlElement(name="parameter")
	@XmlElementWrapper(name="parameters")
	private List<MessageOperationParameter> parameters = new ArrayList<MessageOperationParameter>();

	@XmlElementWrapper(name="items")
	@XmlElements(
		{
			@XmlElement(name="product", type=Product.class),
			@XmlElement(name="productError", type=ProductError.class),
			@XmlElement(name="updateProductNotificationMessage", type=UpdateProductNotificationMessage.class),
			@XmlElement(name="asset", type=Asset.class),
			@XmlElement(name="assetUse", type=AssetUse.class),
			@XmlElement(name="component", type=Component.class),
			@XmlElement(name="componentList", type=ComponentList.class),
			@XmlElement(name="source", type=Source.class),
			@XmlElement(name="reference", type=Reference.class),
			@XmlElement(name="error", type=MessageErrorOp.class),
			@XmlElement(name="masterList", type=MasterList.class)
		}
	)
	private List<Object> items = new ArrayList<Object>();

	public MessageOperation()
	{
	}

	public MessageOperation(OperationType method)
	{
		setOperationType(method);
	}

	public MessageOperationParameter createMessageOperationParameter()
	{
		return new MessageOperationParameter();
	}

	public MessageOperationParameter createMessageOperationParameter(String name, String value)
	{
		MessageOperationParameter output = new MessageOperationParameter();

		output.setName(name);
		output.setValue(value);

		return output;
	}

	public void addItem(Object item)
	{
		items.add(item);
	}

	public OperationType getOperationType()
	{
		return operationType;
	}

	public void setOperationType(OperationType operationType)
	{
		this.operationType = operationType;
	}

	public List<MessageOperationParameter> getParameters()
	{
		return parameters;
	}

	/**
	 * Returns the value of the parameter with the given name,
	 * or null if the parameter is not found.
	 *
	 * @param name  Must not be null
	 */
	public String getParameterValue(String name) {
		for (MessageOperationParameter param : getParameters()) {
			if (name.equals(param.getName())) {
				return param.getValue();
			}
		}

		return null;
	}

	public void setParameters(List<MessageOperationParameter> parameters)
	{
		this.parameters = parameters;
	}

	public List<Object> getItems()
	{
		return items;
	}

	public void setItems(List<Object> items)
	{
		this.items = items;
	}
}
