package com.wiley.permissions.domain.persistence.permissions;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 *
 * @author ttidwell
 * @version 1.0
 */
@Embeddable
public class SourceAddressPK
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@ManyToOne(
		cascade=
		{
			CascadeType.MERGE,
			CascadeType.PERSIST,
			CascadeType.REFRESH
		},
		fetch = FetchType.EAGER
	)
	@JoinColumn(name = "SOURCE_ID", nullable = false)
	private Source source = null;

	@ManyToOne(
		cascade=
		{
			CascadeType.MERGE,
			CascadeType.PERSIST,
			CascadeType.REFRESH
		},
		fetch = FetchType.EAGER
	)
	@JoinColumn(name = "ADDRESS_ID", nullable = false)
	private Address address = null;
	
	public SourceAddressPK()
	{
		
	}

	public SourceAddressPK(Integer sourceId, Integer addressId) {
		source = new Source();
		source.setId(sourceId);
		
		address = new Address();
		address.setId(addressId);
    }

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}

		final SourceAddressPK other = (SourceAddressPK) obj;

		if (this.source != other.source
				&& (this.source == null || !this.source.getId().equals(other.source.getAddresses())))
		{
			return false;
		}
		
		if (this.address != other.address && (this.address == null || !this.address.getId().equals(other.address.getId())))
		{
			return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 29 * hash + (this.source != null ? this.source.hashCode() : 0);
		hash = 29 * hash + (this.address != null ? this.address.hashCode() : 0);
		return hash;
	}

	public Source getSource() {
    	return source;
    }

	public void setSource(Source source) {
    	this.source = source;
    }

	public Address getAddress()
	{
		return address;
	}

	public void setAddress(Address address)
	{
		this.address = address;
	}
}