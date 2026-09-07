package com.wiley.permissions.common.bean;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Transient;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.bean.Merge.CollectionHandling;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.common.utils.InstanceMap;
import com.wiley.permissions.common.utils.InstanceOnlySet;

/**
 * This class is to be used to merge two JavaBeans that are essentially the same
 * and need to have their properties synchronized.
 *
 * @author ttidwell
 */
public class BeanUtility {
	private final static Log log = LogFactory.getLog(BeanUtility.class);

	public final static int DEFAULT_SHORT_VALUE = -1;
	public final static int DEFAULT_INT_VALUE = -1;
	public final static long DEFAULT_LONG_VALUE = -1;
	public final static float DEFAULT_FLOAT_VALUE = -1;
	public final static double DEFAULT_DOUBLE_VALUE = -1;
	public final static byte DEFAULT_BYTE_VALUE = 0;
	public final static char DEFAULT_CHAR_VALUE = 0;
	public final static boolean DEFAULT_BOOLEAN_VALUE = false;

	public final static Object[] BLANK_ARGS = new Object[0];

	public enum ObjectType
	{
		CHAR,
		BYTE,
		SHORT,
		INT,
		LONG,
		FLOAT,
		DOUBLE,
		BOOLEAN,
		BOXED_CHAR,
		BOXED_BYTE,
		BOXED_SHORT,
		BOXED_INT,
		BOXED_LONG,
		BOXED_FLOAT,
		BOXED_DOUBLE,
		BOXED_BOOLEAN,
		BOXED_BIG_INTEGER,
		BOXED_BIG_DECIMAL,
		STRING,
		CLASS,
		ARRAY,
		OBJECT
	}

	/**
	 * Returns null if the property is not found.
	 */
	public static BeanProperty getPropertyFromClass(Class<?> clazz, String propertyName)
	{
		BeanProperty output = null;

		String propertyNameCorrected = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);

		// The first thing we need is to find out if there is a read method for
		// this property.  Read methods are great cuz they take no parameters.
		Method readMethod = null;

		try
		{
			// First we'll try good ol' "get"
			readMethod = clazz.getMethod("get" + propertyNameCorrected, new Class[0]);

			int modifiers = readMethod.getModifiers();

			if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)
				|| Modifier.isAbstract(modifiers) || Modifier.isProtected(modifiers))
			{
				readMethod = null;
			}
		}
		catch (NoSuchMethodException e)
		{
		}

		if (readMethod == null)
		{
			// We didn't find it with "get", so let's try the boolean form
			try
			{
				readMethod = clazz.getMethod("is" + propertyNameCorrected, new Class[0]);

				int modifiers = readMethod.getModifiers();

				if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)
					|| Modifier.isAbstract(modifiers) || Modifier.isProtected(modifiers))
				{
					readMethod = null;
				}
			}
			catch (NoSuchMethodException e)
			{
			}
		}

		if (readMethod != null)
		{
			// OK, we have a valid read method.  Lets see if we can find a valid
			// write method.
			Method writeMethod = null;

			try
			{
				writeMethod = clazz.getMethod("set" + propertyNameCorrected, readMethod.getReturnType());

				int modifiers = writeMethod.getModifiers();

				if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)
					|| Modifier.isAbstract(modifiers) || Modifier.isProtected(modifiers))
				{
					writeMethod = null;
				}
			}
			catch (NoSuchMethodException e)
			{
			}

			// Ok, we have a lot of what we need.
			output = new BeanProperty();

			output.setName(propertyName);

			Class<?> returnType = readMethod.getReturnType();

			output.setPropertyType(getObjectType(returnType));
			output.setType(returnType);
			output.setReadMethod(readMethod);
			output.setWriteMethod(writeMethod);

			// Now we just need to see if we can find a field that matches, roughly
			Field ourField = null;

			Class<?> tmpClass = clazz;

			while (ourField == null)
			{
				for (int x = 0; x < propertyName.length() && ourField == null; x++)
				{
					String tmpProp = propertyName.substring(0, x).toLowerCase() + propertyName.substring(x);

					try
					{
						ourField = tmpClass.getDeclaredField(tmpProp);
					}
					catch (NoSuchFieldException e)
					{
					}
				}

				if (ourField == null)
				{
					for (int x = 0; x < propertyName.length() && ourField == null; x++)
					{
						String tmpProp = propertyName.substring(0, x).toUpperCase() + propertyName.substring(x);

						try
						{
							ourField = tmpClass.getDeclaredField(tmpProp);
						}
						catch (NoSuchFieldException e)
						{
						}
					}
				}

				if (ourField == null)
				{
					if (tmpClass.getSuperclass() != null && !Object.class.equals(tmpClass.getSuperclass()))
					{
						tmpClass = tmpClass.getSuperclass();
					}
					else
					{
						break;
					}
				}
			}

			if (ourField != null)
			{
				output.setName(ourField.getName());
			}

			Map<Class<?>, Annotation> annotations = new HashMap<Class<?>, Annotation>();

			if (readMethod != null)
			{
				Annotation[] annos = getRealMethod(readMethod).getAnnotations();

				for (Annotation anno : annos)
				{
					annotations.put(anno.annotationType(), anno);
				}
			}

			if (ourField != null)
			{
				Annotation[] annos = ourField.getAnnotations();

				for (Annotation anno : annos)
				{
					if (annotations.containsKey(anno.getClass()))
					{
						annotations.remove(anno);
					}

					annotations.put(anno.annotationType(), anno);
				}
			}

			output.setAnnotations(annotations);
		}

		return output;
	}

	public static boolean isHibernateProxy(Class<?> clazz) {
		Class<?> [] interfaces = clazz.getInterfaces();
		for (Class<?> c : interfaces) {
			if (c.getName().equals("org.hibernate.proxy.HibernateProxy"))  return true;
		}
		return false;
	}

	/**
	 * If method belongs to a proxy class and overrides the super class then return the super class method instead,
	 * otherwise return the input method.
	 */
	public static Method getRealMethod(Method method) {
		Class<?> clazz = method.getDeclaringClass();
		if (isHibernateProxy(clazz)) {
			//logFieldsAndMethods(tmpClass);
			// from the above method we know that the proxy classes do not have the fields of the super class,
			// but they have all the methods (overridden) + other methods
			// (they do have 2 fields called default_interceptor and _method_filter)
			Class<?> realClass = clazz.getSuperclass();
			try {
				return realClass.getMethod(method.getName(), method.getParameterTypes());
			}
			catch (NoSuchMethodException ex) {
				// There are other methods on the proxy besides the ones in the super class so
				// not finding a method is not an error.
				//log.debug("Could not get real method for method name = " + method.getName() + ", class = " + realClass);
			}
		}
		return method;
	}

	/**
	 * For debug purposes.
	 * This will not look at super classes.
	 */
	public static void logFieldsAndMethods(Class<?> clazz) {
		log.debug("---------- fields and methods for " + clazz.getSimpleName());
		Field [] fields = clazz.getFields();
		for (Field f : fields) {
			Annotation [] annotations = f.getAnnotations();
			StringBuilder sb = new StringBuilder();
			for (Annotation a : annotations) {
				sb.append(a.getClass().getSimpleName());
				sb.append(" ");
			}
			log.debug("field name = " + f.getName() + ", annotations = " + sb);
		}

		Method [] methods = clazz.getMethods();
		for (Method m : methods) {
			log.debug("method name = " + m.getName());
		}
	}

	/**
	 * Get all properties that are instance (not static), not abstract,
	 * public, and not marked as Transient on the method.
	 */
	public static List<BeanProperty> getAllProperties(Class<?> clazz)
	{
		ArrayList<BeanProperty> output = new ArrayList<BeanProperty>();

		HashMap<String, BeanProperty> outputMap = new HashMap<String, BeanProperty>();

		Class<?> tmpClass = clazz;

		while (true)
		{
			Method methods [] = tmpClass.getDeclaredMethods();

			for (Method method : methods)
			{
				int modifiers = method.getModifiers();

				Transient trans = getRealMethod(method).getAnnotation(Transient.class);

				if (!Modifier.isStatic(modifiers) && !Modifier.isAbstract(modifiers)
					&& Modifier.isPublic(modifiers) && trans == null)
				{
					if (method.getParameterTypes().length == 0)
					{
						String propertyName = null;

						if (method.getName().startsWith("get"))
						{
							propertyName = method.getName().substring(3);
						}
						else if (method.getName().startsWith("is"))
						{
							propertyName = method.getName().substring(2);
						}

						if (propertyName != null)
						{
							propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);

							BeanProperty prop = getPropertyFromClass(tmpClass, propertyName);

							// smarkoff: Don't put property into map if already there (keep property from the top level class)
							if (prop != null && !outputMap.containsKey(prop.getName()))
							{
								outputMap.put(prop.getName(), prop);
							}
						}
					}
				}
			}

			if (tmpClass.getSuperclass() != null && !Object.class.equals(tmpClass.getSuperclass()))
			{
				tmpClass = tmpClass.getSuperclass();
			}
			else
			{
				break;
			}
		} // end while

		output.addAll(outputMap.values());

		return output;
	}

    public static List<BeanProperty> getProperties(Class<?> clazz, Class<? extends Annotation> annoType)
    {
        List<BeanProperty> output = new ArrayList<BeanProperty>();
        List<BeanProperty> properties = getAllProperties(clazz);

        for (BeanProperty property : properties) {
            if (property.getAnnotation(annoType) != null) {
                output.add(property);
            }
        }

        return output;
    }

	/**
	 * Invokes the readMethod of the specified property on the object
	 * @param property
	 * @param object
	 * @return Object
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static Object getPropertyValue (BeanProperty property, Object object)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		Method readMethod = property.getReadMethod();
		Object val = readMethod.invoke(object, new Object[] {});
		return val;
	}

	public static <T extends Annotation> T getAnnotation(Class<T> annotationType, Class<?> clazz)
	{
		T output = null;

		boolean usePackage = false;

		if (!annotationType.equals(Target.class))
		{
			Target targetAnno = getAnnotation(Target.class, annotationType);

			if (targetAnno != null)
			{
				for (ElementType elementType : targetAnno.value())
				{
					if (elementType == ElementType.PACKAGE)
					{
						usePackage = true;

						break;
					}
				}
			}
		}

		Class<?> testClass = clazz;

		while (output == null)
		{
			output = testClass.getAnnotation(annotationType);

			if (output == null && testClass.getSuperclass() != null)
			{
				testClass = testClass.getSuperclass();
			}
			else
			{
				break;
			}
		}

		if (output == null && usePackage)
		{
			output = clazz.getPackage().getAnnotation(annotationType);
		}

		return output;
	}

	public static ObjectType getObjectType(Class<?> clazz)
	{
		ObjectType output = ObjectType.OBJECT;

		if (clazz.isPrimitive())
		{
			if (clazz.getName().equals("short")) {
				output = ObjectType.SHORT;
			}
			else if (clazz.getName().equals("int")) {
				output = ObjectType.INT;
			}
			else if (clazz.getName().equals("long")) {
				output = ObjectType.LONG;
			}
			else if (clazz.getName().equals("float")) {
				output = ObjectType.FLOAT;
			}
			else if (clazz.getName().equals("double")) {
				output = ObjectType.DOUBLE;
			}
			else if (clazz.getName().equals("char")) {
				output = ObjectType.CHAR;
			}
			else if (clazz.getName().equals("byte")) {
				output = ObjectType.BYTE;
			}
			else if (clazz.getName().equals("boolean")) {
				output = ObjectType.BOOLEAN;
			}
		}
		else if (clazz.isArray()) {
			output = ObjectType.ARRAY;
		}
		else {
			if (clazz.equals(Short.class)) {
				output = ObjectType.BOXED_SHORT;
			}
			else if (clazz.equals(Integer.class)) {
				output = ObjectType.BOXED_INT;
			}
			else if (clazz.equals(Long.class)) {
				output = ObjectType.BOXED_LONG;
			}
			else if (clazz.equals(Float.class)) {
				output = ObjectType.BOXED_FLOAT;
			}
			else if (clazz.equals(Double.class)) {
				output = ObjectType.BOXED_DOUBLE;
			}
			else if (clazz.equals(Character.class)) {
				output = ObjectType.BOXED_CHAR;
			}
			else if (clazz.equals(Byte.class)) {
				output = ObjectType.BOXED_BYTE;
			}
			else if (clazz.equals(Boolean.class)) {
				output = ObjectType.BOXED_BOOLEAN;
			}
			else if (clazz.equals(BigInteger.class)) {
				output = ObjectType.BOXED_BIG_INTEGER;
			}
			else if (clazz.equals(BigDecimal.class)) {
				output = ObjectType.BOXED_BIG_DECIMAL;
			}
			else if (clazz.equals(String.class)) {
				output = ObjectType.STRING;
			}
			else if (clazz.equals(Class.class)) {
				output = ObjectType.CLASS;
			}
			else {
				output = ObjectType.OBJECT;
			}
		}

		return output;
	}

	/**
	 * Creates a new instance that is exactly like the bean given as a source.
	 * <br/>
	 * This is a very deep copy.
	 *
	 * @param source
	 * @return
	 */
	public static <T> T copy(T source)
	throws BeanCopyException
	{
		return copy(source, new InstanceMap<T, T>());
	}

	private static <T> T copy(T source, Map<T, T> previouslyCopied)
	throws BeanCopyException
	{
		T output = null;

		if (source != null)
		{
			output = previouslyCopied.get(source);

			if (output == null)
			{
				Class<?> clazz = source.getClass();

				if (source instanceof Collection<?>)
				{
					try
					{
						Collection<?> oldCollection = (Collection<?>) source;
						Collection newCollection = (Collection) clazz.newInstance();

						previouslyCopied.put(source, (T) newCollection);

						for (Object tmpObj : oldCollection)
						{
							newCollection.add(copy(tmpObj));
						}

						output = (T) newCollection;
					}
					catch (IllegalAccessException e)
					{
						throw new BeanCopyException("Could Not Instantiate Copy Collection for class: " + clazz.getName(), e);
					}
					catch (InstantiationException e)
					{
						throw new BeanCopyException("Could Not Instantiate Copy Collection for class: " + clazz.getName(), e);
					}
				}
				else if (source instanceof Map)
				{
					try
					{
						Map oldMap = (Map) source;
						Map newMap = (Map) clazz.newInstance();

						previouslyCopied.put(source, (T) newMap);

						for (Object tmpKey : oldMap.keySet())
						{
							newMap.put(copy(tmpKey), copy(oldMap.get(tmpKey)));
						}

						output = (T) newMap;
					}
					catch (IllegalAccessException e)
					{
						throw new BeanCopyException("Could Not Instantiate Copy Map for class: " + clazz.getName(), e);
					}
					catch (InstantiationException e)
					{
						throw new BeanCopyException("Could Not Instantiate Copy Map for class: " + clazz.getName(), e);
					}
				}
				else if (clazz.isArray())
				{
					Object[] tmpArray = (Object[]) source;

					Object[] tmpArray2 = Arrays.copyOf(tmpArray, tmpArray.length);

					previouslyCopied.put(source, (T) tmpArray2);

					for (int x = 0; x < tmpArray2.length; x++)
					{
						tmpArray2[x] = copy(tmpArray2[x]);
					}

					output = (T) tmpArray2;
				}
				else if (clazz.isPrimitive())
				{
					output = source;
				}
				else
				{
					ObjectType type = getObjectType(clazz);

					switch (type)
					{
						case BOXED_BIG_DECIMAL:
						case BOXED_BIG_INTEGER:
						case BOXED_BOOLEAN:
						case BOXED_BYTE:
						case BOXED_CHAR:
						case BOXED_DOUBLE:
						case BOXED_FLOAT:
						case BOXED_INT:
						case BOXED_LONG:
						case BOXED_SHORT:
						case CLASS:
						{
							output = source;

							break;
						}

						case STRING:
						{
							output = (T) String.valueOf(source);

							break;
						}

						case OBJECT:
						default:
						{
							try
							{
								output = (T) clazz.newInstance();
							}
							catch (IllegalAccessException e)
							{
								throw new BeanCopyException("Could Not Instantiate Copy for class: " + clazz.getName(), e);
							}
							catch (InstantiationException e)
							{
								throw new BeanCopyException("Could Not Instantiate Copy for class: " + clazz.getName(), e);
							}

							previouslyCopied.put(source, output);

							List<BeanProperty> properties = getAllProperties(clazz);

							for (BeanProperty property : properties)
							{
								Field field = property.getField();

								Method writeMethod = property.getWriteMethod();

								Method readMethod = property.getReadMethod();

								Object sourceValue = null;

								if (readMethod != null)
								{
									try
									{
										sourceValue = readMethod.invoke(source, BLANK_ARGS);
									}
									catch (Exception e)
									{
										throw new BeanCopyException("Could Not Obtain Value Of Source Property '"
											+ property.getName() + "' for class: " + clazz.getName(), e);
									}
								}
								else if (field != null)
								{
									try
									{
										sourceValue = field.get(source);
									}
									catch (Exception e)
									{
										throw new BeanCopyException("Could Not Obtain Value Of Source Property '"
											+ property.getName() + "' for class: " + clazz.getName(), e);
									}
								}
								else
								{
									throw new BeanCopyException("Could Not Obtain Value Of Source Property '"
										+ property.getName() + "' for class: " + clazz.getName());
								}

								if (sourceValue != null)
								{
									sourceValue = copy(sourceValue);
								}

								try
								{
									Object args[] = new Object[1];

									args[0] = sourceValue;

									writeMethod.invoke(output, args);
								}
								catch (Exception e)
								{
									throw new BeanCopyException("Could Not Copy Property '"
										+ property.getName() + "' for class: " + clazz.getName(), e);
								}
							}

							break;
						}
					}
				}
			}
		}

		return output;
	}

	public static void merge(Object source, Object target)
	throws BeanMergeException
	{
		merge(source, target, false);
	}

	public static void merge(Object source, Object target, boolean deep)
	throws BeanMergeException
	{
		merge(source, target, deep, new InstanceOnlySet<Object>());
	}

	private static void merge(Object source, Object target, boolean deep, Set<Object> alreadyMerged)
	throws BeanMergeException
	{
		if (source != null && target != null && !alreadyMerged.contains(source))
		{
			alreadyMerged.add(source);

			Class<?> clazz = source.getClass();

			PropertyProtection defaultProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT;
			CollectionHandling defaultCollectionHandling = CollectionHandling.MERGE;

			Merge classMerge = BeanUtility.getAnnotation(Merge.class, clazz);

			if (classMerge != null)
			{
				if (classMerge.propertyProtection() != PropertyProtection.DEFAULT)
				{
					defaultProtection = classMerge.propertyProtection();
				}

				if (classMerge.collectionHandling() != CollectionHandling.DEFAULT)
				{
					defaultCollectionHandling = classMerge.collectionHandling();
				}

			}

			if (defaultProtection != PropertyProtection.NEVER_MERGE)
			{
				List<BeanProperty> properties = BeanUtility.getAllProperties(clazz);

				for (BeanProperty property : properties)
				{
					PropertyProtection protection = defaultProtection;
					CollectionHandling collectionHandling = defaultCollectionHandling;

					Transient propertyTransient = property.getAnnotation(Transient.class);

					// do not process Transient properties
					if (propertyTransient != null) {
						continue;
					}

					Merge propertyMerge = property.getAnnotation(Merge.class);

					if (propertyMerge != null)
					{
						if (propertyMerge.propertyProtection() != PropertyProtection.DEFAULT)
						{
							protection = propertyMerge.propertyProtection();
						}

						if (propertyMerge.collectionHandling() != CollectionHandling.DEFAULT)
						{
							collectionHandling = propertyMerge.collectionHandling();
						}
					}

					if (protection != PropertyProtection.NEVER_MERGE)
					{
						Class<?> propertyClass = property.getType();

						Method sourceWriteMethod = property.getWriteMethod();
						Method sourceReadMethod = property.getReadMethod();

						// smarkoff: new logic 3/2012
						// If the target is not the same class as the source, it may not have the same methods/properties
						// Allow the source to have additional methods/properties that the target may not have
						// and use the appropriate methods when the target does have -- 2 methods of the same name are
						// not the same method if one is on a super class and one is on a subclass (or if they are 2
						// classes without a common base class).
						Method targetReadMethod = sourceReadMethod;
						Method targetWriteMethod = sourceWriteMethod;
						Class<?> targetClass = target.getClass();
						if (!clazz.equals(targetClass)) {
							// check that target also has same readMethod and writeMethod
							try {
								targetReadMethod = targetClass.getMethod(sourceReadMethod.getName(), sourceReadMethod.getParameterTypes());
							}
							catch (NoSuchMethodException ex) {
								targetReadMethod = null;
								log.debug("merge(): target of class [" + targetClass.getSimpleName()
									+ "] does not have readMethod [" + sourceReadMethod.getName()
									+ "] that source class of [" + clazz.getSimpleName() + "] has.");
							}

							if (sourceWriteMethod != null) {
								try {
									targetWriteMethod = targetClass.getMethod(sourceWriteMethod.getName(), sourceWriteMethod.getParameterTypes());
								}
								catch (NoSuchMethodException ex) {
									targetWriteMethod = null;
								}
							}
						}

						if (sourceReadMethod != null && sourceWriteMethod != null && targetReadMethod != null && targetWriteMethod != null)
						{
							Object sourceValue = null;
							Object targetValue = null;

							try {
								sourceValue = sourceReadMethod.invoke(source, BLANK_ARGS);
								targetValue = targetReadMethod.invoke(target, BLANK_ARGS);
							}
							catch (Exception e) {
								throw new BeanMergeException("Could Not Obtain Values Of Property '"
										+ property.getName() + "' for class: " + clazz.getName(), e);
							}

							boolean merge = false;

							if (protection == PropertyProtection.OVERWRITE) {
								merge = true;
							}
							else if (protection == PropertyProtection.OVERWRITE_IF_NOT_DEFAULT) {
								switch (property.getPropertyType()) {
									case BOOLEAN: {
										merge = true;

										break;
									}

									case BYTE: {
										if (propertyMerge != null) {
											merge = !sourceValue.equals(propertyMerge.byteDefault());
										}
										else {
											merge = !sourceValue.equals(DEFAULT_BYTE_VALUE);
										}

										break;
									}

									case CHAR: {
										if (propertyMerge != null) {
											merge = !sourceValue.equals(propertyMerge.charDefault());
										}
										else {
											merge = !sourceValue.equals(DEFAULT_CHAR_VALUE);
										}

										break;
									}

									case SHORT: {
										if (propertyMerge != null) {
											merge = !sourceValue.equals(propertyMerge.shortDefault());
										}
										else {
											merge = !sourceValue.equals(DEFAULT_SHORT_VALUE);
										}

										break;
									}

									case INT: {
										if (propertyMerge != null) {
											merge = !sourceValue.equals(propertyMerge.integerDefault());
										}
										else {
											merge = !sourceValue.equals(DEFAULT_INT_VALUE);
										}

										break;
									}

									case LONG: {
										if (propertyMerge != null) {
											merge = !sourceValue.equals(propertyMerge.longDefault());
										}
										else {
											merge = !sourceValue.equals(DEFAULT_LONG_VALUE);
										}

										break;
									}

									case FLOAT: {
										if (propertyMerge != null) {
											merge = !sourceValue.equals(propertyMerge.floatDefault());
										}
										else {
											merge = !sourceValue.equals(DEFAULT_FLOAT_VALUE);
										}

										break;
									}

									case DOUBLE: {
										if (propertyMerge != null) {
											merge = !sourceValue.equals(propertyMerge.doubleDefault());
										}
										else {
											merge = !sourceValue.equals(DEFAULT_DOUBLE_VALUE);
										}

										break;
									}

									case OBJECT:
									default: {
										merge = (sourceValue != null);

										break;
									}
								}
							}

							if (merge) {
								Object[] args = new Object[1];

								args[0] = sourceValue;

								try {
									if (propertyClass.isArray()) {
										if (
											sourceValue == null ||
											targetValue == null ||
											collectionHandling == CollectionHandling.REPLACE
										)
										{
											targetWriteMethod.invoke(target, args);
										}
										else if (propertyClass.equals(byte[].class)) {
											// smarkoff: (added 8/2009)
											// This may not be complete code but works for the cases we have
											// (asset.file - anything else?)
											byte [] sourceArray = (byte []) sourceValue;
											byte [] targetArray = (byte []) targetValue;

											System.arraycopy(sourceArray, 0, targetArray, 0, targetArray.length);

											args[0] = targetArray;

											targetWriteMethod.invoke(target, args);
										}
										else {
											// This means that we have to basically merge
											Object[] sourceArray = (Object[]) sourceValue;
											Object[] targetArray = (Object[]) targetValue;

											Object[] sortedTarget = Arrays.copyOf(targetArray, targetArray.length);

											Arrays.sort(sortedTarget);

											Object[] holder = new Object[sourceArray.length];

											int newCount = 0;

											for (int x=0; x < sourceArray.length; x++) {
												int test = Arrays.binarySearch(sortedTarget, sourceArray[x]);

												if (test > -1) {
													// TODO: This means it's contained.  We'll need to do a deep merge
												}
												else {
													holder[newCount]=sourceArray[x];
													newCount++;
												}
											}

											Object[] newTarget = Arrays.copyOf(targetArray, targetArray.length + newCount);

											System.arraycopy(holder, 0, newTarget, targetArray.length, newCount);

											args[0] = newTarget;

											targetWriteMethod.invoke(target, args);
										}
									}
									else if (Collection.class.isAssignableFrom(propertyClass)) {
										log.info("Merging Collection " + property.getName()
											+ ": " + protection + " -- " + collectionHandling);

										if (
											sourceValue == null ||
											targetValue == null ||
											collectionHandling == CollectionHandling.REPLACE
										)
										{
											targetWriteMethod.invoke(target, args);
										}
										else {
											// This means that we have to basically merge
											Collection sourceCollection = (Collection) sourceValue;

											Collection targetCollection = (Collection) targetValue;

											for (Object test : sourceCollection) {
												if (targetCollection.contains(test)) {
													// TODO: This needs to be done for deep merges
													log.info("Should be merging item in collection " + property.getName());
												}
												else {
													log.info("Adding Item To Collection " + property.getName());

													targetCollection.add(test);
												}
											}
										}
									}
									else if (Map.class.isAssignableFrom(propertyClass)) {
										if (
											sourceValue == null ||
											targetValue == null ||
											collectionHandling == CollectionHandling.REPLACE
										)
										{
											targetWriteMethod.invoke(target, args);
										}
										else {
											// This means that we have to basically merge
											Map sourceMap = (Map) sourceValue;
											Map targetMap = (Map) targetValue;

											for (Object key : sourceMap.keySet()) {
												if (targetMap.containsKey(key)) {
													// TODO:  This is needed for deep merges.  Need to finish.
												}
												else {
													targetMap.put(key, sourceMap.get(key));
												}
											}
										}
									}
									else {
										if (deep && sourceValue != null && targetValue != null) {
											// TODO:  We desperately need to finish this to handle deep merges
										}
										else {
											targetWriteMethod.invoke(target, args);
										}
									}
								}
								catch (Exception e) {
									throw new BeanMergeException("Could Not Merge Property " + property.getName(), e);
								}
							}
						}
					}
				}
			}
		}
	}

	public static <T extends Object> List<T> clone(List<T> list, Class<T> _class)
	throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException
	{
		List<T> clone = new ArrayList<T>();
		for (T item : list) {
			T nitem = _class.newInstance();
			PropertyUtils.copyProperties(nitem, item);
			clone.add(nitem);
		}
		return clone;
	}

	public static <T extends Object> T clone(T obj, Class<T> _class)
	throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException
	{
		T clone = _class.newInstance();
		PropertyUtils.copyProperties(clone, obj);
		return clone;
	}
}
