package com.wiley.permissions.domain.persistence.permissions;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;

/**
 *
 * @author lnagy, created Aug 30, 2007
 */
@Entity
@PersistenceUnit(unitName="permissions")
@Table(name="ASSET_BASE")
@Inheritance(strategy=InheritanceType.JOINED)
@DiscriminatorColumn(name="ASSET_BASE_TYPE", discriminatorType=DiscriminatorType.STRING)
public class AssetBase
extends ExtendedAuditBase
{
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(AssetBase.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ID", nullable=false, unique=true)
	private Integer id = null;

	@Column(name = "EXTERNAL_ID", nullable=false, length=64)
	@MaterializationKey
	private String externalId;

	@ManyToMany
	@JoinTable(
		name = "PURCHASE_ORDER_2_ASSET",
		joinColumns = @JoinColumn(name = "ASSET_BASE_ID"),
		inverseJoinColumns = @JoinColumn(name = "PURCHASE_ORDER_ID")
	)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private Set<PurchaseOrder> purchaseOrders = new HashSet<PurchaseOrder>();

	@OneToMany(mappedBy="assetBase")
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private Set<ContractAsset> contracts = new HashSet<ContractAsset>();

	public AssetBase() {
		super();
	}

	@Override
	@PrePersist
	public void prePersist() {
		log.debug("prePersist() called");
		super.prePersist();

		if (StringUtils.isBlank(getExternalId())) {
			setExternalId(UniqueIdentifierGenerator.getNextIdentifier("perm.asset_base."));
		}
	}

	@XmlElement
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlID
	@XmlElement
	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public Set<PurchaseOrder> getPurchaseOrders() {
		return purchaseOrders;
	}

	public void setPurchaseOrders(Set<PurchaseOrder> purchaseOrders) {
		this.purchaseOrders = purchaseOrders;
	}

	public Set<ContractAsset> getContracts() {
		return contracts;
	}

	public void setContracts(Set<ContractAsset> contracts) {
		this.contracts = contracts;
	}

	@Transient
	public String getDescription() {
		if (this instanceof Asset) {
			return ((Asset)this).getDescription();
		}
		else if (this instanceof AssetGroup) {
			return ((AssetGroup)this).getDescription();
		}
		return null;
	}

	/**
	 * Base hashCode and equals only on id.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// call getId since using JPA - not sure if necessary
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());

		return result;
	}

	/**
	 * Base hashCode and equals only on id.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof AssetBase)) return false;

		// call getId since using JPA - not sure if necessary
		AssetBase other = (AssetBase) obj;
		if (other.getId() == null || getId() == null) return false;
		return other.getId().equals(getId());
	}
}