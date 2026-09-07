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
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.wiley.permissions.domain.DomainObject;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "CW_PHOTO_ESTIMATE")
public class CwPhotoEstimate
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CW_ID")
	private CommonWork commonWork = null;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "PHOTO_ESTIMATE_TYPE", nullable = false)
	private PhotoEstimateType photoEstimateType = null;

	@Column(name = "ESTIMATED_VALUE", nullable = false)
	private Double estimatedValue;

	@Column(name = "REPRODUCTION_FEES", nullable = false)
	private Double reproductionFees;

	@Column(name = "RESEARCH_FEES", nullable = false)
	private Double researchFees;

	@Column(name = "FREELANCE_RESEARCH_FEES", nullable = false)
	private Double freelanceResearchFees;

	@Column(name = "PHOTOS", nullable = false)
	private Integer photos;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "DATE", nullable = false)
	private Date date = new Date();  // init so web form starts with current date

	public Double getEstimatedValue()
	{
		return estimatedValue;
	}

	public void setEstimatedValue(Double estimatedValue)
	{
		this.estimatedValue = estimatedValue;
	}

	public void setReproductionFees(Double value)
	{
		this.reproductionFees = value;
	}

	public Double getReproductionFees()
	{
		return reproductionFees;
	}

	public void setResearchFees(Double value)
	{
		this.researchFees = value;
	}

	public Double getResearchFees()
	{
		return researchFees;
	}

	public void setFreelanceResearchFees(Double value)
	{
		this.freelanceResearchFees = value;
	}

	public Double getFreelanceResearchFees()
	{
		return freelanceResearchFees;
	}

	public void setPhotos(Integer value)
	{
		this.photos = value;
	}

	public Integer getPhotos()
	{
		return photos;
	}

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public PhotoEstimateType getPhotoEstimateType()
	{
		return photoEstimateType;
	}

	public void setPhotoEstimateType(PhotoEstimateType photoEstimateType)
	{
		this.photoEstimateType = photoEstimateType;
	}

	public CommonWork getCommonWork()
	{
		return commonWork;
	}

	public void setCommonWork(CommonWork commonWork)
	{
		this.commonWork = commonWork;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return date;
	}
}
