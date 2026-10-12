package com.wiley.permissions.camel.web.processor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Builds the CompCopy HTTP GET URL matching Mule {@code http:rest-service-component}
 * query shape in services-mule-config.xml.
 */
public class CompCopyUrlProcessor implements Processor {

	public static final String HEADER_URL = "CompCopyUrl";

	private final String baseUrl;
	private final boolean australia;

	public CompCopyUrlProcessor(String baseUrl, boolean australia) {
		this.baseUrl = baseUrl;
		this.australia = australia;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		Object body = exchange.getIn().getBody();
		if (!(body instanceof Map)) {
			throw new IllegalArgumentException("CompCopy payload must be a Map, was: "
					+ (body == null ? "null" : body.getClass().getName()));
		}
		Map<String, Object> params = (Map<String, Object>) body;
		StringBuilder url = new StringBuilder(baseUrl);
		append(url, "isbn10(a0100)", params.get("isbn10"));
		if (australia) {
			append(url, "account13(a0300)", params.get("acctno"));
		}
		append(url, "fname(a0300)", params.get("fname"));
		append(url, "lname(a0300)", params.get("lname"));
		append(url, "street1(a0300)", params.get("street1"));
		append(url, "street2(a0300)", params.get("street2"));
		append(url, "city(a0300)", params.get("city"));
		append(url, "state(a0300)", params.get("state"));
		append(url, "isocntry(a0040)", params.get("countrycode"));
		append(url, "postcode(a0090)", params.get("postcode"));
		if (!australia) {
			append(url, "vendctry(a0020)", params.get("countrycode"));
		}
		exchange.getIn().setHeader(HEADER_URL, url.toString());
		exchange.getIn().setBody(null);
	}

	private static void append(StringBuilder url, String field, Object value)
			throws UnsupportedEncodingException {
		url.append('+').append(field).append(':');
		if (value != null) {
			url.append(URLEncoder.encode(String.valueOf(value), "UTF-8"));
		}
	}
}
