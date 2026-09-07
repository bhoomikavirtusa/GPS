package com.wiley.permissions.domain.message.pe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 *
 * @author ttidwell
 */
public class ProductSearchResults
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@XmlEnum
	public enum RecordSetCompletion
	{
		@XmlEnumValue("yes")
		YES,
		@XmlEnumValue("no")
		NO
	}

	@XmlAttribute
	private RecordSetCompletion completRecordSet = null;
	@XmlAttribute
	private int resultCount = 0;

	@XmlElement(name="product")
	private List<ProductSearchResult> results = new ArrayList<ProductSearchResult>();

	@XmlElement(name="productSearchError")
	private ProductSearchError error = null;

	public ProductSearchResults()
	{

	}

	public RecordSetCompletion getCompletRecordSet()
	{
		return completRecordSet;
	}

	public void setCompletRecordSet(RecordSetCompletion completRecordSet)
	{
		this.completRecordSet = completRecordSet;
	}

	public int getResultCount()
	{
		return resultCount;
	}

	public void setResultCount(int resultCount)
	{
		this.resultCount = resultCount;
	}

	public List<ProductSearchResult> getResults()
	{
		return results;
	}

	public void setResults(List<ProductSearchResult> results)
	{
		this.results = results;
	}

	public ProductSearchError getError()
	{
		return error;
	}

	public void setError(ProductSearchError error)
	{
		this.error = error;
	}

	@Override
	public String toString() {
		if (error != null) {
			return error.toString();
		}

		if (results == null) {
			return "(results == null)";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("resultCount = " + resultCount);
		sb.append("\n");

		for (ProductSearchResult result : results) {
			sb.append(result);
			sb.append("\n");
		}
		return sb.toString();
	}
}
