package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;

/**
 * This class exists mainly for the purpose of using in the DRules engine
 * since at least v4.x can't match on List<SomeClass>().
 * (Rule does not compile.)
 *
 * @version $Id: ConditionList.java,v 1.1 2010-10-28 00:28:34 lnagy Exp $
 * @author smarkoff, created 8/18/2010
 */
public class ConditionList extends ArrayList<Condition> {

	private static final long serialVersionUID = 1L;

	public ConditionList() {
		super();
	}

	public ConditionList(int initialCapacity) {
		super(initialCapacity);
	}
}
