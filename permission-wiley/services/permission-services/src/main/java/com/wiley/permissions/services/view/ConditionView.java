package com.wiley.permissions.services.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to display the condition list in POView and ContractView (POService.getPODefaultConditions())
 * and for CW conditions.
 *
 * @author lnagy
 */
public class ConditionView {

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(ConditionView.class);

	private String description;
	private String dataType;
	private boolean isSingle;
	private String enumValue;
	private String enumDescription;
	private String conditionValue;
	private String conditionType;
	private boolean canUse = true;  // will sometimes be false for CW conditions
	private boolean isExclusion = false;
	private boolean isDefault;
	private int assetBaseId = 0;

	public ConditionView()
	{
	}

	public ConditionView(String conditionValue, String conditionType)
	{
		super();
		this.conditionValue = conditionValue;
		this.conditionType = conditionType;
	}

	public String getConditionValue()
	{
		return conditionValue;
	}

	public void setConditionValue(String conditionValue)
	{
		this.conditionValue = conditionValue;
	}

	public String getConditionType()
	{
		return conditionType;
	}

	public void setConditionType(String conditionType)
	{
		this.conditionType = conditionType;
	}

	public void setCanUse(boolean canUse) {
		this.canUse = canUse;
	}

	public boolean isCanUse() {
		return canUse;
	}

	public boolean isExclusion()
	{
		return isExclusion;
	}

	public void setExclusion(boolean isExclusion)
	{
		this.isExclusion = isExclusion;
	}

	public int getAssetBaseId()
	{
		return assetBaseId;
	}

	public void setAssetBaseId(int assetBaseId)
	{
		this.assetBaseId = assetBaseId;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getDataType()
	{
		return dataType;
	}

	public void setDataType(String dataType)
	{
		this.dataType = dataType;
	}

	public boolean isSingle()
	{
		return isSingle;
	}

	public void setSingle(boolean isSingle)
	{
		this.isSingle = isSingle;
	}

	public String getEnumValue()
	{
		return enumValue;
	}

	public void setEnumValue(String enumValue)
	{
		this.enumValue = enumValue;
	}

	public String getEnumDescription()
	{
		return enumDescription;
	}

	public void setEnumDescription(String enumDescription)
	{
		this.enumDescription = enumDescription;
	}

	public boolean isDefault()
	{
		return isDefault;
	}

	public void setDefault(boolean isDefault)
	{
		this.isDefault = isDefault;
	}

	@Override
	public String toString() {
		return "description = " + description
			+ ", dataType = " + dataType
			+ ", enumValue = " + enumValue
			+ ", isDefault = " + isDefault
			+ ", conditionValue = " + conditionValue;
	}

	public static class ConditionCategoryView {

		private String code;
		private String description;
		private String displayValues;
		private boolean isExclusion;
		private boolean isSingle;
		private boolean canUse;

		public ConditionCategoryView(ConditionView c)
		{
			code = c.getConditionType();
			description = c.getDescription();
			isExclusion = c.isExclusion();
			isSingle = c.isSingle();
			canUse = c.isCanUse();

			add(c);
		}

		public String getCode()
		{
			return code;
		}

		public void setCode(String code)
		{
			this.code = code;
		}

		public String getDescription()
		{
			return description;
		}

		public void setDescription(String description)
		{
			this.description = description;
		}

		public String getDisplayValues()
		{
			if (isExclusion)
				return "All except: " + displayValues;
			return displayValues;
		}

		public void setDisplayValues(String displayValues)
		{
			this.displayValues = displayValues;
		}

		public void add(ConditionView c) {
			// if po default then we already have the value from the first record
			if (!c.isDefault() || StringUtils.isBlank(displayValues)) {
				if (c.isDefault()) {
					addDisplayValue(c.getConditionValue());
				}
				else {
					if (c.getConditionValue() != null) {
						addDisplayValue(c.getEnumDescription() == null ? c.getConditionValue() : c.getEnumDescription());
					}
				}
			}

			// If first record had no value then exclusion may not have been set properly
			if (c.isExclusion()) {
				isExclusion = true;
			}
		}

		private void addDisplayValue(String value)
		{
			if (StringUtils.isNotBlank (displayValues))
				setDisplayValues (displayValues + ", " + value);
			else
				setDisplayValues (value);
		}

		public boolean isSingle()
		{
			return isSingle;
		}

		public void setSingle(boolean isSingle)
		{
			this.isSingle = isSingle;
		}

		public boolean isCanUse()
		{
			return canUse;
		}

		public void setCanUse(boolean b)
		{
			this.canUse = b;
		}

		public boolean isExclusion()
		{
			return isExclusion;
		}

		@Override
		public String toString() {
			return "code = " + code + ", description = " + description
				+ ", displayValues = " + displayValues + ", isSingle = "
				+ isSingle + ", isExclusion = " + isExclusion;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((code == null) ? 0 : code.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ConditionCategoryView other = (ConditionCategoryView) obj;
			if (code == null)
			{
				if (other.code != null)
					return false;
			}
			else if (!code.equals(other.code))
				return false;
			return true;
		}
	}

	public static Map<ConditionCategoryView, List<ConditionView>> getDisplayConditionsMap (List<ConditionView> conditionViews)
	{
		//log.debug("ConditionsViews: " + StringUtil.collectionToString(conditionViews, "\r\n------------\r\n"));

		Map<ConditionCategoryView, List<ConditionView>> displayConditions =
			new HashMap<ConditionCategoryView, List<ConditionView>>();

		for (ConditionView c : conditionViews) {
			ConditionCategoryView cc = ConditionView.getDisplayConditionsMapKey (displayConditions, c);
			if (null != cc) {
				List<ConditionView> conditions = displayConditions.get (cc);
				conditions.add(c);

				if (StringUtils.isNotBlank (c.getConditionValue())) {
					cc.add(c);
				}
			} else {
				List<ConditionView> conditions = new ArrayList<ConditionView> ();
				conditions.add (c);
				displayConditions.put(new ConditionCategoryView (c), conditions);
			}
		}
		return displayConditions;
	}


	private static ConditionCategoryView getDisplayConditionsMapKey(Map<ConditionCategoryView, List<ConditionView>> displayConditions, ConditionView c)
	{
		Set<ConditionCategoryView> keys = displayConditions.keySet();
		for (ConditionCategoryView key : keys) {
			if (key.equals (new ConditionCategoryView(c)))
				return key;
		}
		return null;
	}
}
