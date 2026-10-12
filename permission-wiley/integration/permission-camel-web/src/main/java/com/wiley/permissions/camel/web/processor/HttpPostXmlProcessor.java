package com.wiley.permissions.camel.web.processor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * HTTP POST of XML body to a fixed absolute URL (PE CGI-BIN / local PROC).
 */
public class HttpPostXmlProcessor implements Processor {

	private static final Log log = LogFactory.getLog(HttpPostXmlProcessor.class);

	private final String url;
	private final int connectTimeoutMs;
	private final int readTimeoutMs;

	public HttpPostXmlProcessor(String url, int connectTimeoutMs, int readTimeoutMs) {
		this.url = url;
		this.connectTimeoutMs = connectTimeoutMs;
		this.readTimeoutMs = readTimeoutMs;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		String xml = exchange.getIn().getBody(String.class);
		if (log.isDebugEnabled()) {
			log.debug("HTTP POST " + url);
		}
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setConnectTimeout(connectTimeoutMs);
		conn.setReadTimeout(readTimeoutMs);
		conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
		byte[] bytes = xml != null ? xml.getBytes(StandardCharsets.UTF_8) : new byte[0];
		conn.setFixedLengthStreamingMode(bytes.length);
		OutputStream out = conn.getOutputStream();
		out.write(bytes);
		out.close();
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
		if (code >= 400) {
			throw new IllegalStateException("HTTP " + code + " from " + url + ": " + body);
		}
		exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, code);
		exchange.getIn().setBody(body);
	}
}
