package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "user_profile")
@NamedQueries ({
	@NamedQuery(
	    name="UserProfile.findByUserIdAndName",
	    query="from UserProfile profile where " +
	    		"profile.user.id = :id and profile.name = :name "
	)
})
public class UserProfile
extends DomainObject {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@MaterializationKey
	private Integer id = null;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_ID")
	@MaterializationKey
	private User user;

	@NotNull
	@Size (max=50)
	private String name;

	@NotNull
	@Size (max=100)
	private String title;

	private boolean visible;
	private boolean required;
	private boolean sortable;
	private boolean searchable;
	private boolean editable;
	
	@Size (max=25)
	@Column(name="SORT_TYPE")
	private String sortType;

	private int width; 
	
	@Column(name="SORT_ORDER")
	private int sortOrder;

	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isSortable() {
		return sortable;
	}

	public void setSortable(boolean sortable) {
		this.sortable = sortable;
	}

	public boolean isSearchable() {
		return searchable;
	}

	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getSortType() {
		return sortType;
	}

	public void setSortType(String stype) {
		this.sortType = stype;
	}
	
}
