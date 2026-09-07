package com.wiley.permissions.domain.persistence.permissions;

import java.io.Serializable;

import javax.persistence.Column;

public class AuthorCommonWorkPK
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Column(name="USER_ID")	
	private Integer userId = null;

	@Column(name="CW_ID")	
	private Integer cwId = null;

	public AuthorCommonWorkPK() {
		super();
	}

	public AuthorCommonWorkPK(int userId, int cwId) {
	    setUserId(userId);
	    setCwId(cwId);
	}

	public AuthorCommonWorkPK(User user, CommonWork cw) {
	    setUserId(user.getId());
	    setCwId(cw.getId());
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


	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof AuthorCommonWorkPK)) return false;
		AuthorCommonWorkPK other = (AuthorCommonWorkPK) obj;
		// use getters due to JPA proxies (not completely sure if necessary in this case)
		if (getCwId() == null) {
			if (other.getCwId() != null)
				return false;
		} else if (!getCwId().equals(other.getCwId()))
			return false;
		if (getUserId() == null) {
			if (other.getUserId() != null)
				return false;
		} else if (!getUserId().equals(other.getUserId()))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// use getters due to JPA proxies (not completely sure if necessary in this case)
		result = prime * result + ((getCwId() == null) ? 0 : getCwId().hashCode());
		result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
		return result;
	}
}
