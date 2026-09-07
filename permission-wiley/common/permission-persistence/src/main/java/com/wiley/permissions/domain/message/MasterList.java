package com.wiley.permissions.domain.message;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;

import com.wiley.permissions.domain.persistence.permissions.Bundle;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Editor;
import com.wiley.permissions.domain.persistence.permissions.GeographicalLocation;
import com.wiley.permissions.domain.persistence.permissions.Medium;
import com.wiley.permissions.domain.persistence.permissions.ProductEdition;
import com.wiley.permissions.domain.persistence.permissions.ProductFamily;
import com.wiley.permissions.domain.persistence.permissions.ProductLine;
import com.wiley.permissions.domain.persistence.permissions.ProductType;
import com.wiley.permissions.domain.persistence.permissions.RelationCode;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Role.RoleType;
import com.wiley.permissions.domain.persistence.permissions.SubMedium;
import com.wiley.permissions.domain.persistence.permissions.SubjectCode;
import com.wiley.permissions.domain.persistence.permissions.User;

/**
 * @author smarkoff
 */
public class MasterList implements Serializable {

	// We don't care about all these MasterList names - just some.
	public static final MasterList
		//AUDIENCE_TYPE = new MasterList("AudienceType"),
	    AUTHOR = new MasterList("Author", true),
	    AUTHOR_ROLE = new MasterList("AuthorRole", true),
	    BUNDLE = new MasterList("Bundle", true),
    	BUSINESS_UNIT = new MasterList("BusinessUnit", true),
    	COMMON_WORK = new MasterList("CommonWork", true),
    	//COURSE_CODE = new MasterList("CourseCode", false),
    	//DISCIPLINE = new MasterList("Discipline", false),
    	//DISCOUNT_GROUP = new MasterList("DiscountGroup", false),
    	EDITION = new MasterList("Edition", true),
    	EDITOR = new MasterList("Editor", false),
    	EMPLOYEE = new MasterList("Employee", true),
    	EMPLOYEE_ROLE = new MasterList("EmployeeRole", true),
    	//FIELD = new MasterList("Field", false),
    	GEOGRAPHICAL_LOCATION = new MasterList("GeographicalLocation", true),
    	//GLOBAL_SUBJECT_CATEGORY = new MasterList("GlobalSubjectCategory", true),
    	//IMPRINT = new MasterList("Imprint", true),
    	//MARKET = new MasterList("Market", true),
    	PRODUCT_FAMILY = new MasterList("ProductFamily", true),
    	PRODUCT_LINE = new MasterList("ProductLine", false),
    	//PRODUCT_PRIORITY = new MasterList("ProductPriority", true),
    	//PRODUCT_SUB_CATEGORY = new MasterList("ProductSubCategory", true),
    	PRODUCT_TYPE = new MasterList("ProductType", true),
    	//PUBLISHER = new MasterList("Publisher", true),
    	//READERSHIP_LEVEL = new MasterList("ReadershipLevel", true),
    	RELATION_CODE = new MasterList("RelationCode", true),
    	//SERIES_CODE = new MasterList("SeriesCode", false),
    	//SUB_DISCIPLINE = new MasterList("SubDiscipline", false),
    	SUBJECT_CODE = new MasterList("SubjectCode", false),
    	SUB_MEDIUM = new MasterList("SubMedium", false),
    	TARGET_MEDIUM = new MasterList("TargetMedium", false);
    	//TRIM_SIZE = new MasterList("TrimSize", true),
    	//VENDOR_TYPE = new MasterList("VendorType", true);

	public static final MasterList [] ALL_WE_NEED = {
		AUTHOR, AUTHOR_ROLE, BUNDLE, BUSINESS_UNIT, COMMON_WORK, EDITION,
		EDITOR, EMPLOYEE, EMPLOYEE_ROLE, GEOGRAPHICAL_LOCATION,
		PRODUCT_FAMILY, PRODUCT_LINE, PRODUCT_TYPE, RELATION_CODE,
		SUBJECT_CODE, SUB_MEDIUM, TARGET_MEDIUM
	};

	public static MasterList masterListForObject(Object o) {
		if (o instanceof User ) {
			User user = (User) o;
			if (user.getType() == User.Type.AUTHOR) return MasterList.AUTHOR;
			if (user.getType() == User.Type.EMPLOYEE) return MasterList.EMPLOYEE;
		}
		if (o instanceof Role) {
			Role role = (Role) o;
			if (role.getRoleType() == RoleType.AUTHOR) return MasterList.AUTHOR_ROLE;
			if (role.getRoleType() == RoleType.EMPLOYEE)  return MasterList.EMPLOYEE_ROLE;
		}
		// Author covered by User above
		// AuthorRole covered by Role above
		if (o instanceof BusinessUnit) return MasterList.BUSINESS_UNIT;
		if (o instanceof ProductEdition) return MasterList.EDITION;
		if (o instanceof Editor) return MasterList.EDITOR;
		// Employee covered by User above
		// EmployeeRole covered by Role above
		if (o instanceof GeographicalLocation) return MasterList.GEOGRAPHICAL_LOCATION;
		if (o instanceof ProductFamily) return MasterList.PRODUCT_FAMILY;
		if (o instanceof ProductLine) return MasterList.PRODUCT_LINE;
		if (o instanceof ProductType) return MasterList.PRODUCT_TYPE;
		if (o instanceof RelationCode) return MasterList.RELATION_CODE;
		if (o instanceof SubjectCode) return MasterList.SUBJECT_CODE;
		if (o instanceof SubMedium) return MasterList.SUB_MEDIUM;
		if (o instanceof Medium) return MasterList.TARGET_MEDIUM;
		if (o instanceof CommonWork) return MasterList.COMMON_WORK;
		if (o instanceof Bundle) return MasterList.BUNDLE;

		return null;
	}

	private static final long serialVersionUID = 1L;

	private String name;
	private String dataSource;
	// updateTime is received in the notification message but illegal in the request message
	private Long updateTime;
	// updatedSince is not part of the notification message but is an optional part of the request message
	private Long updatedSince;
	// global is based on which types of MasterLists Bruce has told me (smarkoff) are effectively global
	// (meaning that the data is really the same across all dataSources, i.e. dataSource is not relevant)
	private boolean global;


	public MasterList() {

	}

	public MasterList(String name, boolean global) {
		setName(name);
		this.global = global;
	}

	public boolean isTypeWeNeed() {
		String name = getName();
		for (MasterList ml : ALL_WE_NEED) {
			if (name.equals(ml.getName()))  return true;
		}

		return false;
	}

	/**
	 * This method is useful to call on MasterList objects from PE, which don't have the global flag set.
	 */
	public boolean computeGlobal() {
		for (MasterList ml : ALL_WE_NEED) {
			if (name.equals(ml.getName())) {
				global = ml.isGlobal();
				return global;
			}
		}

		return false;
	}

	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute
	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	@XmlAttribute
	public Long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Long updateTime) {
		this.updateTime = updateTime;
	}

	@XmlAttribute
	public Long getUpdatedSince() {
		return updatedSince;
	}

	public void setUpdatedSince(Long updatedSince) {
		this.updatedSince = updatedSince;
	}

	public boolean isGlobal() {
		return global;
	}

	@Override
	public String toString() {
		return "name = " + getName()
			+ ", dataSource = " + getDataSource()
			+ ", updateTime = " + getUpdateTime()
			+ ", updatedSince = " + getUpdatedSince()
			+ ", isGlobal = " + isGlobal();
	}
}
