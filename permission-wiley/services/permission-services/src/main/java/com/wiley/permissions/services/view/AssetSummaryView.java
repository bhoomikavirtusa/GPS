package com.wiley.permissions.services.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.persistence.LabelValueBean;

public class AssetSummaryView {

	private List <Object[]> statusCounts;

	private Map<String, Number> covers = new HashMap<String, Number> ();
	private Map<String, Number> internals = new HashMap<String, Number> ();
	private Map<String, Number> costs = new HashMap<String, Number> ();

	public List <Object[]>  getStatusCounts() {
		return statusCounts;
	}

	public List <LabelValueBean> getCountsAsLb() {
		List<LabelValueBean> lb = new ArrayList<LabelValueBean>();
		//List<Object> row;
		for (Object[] columns : getStatusCounts()) {
			//row = new ArrayList<Object>();
			if (columns[1].toString().trim().compareTo("0") != 0) {
				LabelValueBean dBean = new LabelValueBean(columns[0].toString(), columns[1].toString());
				lb.add(dBean);
			}
		}

		return lb;
	}

	public void setStatusCounts(List <Object[]> values) {
		this.statusCounts = values;
	}

	public int getPermissionsRequested() {
		return getPermissionsEntered() + getPermissionsOutstanding();
	}

	public int getPermissionsEntered() {
		List<PermissionStatus> granted = PermissionStatus.getStatuses(PermissionStatus.GRANTED_GROUP);
		int count = 0;
		for (PermissionStatus status : granted) {
			count += getCount(status.getDescription());
		}
		return count;
	}

	public int getPermissionsOutstanding() {
		return getCount(PermissionStatus.FORM_SENT.getDescription());
	}

	public int getPermissionsUnrequested() {
		return getCount(PermissionStatus.UNREQUESTED.getDescription());
	}

	public int getNumberOfAssets() {
		return getCount("Number of Assets");
	}

	public Map<String, Number> getCovers() {
    	return covers;
    }

	public void setCovers(Map<String, Number> covers) {
    	this.covers = covers;
    }

	public Map<String, Number> getInternals() {
    	return internals;
    }

	public void setInternals(Map<String, Number> internals) {
    	this.internals = internals;
    }

	public Map<String, Number> getCosts() {
    	return costs;
    }

	public void setCosts(Map<String, Number> costs) {
    	this.costs = costs;
    }

	private double getTotal (Map<String, Number> tmap, String category) {
		return 	tmap.get(category + "_rm").doubleValue() +
				tmap.get(category + "_royaltyfree").doubleValue() +
				tmap.get(category + "_royaltyfree$$").doubleValue() +
				tmap.get(category + "_free").doubleValue();
	}

	public int getNewTotal() {
		return (int)getTotal (covers, "new") + (int)getTotal (internals, "new");
	}

	public int getReuseTotal() {
		return (int)getTotal (covers, "reuse") + (int)getTotal (internals, "reuse");
	}

	public double getNewTotalCost() {
		return getTotal (costs, "new");
	}

	public double getReuseTotalCost() {
		return getTotal (costs, "reuse");
	}

	public int getCount(String status) {
		int value = 0;

		for (Object[] columns : statusCounts) {
			String s = columns[0].toString();
			if (s.equals(status)) {
				value = new Integer(columns[1].toString());
				return value;
			}
		}

		return value;
	}

}
