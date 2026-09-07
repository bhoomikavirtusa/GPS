package com.wiley.permissions.common.dispatcher;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum OperationType
{
	/**
	 * These are all Operations (used by ServiceDispatcher as well - to not duplicate the list)
	 */
	// Error is an operationType for CMS messages, but the PE
	// messages use the separate messageError section.
	@XmlEnumValue("Error")
	ERROR("Error", "vm://productService.handleErrorMessageOp"),

	@XmlEnumValue("UpdateAsset")
	UPDATE_ASSET("UpdateAsset", "vm://assetService.handleUpdateAssetMessage"),

	@XmlEnumValue("DeleteAsset")
	DELETE_ASSET("DeleteAsset", "vm://assetService.handleDeleteAssetMessage"),

	@XmlEnumValue("GetAsset")
	GET_ASSET("GetAsset", "vm://assetService.handleGetAssetMessage"),

	@XmlEnumValue("UpdateAssetUse")
	UPDATE_ASSET_USE("UpdateAssetUse", "vm://assetUseService.handleUpdateAssetUseMessage"),

	@XmlEnumValue("DeleteAssetUse")
	DELETE_ASSET_USE("DeleteAssetUse", "vm://assetUseService.handleDeleteAssetUseMessage"),

	@XmlEnumValue("GetAssetUse")
	GET_ASSET_USE("GetAssetUse", "vm://assetUseService.handleGetAssetUseMessage"),

	@XmlEnumValue("GetPerformanceStats")
	GET_PERFORMANCE_STATS("GetPerformanceStats", "vm://assetUseService.handleGetPerformanceStats"),

	@XmlEnumValue("UpdateSource")
	UPDATE_SOURCE("UpdateSource", ""),

	@XmlEnumValue("GetSource")
	GET_SOURCE("GetSource", ""),

	CREATE_SOURCE("CreateSource", ""),

	@XmlEnumValue("DeleteSource")
	DELETE_SOURCE("DeleteSource", "vm://sourceService.handleDeleteSource"),


	@XmlEnumValue("UpdateUser")
	UPDATE_USER("UpdateUser", ""),
	
	@XmlEnumValue("GetUser")
	GET_USER("GetUser", ""),

	CREATE_USER("CreateUser", ""),

	@XmlEnumValue("DeleteUser")
	DELETE_USER("DeleteUser", "vm://userService.handleDeleteUser"),

	
	@XmlEnumValue("UpdateComponent")
	UPDATE_COMPONENT("UpdateComponent", "vm://commonWorkService.handleUpdateComponentMessage"),

	@XmlEnumValue("GetComponent")
	GET_COMPONENT("GetComponent", "vm://commonWorkService.handleGetComponentMessage"),

	@XmlEnumValue("DeleteComponent")
	DELETE_COMPONENT("DeleteComponent", "vm://commonWorkService.handleDeleteComponentMessage"),

	@XmlEnumValue("GetComponentList")
	GET_COMPONENT_LIST("GetComponentList", "vm://commonWorkService.handleGetComponentListMessage"),

	@XmlEnumValue("ReportComponentList")
	REPORT_COMPONENT_LIST("ReportComponentList", ""),

	/**
	 * These are Product Engineering operations
	 */
	@XmlEnumValue("productUpdate")
	PRODUCT_UPDATE("productUpdate", ""),

	@XmlEnumValue("masterListUpdate")
	MASTER_LIST_UPDATE("masterListUpdate", ""),

	@XmlEnumValue("allNewProducts")
	ALL_NEW_PRODUCTS("allNewProducts", ""),

	ASSET_USE_COLLECTION_STATUS_VALIDATION ("AssetUseCollectionStatusValidation",
		"vm://permissionStatusValidationService.updatePermissionStatusForAssetUseCollection"),

	SEARCH_PRODUCTS ("SearchProducts", "vm://services.PEProductSearch"),

	GET_LATEST_PRODUCT ("GetLatestProduct", "vm://services.PEProductService"),

	// the LOCAL REST service that returns a product
	GET_LOCAL_LATEST_PRODUCT ("GetLocalLatestProduct", "vm://services.LocalPEProductService"),

	// UPDATE_PRODUCTS and UPDATE_MASTER_LISTS are internal messages
	// produced after receiving PRODUCT_UPDATE or MASTER_LIST_UPDATE from PE
	UPDATE_PRODUCTS ("UpdateProducts", "vm://productService.saveProducts"),

	UPDATE_MASTER_LISTS("UpdateMasterLists", "vm://productService.saveMasterLists"),

	UPDATE_PRODUCT("UpdateProduct", "vm://productService.saveProduct"),

	CACHE_MESSAGE ("CacheMessage", "vm://messageCache.cacheMessage"),

	GET_COMP_COPY ("GetCompCopy", "vm://services.CompCopyService"),

	GET_COMP_AU_COPY ("GetCompCopyAu", "vm://services.CompCopyAuService"),

	// Not being used currently but leave here for now
	GET_RIGHTS_LINK_LICENSE ("GetRightsLinkLicense", "vm://services.RightsLinkLicenseService"),

	UPDATE_COMMON_WORK ("UpdateCommonWork", "vm://productService.updateCommonWork"),

	UPDATE_LOCAL_COMMON_WORK ("UpdateLocalCommonWork", "vm://localProductService.updateCommonWorkFromPE");

	private final String code;
	private final String endpoint;

	private OperationType(String code, String endpoint) {
		this.code = code;
		this.endpoint = endpoint;
	}

	public String getCode() {
		return code;
	}

	public String getEndpoint() {
		return endpoint;
	}
}
