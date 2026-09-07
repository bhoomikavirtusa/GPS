package com.wiley.permissions.domain.persistence.permissions;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.security.web.ThreadLocalUser;
import com.wiley.permissions.security.web.UserPrincipal;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name="AUTHOR_2_CW")
@IdClass(AuthorCommonWorkPK.class)
public class AuthorToCommonWork extends DomainObject {
	private static final long serialVersionUID = 1L;

	private final static Log log = LogFactory.getLog(AuthorToCommonWork.class);

	@Id
	private Integer userId = null;

	@Id
	private Integer cwId = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "DUE_DATE", nullable = true)
	private Date dueDate = new Date();

	@Column(name = "READ_ONLY", nullable = false)
	private boolean readOnly = false;

	@Column(name = "PERMISSION_COMPLETE", nullable = false)
	private boolean permissionComplete = false;

	@ManyToOne
	@JoinColumn(name = "USER_ID" , insertable=false, updatable=false)
	private User user = null;

	@ManyToOne
	@JoinColumn(name = "CW_ID", insertable=false, updatable=false)
	private CommonWork commonWork = null;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "CREATED_USER_ID")
	private User createdUser = User.SYSTEM;

	@PrePersist
	public void prePersist() {
		log.debug("prePersist() called for class " + getClass().getSimpleName());
		reconcileUsers();
	}

	@PreUpdate
	public void preUpdate() {
		log.debug("preUpdate() called for class " + getClass().getSimpleName());
		reconcileUsers();
	}

	/**
	 * We're basically using this to make sure that there is a last updated user
	 * in the audit trail.  It tries to find one from the transaction resources
	 * and if it can't, it defaults to the system permissions user.
	 */
	public void reconcileUsers() {
		if (createdUser.equals(User.SYSTEM)) {
			try {
				User user = new User();
				UserPrincipal userPrincipal = ThreadLocalUser.get();
				Integer userId = null;
				if (null != userPrincipal) {
					userId = userPrincipal.getId();
				}
				log.debug("reconcileUsers(): ThreadLocalUser.get() = " + userId);
				log.debug("reconcileUsers(): currentThread.id = " + Thread.currentThread().getId());

				if (userId == null) {
					user.setId(1);  // Permissions System user
				}
				else {
					user.setId(userId);
				}

				setCreatedUser(user);
			}
			catch (Exception e) {
				log.error("Error reconciling user: ", e);
			}
		}
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getCwId() {
		return cwId;
	}

	public void setCwId(Integer cwId) {
		this.cwId = cwId;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public CommonWork getCommonWork() {
		return commonWork;
	}

	public void setCommonWork(CommonWork commonWork) {
		this.commonWork = commonWork;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isPermissionComplete() {
		return permissionComplete;
	}

	public void setPermissionComplete(boolean permissionComplete) {
		this.permissionComplete = permissionComplete;
	}

	public User getCreatedUser() {
		return createdUser;
	}

	public void setCreatedUser(User createdUser) {
		this.createdUser = createdUser;
	}

	// Base hashCode() and equals only on assetId and SourceId
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		result = prime * result
				+ ((cwId == null) ? 0 : cwId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof AuthorToCommonWork)) return false;
		AuthorToCommonWork other = (AuthorToCommonWork) obj;
		if (userId == null) {
			if (other.userId != null) return false;
		}
		else if (!userId.equals(other.userId)) return false;
		if (cwId == null) {
			if (other.cwId != null) return false;
		}
		else if (!cwId.equals(other.cwId)) return false;
		return true;
	}
}
