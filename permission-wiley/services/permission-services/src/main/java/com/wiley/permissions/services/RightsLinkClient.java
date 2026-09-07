package com.wiley.permissions.services;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.mule.api.lifecycle.InitialisationException;

import com.wiley.permissions.common.transformer.JsonToObject;
import com.wiley.permissions.common.transformer.TransformationException;
import com.wiley.sf.common.lang.ArgUtil;

/**
 * This class is designed so a single instance can be used by multiple threads (it is threadsafe).
 * It is probably more efficient this way despite the synchronization overhead since
 * multiple threads will share the same access token, and new tokens will have be
 * requested less frequently.
 *
 * This class is basically an implementation of OAuth (version 2) which is
 * what rights link uses - the only rightLink-specific thing here really is
 * the license URL - this class could probably be modified to be a more generic
 * OAuth class and the License URL passed in (or course there are plenty of other OAuth
 * libraries out there).
 *
 * @author smarkoff
 */
public class RightsLinkClient {

	private final static Log log = LogFactory.getLog(RightsLinkClient.class);

	// Wired by Spring
	private String tokenUri;
	private String licenseUri;
	private String username;
	private String password;
	private String consumerKey;
	private String consumerSecret;
	private String scope;

	private volatile RightsLinkResponse lastTokens = null;

	public RightsLinkClient() {
	}

	// Note it's possible that 2 threads may both check that (lastTokens == null)
	// at the same time and then both generated new tokens. I see no problem with
	// this - better than putting more synchronization that will cause things
	// to act more single-threaded. It is important that lastTokens is marked
	// volatile (above) since are using using synchronization blocks.

	/**
	 * Returns a StatusCodeAndContent object.
	 * We expect the statusCode to be either 200, in which case the content is the license XML
	 * or 404 which means the license number is not valid.
	 * (For a 401 we thrown an exception, and other statusCodes are certainly possible but not expected.)
	 *
	 * @param licenseNumber  Must be non-blank
	 */
	public StatusCodeAndContent getLicenseXml(String licenseNumber) throws Exception {
		ArgUtil.notBlank(licenseNumber, "licenseNumber");
		if (lastTokens == null) {
			log.debug("getLicenseXml(): No tokens stored - will request.");
			lastTokens = getTokens(null);
			return getLicenseXmlWithNewTokens(licenseNumber);
		}
		else {
			log.debug("getLicenseXml(): Had tokens stored - will try.");
			StatusCodeAndContent scac = getLicenseXml(licenseNumber, lastTokens);
			if (scac.getStatusCode() == 401) {
				log.debug("getLicenseXml(): Stored token was expired.");
				lastTokens = getTokens(lastTokens);
				log.debug("getLicenseXml(): Re-requesting license with new token.");
				scac = getLicenseXmlWithNewTokens(licenseNumber);
			}
			return scac;
		}
	}

	private StatusCodeAndContent getLicenseXmlWithNewTokens(String licenseNumber) throws Exception {
		StatusCodeAndContent scac = getLicenseXml(licenseNumber, lastTokens);
		if (scac.getStatusCode() == 401) {
			throw new RuntimeException("We just refreshed tokens but still no good - some error must have occurred.");
		}
		return scac;
	}

	/**
	 * Returns a StatusCodeAndContent object.
	 * If the statusCode == 200 then the content will be the license XML.
	 * 401 means the access token has expired.
	 * 404 means the license number was not found.
	 *
	 * An exception may be thrown for other (unexpected or not normal) situations.
	 *
	 * @param licenseNumber  Must be non-blank
	 */
	private StatusCodeAndContent getLicenseXml(String licenseNumber, RightsLinkResponse rlTokens) throws Exception {
		ArgUtil.notBlank(licenseNumber, "licenseNumber");

		// Not Using Mule for this anymore since Mule throws exception when get back non-200 response
		// and we need the response body for 3xx and 4xx to know what the error is - especially
		// if the access_token has expired which is a "normal" response (but a 401)
		/*
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(licenseNumber, "licenseNumber");
		// If the license is not valid then this next line throws an exception
		// - RightsLink returns a 302 (redirect) but Mule counts it as an error
		String xml = (String) getServiceDispatcher().send(OperationType.GET_RIGHTS_LINK_LICENSE, params, null);
		*/

		StringBuilder sb = new StringBuilder(licenseUri);
		sb.append(licenseNumber).append(".xml");
		HttpGet get = new HttpGet(sb.toString());
		get.setHeader("Authorization", "Bearer " + rlTokens.getAccess_token());
		// get.toString() doesn't include headers so log these also
		log.debug("getLicenseXml(): requesting: " + get + "\r\n" + formatHeadersForPrintout(get.getAllHeaders()));

		MyResponse myResponse = execute(get); // throws Exception
        HttpResponse httpResponse = myResponse.getHttpResponse();
        log.debug("getLicenseXml(): response statusLine: " + httpResponse.getStatusLine());
        //log.debug("getToken(): response contentLength: " + httpResponse.getEntity().getContentLength());
        log.debug("getLicenseXml(): response headers: \r\n" + formatHeadersForPrintout(httpResponse.getAllHeaders()));
        String content = myResponse.getContent();
        log.debug("getLicenseXml(): response content: " + content);
        int statusCode = httpResponse.getStatusLine().getStatusCode();

        // For license number 3299561377256 in PRODUCTION scope for example, we get some special
        // chars (decimal values 164, 168, 182, 184, 195)
        // But there is nothing to fix - they display as they should - but clearly
        // they are garbage chars - maybe originally they were some unicode chars but they
        // got split into 2 chars for each 2 byte pair -- whatever - we cannot fix the problem here.
        // RightsLink needs to fix the problem at the source - maybe send us the same data but
        // with a UTF-8 charset instead of ISO 8859-1.
        //StringBuilder newContent = new StringBuilder(content.length());
        int specialCharCount = 0;
        for (int i = 0; i < content.length(); i++) {
        	char c = content.charAt(i);
        	if (c < 32 || c > 126) {
        		specialCharCount++;
        		log.debug("char at index " + i + " dec = " + (int)c + ", char = " + c);
        		//newContent.append("&#").append((int)c).append(';');  // &#xxx;
        	}
        	//else {
        		//newContent.append(c);
        	//}
        }
        //content = newContent.toString();
        log.debug("getLicenseXml(): specialCharCount = " + specialCharCount);
        //if (specialCharCount > 0) {
        //	log.debug("getLicenseXml(): revised content: " + content);
        //}

        return new StatusCodeAndContent(statusCode, content);
	}

	/**
	 * This method is only for testing.
	 */
	public String getAccessToken() throws Exception {
		return getTokens(null).getAccess_token();
	}

	/**
	 * Returns new tokens. Will thrown an exception if there was an error getting the tokens.
	 *
	 * @param refreshResponse  May be null which means we don't have a refreshToken to use
	 */
	private RightsLinkResponse getTokens(RightsLinkResponse refreshResponse) throws Exception {
		StringBuilder sb = new StringBuilder(tokenUri);
		if (refreshResponse == null) {
			sb.append("?grant_type=password");
			sb.append("&username=").append(username);
			sb.append("&password=").append(password);
		}
		else {
			sb.append("?grant_type=refresh_token");
			sb.append("&refresh_token=").append(refreshResponse.refresh_token);
		}
		sb.append("&scope=").append(scope);
		HttpPost post = new HttpPost(sb.toString());
		// following not needed - this is the default content-type anyway
		post.setHeader("content-type", "application/x-www-form-urlencoded");
		String authValue = consumerKey + ":" + consumerSecret;
		authValue = Base64.encodeBase64String(authValue.getBytes());
		post.setHeader("authorization", "Basic " + authValue);
		// post.toString() doesn't include headers so log these also
		log.debug("getTokens(): requesting: " + post + "\r\n" + formatHeadersForPrintout(post.getAllHeaders()));

		MyResponse myResponse = execute(post); // throws Exception
        HttpResponse httpResponse = myResponse.getHttpResponse();
        log.debug("getTokens(): response statusLine: " + httpResponse.getStatusLine());
        //log.debug("getToken(): response contentLength: " + httpResponse.getEntity().getContentLength());
        log.debug("getTokens(): response headers: \r\n" + formatHeadersForPrintout(httpResponse.getAllHeaders()));
        String content = myResponse.getContent();
        log.debug("getTokens(): response content: " + content);

        RightsLinkResponse rlResponse = (RightsLinkResponse) JsonToObject.doTransform(content, RightsLinkResponse.class);
        log.debug("getTokens(): response Object: " + rlResponse);

        if (StringUtils.isNotBlank(rlResponse.getError())) {
        	throw new RuntimeException("getTokens not successful: " + rlResponse.getError() + " - " + rlResponse.getError_description());
        }

        return rlResponse;
	}

	private String formatHeadersForPrintout(Header [] headers) {
		StringBuilder sb = new StringBuilder("Number of headers = ");
		sb.append(headers.length);

		for (Header h : headers) {
			sb.append("\r\n");
			sb.append(h.toString());
		}
		return sb.toString();
	}

	public void testJsonToObject() throws InitialisationException, TransformationException {
		String json = "{\r\n"
			+ "\"token_type\": \"bearer\",\r\n"
			+ "\"expires_in\": 3600,\r\n"
			+ "\"refresh_token\": \"aeb46504b203b105349fba090a72e2a\",\r\n"
			+ "\"access_token\": \"793e882b2244ff2023ba9321e3daa8b\"\r\n"
			+ "}";
        RightsLinkResponse object = (RightsLinkResponse) JsonToObject.doTransform(json, RightsLinkResponse.class);
        log.debug("testJsonToObject(): object: " + object);
	}

	private MyResponse execute(HttpUriRequest request) throws Exception {
		//HttpClient client = new DefaultHttpClient();
		HttpClient client = getTestHttpClient();
		try {
			ResponseHandler<MyResponse> responseHandler = new MyResponseHandler();
			MyResponse response = client.execute(request, responseHandler); // throws ClientProtocolException, IOException
			return response;
		}
		/*
		catch (HttpResponseException ex) {
			log.error("execute(): caught HttpResponseException: statusCode = " + ex.getStatusCode(), ex);
			throw new RuntimeException("Bad Request");
		}
		*/
		finally {
			client.getConnectionManager().shutdown();
		}
	}

	class MyResponseHandler implements ResponseHandler<MyResponse> {
		// from testing I found that once handleResponse() is called
		// we can still call most of the methods on the response object
		// but not getContent() which will return an already closed InputStream
		// - so must read the InputStream here in handleResponse

		@Override
		public MyResponse handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			String content = EntityUtils.toString(response.getEntity());

			return new MyResponse(response, content);
		}
	}

	class MyResponse {
		private final HttpResponse httpResponse;
		private final String content;

		public MyResponse(HttpResponse httpResponse, String content) {
			this.httpResponse = httpResponse;
			this.content = content;
		}

		public HttpResponse getHttpResponse() { return httpResponse; }
		public String getContent() { return content; }
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
        registry.register(new Scheme("https", 443, sf));
        ClientConnectionManager ccm = new BasicClientConnectionManager(registry);
        return new DefaultHttpClient(ccm);
	}

	public void setTokenUri(String s) { tokenUri = s; }
	public void setLicenseUri(String s) { licenseUri = s; }
	public void setUsername(String s) { username = s; }
	public void setPassword(String s) { password = s; }
	public void setConsumerKey(String s) { consumerKey = s; }
	public void setConsumerSecret(String s) { consumerSecret = s; }
	public void setScope(String s) { scope = s; }
}

// example Success response: (as documented)
//{
//	"token_type": "bearer",
//	"expires_in": 3600,
//	"refresh_token": "aeb46504b203b105349fba090a72e2a",
//	"access_token": "793e882b2244ff2023ba9321e3daa8b"
//}
// example Error response (not as documented but this is what I get)
//{
//  "error": "invalid_request",
//  "error_description": "Missing parameters: client_id"
//}

class RightsLinkResponse {
	public String scope;
	public String token_type;
	public Integer expires_in;
	public String refresh_token;
	public String access_token;
	public String error;
	public String error_description;

	public String getScope() { return scope; }
	public String getToken_type() { return token_type; }
	public Integer getExpires_in() { return expires_in; }
	public String getRefresh_token() { return refresh_token; }
	public String getAccess_token() { return access_token; }
	public String getError() { return error; }
	public String getError_description() { return error_description; }

	public void setScope(String s) { scope = s; }
	public void setToken_type(String s) { token_type = s; }
	public void setExpires_in(Integer i) { expires_in = i; }
	public void setRefresh_token(String s) { refresh_token = s; }
	public void setAccess_token(String s) { access_token = s; }
	public void setError(String s) { error = s; }
	public void setError_description(String s) { error_description = s; }

	@Override
	public String toString() {
		return "token_type: " + token_type + ",\r\n"
			+ "expires_in: " + expires_in + ",\r\n"
			+ "refresh_token: " + refresh_token + ",\r\n"
			+ "access_token: " + access_token + ",\r\n"
			+ "error: " + error + ",\r\n"
			+ "error_description: " + error_description + "\r\n"
			+ "scope: "+ scope + "\r\n";
	}
}
