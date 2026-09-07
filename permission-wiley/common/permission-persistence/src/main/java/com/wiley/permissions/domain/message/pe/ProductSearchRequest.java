package com.wiley.permissions.domain.message.pe;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author ttidwell
 */
public class ProductSearchRequest
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@XmlElement(name = "productSearch")
	private ProductSearch productSearch = new ProductSearch();

	public ProductSearchRequest()
	{

	}

	public ProductSearch getProductSearch()
	{
		return productSearch;
	}

	public void setProductSearch(ProductSearch productSearch)
	{
		this.productSearch = productSearch;
	}
}
