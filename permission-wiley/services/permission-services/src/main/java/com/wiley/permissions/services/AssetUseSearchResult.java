package com.wiley.permissions.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.springframework.util.CollectionUtils;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Source;

/**
 * Wrapper around Document that provides JSTL (and easier Java) access.
 *
 * @author smarkoff
 */
public class AssetUseSearchResult {

    private final Document document;
    private final boolean assetUseForCurrentCW;
    private Boolean overrideCanceled = null;


    public AssetUseSearchResult(Document document, boolean assetUseForCurrentCW) {
    	this.document = document;
    	this.assetUseForCurrentCW = assetUseForCurrentCW;
    }

    public boolean isAssetUseForCurrentCW() {
    	return assetUseForCurrentCW;
    }

    // ---------- common work stuff -----------

    public int getCommonWorkId() {
    	String s = document.get(AssetUseIndexService.COMMON_WORK_ID);
    	// should always exist for this field (not null)
    	return Integer.parseInt(s);
    }

    // don't think this is being used, apart from assetIndex.jspx
    public CommonWorkStatus getInteriorCWStatus() {
    	String code = document.get(AssetUseIndexService.INTERIOR_CW_STATUS);
    	// normally we don't expect null (not to find in index) but check anyway
    	if (code == null)  return null;
    	else return CommonWorkStatus.getCommonWorkStatusForCode(code);
    }

    // ---------- product stuff ----------

    public String [] getProductIds() {
    	return document.getValues(AssetUseIndexService.PRODUCT_ID);
    }

    public String getAuthorNames() {
    	String [] names = document.getValues(AssetUseIndexService.AUTHOR_NAME);
    	return StringUtils.join(names, "; ");
    }

    public String getTitle() {
    	return document.get(AssetUseIndexService.TITLE);
    }

    public String getIsbn() {
    	return document.get(AssetUseIndexService.ISBN13);//updated for DM-280
    }

    public String getCopyrightYear() {
    	return document.get(AssetUseIndexService.COPYRIGHT_YEAR);
    }

    // ---------- asset stuff ---------

    public int getAssetId() {
    	String s = document.get(AssetUseIndexService.ASSET_ID);
    	// should always exist for this field (not null)
    	return Integer.parseInt(s);
    }

    public String getSourceNames() {
    	String [] names = document.getValues(AssetUseIndexService.SOURCE_NAME);
    	return StringUtils.join(names, ", ");
    }

    public boolean isCameraCopyToCome() {
    	String s = document.get(AssetUseIndexService.CAMERA_COPY_TO_COME);
    	return "true".equalsIgnoreCase(s);
    }

    /**
     * Returns a list of source ids.
     * If there are no sources then a 0-length list will be returned.
     */
    public List<Integer> getSourceIds() {
    	String [] values = document.getValues(AssetUseIndexService.SOURCE_ID);
    	List<Integer> list = new ArrayList<Integer>(values.length);
    	for (String value : values) {
    		list.add(Integer.parseInt(value));
    	}
    	return list;
    }

    /**
     * Return List of Source but with only the id, disabled (nofly), and name
     * attributes set.
     * If there are no sources then 0-length list will be returned.
     */
    public ArrayList<Source> getSources() {
    	String [] values = document.getValues(AssetUseIndexService.SOURCE_ID_NOFLY_NAME);
    	ArrayList<Source> list = new ArrayList<Source>(values.length);
    	for (String value : values) {
    		// format we expect: id:nofly:name
    		int index = value.indexOf(':');
    		int id = Integer.parseInt(value.substring(0, index));
    		value = value.substring(index + 1);
    		index = value.indexOf(':');
    		boolean nofly = Boolean.parseBoolean(value.substring(0, index));
    		value = value.substring(index + 1);
    		Source source = new Source();
    		source.setId(id);
    		source.setNofly(nofly);
    		source.setName(value);
    		list.add(source);
    	}
    	return list;
    }

    public String getSourcesAsHTMLLinks(String contextPath) {
    	List<Source> sources = getSources();

    	if (CollectionUtils.isEmpty(sources))  return "";

		StringBuilder sb = new StringBuilder();
	    boolean gotOne = false;

		for (Source source : sources) {
			if (gotOne) sb.append(", ");

			sb.append("<a href=\"" + contextPath + "/sapp/sources/viewSource/form?sourceId=" + source.getId() + "\">");

			if (source.isNofly())
				sb.append("<span class='disabled'>" + StringEscapeUtils.escapeHtml4(source.getName()) + "</span>");
			else {
				sb.append(StringEscapeUtils.escapeHtml4(source.getName()));
			}

			sb.append("</a>");

			gotOne = true;
		}

		return sb.toString();
    }

    public String getSourcesAsHTML() {
    	return Asset.sourcesAsHTML(getSources());
    }

    public String getMediaType() {
    	return document.get(AssetUseIndexService.MEDIA_TYPE_DISPLAY);
    }

    public String getDescription() {
    	return document.get(AssetUseIndexService.DESCRIPTION);
    }

    public String getSortOrder() {
    	return document.get(AssetUseIndexService.SORT_ORDER);
    }

    public String getWorkForHire() {
    	return document.get(AssetUseIndexService.WORK_FOR_HIRE);
    }

    public String getOwnerType() {
    	return document.get(AssetUseIndexService.OWNER_TYPE_DESCRIPTION);
    }

    public String getOwnerTypeCode() {
    	return document.get(AssetUseIndexService.OWNER_TYPE_CODE);
    }

    /**
     * Note at least for now we have SourceRef (vendor id) on the asset table
     * so only one is allowed even if the Asset has multiple sources.
     */
    public String getSourceRef() {
    	return document.get(AssetUseIndexService.SOURCE_REF_DISPLAY);
    }

    public String getArtist() {
    	return document.get(AssetUseIndexService.ARTIST);
    }

    public boolean isRestrictedUse() {
    	String s = document.get(AssetUseIndexService.RESTRICTED_USE);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isActive() {
    	String s = document.get(AssetUseIndexService.ACTIVE);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isSourceNofly() {
    	String s = document.get(AssetUseIndexService.SOURCE_NOFLY);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isHasAssetFiles() {
    	String s = document.get(AssetUseIndexService.HAS_ASSET_FILES);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isReviewedOrAuthorUnknown() {
    	String s = document.get(AssetUseIndexService.REVIEWED_OR_AUTHOR_UNKNOWN);
    	return "true".equalsIgnoreCase(s);
    }


    // ---------- assetUse stuff ----------

    public int getAssetUseId() {
    	String s = document.get(AssetUseIndexService.ASSET_USE_ID);
    	// should always exist for this field (not null)
    	return Integer.parseInt(s);
    }

    public String getCaption() {
    	return document.get(AssetUseIndexService.CAPTION);
    }

    public String getPagePositionCode() {
    	return document.get(AssetUseIndexService.PAGE_POSITION_CODE);
    }

    public String getPagePositionDescription() {
    	return document.get(AssetUseIndexService.PAGE_POSITION_DISPLAY);
    }

    public String getPermissionComment() {
    	return document.get(AssetUseIndexService.PERMISSION_COMMENT);
    }

    public String getProductionComment() {
    	return document.get(AssetUseIndexService.PRODUCTION_COMMENT);
    }

    public Boolean getHasComments() {
    	return StringUtils.isNotBlank(getPermissionComment()) || StringUtils.isNotBlank(getProductionComment());
    }

    public boolean isRoyaltyFree() {
    	String s = document.get(AssetUseIndexService.ROYALTY_FREE);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isRestrictedUseApproved() {
    	String s = document.get(AssetUseIndexService.RESTRICTED_USE_APPROVED);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isPrintRunWarning() {
    	String s = document.get(AssetUseIndexService.PRINT_RUN_WARNING);
    	return "true".equalsIgnoreCase(s);
    }

    public String getUsage() {
    	return document.get(AssetUseIndexService.USAGE_DISPLAY);
    }

    public PermissionStatus getPermissionStatus() {
    	String code = document.get(AssetUseIndexService.PERMISSION_STATUS_CODE_NOT_NORM);
    	return code == null ? null : PermissionStatus.getPermissionStatusForCode(code);
    }

    /**
     * The only reason to call this method instead of getPermissionStatus().getCode()
     * is to avoid a NullPointerException if the status is null.
     */
    public String getPermissionStatusCode() {
    	return document.get(AssetUseIndexService.PERMISSION_STATUS_CODE_NOT_NORM);
    }

    /**
     * The only reason to call this method instead of getPermissionStatus().getDescription()
     * is to avoid a NullPointerException if the status is null.
     */
    public String getPermissionStatusDescription() {
    	PermissionStatus status = getPermissionStatus();
    	return status == null ? null : status.getDescription();
    }

    public Integer getReplacementId() {
    	String s = document.get(AssetUseIndexService.REPLACEMENT_ID);
    	return (s == null) ? null : new Integer(s);
    }

    public Integer getCreatedGroupId() {
    	String s = document.get(AssetUseIndexService.CREATED_GROUP_ID);
    	return (s == null) ? null : new Integer(s);
    }

    public boolean isCanceled() {
    	if (overrideCanceled != null) {
    		return overrideCanceled;
    	}

    	String s = document.get(AssetUseIndexService.CANCELED);
    	return s.equalsIgnoreCase("true");
    }

    public void setOverrideCanceled(Boolean b) {
    	overrideCanceled = b;
    }

    public String getCreditLine() {
    	return document.get(AssetUseIndexService.CREDIT_LINE);
    }

    public String getPosition() {
    	return document.get(AssetUseIndexService.POSITION_DISPLAY);
    }

    public Integer getComponentId() {
    	String s = document.get(AssetUseIndexService.COMPONENT_ID);
    	return (s == null) ? null : new Integer(s);
    }

    public String getComponentName() {
    	// SORT stored like DISPLAY in this case
    	return document.get(AssetUseIndexService.COMPONENT_NAME_SORT);
    }

    public String getManuscriptPage() {
    	return document.get(AssetUseIndexService.MANUSCRIPT_PAGE);
    }

    public String getFinalPage() {
    	return document.get(AssetUseIndexService.FINAL_PAGE);
    }

    public boolean isReuse() {
    	String s = document.get(AssetUseIndexService.REUSE);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isPickup() {
    	String s = document.get(AssetUseIndexService.PICKUP);
    	return "true".equalsIgnoreCase(s);
    }
    
    public boolean isMediaManager() {
    	String s = document.get(AssetUseIndexService.MEDIAMANAGER);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isNeedPaymentRequest() {
    	String s = document.get(AssetUseIndexService.NEED_PAYMENT_REQUEST);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isPaid() {
    	String s = document.get(AssetUseIndexService.PAID);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isCustom() {
    	String s = document.get(AssetUseIndexService.CUSTOM);
    	return "true".equalsIgnoreCase(s);
    }

    public boolean isSentToProduction() {
    	String s = document.get(AssetUseIndexService.SENT_TO_PRODUCTION);
    	return "true".equalsIgnoreCase(s);
    }

    public Date getLastUpdatedDate() {
    	String s = document.get(AssetUseIndexService.LAST_UPDATED_DATE);
    	return new Date(Long.parseLong(s));
    }

    public boolean isNeedToConfirmCancels() {
    	String s = document.get(AssetUseIndexService.NEED_TO_CONFIRM_CANCELS);
    	return "true".equalsIgnoreCase(s);
    }

    public Date getIndexDate() {
    	String s = document.get(AssetUseIndexService.INDEX_DATE);
		return new Date(Long.parseLong(s));
    }

    /**
     * hasAssetUseFiles || latestContractHasFile (see AssetUseIndexService)
     */
    public boolean isHasFiles() {
    	String s = document.get(AssetUseIndexService.HAS_FILES);
    	return "true".equalsIgnoreCase(s);
    }

    /**
     * returns true if there is at least one contract
     * @return boolean
     */
    public boolean isHasContracts() {
    	String [] contractIds = document.getValues(AssetUseIndexService.LATEST_CONTRACT_ID);
    	return (contractIds.length != 0);
    }

    /**
     * Returns a list of latest contract ids.
     * The size of this list should be no greater than the number of sources
     * for the asset (and may be less).
     * If there are no contracts then a 0-length list will be returned.
     */
    public List<Integer> getLatestContractIds() {
    	String [] values = document.getValues(AssetUseIndexService.LATEST_CONTRACT_ID);
    	List<Integer> list = new ArrayList<Integer>(values.length);
    	for (String value : values) {
    		list.add(Integer.parseInt(value));
    	}
    	return list;
    }

    public double getEstimatedCost() {
    	String s = document.get(AssetUseIndexService.ESTIMATED_COST);
    	return Double.parseDouble(s);
    }

    public Integer getGbpmCategory() {
    	String s = document.get(AssetUseIndexService.GBPM_CATEGORY);
    	return (s == null) ? null : new Integer(s);
    }

    public ImportSource getImportSource() {
    	String s = document.get(AssetUseIndexService.IMPORT_SOURCE_CODE);
    	return (s == null) ? null : ImportSource.getImportSourceForCode(new Integer(s));
    }

	/**
	 * Analogous to AssetUse.exclude().
	 */
	public boolean exclude() {
		if (PermissionStatus.isComplete(getPermissionStatus())) {
			return true;
		}
		// if no source
		if (StringUtils.isBlank(getSourceNames())) {
			return true;
		}

		return false;
	}

	/**
	 * Analogous to AssetUse.isGranted().
	 */
	public boolean isGranted() {
		return PermissionStatus.isGranted(getPermissionStatus());
	}

	public boolean isComplete() {
		return PermissionStatus.isComplete(getPermissionStatus());
	}

	public boolean isNoFlyMatchApproved() {
    	String s = document.get(AssetUseIndexService.NOFLY_MATCH_APPROVED);
    	return "true".equalsIgnoreCase(s);
    }
}
