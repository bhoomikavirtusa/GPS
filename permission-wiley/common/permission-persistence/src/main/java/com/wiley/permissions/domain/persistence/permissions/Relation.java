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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "RELATION")
@Cache (usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Relation
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@ManyToOne
	@JoinColumn(name = "PRODUCT_ID")
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	private Product product;

	@ManyToOne
	@JoinColumn(name = "CODE")
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	// important: field name "code" must match getter "getCode" otherwise JPARepository won't see the MaterializationKey annotation
	private RelationCode code;

	@Column(name = "RELATED_WID", nullable = false, length = 50)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	private String relatedWid;

	public Relation() {
		super();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	@XmlElement
	@XmlIDREF
	public RelationCode getCode() {
		return code;
	}

	public void setCode(RelationCode relationCode) {
		this.code = relationCode;
	}

	@XmlElement
	public String getRelatedWid() {
		return relatedWid;
	}

	public void setRelatedWid(String relatedWid) {
		this.relatedWid = relatedWid;
	}

	/**
	 * Base on DB unique criteria (product, relationCode, relatedWid).
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((product == null) ? 0 : product.hashCode());
		result = prime * result
				+ ((relatedWid == null) ? 0 : relatedWid.hashCode());
		result = prime * result
				+ ((code == null) ? 0 : code.hashCode());
		return result;
	}

	/**
	 * Base on DB unique criteria (product, relationCode, relatedWid).
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof Relation)) return false;
		Relation other = (Relation) obj;
		if (product == null) {
			if (other.product != null) return false;
		}
		else if (!product.equals(other.product)) return false;
		if (relatedWid == null) {
			if (other.relatedWid != null) return false;
		}
		else if (!relatedWid.equals(other.relatedWid)) return false;
		if (code == null) {
			if (other.code != null) return false;
		}
		else if (!code.equals(other.code)) return false;
		return true;
	}
}
