package com.wiley.permissions.domain.rightslink;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="licenses")
public class Licenses implements Serializable {
	
	private static final long serialVersionUID = 1L;

	List<License> licenses = new ArrayList<License> ();

	@XmlElements(
	{
		@XmlElement(name="license", type=License.class)
	})
	public List<License> getLicenses() {
		return licenses;
	}

	public void setLicenses(List<License> licenses) {
		this.licenses = licenses;
	}
}
