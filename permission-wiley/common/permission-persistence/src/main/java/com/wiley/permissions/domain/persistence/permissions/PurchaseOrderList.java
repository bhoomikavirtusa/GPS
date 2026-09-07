package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The only reason we have this class is that the old version of Drools we
 * are using cannot handle generics.
 */
public class PurchaseOrderList extends ArrayList<PurchaseOrder> {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PurchaseOrderList.class);

	private static final long serialVersionUID = 1L;

	public PurchaseOrderList() {
		super();
	}

	public PurchaseOrderList(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public boolean add(PurchaseOrder o) {
		boolean success = false;

		if (o != null) {
			if (!contains(o))
				success = super.add(o);
		}

		return success;
	}
}
