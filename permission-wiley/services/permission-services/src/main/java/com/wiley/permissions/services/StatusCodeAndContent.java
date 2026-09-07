package com.wiley.permissions.services;

/**
 * This class encapsulates the data returned by RightsLinkClient.getLicenseXml(licenseNumber).
 *
 * @author smarkoff
 */
public class StatusCodeAndContent {
	private final int statusCode;
	private final String content;

	public StatusCodeAndContent(int statusCode, String content) {
		this.statusCode = statusCode;
		this.content = content;
	}

	public int getStatusCode() { return statusCode; }
	public String getContent() { return content; }
}
