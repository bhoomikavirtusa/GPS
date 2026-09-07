package com.wiley.permissions.web.shared.controllers.landing;

import com.wiley.permissions.services.AssetUseSearchResult;

/**
 * contains asset info used to generate the search results for assets search
 * in wizard
 *
 * @version $Rev: 54929 $ $Date: 2013-11-06 23:40:38 $
 */
public class AssetSearchView  {

    // ----------------------------------------------------------- Constructors

    /**
     * Default constructor.
     */
    public AssetSearchView() {
    	setMediaType("Photo");
    }

    /**
     * Construct an instance with the supplied property values.
     *
     * @param label The label to be displayed to the user.
     * @param value The value to be returned to the server.
     */
    public AssetSearchView(AssetUseSearchResult result) {
      this.setAssetId(String.valueOf(result.getAssetId()));
      this.setPagePosition(result.getPosition());
      this.setAssetDescription(result.getDescription());
      this.setMediaType(result.getMediaType());
      this.setUsage(result.getUsage());
      this.setComponent(result.getComponentName());
    }

    // ------------------------------------------------------------- Properties


    /**
     * The property which supplies the option label visible to the end user.
     */
    private String assetId;  // TODO: maybe change to int
    private String mediaType;
    private String component = null;
    private String assetDescription = null;
    private String pagePosition = null;
    private String usage = null;

    public String getAssetId() {
		return assetId;
	}

	public void setAssetId(String value) {
		this.assetId = value;
	}

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String value) {
		this.mediaType = value;
	}

    public String getComponent() {
        return this.component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getAssetDescription() {
        return this.assetDescription;
    }

    public void setAssetDescription(String description) {
        this.assetDescription = description;
    }

	public String getPagePosition() {
		return pagePosition;
	}

	public void setPagePosition(String pagePosition) {
		this.pagePosition = pagePosition;
	}

	public String getUsage() {
		return usage;
	}

	public void setUsage(String value) {
		this.usage = value;
	}
}
