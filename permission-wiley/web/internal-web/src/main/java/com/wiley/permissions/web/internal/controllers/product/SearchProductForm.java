package com.wiley.permissions.web.internal.controllers.product;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

public class SearchProductForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	private String searchString;
	private String searchField;  // "author", "title", "isbn", or "all"

	private List<ProductSearchResult> results = null;
	private String selectedProduct = null;
	private String selectedProductDataSource = null;
	private String commonWork = null;
	private boolean afterFirstHit = false;  // not currently used

	// can specify a view name where the redirect will take us
	// after the product is selected
	private String target = null;

	public SearchProductForm() {
		super();
	}

	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
		this.searchString = StringUtils.stripToEmpty(searchString);
	}

	public String getSearchField() {
		return searchField;
	}

	public void setSearchField(String searchField) {
		this.searchField = searchField;
	}

	public List<ProductSearchResult> getResults() {
		return results;
	}

	public void setResults(List<ProductSearchResult> results) {
		this.results = results;
	}

	public String getSelectedProduct() {
		return selectedProduct;
	}

	public void setSelectedProduct(String selectedProduct) {
		this.selectedProduct = selectedProduct;
	}

	public String getSelectedProductDataSource() {
		return selectedProductDataSource;
	}

	public void setSelectedProductDataSource(String selectedProductDataSource) {
		this.selectedProductDataSource = selectedProductDataSource;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String targetView) {
		this.target = targetView;
	}

	public boolean getAfterFirstHit() {
		return afterFirstHit;
	}

	public void setAfterFirstHit(boolean afterFirstHit) {
		this.afterFirstHit = afterFirstHit;
	}

	public String getCommonWork() {
		return commonWork;
	}

	public void setCommonWork(String commonWork) {
		this.commonWork = commonWork;
	}

	@Override
	public String toString() {
		return super.toString()
		    + ", target = " + target
		    + ", searchField = " + searchField
		    + ", searchString = " + searchString
		    + ", (other fields not shown)";
	}
}
