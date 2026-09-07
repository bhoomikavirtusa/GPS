package com.wiley.permissions.domain.message.pe;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author ttidwell
 */
public class ProductSearchReply
implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	@XmlElement(name="productSearchResults")
	private ProductSearchResults productSearchResults = null;

	public ProductSearchReply()
	{

	}

	public ProductSearchResults getProductSearchResults()
	{
		return productSearchResults;
	}

	public void setProductSearchResults(ProductSearchResults productSearchResults)
	{
		this.productSearchResults = productSearchResults;
	}
}
