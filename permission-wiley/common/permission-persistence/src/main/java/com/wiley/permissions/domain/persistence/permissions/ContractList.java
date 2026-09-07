package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The only reason we have this class is that the old version of Drools we
 * are using cannot handle generics.
 * 
 * @version $Id: ContractList.java,v 1.17 2013-08-16 20:45:11 smarkoff Exp $
 */
public class ContractList extends ArrayList<Contract> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(ContractList.class);

	public ContractList() {
		super();
	}

	public ContractList(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public boolean add(Contract c) {
		boolean success = false;

		if (c != null && !contains(c)) {
			success = super.add(c);
		}

		return success;
	}
}
