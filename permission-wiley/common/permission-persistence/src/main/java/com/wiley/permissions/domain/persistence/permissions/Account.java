package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.wiley.permissions.domain.DomainObject;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "account")
public class Account
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@Column(name = "ACCT_NUMBER", nullable = true, length = 20)
	private String accountNumber;

	@Column(name = "SUB_CODE", nullable = true, length = 20)
	private String subCode;

	@Column(name = "DESCRIPTION", nullable = true, length=100)
	private String description = null;

	public Account() {
		super();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	@Transient
	public String getAccountNumberSubCode() {
		return getAccountNumber() + "-" + getSubCode();
	}

	@Transient
	public String getAccountNumberSubCodeDescription() {
		return getAccountNumber() + "-" + getSubCode() + " (" + getDescription() + ")";
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getSubCode() {
		return subCode;
	}

	public void setSubCode(String subCode) {
		this.subCode = subCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	// base equals() and hashCode() just on id

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof Account)) return false;
		Account other = (Account) obj;
		if (id == null) {
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		return true;
	}
}
