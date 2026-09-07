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
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "FAVORITE_GROUP")
@Cache (usage = CacheConcurrencyStrategy.READ_WRITE)
public class FavoriteGroup
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false)
	@MaterializationKey
	private Integer id;

	@Column(name = "DESCRIPTION", nullable = false, length = 100, unique = true)
	private String description;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "SOURCE_2_FAVORITE_GROUP",
		joinColumns = @JoinColumn(name = "FAVORITE_GROUP_ID"),
		inverseJoinColumns = @JoinColumn(name = "SOURCE_ID")
	)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT,
			collectionHandling=Merge.CollectionHandling.REPLACE)
	private List<Source> sources = new ArrayList<Source>();

	public FavoriteGroup() {
		super();
	}

	public FavoriteGroup(Integer id, String description) {
		this.id = id;
		this.description = description;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Source> getSources() {
		return sources;
	}

	public void setSources(List<Source> sources) {
		this.sources = sources;
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
		    + ", description = " + description
		    ;
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
		if (!(obj instanceof FavoriteGroup)) return false;
		FavoriteGroup other = (FavoriteGroup) obj;
		if (id == null) {
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		return true;
	}
}
