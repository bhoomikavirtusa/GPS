package com.wiley.permissions.domain.persistence.permissions;


import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.hibernate.annotations.Where;

import com.wiley.permissions.domain.DomainObject;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name="USER_GROUP")
@NamedQueries ({
	@NamedQuery(
	    name="UserGroup.findByName",
	    query="from UserGroup where name like ?1"
	),
	@NamedQuery(
		    name="UserGroup.findById",
		    query="from UserGroup where id = ?1"
	)
})
public class UserGroup
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	public static final UserGroup
		GE_PHOTO_EDITOR = new UserGroup("GE Photo Editors", "GE Photo Editors"),
		CREATIVE_SERVICES = new UserGroup("Creative Services", "Creative Services US PD"),
		AUTHOR_GROUP = new UserGroup("Authors", "Authors"),
		FREELANCER_GROUP = new UserGroup("Freelancers", "Freelancers"),
		PD_GROUP = new UserGroup("PD Editorial", "PD Editorial"),
		GE_GROUP = new UserGroup("GE Editorial", "GE Editorial"),
		GR_GROUP = new UserGroup("GR Editorial", "GR Editorial"),
		CORPORATE_GROUP = new UserGroup("Corporate", "Corporate"),
	    AUS_GROUP = new UserGroup("AUS Editorial", "Australia Editorial");

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@Column(name="NAME", nullable=false, length=20, unique=true)
	private String name = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 200)
	private String description = null;

	@OneToMany(mappedBy="group")
	// smarkoff: added Where clause so that when user goes to Manage User Groups and to Authors group
	// doesn't try to load huge number of records that we don't care about because they are not users
	// that actually login to the system (but main issue here is performance)
	// no other group besides Authors has user will null email
	@Where(clause="email is not null")
	private List<User> users;

	public UserGroup() {
	}

	public UserGroup(String name) {
		setName(name);
	}

	public UserGroup(String name, String description) {
		setName(name);
		setDescription (description);
	}

	@XmlID
	@XmlElement
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElement
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<User> getUsers() {
		return users;
	}

	@Transient
	public List<User> getUsersNotNull() {
		if (users == null) {
			users = new ArrayList<User>();
		}
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}

	public static UserGroup getInstance (String ldapGroupName) {
		if (ldapGroupName == null)  return null;

		if (ldapGroupName.equalsIgnoreCase("Professional Development")) {
			return UserGroup.PD_GROUP;
		} else if (ldapGroupName.equalsIgnoreCase("Global Education")) {
			return UserGroup.GE_GROUP;
		} else if (ldapGroupName.equalsIgnoreCase("Scientific, Technical, Medical, and Scholarly")) {
			return UserGroup.GR_GROUP;
		// Global Research is the new name for STMS (Scientific, Technical, Medical, and Scholarly)
		} else if (ldapGroupName.equalsIgnoreCase("Global Research")) {
			return UserGroup.GR_GROUP;
		} else if (ldapGroupName.equalsIgnoreCase("Australia")) {
			return UserGroup.AUS_GROUP;
		} else if (ldapGroupName.equalsIgnoreCase("Corporate")) {
			return UserGroup.CORPORATE_GROUP;
		}

		return null;
	}
}
