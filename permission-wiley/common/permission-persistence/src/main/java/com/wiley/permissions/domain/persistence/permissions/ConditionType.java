package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

/**
 * @version $Id: ConditionType.java,v 1.84.2.1 2018-01-18 15:08:03 nchandra Exp $
 * @author smarkoff
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "CONDITION_TYPE")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class ConditionType
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// constants that should always match the db
	// smarkoff: I don't think I need the other fields in these conditions - double check
	// that the database never gets updated in any way by not specifying all the fields here
	public static final ConditionType
    	MEDIUM = new ConditionType(ConditionCode.MEDIUM, "What media types are granted?"),
    	MEDIUM_ALL = new ConditionType(ConditionCode.MEDIUM_ALL, "All media types including future types"),
    	MEDIUM_PHYSICAL_ELECTRONIC = new ConditionType(ConditionCode.MEDIUM_PHYSICAL_ELECTRONIC, "All physical and electronic media"),
    	MEDIUM_PHYSICAL_EBOOK_WEB = new ConditionType(ConditionCode.MEDIUM_PHYSICAL_EBOOK_WEB, "All physical media and ebook/web"),
    	MEDIUM_ALL_PHYSICAL = new ConditionType(ConditionCode.MEDIUM_ALL_PHYSICAL, "All physical media including print and CDROM"),
    	MEDIUM_PRINT_ONLY = new ConditionType(ConditionCode.MEDIUM_PRINT_ONLY, "Print only"),
    	MEDIUM_NO_MENTION = new ConditionType(ConditionCode.MEDIUM_NO_MENTION, "No mention of media types"),

	    SALES_TERRITORY = new ConditionType(ConditionCode.SALES_TERRITORY, "What distribution or sales territory rights are granted?"),
	    SALES_WORLD = new ConditionType(ConditionCode.SALES_WORLD, "Other"),
	    SALES_WORLD_ALIAS = new ConditionType(ConditionCode.SALES_WORLD_ALIAS, "Worldwide"),
	    SALES_NORTH_AMERICA_ALIAS =  new ConditionType(ConditionCode.SALES_NORTH_AMERICA_ALIAS, "North America"),
	    SALES_NO_MENTION = new ConditionType(ConditionCode.SALES_NO_MENTION, "No mention of distribution/sales territories"),

	    LANGUAGE = new ConditionType(ConditionCode.LANGUAGE, "What language rights are granted?"),
	    LANGUAGE_ALL = new ConditionType(ConditionCode.LANGUAGE_ALL, "Other (select as many as apply)"),
	    LANGUAGE_ALL_ALIAS = new ConditionType(ConditionCode.LANGUAGE_ALL_ALIAS, "All Languages"),
	    LANGUAGE_ENGLISH = new ConditionType(ConditionCode.LANGUAGE_ENGLISH, "English"),
	    LANGUAGE_ENGLISH_ALIAS = new ConditionType(ConditionCode.LANGUAGE_ENGLISH_ALIAS, "English only"),
	    LANGUAGE_FRENCH = new ConditionType(ConditionCode.LANGUAGE_FRENCH, "French"),
	    LANGUAGE_GERMAN = new ConditionType(ConditionCode.LANGUAGE_GERMAN, "German"),
	    LANGUAGE_NO_MENTION = new ConditionType(ConditionCode.LANGUAGE_NO_MENTION, "No mention of language"),

	    PRINT_RUN = new ConditionType(ConditionCode.PRINT_RUN, "Does the grant/letter include any reference to print run?"),
	    PRINT_RUN_UNLIMITED = new ConditionType(ConditionCode.PRINT_RUN_UNLIMITED, "Unlimited print run is granted"),
	    PRINT_RUN_LIMIT = new ConditionType(ConditionCode.PRINT_RUN_LIMIT, "Maximum copies Wiley can print"),
	  	PRINT_RUN_LIMIT_BOX = new ConditionType(ConditionCode.PRINT_RUN_LIMIT_BOX, ""),
	  	PRINT_RUN_NO_MENTION = new ConditionType(ConditionCode.PRINT_RUN_NO_MENTION, "Print run is not mentioned"),

	  	PRINT_RUN_EBOOK = new ConditionType(ConditionCode.PRINT_RUN_EBOOK, "Total Ebook Print Run"),

		DERIVATIVE_WORKS = new ConditionType(ConditionCode.DERIVATIVE_WORKS, "Does the grant cover use of the asset(s) in deriviative works?"),
		DERIVATIVE_WORKS_ALL = new ConditionType(ConditionCode.DERIVATIVE_WORKS_ALL,
				"Wiley can include the asset(s) in any ancillaries, derivatives and custom works"),
		DERIVATIVE_WORKS_ANC_AND_DERIV = new ConditionType(ConditionCode.DERIVATIVE_WORKS_ANC_AND_DERIV,
				"Wiley can include the asset(s) in ancillaries and derivatives with the exception of custom"),
		DERIVATIVE_WORKS_MAIN_ONLY = new ConditionType(ConditionCode.DERIVATIVE_WORKS_MAIN_ONLY,
				"Grant states asset(s) cannot be included in any derivative works"),
		DERIVATIVE_WORKS_NO_MENTION = new ConditionType(ConditionCode.DERIVATIVE_WORKS_NO_MENTION, "No mention of derivative works"),

		EDITION = new ConditionType(ConditionCode.EDITION, "Does the grant/letter specify use in particular editions?"),
		EDITION_THIS_FUTURE_AUTHOR = new ConditionType(ConditionCode.EDITION_THIS_FUTURE_AUTHOR,
				"Granted for this, future editions and/or entire author series"),
		EDITION_FUTURE = new ConditionType(ConditionCode.EDITION_FUTURE, "Granted for this edition and all future editions"),
		EDITION_THIS = new ConditionType(ConditionCode.EDITION_THIS, "Granted for this edition only"),
		EDITION_NO_MENTION = new ConditionType(ConditionCode.EDITION_NO_MENTION, "No mention of editions"),

		SUBLICENSE = new ConditionType(ConditionCode.SUBLICENSE, "Are sublicensing rights granted?"),
		SUBLICENSE_RIGHT = new ConditionType(ConditionCode.SUBLICENSE_RIGHT, "Wiley can include the asset(s) when sub-licensing product"),
		SUBLICENSE_NO_RIGHT = new ConditionType(ConditionCode.SUBLICENSE_NO_RIGHT, "Wiley cannot include the asset(s) when sub-licensing product"),
		SUBLICENSE_NO_MENTION = new ConditionType(ConditionCode.SUBLICENSE_NO_MENTION, "This grant does not mention sublicensing"),

		//Start: Added to implement DM-122
		SEATS = new ConditionType(ConditionCode.SEATS, "Does the grant/letter include any reference to Seats?"),
	    SEATS_UNLIMITED = new ConditionType(ConditionCode.SEATS_UNLIMITED, "Unlimited Seats are granted"),
	    SEATS_LIMIT = new ConditionType(ConditionCode.SEATS_LIMIT, "Maximum Seats allowed"),
	  	SEATS_LIMIT_BOX = new ConditionType(ConditionCode.SEATS_LIMIT_BOX, ""),
	  	SEATS_NO_MENTION = new ConditionType(ConditionCode.SEATS_NO_MENTION, "Seats limitation is not mentioned"),
	  	//End: Added to implement DM-122

		OTHER = new ConditionType(ConditionCode.OTHER, "Other restrictions and permission notes"),
		NOTES = new ConditionType(ConditionCode.NOTES, "Notes");

	public enum ConditionCode {
		MEDIUM ("medium"),
		MEDIUM_ALL ("medium_all"),
		MEDIUM_PHYSICAL_ELECTRONIC ("medium_physical_electronic"),
		MEDIUM_PHYSICAL_EBOOK_WEB ("medium_physical_ebook_web"),
		MEDIUM_ALL_PHYSICAL ("medium_all_physical"),
		MEDIUM_PRINT_ONLY ("medium_print_only"),
		MEDIUM_NO_MENTION ("medium_no_mention"),

		SALES_TERRITORY ("sales"),
		SALES_WORLD ("sales_world"),
		SALES_WORLD_ALIAS ("sales_world_alias"),
		SALES_NORTH_AMERICA_ALIAS ("sales_north_america_alias"),
		SALES_NORTH_AMERICA ("sales_north_america"),
		SALES_NO_MENTION ("sales_no_mention"),

		LANGUAGE ("language"),
		LANGUAGE_ALL ("language_all"),
		LANGUAGE_ALL_ALIAS ("language_all_alias"),
		LANGUAGE_ENGLISH ("language_eng"),
		LANGUAGE_ENGLISH_ALIAS ("language_eng_alias"),
		LANGUAGE_FRENCH ("language_fre"),
		LANGUAGE_GERMAN ("language_deu"),
		LANGUAGE_NO_MENTION ("language_no_mention"),

		PRINT_RUN ("print_run"),
		PRINT_RUN_UNLIMITED ("print_run_unlimited"),
		PRINT_RUN_LIMIT ("print_run_limit"),
		PRINT_RUN_LIMIT_BOX ("print_run_limit_box"),
		PRINT_RUN_NO_MENTION ("print_run_no_mention"),
		PRINT_RUN_EBOOK ("print_run_ebook"),

		DERIVATIVE_WORKS ("dwork"),
		DERIVATIVE_WORKS_ALL ("dwork_all"),
		DERIVATIVE_WORKS_ANC_AND_DERIV ("dwork_anc_and_deriv"),
		DERIVATIVE_WORKS_MAIN_ONLY ("dwork_main_only"),
		DERIVATIVE_WORKS_NO_MENTION ("dwork_no_mention"),

		EDITION ("edition"),
		EDITION_THIS_FUTURE_AUTHOR ("edition_c_f_author"),
		EDITION_FUTURE ("edition_all_c_and_f"),
		EDITION_THIS ("edition_this"),
		EDITION_NO_MENTION ("edition_no_mention"),

		SUBLICENSE ("sublicense"),
		SUBLICENSE_RIGHT ("sublicense_right"),
		SUBLICENSE_NO_RIGHT ("sublicense_no_right"),
		SUBLICENSE_NO_MENTION ("sublicense_no_mention"),

		//Start: Added to implement DM-122
		SEATS ("seats"),
		SEATS_UNLIMITED ("seats_unlimited"),
		SEATS_LIMIT ("seats_limit"),
		SEATS_LIMIT_BOX ("seats_limit_box"),
		SEATS_NO_MENTION ("seats_no_mention"),
		//End: Added to implement DM-122

		OTHER ("other"),
		NOTES ("other_notes");

		private final String code;

		private ConditionCode(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = false, length = 100)
	private String description = null;

	@Column(name = "PARENT_CODE", length = 20)
	private String parentCode = null;

	@ManyToOne
	@JoinColumn(name = "DATA_TYPE", nullable = true)
	private DataType dataType;

	@Column(name = "VALIDATION_CLASS", length = 100)
	private String validationClass;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;

	@Column(name = "CAN_SEE_IN_CW", nullable = false)
	private boolean canSeeInCW = false;

	@Column(name = "CAN_EDIT_IN_CW", nullable = false)
	private boolean canEditInCW = false;

	@Column(name = "CAN_SEE_IN_CT", nullable = false)
	private boolean canSeeInContract = true;

	@Column(name = "CAN_EDIT_IN_CT", nullable = false)
	private boolean canEditInContract = true;

	@Column(name = "CAN_SEE_IN_MADEAL", nullable = false)
	private boolean canSeeInMadeal = true;

	@Column(name = "CAN_EDIT_IN_MADEAL", nullable = false)
	private boolean canEditInMadeal = true;

	public ConditionType() {
		super();
	}

	public ConditionType(ConditionCode code, String description) {
		this.code = code.getCode();
		this.description = description;
	}

	/**
	 * Copies code, description, dataType, and validationClass fields
	 * to a new ConditionNode object.
	 * You may wish to set other node fields (such as value, rollupValue, canSee, canEdit)
	 * after calling this method.
	 * Keep in mind that if you call this method with one of the ConditionType
	 * constants from above, only the code and description fields are set
	 * in the constants (so you may have to set dataType/validationClass on the node also).
	 */
	public ConditionNode toConditionNode() {
		ConditionNode node = new ConditionNode();
		node.setCode(getCode());
		node.setDescription(getDescription());
		node.setDataType(getDataType());
		node.setValidationClass(getValidationClass());
		return node;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getParentCode() {
		return parentCode;
	}

	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	public DataType getDataType() {
		return dataType;
	}

	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}

	public String getValidationClass() {
		return validationClass;
	}

	public void setValidationClass(String validationClass) {
		this.validationClass = validationClass;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean getCanSeeInCW() {
	    return canSeeInCW;
	}

	public void setCanSeeInCW(boolean canSeeInCW) {
	    this.canSeeInCW = canSeeInCW;
	}

	public boolean getCanEditInCW() {
	    return canEditInCW;
	}

	public void setCanEditInCW(boolean canEditInCW) {
	    this.canEditInCW = canEditInCW;
	}

	public boolean getCanSeeInContract() {
	    return canSeeInContract;
	}

	public void setCanSeeInContract(boolean canSeeInContract) {
	    this.canSeeInContract = canSeeInContract;
	}

	public boolean getCanEditInContract() {
	    return canEditInContract;
	}

	public void setCanEditInContract(boolean canEditInContract) {
	    this.canEditInContract = canEditInContract;
	}

	public boolean getCanSeeInMadeal() {
    	return canSeeInMadeal;
    }

	public void setCanSeeInMadeal(boolean canSeeInMadeal) {
    	this.canSeeInMadeal = canSeeInMadeal;
    }

	public boolean getCanEditInMadeal() {
    	return canEditInMadeal;
    }

	public void setCanEditInMadeal(boolean canEditInMadeal) {
    	this.canEditInMadeal = canEditInMadeal;
    }

	@Override
	public String toString() {
		// use getters due to JPA
		return "code = " + getCode()
			+ ", description = " + getDescription()
			+ ", validationClass = " + getValidationClass()
			+ ", parentCode = " + getParentCode()
			+ ", dataType = {" + getDataType() + "}"
			+ ", canSeeInCW = " + getCanSeeInCW()
			+ ", canEditInCW = " + getCanEditInCW()
			+ ", canSeeInContract = " + getCanSeeInContract()
			+ ", canEditInContract = " + getCanEditInContract()
			+ ", sortOrder = " + getSortOrder();
	}

	// Base hashCode() and equals() method only on code.

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof ConditionType)) return false;
		ConditionType other = (ConditionType) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;
		return true;
	}
}
