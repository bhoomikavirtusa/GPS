package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.DomainObject;

/**
 * @version $Id: Condition.java,v 1.51 2014-03-22 01:41:49 smarkoff Exp $
 * @author smarkoff
 */
@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "CONDITION_VALUE")
public class Condition
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// constants for defaults for CW Conditions - used in ConditionRepository.createDefaultCWConditions()
	// best NOT to use these constants anywhere else in the code - use ConditionType constants instead
	public final static Condition
		MEDIUM = new Condition(ConditionType.MEDIUM, null, "All media types including future types"),
		MEDIUM_ALL = new Condition(ConditionType.MEDIUM_ALL, "true", null),

		SALES_TERRITORY = new Condition(ConditionType.SALES_TERRITORY, null, "Worldwide"),
		SALES_WORLD_ALIAS = new Condition(ConditionType.SALES_WORLD_ALIAS, "true", null),

		LANGUAGE = new Condition(ConditionType.LANGUAGE, null, "English only"),
		LANGUAGE_ALL = new Condition(ConditionType.LANGUAGE_ALL, "true", null),
		LANGUAGE_ENGLISH = new Condition(ConditionType.LANGUAGE_ENGLISH, "true", null),
		LANGUAGE_ENGLISH_ALIAS = new Condition(ConditionType.LANGUAGE_ENGLISH_ALIAS, "true", null),

		PRINT_RUN = new Condition(ConditionType.PRINT_RUN, null, "Unlimited print run is granted"),
		PRINT_RUN_UNLIMITED = new Condition(ConditionType.PRINT_RUN_UNLIMITED, "true", null),

		DERIVATIVE_WORKS = new Condition(ConditionType.DERIVATIVE_WORKS, null,
				"Wiley can include the asset(s) in any ancillaries, derivatives and custom works"),
		DERIVATIVE_WORKS_ALL = new Condition(ConditionType.DERIVATIVE_WORKS_ALL, "true", null),

		EDITION = new Condition(ConditionType.EDITION, null, "Granted for this edition only"),
		EDITION_THIS = new Condition(ConditionType.EDITION_THIS, "true", null);

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(Condition.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private int id;

	@ManyToOne
	@JoinColumn(name = "CONDITION_TYPE")
	private ConditionType type;

	@Column(name = "VALUE")
	private String value;

	@Column(name = "ROLLUP_VALUE")
	private String rollupValue;


	public Condition() {
		super();
	}

	public Condition(ConditionType type) {
	    this.type = type;
	}

	public Condition(ConditionType type, String value) {
		this.type = type;
		this.value = value;
	}

	public Condition(ConditionType type, String value, String rollupValue) {
	    this.type = type;
	    this.value = value;
	    this.rollupValue = rollupValue;
	}

	/**
	 * Copy constructor - copied everything but the id.
	 * @param c
	 */
	private Condition(Condition c) {
		this.type = c.getType();
		this.value = c.getValue();
		this.rollupValue = c.getRollupValue();
	}

	/**
	 * Return a copy except for the id.
	 */
	public Condition copyNoId() {
		return new Condition(this);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ConditionType getType() {
		return type;
	}

	public void setType(ConditionType type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public boolean isValueTrue() {
		return "true".equals(value);
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getRollupValue() {
		return rollupValue;
	}

	public void setRollupValue(String rollupValue) {
		this.rollupValue = rollupValue;
	}

	/*
	@Transient
	public void validate(CommonWork cw)
	throws ConditionBlankException, NumberFormatException, ConditionInvalidProductFamilyException
	{
		if (CollectionUtils.isEmpty(getValues())) {
			throw new ConditionBlankException();
		}

		if (getType().getDataType().equals(DataType.INT)) {
			for (String value: this.getStringValues()) {
			    Integer.parseInt(value);
			}
		}
	}*/

	@Override
	public String toString() {
		// use getters due to JPA
		return "id = " + getId() + ", type = " + ((getType() == null) ? "null" : getType().getCode())
			+ ", value = " + getValue();
	}

	/**
	 * Base on id.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// call getId() since using JPA - not sure if necessary
		result = prime * result + getId();
		return result;
	}

	/**
	 * Base on id.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof Condition)) return false;
		Condition other = (Condition) obj;
		// call getId() since using JPA - not sure if necessary
		// dealing with int (not Integer) here
		if (getId() != other.getId()) return false;
		return true;
	}
}
