package com.wiley.permissions.domain.persistence.permissions;

import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.StringUtil;

/**
 * All fields of this class are immutable.
 * explanation should only be looked at if !match.
 *
 * @author smarkoff
 */
public class ConditionMatchResult {
	private final boolean match;
	private final String explanation;

	/**
	 * Only use this constructor when match is true.
	 */
	public ConditionMatchResult() {
		this.match = true;
		this.explanation = null;
	}

	/**
	 * Only use this constructor when match is false.
	 * @param explanation  Must be non-blank
	 */
	public ConditionMatchResult(String explanation) {
		ArgUtil.notBlank(explanation, "explanation");
		this.match = false;
		// 1000 matches the column size of status_explanation on the asset_use,
		// au_source_perm_status, and asset_perm_ref tables
		this.explanation = StringUtil.truncate(explanation, 1000, true);
	}

	public boolean isMatch() {
		return match;
	}

	public String getExplanation() {
		return explanation;
	}

	@Override
	public String toString() {
		return "match = " + match
			+ "\nexplanation = " + explanation;
	}
}
