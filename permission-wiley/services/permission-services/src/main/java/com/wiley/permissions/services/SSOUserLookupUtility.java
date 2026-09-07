package com.wiley.permissions.services;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;

import com.wiley.permissions.security.sso.SSOUser;
import com.wiley.sf.common.monitor.PerformanceMonitor;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 *
 * @author ttidwell
 */
public class SSOUserLookupUtility {

	private final static Log log = LogFactory.getLog(SSOUserLookupUtility.class);

	// These all wired by Spring
	private ContextSource contextSource;
	private LdapTemplate ldapTemplate;
	private String ldapBase;
	private PerformanceMonitor monitor;


	public SSOUserLookupUtility() {
	}

	private AndFilter getDefaultAndFilter() {
		AndFilter output = new AndFilter();
		// smarkoff: 1/2013 changed "jwsperson" to just "person" because Wiley Customers users only have "person"
		// (Wiley Users have both "jwsperson" and "person")
		output.and(new EqualsFilter("objectclass", "person"));
		return output;
	}

	public SSOUser findByDN(String dn) {
		SSOUser output = null;
		output = (SSOUser) ldapTemplate.lookup(dn, new SSOUserAttributesMapper());
		return output;
	}

	public SSOUser findByEmail(String email) {
		PerfTimer timer = monitor.startTimer("SSOUserLookupUtility::findByEmail");
		SSOUser output = null;

		AndFilter filter = getDefaultAndFilter();
		filter.and(new EqualsFilter("mail", email));

		//List<SSOUser> results = ldapTemplate.search(ldapBase, filter.encode(), new SSOUserAttributesMapper());
		List<SSOUser> results = searchWithRetry(ldapBase, filter.encode(), new SSOUserAttributesMapper());

		if (results.size() > 0) {
			output = results.get(0);
		}

		timer.stopTimer();
		return output;
	}

	@SuppressWarnings("unchecked")
	private List<SSOUser> searchWithRetry(String base, String filter, ContextMapper mapper) {
		final int maxTries = 3;
		for (int i = 1; i < maxTries; i++) {
			try {
				// Hack for when LDAP server is unreachable due to network issues
				// - also need to change authenticateByEmail() below
				/*
				SSOUser ssoUser = new SSOUser();
				ssoUser.setEmail("smarkoff@wiley.com");
				ssoUser.setFirstName("Steve");
				ssoUser.setLastName("Markoff");
				ssoUser.setLdapDN("CN=Steve Markoff,OU=San Francisco,OU=United States,OU=North America,OU=Wiley Users,DC=wiley,DC=com");
				ssoUser.setUserId("smarkoff");
				List<SSOUser> list = new ArrayList<SSOUser>(1);
				list.add(ssoUser);
				return list;
				*/
				return ldapTemplate.search(base, filter, mapper);
			}
			catch (org.springframework.ldap.ServiceUnavailableException ex) {
				log.info("searchWithRetry(): caught ServiceUnavailableException on try "
						+ i + ", will retry after 5 seconds...");

				try { Thread.sleep(5000); }
				catch (InterruptedException ex2) { }
			}
		}

		return ldapTemplate.search(base, filter, mapper);
	}

	public SSOUser findByUsername(String username) {
		SSOUser output = null;

		AndFilter filter = getDefaultAndFilter();
		filter.and(new EqualsFilter("uid", username));

		@SuppressWarnings("unchecked")
		List<SSOUser> results = ldapTemplate.search(ldapBase, filter.encode(), new SSOUserAttributesMapper());

		if (results.size() > 0) {
			output = results.get(0);
		}

		return output;
	}

	public List<SSOUser> findByFullName(String firstName, String lastName) {
		AndFilter filter = getDefaultAndFilter();
		filter.and(new EqualsFilter("givenname", firstName));
		filter.and(new EqualsFilter("sn", lastName));

		@SuppressWarnings("unchecked")
		List<SSOUser> output = ldapTemplate.search(ldapBase, filter.encode(), new SSOUserAttributesMapper());

		return output;
	}

	public List<SSOUser> findByFirstName(String firstName) {
		AndFilter filter = getDefaultAndFilter();
		filter.and(new EqualsFilter("givenname", firstName));
		@SuppressWarnings("unchecked")
		List<SSOUser> output = ldapTemplate.search(ldapBase, filter.encode(), new SSOUserAttributesMapper());
		return output;
	}

	public List<SSOUser> findByLastName(String lastName) {
		AndFilter filter = getDefaultAndFilter();
		filter.and(new EqualsFilter("sn", lastName));
		@SuppressWarnings("unchecked")
		List<SSOUser> output = ldapTemplate.search(ldapBase, filter.encode(), new SSOUserAttributesMapper());
		return output;
	}

	/**
	 * Returns whether the user authenticates against ldap using the given email
	 * and password. Might be good to call findByEmail() before calling this method
	 * so that if this method returns false you can distinguish between a user that
	 * not exist in ldap (email) vs a bad password.
	 *
	 * @param email  Must be non-null
	 * @param password  Must be non-null
	 * @return  Whether the authentication was successful
	 */
	public boolean authenticateByEmail(String email, String password) {
		log.debug("authenticateByEmail(): email = " + email);
		AndFilter filter = getDefaultAndFilter();
		filter.and(new EqualsFilter("mail", email));

		// Hack for when network down and ldap unreachable - also need to change searchWithRetry() above
		//return true;
		return ldapTemplate.authenticate(ldapBase, filter.encode(), password);
	}

	public ContextSource getContextSource() {
		return contextSource;
	}

	public void setContextSource(ContextSource contextSource) {
		this.contextSource = contextSource;
	}

	public LdapTemplate getLdapTemplate() {
		return ldapTemplate;
	}

	public void setLdapTemplate(LdapTemplate ldapTemplate) {
		this.ldapTemplate = ldapTemplate;
	}

	public String getLdapBase() {
		return ldapBase;
	}

	public void setLdapBase(String ldapBase) {
		this.ldapBase = ldapBase;
	}

	public PerformanceMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(PerformanceMonitor monitor) {
		this.monitor = monitor;
	}

	private class SSOUserAttributesMapper
	implements ContextMapper
	{
		@Override
		public Object mapFromContext(Object obj) {
			DirContextAdapter context = (DirContextAdapter) obj;
			Attributes attributes = context.getAttributes();
			SSOUser output = new SSOUser();

			try {
				output.setLdapDN(context.getDn().toString());

				if (attributes.get("uid") != null) {
					output.setUserId((String) attributes.get("uid").get());
				}

				if (attributes.get("mail") != null) {
					output.setEmail((String) attributes.get("mail").get());
				}

				if (attributes.get("givenname") != null) {
					output.setFirstName((String) attributes.get("givenname").get());
				}

				if (attributes.get("sn") != null) {
					output.setLastName((String) attributes.get("sn").get());
				}

				String location = null;
				if (attributes.get("co") != null  ) {
					location = (String) attributes.get("co").get();
				}
				// for australia users we use location to figure out group
				if(null != location && location.compareTo("Australia") == 0 ) {
					output.setLdapGroupName(location);
				} else {
					if (attributes.get("jwsgroupname") != null) {
						output.setLdapGroupName((String) attributes.get("jwsgroupname").get());
					}
				}
				
			

				// smarkoff: (from observation) cn is reliably fullName for
				// employees but not for authors so we don't use it anymore
			}
			catch (NamingException e) {
				log.warn("mapFromContext(): caught exception: ", e);
				output = null;
			}

			return output;
		}
	}
}
