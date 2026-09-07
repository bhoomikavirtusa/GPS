package com.wiley.permissions.web.internal.controllers.myaccount;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Condition;
import com.wiley.permissions.domain.persistence.permissions.ConditionType;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;
import com.wiley.sf.common.lang.StringUtil;

/**
 * This class can/should be deleted once "classic" permissions is gone.
 * It is obsolete (references old condition structures).
 *
 * @version $Id: ConditionForm.java,v 1.5 2013-02-20 01:21:10 smarkoff Exp $
 * @author smarkoff
 */
public class ConditionForm
extends BaseFormBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ConditionForm.class);

	private static final long serialVersionUID = 1L;

	private List<Condition> conditions = null;
	private ConditionType conditionType = null;
	private String[] value = null;
	private boolean exclusion = false;
	private String deleteConditionId = null;

	public ConditionForm() {
		throw new RuntimeException("This class should not be used anymore.");
	}

	public ConditionForm(
			List<Condition> conditions,
			ConditionType conditionType,
			String[] values,
			boolean exclusion) {
		throw new RuntimeException("This class should not be used anymore.");

		//this.conditions = conditions;
		//this.conditionType = conditionType;
		//this.value = values;
		//this.exclusion = exclusion;
	}

	public List<Condition> getConditions() {
		return conditions;
	}

	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}

	public ConditionType getConditionType() {
		return conditionType;
	}

	public void setConditionType(ConditionType conditionType) {
		this.conditionType = conditionType;
	}

	public boolean isExclusion() {
		return exclusion;
	}

	public void setExclusion(boolean exclusion) {
		this.exclusion = exclusion;
	}

	public String getDeleteConditionId() {
		return deleteConditionId;
	}

	public void setDeleteConditionId(String deleteConditionId) {
		this.deleteConditionId = deleteConditionId;
	}

	@Override
	public String toString() {
		// use getters due to the way JPA works
		return new ToStringBuilder(this)
			.append("exclusion", isExclusion())
			.append("deleteConditionId", getDeleteConditionId())
			.append("conditionValue", StringUtil.arrayToString(value, ", "))
			.append("conditiontype", getConditionType())
			.toString();
	}

	public String[] getValue() {
		return value;
	}

	public void setValue(String[] value) {
		this.value = value;
		/* (smarkoff: experimental)
		if (value.length > 1) {
			ArrayList<String> list = new ArrayList<String>(value.length);
			for (String v : value) {
				if (StringUtils.isNotBlank(v)) list.add(v);
			}
			if (value.length > list.size()) {
				log.debug("setValue(): shortened list from " + value.length + " to " + list.size());
				this.value = (String[]) CollectionUtils.toArrayOfComponentType(list, String.class);
			}
		}
		*/
	}

	public Condition getCondition() {
		Condition condition = new Condition();
		condition.setType(getConditionType());
		condition.setValue("n/a - this class is obsolete");
		//condition.setValues(getValue());
		//condition.setExclusion(isExclusion());

		return condition;
	}
}
