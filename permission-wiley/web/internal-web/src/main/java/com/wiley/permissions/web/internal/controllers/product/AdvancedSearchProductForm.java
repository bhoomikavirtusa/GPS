package com.wiley.permissions.web.internal.controllers.product;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

public class AdvancedSearchProductForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	private String searchString;
	private String searchField;  // "author", "title", "isbn", or "all"

	private String title;
	private String author;
	private String edition;
	private String productLine;
	private String editorCode;
	private String copyrightYear;
	private String pubStatus;

	private Boolean titleCheck = false;
	private Boolean authorCheck = false;
	private Boolean editionCheck = false;
	private Boolean productLineCheck = false;
	private Boolean editorCodeCheck = false;
	private Boolean copyrightYearCheck = false;
	private Boolean pubStatusCheck = false;


	private List<ProductSearchResult> results = null;
	private String selectedProduct = null;
	private String selectedProductDataSource = null;
	private String commonWork = null;
	private boolean afterFirstHit = false;  // not currently used

	// can specify a view name where the redirect will take us
	// after the product is selected
	private String target = null;

	public AdvancedSearchProductForm() {
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

	public boolean getAfterFirstHit()
	{
		return afterFirstHit;
	}

	public void setAfterFirstHit(boolean afterFirstHit)
	{
		this.afterFirstHit = afterFirstHit;
	}

	public String getCommonWork()
	{
		return commonWork;
	}

	public void setCommonWork(String commonWork)
	{
		this.commonWork = commonWork;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = StringUtils.stripToEmpty(title);
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = StringUtils.stripToEmpty(author);
	}

	public String getEdition() {
		return edition;
	}

	public void setEdition(String edition) {
		this.edition = StringUtils.stripToEmpty(edition);
	}

	public String getProductLine() {
		return productLine;
	}

	public void setProductLine(String productLine) {
		this.productLine = StringUtils.stripToEmpty(productLine);
	}

	public String getEditorCode() {
		return editorCode;
	}

	public void setEditorCode(String editorCode) {
		this.editorCode = StringUtils.stripToEmpty(editorCode);
	}

	public String getCopyrightYear() {
		return copyrightYear;
	}

	public void setCopyrightYear(String copyrightYear) {
		this.copyrightYear = StringUtils.stripToEmpty(copyrightYear);
	}

	public String getPubStatus() {
		return pubStatus;
	}

	public void setPubStatus(String pubStatus) {
		this.pubStatus = StringUtils.stripToEmpty(pubStatus);
	}

	public Boolean getTitleCheck() {
		return titleCheck;
	}

	public void setTitleCheck(Boolean titleCheck) {
		this.titleCheck = titleCheck;
	}

	public Boolean getAuthorCheck() {
		return authorCheck;
	}

	public void setAuthorCheck(Boolean authorCheck) {
		this.authorCheck = authorCheck;
	}

	public Boolean getEditionCheck() {
		return editionCheck;
	}

	public void setEditionCheck(Boolean editionCheck) {
		this.editionCheck = editionCheck;
	}

	public Boolean getProductLineCheck() {
		return productLineCheck;
	}

	public void setProductLineCheck(Boolean productLineCheck) {
		this.productLineCheck = productLineCheck;
	}

	public Boolean getEditorCodeCheck() {
		return editorCodeCheck;
	}

	public void setEditorCodeCheck(Boolean editorCodeCheck) {
		this.editorCodeCheck = editorCodeCheck;
	}

	public Boolean getCopyrightYearCheck() {
		return copyrightYearCheck;
	}

	public void setCopyrightYearCheck(Boolean copyrightYearCheck) {
		this.copyrightYearCheck = copyrightYearCheck;
	}

	public Boolean getPubStatusCheck() {
		return pubStatusCheck;
	}

	public void setPubStatusCheck(Boolean pubStatusCheck) {
		this.pubStatusCheck = pubStatusCheck;
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
