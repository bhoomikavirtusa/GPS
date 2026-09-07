package com.wiley.permissions.domain.persistence.permissions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 *
 * @author cn
 */

@XmlRootElement(name="permissions_count")
public class ProductCount
extends AuditBase
{
	private static final long serialVersionUID = 1L;

	private String isbn = null;

	private String productType = null;

	private Integer count = null;

	private String errorMessage = null;


	@XmlElement(name="isbn")
	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	@XmlElement(name="prodtyp")
	public String getProductType() {
		return productType;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}

	@XmlElement(name="count")
	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	@XmlElement(name="error-msg")
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("ISBN :: ").append(getIsbn()).append("; Product Type :: ").append(getProductType()).append("; Count :: ")
				.append(getCount()).append("; Error Message :: ").append(getErrorMessage()).toString();

	}
}
