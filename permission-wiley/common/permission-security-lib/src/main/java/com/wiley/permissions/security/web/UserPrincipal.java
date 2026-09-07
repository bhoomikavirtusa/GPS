package com.wiley.permissions.security.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 *
 * @author ttidwell
 */
public class UserPrincipal
implements Authentication /*implements Principal*/
{
	private static final long serialVersionUID = 1L;

	private Integer id = null;

	private String uniqueName;
	private String firstName;
	private String lastName;
	private String email;

	private String customFilter;
	private int groupId = 0;
	private boolean customMode = false;
	private boolean showRequest = true;
	private boolean authenticated = false;

	private String currencyCode;
	private String countryCode;
	private String dateFormat;

	private Set<PrivilegePrincipal> globalPrivileges = new HashSet<PrivilegePrincipal>();
	private Set<PrivilegePrincipal> cwPrivileges = new HashSet<PrivilegePrincipal>();

	public UserPrincipal() {
	}

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> list = new ArrayList<GrantedAuthority>();
		for (PrivilegePrincipal role : globalPrivileges) {
			list.add(new SimpleGrantedAuthority(role.getCode()));
		}

		for (PrivilegePrincipal role : cwPrivileges) {
			list.add(new SimpleGrantedAuthority(role.getCode()));
		}
		return list;
	}

	public boolean hasPrivilege(String privilegeCode) {
		// lnagy - because the global and common work privileges will not overlap, we can do the test
		// with both here
		for (PrivilegePrincipal rp : globalPrivileges) {
			if (rp.getCode().equals(privilegeCode)) {
				return true;
			}
		}

		for (PrivilegePrincipal rp : cwPrivileges) {
			if (rp.getCode().equals(privilegeCode)) {
				return true;
			}
		}
		return false;
	}

	public String getName() {
		return uniqueName;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Set<PrivilegePrincipal> getGlobalPrivileges() {
		return globalPrivileges;
	}

	public void setGlobalPrivileges(Set<PrivilegePrincipal> privilege) {
		this.globalPrivileges = privilege;
	}

	public Set<PrivilegePrincipal> getCwPrivileges() {
		return cwPrivileges;
	}

	public void setCwPrivileges(Set<PrivilegePrincipal> cwPrivileges) {
		this.cwPrivileges = cwPrivileges;
	}

	public boolean isShowRequest() {
		return showRequest;
	}

	public void setShowRequest(boolean showRequest) {
		this.showRequest = showRequest;
	}

	public boolean isCustomMode() {
		return customMode;
	}

	public void setCustomMode(boolean customMode) {
		this.customMode = customMode;
	}

	public String getCustomFilter() {
		return customFilter;
	}

	public void setCustomFilter(String customFilter) {
		this.customFilter = customFilter;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	@Override
	public Object getCredentials() {
		return "[PROTECTED]";
	}

	@Override
	public Object getDetails() {
		return null;
	}

	@Override
	public Object getPrincipal() {
		return getEmail();
	}

	@Override
	public boolean isAuthenticated() {
		return authenticated;
	}

	@Override
	public void setAuthenticated(boolean authenticated) throws IllegalArgumentException
	{
		this.authenticated = authenticated;
	}

	@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(": ");
        sb.append("Principal: ").append(this.getPrincipal()).append("; ");
        sb.append("Credentials: [PROTECTED]; ");
        sb.append("Authenticated: ").append(this.isAuthenticated()).append("; ");
        sb.append("Details: ").append(this.getDetails()).append("; ");
        Collection<GrantedAuthority> authorities = getAuthorities();
        if (!authorities.isEmpty()) {
            sb.append("Granted Authorities: ");
            int i = 0;
            for (GrantedAuthority authority: authorities) {
                if (i++ > 0) {
                    sb.append(", ");
                }
                sb.append(authority);
            }
        } else {
            sb.append("Not granted any authorities");
        }
        return sb.toString();
    }
}
