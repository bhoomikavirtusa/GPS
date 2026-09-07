package com.wiley.permissions.services;

import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.xml.sax.InputSource;

import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.permissions.common.transformer.JAXBContextProvider;

/**
 * This client uses web services to make changes to the ldap information
 * for external users. The ldap protocol is NOT used because it is Wiley's
 * policy NOT to update ldap information in that way.
 *
 * @author smarkoff
 */
public class ExternalUserLdapClient {

	private final static Log log = LogFactory.getLog(ExternalUserLdapClient.class);

	// For now hard coded, can wire via Spring later
	private final String URI = "https://dirsvc.wiley.com:8443/cgi/customer";


	public ExternalUserLdapClient() {
	}

	/**
	 *
	 * @param email  Must be non-blank
	 * @param firstName  May be null
	 * @param lastName   May be null
	 * @param password   If null, then generated password returned.
	 * @throws Exception  If return status not successful or any other problem
	 * @return generated password if none was given in input
	 */
	public String createAccount(String email, String firstName, String lastName, String password) throws Exception
	{
		ArgUtil.notBlank(email, "email");

		// My first thought was to use post.get/setParams (see commented out
		// code in deleteAccount() below) but this doesn't seem to work.
		// - Neither does creating a List<NameValuePair> as follows
		// - Simon Finch seemed to suggest that the server handling these
		// requests cannot handle the parameters unless the content-type
		// is "multipart/form-data" but I tried messing with this and didn't
		// work either - more because of issues with HttpClient.
		// What does work is to send params as queryString.

		/*
		HttpPost post = new HttpPost(URI);
        List<NameValuePair> nvpList = new ArrayList<NameValuePair>();
        nvpList.add(new BasicNameValuePair("action", "create_account"));
        nvpList.add(new BasicNameValuePair("mail", email));

		if (StringUtils.isNotBlank(firstName)) {
			nvpList.add(new BasicNameValuePair("first_name", firstName));
		}
		if (StringUtils.isNotBlank(lastName)) {
			nvpList.add(new BasicNameValuePair("last_name", lastName));
		}
		if (StringUtils.isNotBlank(password)) {
			nvpList.add(new BasicNameValuePair("password", password));
		}
		post.setEntity(new CustomUrlEncodedFormEntity(nvpList));
		// causes error - possibly because header already set
		//post.setHeader("Content-Type", "multipart/form-data");
		*/

		StringBuilder sb = new StringBuilder(URI);
		sb.append("?action=create_account&mail=").append(email);
		if (StringUtils.isNotBlank(firstName)) {
			sb.append("&first_name=").append(firstName);
		}
		if (StringUtils.isNotBlank(lastName)) {
			sb.append("&last_name=").append(lastName);
		}
		if (StringUtils.isNotBlank(password)) {
			sb.append("&password=").append(password);
		}
		HttpPost post = new HttpPost(sb.toString());

        String responseBody = execute(post); // throws Exception
        log.debug("createAccount(): responseBody: " + responseBody);
        SSOResponse response = xmlToObject(responseBody);
        //log.debug("createAccount(): response Object: " + response);
        if (!"success".equalsIgnoreCase(response.getStatus())) {
        	throw new RuntimeException("createAccount not successful: " + response.getMessage());
        }
        return response.getPassword();  // may be null
	}

	private String execute(HttpUriRequest request) throws Exception {
		//HttpClient client = new DefaultHttpClient();
		HttpClient client = getTestHttpClient();
		try {
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = client.execute(request, responseHandler); // throws ClientProtocolException, IOException
			return responseBody;
		}
		finally {
			client.getConnectionManager().shutdown();
		}
	}

	// -- for HttpClient 4.1.x and 4.2.x
	private HttpClient getTestHttpClient() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
		SSLSocketFactory sf = new SSLSocketFactory(new TrustStrategy() {
	     	@Override
	        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	            return true;
	        }
	    });
	    SchemeRegistry registry = new SchemeRegistry();
	    registry.register(new Scheme("https", 8443, sf));
	    ClientConnectionManager ccm = new BasicClientConnectionManager(registry);
	    return new DefaultHttpClient(ccm);
	}

	/*
	// -- for HttpClient 4.0.x
	private HttpClient getTestHttpClient() throws Exception {
		SSLContext sslContext = SSLContext.getInstance("SSL");

		// set up a TrustManager that trusts everything
		sslContext.init(null, new TrustManager[] { new X509TrustManager() {
		            public X509Certificate[] getAcceptedIssuers() {
		                    //System.out.println("getAcceptedIssuers =============");
		                    return null;
		            }

		            public void checkClientTrusted(X509Certificate[] certs,
		                            String authType) {
		                    //System.out.println("checkClientTrusted =============");
		            }

		            public void checkServerTrusted(X509Certificate[] certs,
		                            String authType) {
		                    //System.out.println("checkServerTrusted =============");
		            }
		} }, new SecureRandom());

		SSLSocketFactory sf = new SSLSocketFactory(sslContext);
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("https", sf, 8443));

		ClientConnectionManager ccm = new SingleClientConnManager(null, registry);
		return new DefaultHttpClient(ccm, null);
	}*/

	/**
	 *
	 * @param email  Must be non-blank
	 * @param firstName  May be null
	 * @param lastName   May be null
	 * @throws Exception  If return status not successful or any other problem
	 */
	public void modifyAccount(String email, String firstName, String lastName) throws Exception {
		ArgUtil.notBlank(email, "email");

		StringBuilder sb = new StringBuilder(URI);
		sb.append("?action=modify_account&mail=").append(email);
		if (StringUtils.isNotBlank(firstName)) {
			sb.append("&first_name=").append(firstName);
		}
		if (StringUtils.isNotBlank(lastName)) {
			sb.append("&last_name=").append(lastName);
		}
		HttpPost post = new HttpPost(sb.toString());

        String responseBody = execute(post); // throws Exception
        log.debug("modifyAccount(): responseBody: " + responseBody);
        SSOResponse response = xmlToObject(responseBody);
        //log.debug("createAccount(): response Object: " + response);
        if (!"success".equalsIgnoreCase(response.getStatus())) {
        	throw new RuntimeException("modifyAccount not successful: " + response.getMessage());
        }
	}

	/**
	 *
	 * @param email  Must be non-blank
	 * @throws Exception  If return status not successful or any other problem
	 */
	public void deleteAccount(String email) throws Exception {
		ArgUtil.notBlank(email, "email");

		/*
		HttpPost post = new HttpPost(URI);
		post.setHeader("content-type", "application/x-www-form-urlencoded");
		post.setHeader("content-type", "multipart/form-data");

		HttpParams params = post.getParams();
		params.setParameter("action", "delete_account");
		params.setParameter("mail", email);
		*/

		StringBuilder sb = new StringBuilder(URI);
		sb.append("?action=delete_account&mail=").append(email);
		HttpPost post = new HttpPost(sb.toString());

		String responseBody = execute(post); // throws ClientProtocolException, IOException
        log.debug("deleteAccount(): responseBody: " + responseBody);
        SSOResponse response = xmlToObject(responseBody);
        //log.debug("createAccount(): response Object: " + response);
        if (!"success".equalsIgnoreCase(response.getStatus())) {
        	throw new RuntimeException("deleteAccount not successful: " + response.getMessage());
        }
	}

	/**
	 *
	 * @param email  Must be non-blank
	 * @param password   If null, then generated password returned.
	 * @throws Exception  If return status not successful or any other problem
	 */
	public String setPassword(String email, String password) throws Exception {
		ArgUtil.notBlank(email, "email");

		StringBuilder sb = new StringBuilder(URI);
		sb.append("?action=set_password&mail=").append(email);
		if (StringUtils.isNotBlank(password)) {
			sb.append("&password=").append(password);
		}
		HttpPost post = new HttpPost(sb.toString());

        String responseBody = execute(post); // throws Exception
        log.debug("setPassword(): responseBody: " + responseBody);
        SSOResponse response = xmlToObject(responseBody);
        //log.debug("createAccount(): response Object: " + response);
        if (!"success".equalsIgnoreCase(response.getStatus())) {
        	throw new RuntimeException("setPassword not successful: " + response.getMessage());
        }
        return response.getPassword();  // may be null
	}

	/*
	class CustomUrlEncodedFormEntity extends UrlEncodedFormEntity {
		public CustomUrlEncodedFormEntity(List<? extends NameValuePair> parameters)
				throws UnsupportedEncodingException {
			super(parameters);
		}

		@Override
		public Header getContentType() {
			// override normal value of "application/x-www-form-urlencoded"
			return new BasicHeader("content-type", "multipart/form-data");
		}
	}*/

	public static SSOResponse xmlToObject(String xml)
    	throws JAXBException
    {
		JAXBContext jaxbContext = JAXBContextProvider.getContext(SSOResponse.class); //JAXBContext.newInstance(SSOResponse.class);
			// throws JAXBException
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			// throws JAXBException
		SSOResponse response = (SSOResponse)
			unmarshaller.unmarshal(new InputSource(new StringReader(xml)));
			// throws JAXBException
		return response;
    }
}

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="response")
class SSOResponse {
	public String status;
	public String message;
	public String password;

	@XmlElement
	public String getStatus() { return status; }
	@XmlElement
	public String getMessage() { return message; }
	@XmlElement
	public String getPassword() { return password; }

	public void setStatus(String s) { status = s; }
	public void setMessage(String s) { message = s; }
	public void setPassword(String s) { password = s; }

	@Override
	public String toString() {
		return "status = [" + status + "]\r\nmessage [" + message + "]\r\npassword [" + password + "]";
	}
}
