package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

/**
 *
 * @author ttidwell
 */
@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "COMPONENT_CATEGORY")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class ComponentCategory
extends DomainObject
{

	// These constants should match the database values (code, description) exactly.
	public static final ComponentCategory
		AA = new ComponentCategory("AA", "About the Author"),
		AC = new ComponentCategory("AC", "Acknowledgments"),
		ACW = new ComponentCategory("ACW", "Abbreviated Concept"),
		ADW = new ComponentCategory("ADW", "Audio"),
		AN = new ComponentCategory("AN", "Author's Note"),
		ANW = new ComponentCategory("ANW", "Animations"),
		APPENDIX = new ComponentCategory("AP", "Appendix"),
		AR = new ComponentCategory("AR", "MRW Article"),
		ASW = new ComponentCategory("ASW", "Assignment"),
		BAW = new ComponentCategory("BAW", "Back Matter"),
		BC = new ComponentCategory("BC", "Bonus Chapter"),
		BG = new ComponentCategory("BG", "A Biographical Article in an MRW"),
		BI = new ComponentCategory("BI", "Bibliography"),
		BM = new ComponentCategory("BM", "General Backmatter"),
		BOW = new ComponentCategory("BOW", "Book Companion Site"),
		BUW = new ComponentCategory("BUW", "Business Plan"),
		CA = new ComponentCategory("CA", "Current Protocols Appendix Unit"),
		CHAPTER = new ComponentCategory("CH", "Chapter"),
		CI = new ComponentCategory("CI", "Color Insert"),
		CON = new ComponentCategory("CON", "Contract"),
		COW = new ComponentCategory("COW", "Concept Module"),
		CP = new ComponentCategory("CP", "Current Protocols Ordinary Unit"),
		CR = new ComponentCategory("CR", "Chronology"),
		CS = new ComponentCategory("CS", "Case Study"),
		COVER = new ComponentCategory("CVW", "Cover"),
		DAW = new ComponentCategory("DAW", "Data Sets"),
		DC = new ComponentCategory("DC", "Dedication"),
		DEW = new ComponentCategory("DEW", "Design"),
		DF = new ComponentCategory("DF", "A Definition Entry in an MRW"),
		DJ = new ComponentCategory("DJ", "Dust Jacket Copy"),
		DM = new ComponentCategory("DM", "Digital Media"),
		DR = new ComponentCategory("DR", "Diagnostic Review"),
		EB = new ComponentCategory("EB", "Editorial Board"),
		EG = new ComponentCategory("EG", "Epigraph"),
		EN = new ComponentCategory("EN", "Endorsements"),
		ENW = new ComponentCategory("ENW", "End matter"),
		EP = new ComponentCategory("EP", "Epilogue"),
		ER = new ComponentCategory("ER", "Erratum"),
		EXW = new ComponentCategory("EXW", "Example"),
		FIW = new ComponentCategory("FIW", "Financials"),
		FRW = new ComponentCategory("FRW", "Front Matter"),
		FW = new ComponentCategory("FW", "Foreword"),
		GL = new ComponentCategory("GL", "Glossary"),
		GRW = new ComponentCategory("GRW", "Graphics"),
		HI = new ComponentCategory("HI", "Historical Perspective"),
		HIW = new ComponentCategory("HIW", "Higher Practice"),
		ICW = new ComponentCategory("ICW", "Icons"),
		ILW = new ComponentCategory("ILW", "Illustrations"),
		IMW = new ComponentCategory("IMW", "Image"),
		IN = new ComponentCategory("IN", "Index"),
		IND = new ComponentCategory("IND", "Interior Design"),
		INW = new ComponentCategory("INW", "Interactives"),
		IR = new ComponentCategory("IR", "Intervention Review"),
		IT = new ComponentCategory("IT", "Introduction"),
		KN = new ComponentCategory("KN", "Keynote (in ELS)"),
		LAW = new ComponentCategory("LAW", "Layout"),
		LI = new ComponentCategory("LI", "Lists of Tables, Figures, Maps, etc."),
		ME = new ComponentCategory("ME", "Media Items, Including Disclaimers"),
		MEW = new ComponentCategory("MEW", "Media Spec"),
		MR = new ComponentCategory("MR", "Methodology Review"),
		OV = new ComponentCategory("OV", "Overview"),
		PE = new ComponentCategory("PE", "Part Epilogue"),
		PF = new ComponentCategory("PF", "Book or Series Preface"),
		PHW = new ComponentCategory("PHW", "Photos"),
		PI = new ComponentCategory("PI", "Part Introduction"),
		PN = new ComponentCategory("PN", "Publisher's Note"),
		PP = new ComponentCategory("PP", "Generic Front matter"),
		PR = new ComponentCategory("PR", "Prologue"),
		PRF = new ComponentCategory("PRF", "Preface"),
		PRM = new ComponentCategory("PRM", "Promotion"),
		PRW = new ComponentCategory("PRW", "Practice"),
		PV = new ComponentCategory("PV", "Provisional Record"),
		REW = new ComponentCategory("REW", "Review"),
		RFR = new ComponentCategory("RFR", "References"),
		SA = new ComponentCategory("SA", "Structured Abstract"),
		SBC = new ComponentCategory("SBC", "Sub concept"),
		SBI = new ComponentCategory("SBI", "Subject Index"),
		SH = new ComponentCategory("SH", "Short Article in an MRW"),
		SI = new ComponentCategory("SI", "Subpart Introduction"),
		SPW = new ComponentCategory("SPW", "Specifications"),
		SS = new ComponentCategory("SS", "Sub subpart Introduction"),
		SUW = new ComponentCategory("SUW", "Survey"),
		TN = new ComponentCategory("TN", "Translator's Note"),
		UR = new ComponentCategory("UR", "Overview of Reviews"),
		VIW = new ComponentCategory("VIW", "Videos"),
		WEW = new ComponentCategory("WEW", "Web Resources"),
		XX = new ComponentCategory("XX", "Miscellaneous")
	    ;

	private static final long serialVersionUID = 1L;

	public static final ComponentCategory [] ALL_COMPONENT_CATEGORIES = {
		AA, AC, ACW, ADW, AN, ANW, APPENDIX, AR, ASW, BAW, BC, BG, BI, BM, BOW, BUW, CA, CHAPTER,
		CI, CON, COW, CP, CR, CS, COVER, DAW, DC, DEW, DF, DJ, DM, DR, EB, EG, EN, ENW,
		EP, ER, EXW, FIW, FRW, FW, GL, GRW, HI, HIW, ICW, ILW, IMW, IN, IND, INW, IR,
		IT, KN, LAW, LI, ME, MEW, MR, OV, PE, PF, PHW, PI, PN, PP, PR, PRF, PRM, PRW,
		PV, REW, RFR, SA, SBC, SBI, SH, SI, SPW, SS, SUW, TN, UR, VIW, WEW, XX
	};

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = false, length = 50)
	private String description = null;

	public ComponentCategory()
	{
	}

	public ComponentCategory(String code, String description) {
		this.code = code;
		this.description = description;
	}

	public static ComponentCategory getComponentCategory(String name) {
		if (StringUtils.isBlank(name))
			return null;

		name = name.trim();

		if (name.toLowerCase().contains("chapter")) {
			return ComponentCategory.CHAPTER;
		} else if (name.toLowerCase().contains("appendix")) {
			return ComponentCategory.APPENDIX;
		} else if (name.toLowerCase().contains("cover")) {
			return ComponentCategory.COVER;
		} else {
			return null;
		}
	}

	@XmlElement
	@XmlID
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Returns null if the given description does not match any Usage.
	 */
	public static ComponentCategory getByDescription(String description) {
		for (ComponentCategory cc : ALL_COMPONENT_CATEGORIES) {
			if (StringUtils.equalsIgnoreCase(description, cc.getDescription()))  return cc;
		}
		return null;
	}

	/**
	 * Returns null if the given code does not match any Usage.
	 */
	public static ComponentCategory getByCode(String code) {
		for (ComponentCategory cc : ALL_COMPONENT_CATEGORIES) {
			if (StringUtils.equalsIgnoreCase(code, cc.getCode()))  return cc;
		}
		return null;
	}

	/**
	 * Returns null if the given description does not match any Usage.
	 */
	public static ComponentCategory getByMatchingDescription(String description) {
		for (ComponentCategory cc : ALL_COMPONENT_CATEGORIES) {
			if (description.startsWith(cc.getDescription())) return cc;
		}
		return ComponentCategory.XX;
	}

	@Override
	public String toString() {
		return "code = " + getCode() + ", description = " + getDescription();
	}

	// base equals and hashCode on the @XmlID which is code

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof ComponentCategory)) return false;
		ComponentCategory other = (ComponentCategory) obj;
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
