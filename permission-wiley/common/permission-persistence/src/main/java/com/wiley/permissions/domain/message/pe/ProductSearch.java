package com.wiley.permissions.domain.message.pe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.wiley.permissions.domain.message.pe.ProductSearchTerm.ProductSearchConnective;
import com.wiley.permissions.domain.message.pe.ProductSearchTerm.ProductSearchField;
import com.wiley.permissions.domain.message.pe.ProductSearchTerm.ProductSearchOperator;

/**
 *
 * @author ttidwell
 */
public class ProductSearch
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@XmlAttribute
	private String dataSource = null;

	@XmlAttribute
	private int maxResults = 0;

	@XmlElement(name = "searchTerm")
	private List<ProductSearchTerm> searchTerms = new ArrayList<ProductSearchTerm>();

	public ProductSearch()
	{
	}

	public void addSearchTerm(ProductSearchField field, ProductSearchOperator operator, ProductSearchConnective connective, String term)
	{
		ProductSearchTerm pst = new ProductSearchTerm();

		pst.setField(field);
		pst.setOperator(operator);
		pst.setConnective(connective);
		pst.setTerm(term);

		searchTerms.add(pst);
	}

	public String getDataSource()
	{
		return dataSource;
	}

	public void setDataSource(String dataSource)
	{
		this.dataSource = dataSource;
	}

	public int getMaxResults()
	{
		return maxResults;
	}

	public void setMaxResults(int maxResults)
	{
		this.maxResults = maxResults;
	}

	public List<ProductSearchTerm> getSearchTerms()
	{
		return searchTerms;
	}

	public void setSearchTerms(List<ProductSearchTerm> searchTerms)
	{
		this.searchTerms = searchTerms;
	}
}
