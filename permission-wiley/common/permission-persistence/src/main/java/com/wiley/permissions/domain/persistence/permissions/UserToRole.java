package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

/**
 * lnagy - if you might want to try to not use redundancy, please check this
 * article:
 * http://sieze.wordpress.com/2009/09/04/mapping-a-many-to-many-join-table
 * -with-extra-column-using-jpa/
 *
 * @author ttidwell
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "USER_2_ROLE", uniqueConstraints = @UniqueConstraint(columnNames={"USER_ID", "ROLE_ID", "PRODUCT_ID"}))
public class UserToRole extends DomainObject {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@ManyToOne (cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinColumn(name = "USER_ID")
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private User user;

	@ManyToOne (cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinColumn(name = "PRODUCT_ID", referencedColumnName = "ID", nullable = true)
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private Product product;

	@ManyToOne (cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinColumn(name = "ROLE_ID", referencedColumnName = "ID")
	@MaterializationKey(mode = MaterializationKey.Mode.ADDITIVE)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private Role role;

	public UserToRole()
	{
	}

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	@Transient
	public boolean isAuthor()
	{
		// this works even when role is null (returns false in this case)
		return Role.AUTHOR.equals(role);
	}

	@Transient
	public boolean hasNonAuthorRole()
	{
		return null != role && !role.equals(Role.AUTHOR);
	}

	@XmlElement
	@XmlIDREF
	public User getUser()
	{
		return user;
	}

	public void setUser(User user)
	{
		this.user = user;
	}

	@XmlElement
	@XmlIDREF
	public Product getProduct()
	{
		return product;
	}

	public void setProduct(Product product)
	{
		this.product = product;
	}

	@XmlElement
	public Role getRole()
	{
		return role;
	}

	public void setRole(Role role)
	{
		this.role = role;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id = " + getId() + ", ");
		sb.append("product id = " + ((getProduct() != null) ? getProduct().getId() : "null") + ", ");
		sb.append("user id = " + getUser().getId() + ", ");
		sb.append("role code: " + getRole().getCode());
		return sb.toString();
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
		if (!(obj instanceof UserToRole)) return false;
		UserToRole other = (UserToRole) obj;
		if (id == null) {
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		return true;
	}
}
