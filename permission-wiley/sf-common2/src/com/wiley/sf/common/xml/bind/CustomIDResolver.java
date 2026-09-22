package com.wiley.sf.common.xml.bind;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import com.sun.xml.bind.IDResolver;

/**
 * This class is safe to use multiple times sequentially by the same thread
 * but not safe to use in multiple threads simultaneously.
 *
 * We have to create this CustomIDResolver because the default one does not
 * work properly when we have REFs of different classes (tags) with same
 * value. For example ProductStatus E and Medium also E.
 * (smarkoff: I am surprised that the default resolver does not handle this
 * properly, but at least with JDK 1.6.0_31 and below it does not.
 * Is this situation discussed at all in the JAXB specification?)
 *
 * smarkoff: Added another feature which is to handle REFs that are missing a
 * full object. (Default behavior from testing is just to omit the XML for
 * REFs with no matching full object!)
 * This is tricky because we can't just always create a REF object when asked
 * in resolve() method and leave it at that -- because if there is first a REF
 * in the XML and then a full object later on, we need to make sure that our
 * created object is overwritten with the full object.
 *
 * smarkoff: Added another feature to deal with resolve being called with
 * a targetType of java.lang.Object.class instead of something more specific.
 * This seems to happen with you have a list of elements instead a wrapper
 * element.
 *
 * @author lnagy, smarkoff
 */
public class CustomIDResolver extends IDResolver {

	private static final Log log = LogFactory.getLog(CustomIDResolver.class);

	private final Map<String, Object> fullMap = new HashMap<String, Object>();
	private final Map<String, Object> refMap = new HashMap<String, Object>();

	private final Class<?> targetTypeForObject;

	public CustomIDResolver() {
	    targetTypeForObject = null;
	}

	/**
	 * If you have a list of elements inside a wrapper element then resolve may be called
	 * with targetType of Object instead of the proper class. You can fix this by specifying
	 * what class to use instead of Object.
	 *
	 * @param targetTypeForObject  May be null
	 */
	public CustomIDResolver(Class<?> targetTypeForObject) {
	    this.targetTypeForObject = targetTypeForObject;
	}

	/**
	 * If you have a list of elements inside a wrapper element then resolve may be called
     * with targetType of Object instead of the proper class. You can fix this by specifying
     * what class to use instead of Object.
     *
	 * @param targetTypeForObject  Must be a valid class name or a ClassNotFoundException will be thrown
	 */
	public CustomIDResolver(String targetTypeForObject) throws ClassNotFoundException {
	    this.targetTypeForObject = Class.forName(targetTypeForObject);
	}

	@Override
	public void startDocument(ValidationEventHandler eventHandler) throws SAXException {
		// log hashCode just to identify this instance
		log.debug("startDocument(): called, hashCode = " + hashCode());
		fullMap.clear();
		refMap.clear();
		super.startDocument(eventHandler);
	}

	/**
	 * Implements IDResolver abstract method.
	 */
	@Override
	public void bind(String id, Object obj) {
		//smarkoff: From observation, obj does not have any properties set at this point!
		// Even if properties are specified in the XML. (This is not what I expected.)
		// I assume they are set later. This means we cannot fix ref objects that
		// we created in resolve() here - instead do in the endDocument() method.

		log.debug("bind(): id = " + id + ", simpleName = " + obj.getClass().getSimpleName());
		// use @ as separator character because it is not a legal character in a Java class name (not a letter or digit)
		// but keep mind that it's possible the id String also has this character in it
		String key = id + "@" + obj.getClass().getCanonicalName();
		fullMap.put(key, obj);
	}

	/**
	 * Implements IDResolver abstract method.
	 */
	@Override
	public Callable<?> resolve(final String id, final Class targetType) {
		return new Callable<Object>() {
			public Object call() throws Exception {
				if (StringUtils.isBlank(id)) {
					// this happens if you have a blank element such as "<productLine />" which we sometimes have in productEdition
					// because our XSL always creates this element for productEdition even if there is no productLine
					log.debug("resolve(): called with blank id for class [" + targetType.getSimpleName() + "]");
					return null;
				}

				Class<?> targetType2 = targetType;

                if (targetType.equals(java.lang.Object.class)) {
                    if (targetTypeForObject == null) {
                        log.debug("resolve(): targetType of java.lang.Object - targetTypeForObject not specified on ctor.");
                    }
                    else {
                        log.debug("resolve(): targetType of java.lang.Object - will use " + targetTypeForObject.getName());
                        targetType2 = targetTypeForObject;
                    }
                }

				String key = id + "@" + targetType2.getCanonicalName();
				Object output = fullMap.get(key);
				String extra = (output == null ? "NOT" : "WAS");
				log.debug("resolve(): id [" + id + "] class [" + targetType2.getSimpleName() + "] " + extra + " found in fullMap");

				if (output != null)  return output;

				// this logic deals with the situation where the XML contains an IDREF but not the full object
				// however it creates a problem if the IDREF appears before the full object, which we fix in
				// endDocument().

				// first see if we already created a ref object, if not then create one
				output = refMap.get(key);
				extra = (output == null ? "NOT" : "WAS");
				log.debug("resolve(): id [" + id + "] class [" + targetType2.getSimpleName() + "] " + extra + " found in refMap");

				if (output != null) return output;

				// create ref object
				try {
				    Object o = targetType2.newInstance();  // throws InstantiationException, IllegalAccessException
				    boolean success = setXmlID(o, id);
				    // if !success then setXmlID() will have logged a reason
				    if (success) {
				        log.debug("resolve(): created ref object for " + targetType2.getSimpleName() + ", id = " + id);
				        refMap.put(key, o);
				        return o;
				    }
				}
				catch (Exception ex) {
					log.warn("resolve(): caught exception trying to create ref object: ", ex);
				}

				return output;
			}
		};
	}

	@Override
	public void endDocument() throws SAXException {
		// log hashCode just to identify this instance
		log.debug("endDocument(): called, hashCode = " + hashCode());
		super.endDocument();

		fixRefObjects();
	}

	private void fixRefObjects() {
		// fixes idRef objects created unnecessarily in resolve()
		// Note this will not work unless the object has overridden the equals method
		// to be based on only the XmlID-marked attribute.
		Set<String> keys = refMap.keySet();
		for (String key : keys) {
			Object refObject = refMap.get(key);
			fixRefObject(key, refObject);
		}
	}

	private String getIdFromKey(String key) {
		// keep in mind that @ could be part of the id but not part of the class name
		int index = key.lastIndexOf('@');
		return key.substring(0, index);
	}

	private void fixRefObject(String key, Object refObject) {
		Object fullObject = fullMap.get(key);
		String id = getIdFromKey(key);
		if (fullObject == null) {
			log.debug("fixRefObject(): no fullObject for id [" + id + "] class [" + refObject.getClass().getSimpleName() + "].");
			return;
		}

		try {
			PropertyUtils.copyProperties(refObject, fullObject);  // dest, orig
			log.debug("fixRefObject(): copied properties for id [" + id + "] class [" + refObject.getClass().getSimpleName() + "].");
		}
		catch (Exception ex) {
			log.debug("fixRefObject(): caught exception copying properties: ", ex);
		}
	}

	/**
	 * Looks for a public get method marked as XmlID, or if no method is marked,
	 * looks for a field marked as XmlID (field must have a corresponding get method to be found).
	 * Once finds an XmlId, checks if there is a set method and if so calls that with the given id.
	 * The set method must take a single String parameter.
	 * If a set method was found and called successfully, returns true.
	 *
	 * @param clazz  Must be non-null
	 * @param id  May be null (but normally won't be)
	 */
	public static boolean setXmlID(Object object, String id) {
		Class<?> clazz = object.getClass();
		String foundUpperName = null;
		Method [] methods = clazz.getMethods();
		// don't consider is_xx methods here because they won't be String
		for (Method m : methods) {
			if (m.getName().startsWith("get")) {
				String upperName = m.getName().substring(3);
				String lowerName = upperName.substring(0, 1).toLowerCase() + upperName.substring(1);
				if (m.getAnnotation(XmlID.class) != null) {
					foundUpperName = upperName;
					break;
				}

				try {
					Field f = clazz.getDeclaredField(lowerName);
					if (f.getAnnotation(XmlID.class) != null) {
						foundUpperName = upperName;
						break;
					}
				}
				catch (NoSuchFieldException ex) { }
			}
		}

		if (foundUpperName == null) {
		    log.debug("setXmlID(): No method or field marked with @XmlID [class: " + clazz.getName() + "].");
		    return false;
		}

		try {
			Method m = clazz.getMethod("set" + foundUpperName, String.class);
			m.invoke(object, id);  // throws InvocationTargetException, IllegalAccessException
			return true;
		}
		catch (NoSuchMethodException ex) {
		    log.debug("setXmlID(): No such method set" + foundUpperName + " on " + clazz.getName());
			return false;
		}
		catch (InvocationTargetException ex2) {
			log.debug("setXmlID(): Unable to call set" + foundUpperName + " on " + clazz.getName());
			return false;
		}
		catch (IllegalAccessException ex3) {
			log.debug("setXmlID(): Unable to call set" + foundUpperName + " on " + clazz.getName());
			return false;
		}
	}
}
