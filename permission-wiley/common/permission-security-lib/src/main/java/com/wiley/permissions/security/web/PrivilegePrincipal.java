package com.wiley.permissions.security.web;

import java.security.Principal;

/**
 *
 * @author ttidwell
 */
public class PrivilegePrincipal
implements Principal
{
	private String code = null;
	private Integer productId = null;
	
	public Integer getProductId()
	{
		return productId;
	}

	public void setProductId(Integer productId)
	{
		this.productId = productId;
	}

	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}
	
	public String getName()
	{
		return code;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result + ((productId == null) ? 0 : productId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PrivilegePrincipal other = (PrivilegePrincipal) obj;
		if (code != null ? !code.equals(other.code) : other.code != null) 
			return false;		
		if (productId != null ? !productId.equals(other.productId) : other.productId != null) 
			return false;
		return true;
	}
}
