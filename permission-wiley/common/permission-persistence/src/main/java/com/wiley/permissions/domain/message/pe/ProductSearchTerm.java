package com.wiley.permissions.domain.message.pe;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlValue;

/**
 *
 * @author ttidwell
 */
public class ProductSearchTerm
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@XmlEnum
	public enum ProductSearchField
	{
		@XmlEnumValue("title")
		TITLE,
		@XmlEnumValue("author")
		AUTHOR,
		@XmlEnumValue("businessUnit")
		BUSINESS_UNIT,
		@XmlEnumValue("productLine")
		PRODUCT_LINE,
		@XmlEnumValue("isbn10")
		ISBN_10,
		@XmlEnumValue("isbn13")
		ISBN_13,
		@XmlEnumValue("pnumber")
		PRODUCT_NUMBER,
		@XmlEnumValue("commonWork")
		COMMON_WORK,
		@XmlEnumValue("copyrightYear")
		COPYRIGHT_YEAR,
		@XmlEnumValue("editionNumber")
		EDITION_NUMBER,
		@XmlEnumValue("medium")
		MEDIUM,
		@XmlEnumValue("editor")
		EDITOR,
		@XmlEnumValue("status")
		STATUS
	}

	@XmlEnum
	public enum ProductSearchOperator
	{
		@XmlEnumValue("equals")
		EQUALS,
		@XmlEnumValue("not equals")
		NOT_EQUALS
	}

	@XmlEnum
	public enum ProductSearchConnective
	{
		@XmlEnumValue("and")
		AND,
		@XmlEnumValue("or")
		OR

	}
	@XmlAttribute
	private ProductSearchField field = null;

	@XmlAttribute
	private ProductSearchOperator operator = null;

	@XmlAttribute
	private ProductSearchConnective connective = null;

	@XmlValue
	private String term = null;

	public ProductSearchTerm()
	{
	}

	public ProductSearchTerm(
			ProductSearchField field, 
			ProductSearchOperator operator, 
			ProductSearchConnective connective, String term)
	{
		this.setField(field);
		this.setOperator(operator);
		this.setConnective(connective);
		this.setTerm(term);
	}

	public ProductSearchField getField()
	{
		return field;
	}

	public void setField(ProductSearchField field)
	{
		this.field = field;
	}

	public ProductSearchOperator getOperator()
	{
		return operator;
	}

	public void setOperator(ProductSearchOperator operator)
	{
		this.operator = operator;
	}

	public ProductSearchConnective getConnective()
	{
		return connective;
	}

	public void setConnective(ProductSearchConnective connective)
	{
		this.connective = connective;
	}

	public String getTerm()
	{
		return term;
	}

	public void setTerm(String term)
	{
		this.term = term;
	}
}
