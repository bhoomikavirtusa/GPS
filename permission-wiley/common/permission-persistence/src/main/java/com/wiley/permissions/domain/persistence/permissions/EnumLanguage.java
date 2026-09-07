package com.wiley.permissions.domain.persistence.permissions;

import java.util.Locale;

import javax.persistence.Entity;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

/**
 * I will say probably we do not need the table because we have to change the code anyway when we add a new locale
 * @author lnagy
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name="ENUM_LANGUAGE")
public class EnumLanguage extends EnumData
{
	public static final String EN = "en";

	public static Locale[] supportedLocales = {
		    Locale.GERMAN,
		    Locale.ENGLISH,
		    Locale.FRENCH,
		    new Locale ("es", "ES")
	};

	public EnumLanguage() { }

	public EnumLanguage(String code, String description) {
		setCode(code);
		setDescription(description);
	}

	public static Locale getLocale(String languageCode) {
		for (Locale locale : supportedLocales) {
			if (locale.getLanguage().equalsIgnoreCase(languageCode))
				return locale;
		}
		return Locale.ENGLISH;
	}
}
