package com.wiley.permissions.web.shared.controllers.landing;

import org.apache.commons.lang3.math.NumberUtils;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;

/**
 * Contains asset info used to generate the search results for assets search
 * in wizard.
 *
 * @version $Rev: 54929 $ $Date: 2013-11-22 23:22:00 $
 */
public class AssetRenumberView  {

    // ----------------------------------------------------------- Constructors

    /**
     * Default constructor.
     */
    public AssetRenumberView() {

    }

    /**
     * Construct an instance with the supplied property values.
     *
     * @param label The label to be displayed to the user.
     * @param value The value to be returned to the server.
     */
    public AssetRenumberView(AssetUse au) {
      this.setAssetId(au.getAsset().getId());
      this.setAssetUseId(au.getId());
      this.setPosition(au.getPosition());
      this.setAssetDescription(au.getAsset().getDescription());
      if (null != au.getAsset().getMediaType())
    	  this.setMediaType(au.getAsset().getMediaType().getDescription());
      this.setUsage(au.getUsage().getCode());
      this.setComponent(au.getComponentName());
      this.setNewPosition(au.getPosition());
      this.setComponentId(au.getComponent().getId());
    }

    // ------------------------------------------------------------- Properties

    /**
     * The property which supplies the option label visible to the end user.
     */
    private Integer assetId;
    private Integer assetUseId;
    private String mediaType;
    private String component = null;
    private String assetDescription = null;
    private String position = null;
    private String newPosition = null;
    private String usage = null;
    private String prefix = null;
    private String separator = null;
    private String suffix = null;
    private String alphaSuffix = null;
    private Integer componentId;


    public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String value) {
		this.prefix = value;
	}

	public Integer getComponentId() {
		return componentId;
	}

	public void setComponentId(Integer value) {
		this.componentId = value;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String value) {
		this.separator = value;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String value) {
		this.suffix = value;
	}

	public String getAlphaSuffix() {
		return alphaSuffix;
	}

	public void setAlpahSuffix(String value) {
		this.alphaSuffix = value;
	}

    public Integer getAssetId() {
		return assetId;
	}

	public void setAssetId(Integer value) {
		this.assetId = value;
	}

	public Integer getAssetUseId() {
		return assetUseId;
	}

	public void setAssetUseId(Integer value) {
		this.assetUseId = value;
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

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
		if (null == position) return;

		// set up the components that may be renumbered
		 if (NumberUtils.isDigits(position)) {
			 this.prefix = position;
			 this.separator = "";
			 this.suffix = "";
			 this.alphaSuffix = "";
		 } else {
			 int idx1 = position.indexOf(".");
			 int idx2 = position.indexOf("-");
			 if (idx1 > -1) {
				 this.separator = ".";
				 this.prefix = position.substring(0, idx1);
				 this.suffix = position.substring(idx1 + 1 );
				 this.alphaSuffix = "";
			 }

			 if (idx2 > -1) {
				 this.separator = "-";
				 this.prefix = position.substring(0, idx2);
				 this.suffix = position.substring(idx2 + 1 );
				 this.alphaSuffix = "";
			 }

			 if (idx2 < 0 && idx1 < 0) {
				 this.prefix = position;
				 this.separator = "";
				 this.suffix = "";
				 this.alphaSuffix = "";
			 }

			 if (!NumberUtils.isDigits(this.suffix)) {
				 String wk = "";
				 String alphaWk = "";
				 for (int y= 0; y < this.suffix.length(); y ++) {
					 String character = this.suffix.substring(y,y + 1);
					 if(NumberUtils.isDigits(character)) {
						 wk = wk + character;
					 } else {
						 alphaWk = alphaWk + character;
					 }
				 }
				 this.alphaSuffix = alphaWk;
				 this.suffix = wk;
			 }
		 }
	}

	public String getNewPosition() {
		return newPosition;
	}

	public void setNewPosition(String newPosition) {
		this.newPosition = newPosition;
	}

	public String getUsage() {
		return usage;
	}

	public void setUsage(String value) {
		this.usage = value;
	}

}
