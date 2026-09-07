package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 *
 * @author ttidwell
 */
@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "COMP_COPY")
@XmlRootElement(name="permissions_comp")
public class CompCopy
extends AuditBase
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ID", nullable=false, unique=true)
	private Integer id = null;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="CONTRACT_ID", nullable=false)
	private Contract contract = null;

	@Column(name = "NUM_COPIES", nullable=false)
	private Integer numberOfCopies = null;

	@Column(name = "ORDER_NUM", nullable=true)
	private String orderNum = null;

	@Column(name = "SUCCESS_FLAG", nullable=true)
	private String successFlag = null;
	
	@Column(name = "ERROR_MSG", nullable=true)
	private String errorMsg = null;
	
	
	/**
	 * Id of Wintouch Address this is being sent to.
	 */
	@Column(name = "ADDRESS_ID", nullable=true)
	private Integer addressId = null;

	@Transient
	private Address address;

	public CompCopy()
	{

	}

	@XmlElement
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

	@XmlElement(name="order-num")
	public String getOrderNum()
	{
		return orderNum;
	}
	
	public void setOrderNum(String value)
	{
		this.orderNum = value;
	}
	
	@XmlElement(name="success-flag")
	public String getSuccessFlag()
	{
		return successFlag;
	}
	
	public void setSuccessFlag(String value)
	{
		this.successFlag = value;
	}
	
	@XmlElement(name="error-msg")
	public String getErrorMsg()
	{
		return errorMsg;
	}
	
	public void setErrorMsg(String value)
	{
		this.errorMsg = value;
	}
	
	@XmlElement(name="num-copies")
	public Integer getNumberOfCopies()
	{
		return numberOfCopies;
	}

	public void setNumberOfCopies(Integer numberOfCopies)
	{
		this.numberOfCopies = numberOfCopies;
	}

	public Address getAddress()
	{
		return address;
	}

	public void setAddress(Address address)
	{
		this.address = address;
	}

	public Integer getAddressId()
	{
		return addressId;
	}

	public void setAddressId(Integer addressId)
	{
		this.addressId = addressId;
	}
}
