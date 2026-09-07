package com.wiley.permissions.domain.persistence.permissions;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "PAYMENT_REQUEST")
public class PaymentRequest
extends ExtendedAuditBase
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@OneToOne
	@JoinColumn(name = "CONTRACT_ID")
	private Contract contract;

	@Column(name = "NUMBER", nullable = true, length = 50)
	private String number;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "DATE_SUBMITTED", nullable = false)
	private Date dateSubmitted;

	@Column(name = "IS_PAID", nullable = false)
	private boolean paid = false;

	@Column(name = "COST_CENTER", nullable = true, length = 20)
	private String costCenter;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="ACCOUNT_ID")
	private Account account = null;


	public PaymentRequest()
	{
		super();
	}

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public Contract getContract()
	{
		return contract;
	}

	public void setContract(Contract contract)
	{
		this.contract = contract;
	}

	public String getNumber()
	{
		return number;
	}

	public void setNumber(String number)
	{
		this.number = number;
	}

	public Date getDateSubmitted()
	{
		return dateSubmitted;
	}

	public void setDateSubmitted(Date dateSubmitted)
	{
		this.dateSubmitted = dateSubmitted;
	}

	public boolean isPaid()
	{
		return paid;
	}

	public void setPaid(boolean paid)
	{
		this.paid = paid;
	}

	public String getCostCenter()
	{
		return costCenter;
	}

	public void setCostCenter(String costCenter)
	{
		this.costCenter = costCenter;
	}

	public Account getAccount()
	{
		return account;
	}

	public void setAccount(Account account)
	{
		this.account = account;
	}
}
