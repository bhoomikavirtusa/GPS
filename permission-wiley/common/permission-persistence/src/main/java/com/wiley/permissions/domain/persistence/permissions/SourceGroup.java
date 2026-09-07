package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "SOURCE_GROUP")
@Cache (usage = CacheConcurrencyStrategy.READ_WRITE)
public class SourceGroup
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false)
	@MaterializationKey
	private Integer id;

	@Column(name = "NAME", nullable = false, length = 50, unique = true)
	private String name;

	@Column(name = "NOFLY", nullable = false)
	private boolean nofly;

	@Column(name = "COMMENT", length = 1000)
	private String comment;

	@OneToMany(mappedBy = "sourceGroup", fetch = FetchType.EAGER)
	@OrderBy( "name ASC")
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private List<Source> sources = new ArrayList<Source>();

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "ROYALTY_FREE_DEAL",
		joinColumns = @JoinColumn(name = "SOURCE_GROUP_ID", updatable = false, insertable = false),
		inverseJoinColumns = @JoinColumn(name = "ID")
	)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	@OrderBy  // need this for AssetRepository.updateTotalSeatsAndPrintRunByAssetId()
	private List<RoyaltyFreeDeal> royaltyFreeDeals = new ArrayList<RoyaltyFreeDeal>();

	@OneToMany
	@JoinTable(name = "MASTER_AGREEMENT_DEAL",
		joinColumns = @JoinColumn(name = "SOURCE_GROUP_ID"),
		inverseJoinColumns = @JoinColumn(name = "ID")
	)
	private List<MasterAgreementDeal> masterAgreementDeals = new ArrayList<MasterAgreementDeal>();

	@Transient
	private boolean isBeingUsed = false;

	public SourceGroup() {
		super();
	}

	public SourceGroup(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public boolean isNofly() {
		return nofly;
	}

	public void setNofly(boolean nofly) {
		this.nofly = nofly;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = StringUtils.trimToNull(comment);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public List<Source> getSources() {
		return sources;
	}

	public void setSources(List<Source> sources) {
		this.sources = sources;
	}


	public List<RoyaltyFreeDeal> getRoyaltyFreeDeals() {
		return royaltyFreeDeals;
	}

	public void setRoyaltyFreeDeals(List<RoyaltyFreeDeal> royaltyFreeDeals) {
		this.royaltyFreeDeals = royaltyFreeDeals;
	}

	public List<MasterAgreementDeal> getMasterAgreementDeals() {
		return masterAgreementDeals;
	}

	public void setMasterAgreementDeals(List<MasterAgreementDeal> masterAgreementDeals) {
		this.masterAgreementDeals = masterAgreementDeals;
	}

	@Transient
	public boolean hasMasterAgreementActive() {
		if (CollectionUtils.isEmpty(getMasterAgreementDeals())) {
			return false;
		} else {
			for (MasterAgreementDeal ma : masterAgreementDeals) {
				if (ma.isActive())
					return true;
			}
			return false;
		}
	}

	@Transient
	public MasterAgreementDeal getActiveMasterAgreement() {
		if (CollectionUtils.isEmpty(getMasterAgreementDeals())) {
			return null;
		} else {
			for (MasterAgreementDeal ma : masterAgreementDeals) {
				if (ma.isActive())
					return ma;
			}
			return null;
		}
	}

	@Transient
	public String getMembers() {
		String members = "";
		if (CollectionUtils.isEmpty(getSources())) {
			return null;
		} else {
			boolean first = true;
			for (Source src : getSources()) {
				if(! first) {
					members = members + ", ";
				}
				members = members + src.getName();
				first = false;
			}
			return members;
		}
	}


	// this is a transient set by a controller after inspecting SourceRepository
	public boolean getIsBeingUsed() {
		return isBeingUsed;
	}

	// this is a transient set by a controller after inspecting SourceRepository
	public void setIsBeingUsed(boolean isBeingUsed) {
		this.isBeingUsed = isBeingUsed;
	}

	/**
	 * We automatically correct the code if only the case is wrong.
	 *
	 * @throws ValidateException
	 */
	public void validate() throws ValidateException {
		return;
	}

	@Override
	public String toString() {
		return "id = " + id
		    + ", name = " + name;
	}

	/**
	 * Base on id.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (!(obj instanceof SourceGroup)) return false;
		SourceGroup other = (SourceGroup) obj;
		if (id == null) {
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		return true;
	}
}
