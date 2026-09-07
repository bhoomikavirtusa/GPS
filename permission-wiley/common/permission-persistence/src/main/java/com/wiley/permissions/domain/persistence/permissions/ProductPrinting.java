package com.wiley.permissions.domain.persistence.permissions;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlElement;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name="PRODUCT_PRINTING")
public class ProductPrinting
extends AuditBase
{
	private static final long serialVersionUID = 1L;

	// lnagy - no @Merge required because the whole collection will be replaced
	// in Product we have Merge.CollectionHandling.REPLACE

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@ManyToOne
	@JoinColumn(name = "PRODUCT_ID")
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	private Product product = null;

	@Column(name = "PRINTING_NUMBER", nullable = false)
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	private Integer printingNumber = null;

	@Column(name = "DISTRIBUTION_CENTER", nullable = false)
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	private Integer distributionCenter = null;

	@Column(name = "PO_NUMBER", nullable = false, length = 16)
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	private String poNumber = null;

	@Column(name = "PO_STATUS", nullable = false, length = 50)
	private String poStatus = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "PO_DATE", nullable = true)
	private Date poDate = null;

	@Column(name = "ORDER_QUANTITY", nullable = false)
	private Integer orderQuantity = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "EXPECTED_DELIVERY_DATE", nullable = true)
	private Date expectedDeliveryDate = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "RECEIVED_DATE", nullable = true)
	private Date receivedDate = null;

	@Column(name = "RECEIVED_QUANTITY", nullable = false)
	private Integer receivedQuantity = null;

	@XmlElement
	public Integer getDistributionCenter() {
		return distributionCenter;
	}

	public void setDistributionCenter(Integer distributionCenter) {
		this.distributionCenter = distributionCenter;
	}

	@XmlElement
	public Date getExpectedDeliveryDate() {
		return expectedDeliveryDate;
	}

	public void setExpectedDeliveryDate(Date expectedDeliveryDate) {
		this.expectedDeliveryDate = expectedDeliveryDate;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElement
	public Integer getOrderQuantity() {
		return orderQuantity;
	}

	public void setOrderQuantity(Integer orderQuantity) {
		this.orderQuantity = orderQuantity;
	}

	@XmlElement
	public Date getPoDate() {
		return poDate;
	}

	public void setPoDate(Date poDate) {
		this.poDate = poDate;
	}

	@XmlElement
	public String getPoNumber() {
		return poNumber;
	}

	public void setPoNumber(String poNumber) {
		this.poNumber = poNumber;
	}

	@XmlElement
	public String getPoStatus() {
		return poStatus;
	}

	public void setPoStatus(String poStatus) {
		this.poStatus = poStatus;
	}

	@XmlElement(name="number")
	public Integer getPrintingNumber() {
		return printingNumber;
	}

	public void setPrintingNumber(Integer printingNumber) {
		this.printingNumber = printingNumber;
	}

	@XmlElement
	public Date getReceivedDate() {
		return receivedDate;
	}

	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}

	@XmlElement
	public Integer getReceivedQuantity() {
		return receivedQuantity;
	}

	public void setReceivedQuantity(Integer receivedQuantity) {
		this.receivedQuantity = receivedQuantity;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	@Override
	public String toString() {
		// use getters due to JPA
		Product product = getProduct();
		return "id = " + getId()
			+ ", product.id = " + (product == null ? "null" : product.getId())
			+ ", printingNumber = " + getPrintingNumber()
			+ ", poNumber = " + getPoNumber()
			+ ", distributionCenter = " + getDistributionCenter()
			+ ", poStatus = " + getPoStatus()
			+ ", poDate = " + getPoDate()
			+ ", orderQuanity = " + getOrderQuantity()
			+ ", (and few other fields).";
	}

	/**
	 * Base on DB unique criteria (product, distributionCenter, poNumber, printingNumber).
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((distributionCenter == null) ? 0 : distributionCenter
						.hashCode());
		result = prime * result
				+ ((poNumber == null) ? 0 : poNumber.hashCode());
		result = prime * result
				+ ((printingNumber == null) ? 0 : printingNumber.hashCode());
		result = prime * result + ((product == null) ? 0 : product.hashCode());
		return result;
	}

	/**
	 * Base on DB unique criteria (product, distributionCenter, poNumber, printingNumber).
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ProductPrinting)) return false;
		ProductPrinting other = (ProductPrinting) obj;
		if (distributionCenter == null) {
			if (other.distributionCenter != null) return false;
		}
		else if (!distributionCenter.equals(other.distributionCenter))
			return false;
		if (poNumber == null) {
			if (other.poNumber != null) return false;
		}
		else if (!poNumber.equals(other.poNumber)) return false;
		if (printingNumber == null) {
			if (other.printingNumber != null) return false;
		}
		else if (!printingNumber.equals(other.printingNumber)) return false;
		if (product == null) {
			if (other.product != null) return false;
		}
		else if (!product.equals(other.product)) return false;
		return true;
	}
}
