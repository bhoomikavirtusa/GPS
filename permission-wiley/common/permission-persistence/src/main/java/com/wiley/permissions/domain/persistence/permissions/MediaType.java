package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "MEDIA_TYPE")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class MediaType
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// The following constants should match the database exactly.
	public static final MediaType
		ANIMATION = new MediaType("Animation", "Animation"),
		ARTICLE = new MediaType("Article", "Article"),
		AUDIO = new MediaType("Audio", "Audio"),
		CARTOON = new MediaType("Cartoon", "Cartoon"),
		CLIP_ART = new MediaType("Clip Art", "Clip Art"),
		COVER_DESIGN = new MediaType("Cover Design", "Cover Design"),
		DATA_SET = new MediaType("Data Set", "Data Set"),
		EXTRACT = new MediaType("Extract", "Extract"),
		GRAPH = new MediaType("Graph", "Graph"),
		ICON = new MediaType("Icon", "Icon"),
		ILLUSTRATION = new MediaType("Illustration", "Illustration"),
		INTERACTIVITY = new MediaType("Interactivity", "Interactivity"),
		LECTURE_PRESENTATION = new MediaType("Lecture Presentation", "Lecture Presentation"),
		LIST = new MediaType("List", "List"),
		MAP = new MediaType("Map", "Map"),
	    PHOTO = new MediaType("Photo", "Photo"),
	    QUESTION = new MediaType("Question", "Question"),
	    REALIA = new MediaType("Realia", "Realia"),
	    SCREENSHOT = new MediaType("Screenshot", "Screenshot"),
	    TABLE = new MediaType("Table", "Table"),
	    TEXT = new MediaType("Text", "Text"),
	    TUTORIAL = new MediaType("Tutorial", "Tutorial"),
	    VIDEO = new MediaType("Video", "Video");

	public static final MediaType [] ALL_MEDIA_TYPES = {
		ANIMATION, ARTICLE, AUDIO, CARTOON, CLIP_ART, COVER_DESIGN, DATA_SET, EXTRACT,
		GRAPH, ICON, ILLUSTRATION, INTERACTIVITY, LECTURE_PRESENTATION, LIST, MAP,
		PHOTO, QUESTION, REALIA, SCREENSHOT, TABLE, TEXT, TUTORIAL, VIDEO
	};

	@Id
	@Column(name = "CODE", unique = true, nullable = false, length = 20)
	@MaterializationKey
	private String code;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;


	public MediaType() {
		super();
	}

	/**
	 * Used by party3rdAssetDetails.jspx.
	 */
	@Transient
	public boolean isPhotoEquivalent() {
		// 6/2014 - note if you are going to change this then probably you also want to change some other
		// spots in the code such as POAssetsReport2.jrxml and RequestAssetsReport2.jrxml
		// (and these reports are in both Internal and Author)
		return PHOTO.equals(this) || CARTOON.equals(this) || ILLUSTRATION.equals(this) || MAP.equals(this) || GRAPH.equals(this);
	}

	/**
	 * Used by party3rdAssetDetails.jspx.
	 */
	@Transient
	public boolean isTextEquivalent() {
		return TEXT.equals(this) || ARTICLE.equals(this) || EXTRACT.equals(this);
	}

	public MediaType(String code, String description) {
		this.code = code;
		this.description = description;
	}

	@XmlElement
	@XmlID
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@XmlElement
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * Returns null if the given description does not match any Usage.
	 */
	public static MediaType getByDescription(String description) {
		for (MediaType mediaType : ALL_MEDIA_TYPES) {
			if (StringUtils.equalsIgnoreCase(description, mediaType.getDescription()))  return mediaType;
		}
		return null;
	}

	/**
	 * We automatically correct the code if only the case is wrong.
	 *
	 * @throws ValidateException
	 */
	public void validate() throws ValidateException {
		for (MediaType mt: ALL_MEDIA_TYPES) {
			if (mt.getCode().equals(code))  return;
			if (mt.getCode().equalsIgnoreCase(code)) {
				code = mt.getCode();
				return;
			}
		}

		throw new ValidateException("\"" + code + "\" is not a valid MediaType code");
	}

	@Override
	public String toString() {
		// use getters due to the way JPA works
	    return "code = " + getCode()
	        + ", description = " + getDescription()
	        + ", sortOrder = " + getSortOrder();
	}

	// base equals() and hashCode() just on code field

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof MediaType)) return false;
		MediaType other = (MediaType) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());

		return result;
	}
}
