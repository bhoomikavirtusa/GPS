package com.wiley.permissions.camel.web.processor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Plain HTTP GET using the absolute URL in {@link CompCopyUrlProcessor#HEADER_URL}.
 * Avoids Camel http URI parsing of LANSA-style query segments.
 */
public class HttpGetProcessor implements Processor {

	private static final Log log = LogFactory.getLog(HttpGetProcessor.class);

	private final int connectTimeoutMs;
	private final int readTimeoutMs;

	public HttpGetProcessor() {
		this(60000, 60000);
	}

	public HttpGetProcessor(int connectTimeoutMs, int readTimeoutMs) {
		this.connectTimeoutMs = connectTimeoutMs;
		this.readTimeoutMs = readTimeoutMs;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		String url = exchange.getIn().getHeader(CompCopyUrlProcessor.HEADER_URL, String.class);
		if (url == null || url.isEmpty()) {
			throw new IllegalStateException("Missing header " + CompCopyUrlProcessor.HEADER_URL);
		}
		if (log.isDebugEnabled()) {
			log.debug("HTTP GET " + url);
		}
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(connectTimeoutMs);
		conn.setReadTimeout(readTimeoutMs);
		conn.setInstanceFollowRedirects(true);
		int code = conn.getResponseCode();
		InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
		String body = "";
		if (in != null) {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			byte[] chunk = new byte[4096];
			int n;
			while ((n = in.read(chunk)) >= 0) {
				buf.write(chunk, 0, n);
			}
			in.close();
			body = new String(buf.toByteArray(), StandardCharsets.UTF_8);
		}
		conn.disconnect();
		exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, code);
		exchange.getIn().setBody(body);
	}
}
