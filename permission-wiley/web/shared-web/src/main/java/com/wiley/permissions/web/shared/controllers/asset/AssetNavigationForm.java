package com.wiley.permissions.web.shared.controllers.asset;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;

public class AssetNavigationForm {
	private static final Log log = LogFactory.getLog(AssetNavigationForm.class);
	
	private AssetUse assetUse = new AssetUse();
	private List<String> auIds = new ArrayList<String>();
	private int index = 0;

	public AssetUse getAssetUse()
	{
		return assetUse;
	}

	public void setAssetUse(AssetUse assetUse)
	{
		this.assetUse = assetUse;
	}

	public void setAUIds(String[] aIds)
	{
		// do not add if is 0
		if (1 == aIds.length && aIds[0].equals("0"))
			return;
		CollectionUtils.addAll(auIds, aIds);
		index = 0;
	}

	public void addAuid(String auId)
	{
		log.debug ("Before " +  auIds);
		if (auIds.contains(auId)) {
			log.debug ("Already contains id...");
			return;
		}
		auIds.add(auId);
		log.debug ("After " + auIds);		
	}

	public String getNextId()
	{
		index++;
		if (index == auIds.size())
			index = auIds.size() - 1;
		return auIds.get(index);
	}

	public String getPrevId()
	{
		--index;
		if (index < 0)
			index = 0;
		return auIds.get(index);
	}

	public String getId()
	{
		if (auIds.size() <= index) {
			return "0";
		}
		return auIds.get(index);
	}

	public boolean isLast(String id)
	{
		log.debug ("Is last " +  auIds + " " + id);
		if (id.equals("0"))
			return true;
		int n = auIds.indexOf(id);
		if (-1 != n && n == auIds.size() - 1) {
			log.debug ("Is last : yes");			
			return true;
		}
		log.debug ("Is last : no");
		return false;
	}

	public boolean isFirst(String id)
	{
		log.debug ("Is first " +  auIds + " " + id);
		if (id.equals("0") && auIds.size() == 0)
			return true;
		int n = auIds.indexOf(id);
		if (n == 0) {
			log.debug ("Is first : yes");
			return true;
		}
		log.debug ("Is first : no");
		return false;
	}
}
