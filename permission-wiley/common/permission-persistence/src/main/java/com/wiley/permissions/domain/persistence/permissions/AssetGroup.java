package com.wiley.permissions.domain.persistence.permissions;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;

/**
 * Permission-Asset
 * @author lnagy, created Aug 30, 2007
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "ASSET_GROUP")
@DiscriminatorValue(value = "GROUP")
public class AssetGroup
extends AssetBase
{
	private static final long serialVersionUID = 1L;

	@Column(name = "NAME", nullable = false, length = 100)
	private String name;

	@Column(name = "DESCRIPTION", nullable = false, length = 100)
	private String description;

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
	private Source source = null;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "assetGroup")
	private Set<Asset> assets = new HashSet<Asset>();

	public AssetGroup()
	{
		super();
	}

	@XmlElement
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@XmlElement
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@XmlElement
	public Source getSource()
	{
		return source;
	}

	public void setSource(Source source)
	{
		this.source = source;
	}

	public Set<Asset> getAssets()
	{
		return assets;
	}

	public void setAssets(Set<Asset> assets)
	{
		this.assets = assets;
	}
}
