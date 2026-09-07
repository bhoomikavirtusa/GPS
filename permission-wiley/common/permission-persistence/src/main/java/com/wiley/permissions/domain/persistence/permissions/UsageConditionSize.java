package com.wiley.permissions.domain.persistence.permissions;

import com.wiley.sf.common.lang.ArgUtil;

public class UsageConditionSize implements Comparable<UsageConditionSize> {

	private final EnumUsageCondition usageCondition;
	private final Size size;

	/**
	 * @param usage  May be null
	 * @size  May NOT be null
	 */
	public UsageConditionSize(EnumUsageCondition usage, Size size) {
		ArgUtil.notNull(size, "size");
		this.usageCondition = usage;
		this.size = size;
	}

	public EnumUsageCondition getUsageCondition() { return usageCondition; }
	public Size getSize() { return size; }

	@Override
	public String toString() {
		String usageConditionCode = usageCondition == null ? null : usageCondition.getCode();
		return "usage: " + usageConditionCode + ", size: " + size.getCode();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((size == null) ? 0 : size.hashCode());
		result = prime * result
				+ ((usageCondition == null) ? 0 : usageCondition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof UsageConditionSize))
			return false;
		UsageConditionSize other = (UsageConditionSize) obj;
		if (size == null) {
			if (other.size != null)
				return false;
		} else if (!size.equals(other.size))
			return false;
		if (usageCondition == null) {
			if (other.usageCondition != null)
				return false;
		} else if (!usageCondition.equals(other.usageCondition))
			return false;
		return true;
	}

	/**
	 * Base this only on Usage for now - used only by ConditionRepositoryTest
	 * which needs sorted by Usage.
	 */
	@Override
	public int compareTo(UsageConditionSize ucs) {
		if (getUsageCondition() == null || ucs.getUsageCondition() == null) {
			if (getUsageCondition() == null && ucs.getUsageCondition() == null)  return 0;
			return getUsageCondition() == null ? -1 : 1;
		}

		return getUsageCondition().compareTo(ucs.getUsageCondition());
	}
}
