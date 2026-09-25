package com.wiley.permissions.persistence.repository;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.bean.BeanMergeException;
import com.wiley.permissions.common.bean.BeanProperty;
import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.common.bean.BeanUtility.ObjectType;
import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.common.utils.InstanceMap;
import com.wiley.permissions.common.utils.InstanceOnlySet;
import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.permissions.domain.MaterializationKey.Mode;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.sf.common.monitor.PerformanceMonitor;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

public class JPARepository {

	private static final Log log = LogFactory.getLog(JPARepository.class);

	protected PerformanceMonitor monitor = null;

	// we put the entityManager in JPARepository so we do not have to overwrite in every single repository
	// and also so we can instantiate a JPARepository if needed
	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	protected EntityManager getEntityManager() {
		return entityManager;
	}

	public <T extends Object> T merge(T object) throws PersistenceException {
		return getEntityManager().merge(object);
	}

	public void clear() throws PersistenceException {
		getEntityManager().clear();
	}

	public <T extends Object> T persist(T object) throws PersistenceException {
		getEntityManager().persist(object);
		return object;
	}

	public void remove(Object object) throws PersistenceException {
		getEntityManager().remove(object);
	}

	public Query createQuery(String sqlString) {
		return getEntityManager().createQuery(sqlString);
	}

	public <T extends Object> TypedQuery<T> createQuery(String sqlString, Class<T> clazz) {
		return getEntityManager().createQuery(sqlString, clazz);
	}

	public <T extends Object> Query createNativeQuery(String sqlString) {
		return getEntityManager().createNativeQuery(sqlString);
	}

	public <T extends Object> Query createNativeQuery(String sqlString, Class<T> clazz) {
		return getEntityManager().createNativeQuery(sqlString, clazz);
	}

	public <T extends Object> Query createNativeQuery(String sqlString, String resultSetMapping) {
		return getEntityManager().createNativeQuery(sqlString, resultSetMapping);
	}

	public Query createNamedQuery(String name) {
		return getEntityManager().createNamedQuery(name);
	}

	public <T extends Object> TypedQuery<T> createNamedQuery(String name, Class<T> clazz) {
		return getEntityManager().createNamedQuery(name, clazz);
	}

	/**
	 * Loads an object from the DB with it's given Primary Key object. lnagy - cannot get rid of this method
	 * because of return NULL if NoResultException
	 *
	 * @param Object
	 * @return Object
	 * @throws PersistenceException
	 */
	public <T extends Object> T find(Class<T> clazz, Object pk) throws PersistenceException
	{
		return find (getEntityManager(), clazz, pk);
	}

	public <T extends Object> T find(EntityManager em, Class<T> clazz, Object pk) throws PersistenceException
	{
		// based on some errors in PERM-MULE-PROC, sometimes this method is called with a null pk
		if (null == pk) {
			log.debug("find(): called with PK null, class " + clazz);
			return null;
		}

		log.trace("find(): id [" + pk.toString() + "] class [" + clazz.getName() + "]");

		T output = null;

		try {
			output = em.find(clazz, pk);
		}
		catch (NoResultException e) {
			// Do Nothing
		}

		return output;
	}

	/**
	 * Loads all objects for a specified class.
	 *
	 * @param Class clazz
	 * @return List<Object>
	 *
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public <T extends Object> List<T> loadAll(Class<T> clazz) throws PersistenceException
	{
		return loadAll(clazz, null);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public <T extends Object> List<T> loadAll(Class<T> clazz, Properties properties)
	throws PersistenceException
	{
		return loadAll (getEntityManager(), clazz, properties);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public <T extends Object> List<T> loadAll(EntityManager em, Class<T> clazz, Properties properties)
			throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("JPARepository::loadAll::" + clazz.getSimpleName());

		// First separate possible "orderBy" from rest of the properties
		String orderBy = null;

		if (properties != null && properties.size() > 0) {
			orderBy = properties.getProperty("orderBy");
			if (orderBy != null) {
				properties.remove("orderBy");
			}
		}

		StringBuilder queryBuild = new StringBuilder("select obj123752 from ");
		queryBuild.append(clazz.getName()).append(" obj123752");

		Map<String, Object> values = new HashMap<String, Object>();

		if (properties != null && properties.size() > 0) {
			queryBuild.append(" WHERE ");
			Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();

			while (iterator.hasNext()) {
				Entry<Object, Object> entry = iterator.next();
				String key = (String) entry.getKey();
				String varName = key + "Value";
				String value = (String) entry.getValue();
				queryBuild.append(key).append("= :").append(varName);

				if (iterator.hasNext()) {
					queryBuild.append(" AND ");
				}

				Object newValue = value;
				BeanProperty prop = BeanUtility.getPropertyFromClass(clazz, key);

				if (prop != null) {
					Class<?> type = prop.getType();

					if (prop.getPropertyType() != ObjectType.OBJECT) {
						PropertyEditor editor = PropertyEditorManager.findEditor(type);

						if (editor != null) {
							editor.setAsText(value);
							newValue = editor.getValue();
						}
					}
				}

				values.put(varName, newValue);
			}
		}

		if (StringUtils.isNotBlank(orderBy)) {
			queryBuild.append(" ORDER BY ").append(orderBy);
		}

		String queryString = queryBuild.toString();
		log.debug("loadAll(): queryString: " + queryString);
		TypedQuery<T> query = em.createQuery(queryString, clazz);

		for (Entry<String, Object> entry : values.entrySet()) {
			query.setParameter(entry.getKey(), entry.getValue());
		}

		List<T> output = query.getResultList();
		timer.stopTimer();

		return output;
	}

	/**
	 * Generally to be used only upon receiving a message. Designed to load object from db and overwrite only
	 * fields that are set in messages - because otherwise null fields in message would update db to null.
	 * This is the doozy.
	 *
	 * @param <T>
	 * @param object
	 * @return
	 * @return
	 */
	public <T extends Object> T savePartialEntity(T input) throws PersistenceException
	{
		return savePartialEntity(getEntityManager(), input);
	}

	private <T extends Object> T savePartialEntity(EntityManager em, T input) throws PersistenceException
	{
		if (input == null)  return null;

		PerfTimer timer = monitor.startTimer("JPARepository::savePartialEntity::" + input.getClass().getSimpleName());

		T output = null;
		Set<Object> entities = new InstanceOnlySet<Object>();
		Map<Object, Object> materialized = new InstanceMap<Object, Object>();

		try {
			gatherEntities(em, input, entities, 0);
			// monitor.addMilestone("JPARepository::savePartialEntity::" +
			// input.getClass(), "gatherEntities");
		}
		catch (Exception e) {
			log.debug("savePartialEntity(): caught exception from gatherEntities(): ", e);
			throw new PersistenceException("Could Not Gather Entities", e);
		}

		for (Object object : entities) {
			Object tmp = materialize(em, object);

			if (tmp != object) {
				materialized.put(object, tmp);

				if (object == input) {
					output = (T) tmp;
				}
			}
		}
		// monitor.addMilestone("JPARepository::savePartialEntity::" +
		// input.getClass(), "materialize");

		// Now we need to simply replace all the instance of the replaced
		// objects with the instances they were replaced with.

		log.info("savePartialEntity(): About to replace " + entities.size() + " with " + materialized.values().size());

		// Set alreadyDone = new InstanceOnlySet();

		for (Object object : entities) {
			replaceEntities(object, materialized, entities);
		}

		// monitor.addMilestone("JPARepository::savePartialEntity::" +
		// input.getClass(), "replaceEntities");

		// There was no replacement for the input object, so we'll just return it
		if (output == null) {
			output = input;
		}

		try {
			mergeEntities(materialized);
		}
		catch (BeanMergeException e) {
			throw new PersistenceException("Could Not Merge Entities", e);
		}
		// monitor.addMilestone("JPARepository::savePartialEntity::" +
		// input.getClass(), "mergeEntities");

		// Everything we needed is done, let's merge.
		log.debug("savePartialEntity(): Class = " + output.getClass().getName());

		if (hasNullPersistenceId(output)) {
			log.debug("savePartialEntity(): null @Id object - PERSIST");
			em.persist(output);
		}
		else {
			log.debug("savePartialEntity(): non-null @Id object - MERGE");
			em.merge(output);
		}
		// monitor.addMilestone("JPARepository::savePartialEntity::" +
		// input.getClass(), "persist");

		timer.stopTimer();

		return output;
	}

	/**
	 * This method materialized an entity using it's materialization keys. If an object in the database is
	 * found with the matching keys, it is returned. Otherwise, the input is returned. Please keep in mind
	 * that if the object is found, then the returned object will not have any changes from the input object.
	 * It will be a fresh version from the database.
	 *
	 * @param Object
	 * @param input
	 * @return
	 * @throws com.wiley.permissions.persistence.PersistenceException
	 */
	private <T extends Object> T materialize(EntityManager em, T input) throws PersistenceException
	{
		T output = null;

		if (input != null) {
			Class<?> clazz = input.getClass();
			T test = null;

			if (isEntity(clazz)) {
				if (!em.contains(input)) {
					List<BeanProperty> properties = BeanUtility.getAllProperties(clazz);
					Map<BeanProperty, Object> uniqueKeys = new HashMap<BeanProperty, Object>();
					Map<BeanProperty, Object> additiveKeys = new HashMap<BeanProperty, Object>();
					boolean doAdditive = true;

					// We need to gather all the keys we can use to materialize
					// this thing.
					for (BeanProperty property : properties) {
						MaterializationKey key = property.getAnnotation(MaterializationKey.class);

						if (key != null) {
							Method readMethod = property.getReadMethod();
							Object value = null;

							try {
								value = readMethod.invoke(input, BeanUtility.BLANK_ARGS);
							}
							catch (IllegalAccessException e) {
								throw new PersistenceException("Cannot Read Value For Property "
										+ property.getName() + " for object type: "
										+ input.getClass().getName(), e);
							}
							catch (IllegalArgumentException e) {
								throw new PersistenceException("Cannot Read Value For Property "
										+ property.getName() + " for object type: "
										+ input.getClass().getName(), e);
							}
							catch (InvocationTargetException e) {
								throw new PersistenceException("Cannot Read Value For Property "
										+ property.getName() + " for object type: "
										+ input.getClass().getName(), e);
							}

							if (value != null) {
								boolean entity = isEntity(value);

								if (entity && !em.contains(value)) {
									// This means that one of our keys isn't
									// materialzied. We need that to happen
									// first.
									value = materialize(em, value);

									// lnagy - if it is part of Additive key, we
									// need to add also the NULL value as part
									// of the key
									if ((value == null || !em.contains(value)) && key.mode() == Mode.ADDITIVE
											&& doAdditive)
									{
										additiveKeys.put(property, null);
									}
								}

								// This bit of logic is hard. Usually, you'd
								// know it's transient by the fact
								// that it's primary key is null or the default
								// value. However, in the case
								// of some meta-data objects, we get that key.
								// So it's not null, but the object
								// isn't associated with the EntityManager. We
								// need to detect that.
								//
								// If the EntityManager is unaware, then that
								// means that we don't have a version
								// of this object in the DB and it cannot be
								// used as a key.

								if (!entity || (entity && em.contains(value))) {
									if (key.mode() == Mode.UNIQUE) {
										uniqueKeys.put(property, value);
									}
									else if (key.mode() == Mode.ADDITIVE && doAdditive) {
										additiveKeys.put(property, value);
									}
								}
							}
							else {
								if (key.mode() == Mode.ADDITIVE) {
									if (key.nullable()) {
										doAdditive = false;
										additiveKeys.clear();
									}
								}
							}
						}
					}

					String className = input.getClass().getName();
					// smarkoff: If don't do this fix then get IllegalArgumentException on line
					// below where tries to set parameter marker -- which is an odd error
					// message to get (seems unrelated to the problem).
					int assistIndex = className.indexOf("_$$_javassist");
					if (assistIndex != -1) {
						log.info("materialize(): correcting className of [" + className + "]");
						className = className.substring(0, assistIndex);
					}
					String queryBaseStr = "from " + className + " test where ";

					Iterator<Entry<BeanProperty, Object>> it = uniqueKeys.entrySet().iterator();

					while (it.hasNext() && test == null) {
						Entry<BeanProperty, Object> entry = it.next();
						BeanProperty property = entry.getKey();
						MaterializationKey mk = property.getAnnotation(MaterializationKey.class);
						Object value = entry.getValue();
						String queryStr = queryBaseStr;
						String propName = "test." + property.getName();

						if (mk.alwaysTrim() && property.getPropertyType() == ObjectType.STRING) {
							propName = "TRIM(" + propName + ")";
							value = ((String) value).trim();
						}

						if (mk.ignoreCase()
								&& (property.getPropertyType() == ObjectType.STRING || property
										.getPropertyType() == ObjectType.CHAR))
						{
							propName = "LOWER(" + propName + ")";
							value = ((String) value).toLowerCase();
						}

						// Hibernate 5.6+ rejects legacy HQL positional "?" — must use JPA "?1"
						queryStr += propName + " = ?1";
						Query query = em.createQuery(queryStr);
						query.setParameter(1, value);

						try {
							test = (T) query.getSingleResult();
							log.debug("materialize(): No exception (got result) running unique queryStr = [" + queryStr + "], param value = " + value);
						}
						catch (NoResultException e) {
							log.debug("materialize(): Got NoResultException (OK) with unique queryStr = [" + queryStr + "], param value = " + value);
						}
						catch (Exception e) {
							log.warn("materialize(): caught exception running unique queryStr = [" + queryStr + "], param value = " + value, e);
							// TODO: this throw is causing problems with the product import attempting to get
							// a user record
							// we need to figure out why this is happening and fix it
							// but for now, ignoring this condition allows the product import to work.
							// throw new PersistenceException("Cannot Find Object " +
							// input.getClass().getName()
							// + " By Unique Materialization Key: " + property.getName(), e);
						}
					}

					if (test == null && additiveKeys.size() > 0 && doAdditive) {
						it = additiveKeys.entrySet().iterator();
						String additiveQueryStr = queryBaseStr;

						while (it.hasNext()) {
							Entry<BeanProperty, Object> entry = it.next();
							BeanProperty property = entry.getKey();
							Object value = entry.getValue();

							if (null == value) {
								additiveQueryStr += " test." + property.getName() + " IS NULL ";
							}
							else {
								additiveQueryStr += " test." + property.getName() + " = :"
										+ property.getName();
							}

							if (it.hasNext()) {
								additiveQueryStr += " AND";
							}
						}

						log.debug("materialize(): additiveQueryStr: " + additiveQueryStr);
						Query query = em.createQuery(additiveQueryStr);
						it = additiveKeys.entrySet().iterator();
						String values = "";

						while (it.hasNext()) {
							Entry<BeanProperty, Object> entry = it.next();
							BeanProperty property = entry.getKey();
							Object value = entry.getValue();
							values += value + ",";

							if (null != value) {
								query.setParameter(property.getName(), value);
							}
						}

						try {
							test = (T) query.getSingleResult();
						}
						catch (NoResultException e) {
							// Do Nothing
						}
						catch (Exception e) {
							String msg = "caught exception finding object [" + input.getClass().getName()
								+ "] running additiveQueryStr = [" + additiveQueryStr + "], value(s) = " + values;
							log.warn("materialize(): " + msg, e);
							throw new PersistenceException(msg, e);
						}
					}
				}
				// monitor.stopTimer("JPAManager::materialize::" +
				// input.getClass());
			}

			if (test != null) {
				output = test;
			}
			else {
				output = input;
			}
		}
		return output;
	}

	/**
	 * Method to help load by id and some lazy properties.
	 * For the lazy loaded properties just call getId method to instantiate the objects.
	 * We handle just Entity objects and collections for now.
	 * This method can be improved to handle more situations as needed.
	 * Now handles nested properties (e.g. "assetUse.asset.sources")
	 * @param <T>
	 * @param object
	 * @param properties
	 * @return Object
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public <T extends Object> T lazyLoad (Class<T> clazz, Object id, String[] properties) throws Exception
	{
		T object = find(clazz, id);

		if (object == null || properties == null)
			return null;

		if (isEntity(object)) {
			for (String property : properties) {
				lazyLoad(object, property);
			}
		}
		return object;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public void lazyLoad(Object object, String property) throws Exception
	{
		// split possible nested property such as "assetUse.asset.sources" into head and tail for recursion
		String propHead = property;
		String propTail = null;
		int dotIndex = property.indexOf('.');
		if (dotIndex != -1) {
			if (dotIndex == 0) {
				throw new IllegalArgumentException("Illegal property format [" + property + "] cannot start with dot.");
			}
			propHead = property.substring(0, dotIndex);
			if (property.length() == dotIndex) {
				throw new IllegalArgumentException("Illegal property format [" + property + "] cannot end with dot.");
			}
			propTail = property.substring(dotIndex + 1);
		}
		property = propHead;

		BeanProperty prop = BeanUtility.getPropertyFromClass(object.getClass(), property);
		log.debug("lazyLoad(): property: " + property);
		if (null == prop) {
			log.debug("lazyLoad(): No such property [" + property + "] in class [" + object.getClass().getName() + "]");
			return;
		}
		Method readMethod = prop.getReadMethod();
		Object propValue = readMethod.invoke(object, new Object[] {});
		if (propValue == null)
			return;
		Class<?> propValueClass = propValue.getClass();

		if (propValueClass.isArray()) {
			Object values[] = (Object[]) propValue;
			for (Object value : values) {
				List<BeanProperty> fields = BeanUtility.getProperties(value.getClass(), Id.class);
				for (BeanProperty field : fields) {
					BeanUtility.getPropertyValue (field, value);
				}
			}
		}
		else if (propValue instanceof Collection<?>) {
			for (Object value : (Collection<?>) propValue) {
				List<BeanProperty> fields = BeanUtility.getProperties(value.getClass(), Id.class);
				for (BeanProperty field : fields) {
					BeanUtility.getPropertyValue (field, value);
				}
			}
		}
		else {
			// Reading only the @Id field never triggers real initialization of a Hibernate proxy
			// (the identifier is already known without a DB round-trip); force a real load here so
			// the view doesn't hit a LazyInitializationException after this transaction closes.
			Hibernate.initialize(propValue);
			List<BeanProperty> fields = BeanUtility.getProperties(propValue.getClass(), Id.class);
			for (BeanProperty field : fields) {
				BeanUtility.getPropertyValue (field, propValue);
			}
			if (StringUtils.isNotBlank(propTail)) {
				log.debug("lazyLoad(): recursing [" + propTail + "]");
				lazyLoad(propValue, propTail);
			}
		}
	}

	private void gatherEntities(EntityManager em, Object object, Set<Object> currentSet, int level) throws Exception
	{
		if (object == null) return;

		Class<?> clazz = object.getClass();
		String className = object.getClass().getName();

		if (!className.startsWith("java.lang.") && !className.startsWith("java.sql.") && !className.startsWith("java.util.")) {
			StringBuilder sb = new StringBuilder("gatherEntities(): level " + level + " className: ");
			for (int i = 0; i < level; i++) {
				sb.append("  ");
			}
			sb.append(className);
			log.debug(sb.toString());
		}

		// Napoleon fix - excluding common work to prevent circular relation between beans
		// With Hibernate, className will look like "com.wiley.permissions.domain.persistence.permissions.CommonWork_$$_javassist_59".
		// (note the javassist stuff on the end)
		if (className.startsWith("com.wiley.permissions.domain.persistence.permissions.CommonWork")) {
			CommonWork cw = (CommonWork) object;
			if (cw.getCode().contains("perm.cw")) {
				log.debug("gatherEntities(): Ignoring dummy CW: " + cw);
				return;
			}
		}
		// end of Napoleon fix

		if (object instanceof Collection<?>) {
			for (Object value : (Collection<?>) object) {
				gatherEntities(em, value, currentSet, level + 1);
			}
		}
		else if (object instanceof Map<?, ?>) {
			Map<?, ?> tmpMap = (Map<?, ?>) object;
			for (Object value : tmpMap.entrySet()) {
				Entry entry = (Entry) value;
				gatherEntities(em, entry.getKey(), currentSet, level + 1);
				gatherEntities(em, entry.getValue(), currentSet, level + 1);
			}
		}
		// smarkoff: do NOT use "if (clazz.isArray())" because this will return true for a primitive array (eg. byte[] or int[])
		else if (object instanceof Object[]) {
			Object values[] = (Object[]) object;
			for (Object value : values) {
				gatherEntities(em, value, currentSet, level + 1);
			}
		}
		else {
			if (isEntity(object) && !currentSet.contains(object)) {
				currentSet.add(object);

				// Note BeanUtility.getAllProperties() will ignore Transient methods
				List<BeanProperty> properties = BeanUtility.getAllProperties(clazz);

				for (BeanProperty property : properties) {
					Merge merge = property.getAnnotation(Merge.class);
					if (merge == null || merge.propertyProtection() != PropertyProtection.NEVER_MERGE) {
						Method readMethod = property.getReadMethod();
						Object value = readMethod.invoke(object, new Object[] {});
						gatherEntities(em, value, currentSet, level + 1);
					}
					else {
						String shortName = ClassUtils.getShortClassName(className);
						log.debug("gatherEntities(): stop traverse requested at property ["
							+ shortName + "." + property.getName() + "], level " + level);
					}
				}
			}
		}
	}

	// This method is one of the most painful in the process of persisting
	// and merging the entities involved.
	private <T extends Object> T replaceEntities(T input, Map<Object, Object> replacements, Set<?> allEntities)
			throws PersistenceException
	{
		T output = null;

		if (input != null) {
			Class<?> clazz = input.getClass();

			if (input instanceof Collection<?>) {
				Collection<Object> original = (Collection<Object>) input;
				Object values[] = new Object[original.size()];
				values = original.toArray(values);
				original.clear();

				for (Object value : values) {
					Object newVal = replaceEntities(value, replacements, allEntities);
					original.add(newVal);
				}
			}
			else if (input instanceof Map<?, ?>) {
				Map<Object, Object> original = (Map<Object, Object>) input;
				Object keys[] = new Object[original.size()];
				Object values[] = new Object[original.size()];
				keys = original.keySet().toArray(keys);
				values = original.keySet().toArray(values);
				original.clear();

				for (int x = 0; x < keys.length; x++) {
					Object key = replaceEntities(keys[x], replacements, allEntities);
					Object value = replaceEntities(values[x], replacements, allEntities);
					original.put(key, value);
				}
			}
			// smarkoff: do NOT use "if (clazz.isArray())" because this will return true for a primitive array (eg. byte[] or int[])
			else if (input instanceof Object[]) {
				Object values[] = (Object[]) input;

				for (int x = 0; x < values.length; x++) {
					Object value = replaceEntities(values[x], replacements, allEntities);
					values[x] = value;
				}
			}
			else if (allEntities.contains(input)) {
				output = (T) replacements.get(input);

				// Note BeanUtility.getAllProperties() will ignore Transient methods
				List<BeanProperty> properties = BeanUtility.getAllProperties(clazz);

				for (BeanProperty property : properties) {
					Method readMethod = property.getReadMethod();
					Object oldValue = null;

					try {
						oldValue = readMethod.invoke(input, BeanUtility.BLANK_ARGS);
					}
					catch (Exception e) {
						throw new PersistenceException("Cannot Read Value For Property " + property.getName()
								+ " for object type: " + input.getClass().getName(), e);
					}

					if (oldValue != null) {
						if (oldValue instanceof Collection<?>) {
							replaceEntities(oldValue, replacements, allEntities);
						}
						else if (oldValue instanceof Map<?, ?>) {
							replaceEntities(oldValue, replacements, allEntities);
						}
						else if (property.getType().isArray()) {
							replaceEntities(oldValue, replacements, allEntities);
						}
						else if (isEntity(oldValue)) {
							Object newValue = replacements.get(oldValue);

							if (newValue != null) {
								Method writeMethod = property.getWriteMethod();

								try {
									Object args[] = new Object[] { newValue };
									writeMethod.invoke(input, args);
								}
								catch (Exception e) {
									throw new PersistenceException("Cannot Set Value For Property "
											+ property.getName() + " for object type: "
											+ input.getClass().getName(), e);
								}
							}
						}
					}
				} // end for
			}
		}

		if (output == null) {
			output = input;
		}
		return output;
	}

	/**
	 * This method merges entities that were scheduled for replacement by the other savePartial methods.
	 *
	 * @param replacements
	 * @throws com.wiley.permissions.common.bean.BeanMergeException
	 */
	private void mergeEntities(Map<Object, Object> replacements) throws BeanMergeException
	{
		// monitor.startTimer("JPAManager::mergeEntities");
		for (Object original : replacements.keySet()) {
			Object replacement = replacements.get(original);

			if (replacement != null && original != replacement) {
				BeanUtility.merge(original, replacement, false);
			}
		}
		// monitor.stopTimer("JPAManager::mergeEntities");
	}

	private boolean isEntity(Class<?> clazz) {
		boolean output = false;

		if (clazz != null) {
			if (BeanUtility.getAnnotation(Entity.class, clazz) != null) {
				output = true;
			}
		}

		return output;
	}

	private boolean isEntity(Object object) {
		boolean output = false;

		if (object != null) {
			output = isEntity(object.getClass());
		}

		return output;
	}

	/**
	 * Determines if an Entity object has a null primary key value (as indicated by Id annotation)
	 * (and therefore is definitely not attached to the EntityManager context).
	 */
	private boolean hasNullPersistenceId(Object object) {
		boolean output = false;

		if (object != null) {
			Entity objectEntity = BeanUtility.getAnnotation(Entity.class, object.getClass());

			if (objectEntity != null) {
				List<BeanProperty> props = BeanUtility.getAllProperties(object.getClass());

				for (int x = 0; x < props.size() && !output; x++) {
					BeanProperty prop = props.get(x);
					Id idTest = prop.getAnnotation(Id.class);

					if (idTest != null) {
						Method readMethod = prop.getReadMethod();
						Object value = null;

						try {
							value = readMethod.invoke(object, BeanUtility.BLANK_ARGS);
						}
						catch (Exception e) {
						}

						if (value == null) {
							output = true;
						}
					}
				}
			}
			// monitor.startTimer("JPAManager::isTransient::" +
			// object.getClass());
		}
		return output;
	}

	/**
	 * Returns a formatted string with all the constraintViolations after a bean has been validated
	 *
	 * @param cve
	 * @return String
	 */
	public static String buildValidationErrorMessage(ConstraintViolationException cve) {
		return buildValidationErrorMessage(cve.getConstraintViolations());
	}

	public static String buildValidationErrorMessage(Set<? extends ConstraintViolation<?>> set) {
		StringBuilder sb = new StringBuilder();

		for (ConstraintViolation<?> cv : set) {
			sb.append("invalid value for: '" + cv.getPropertyPath().toString() + "': "
					+ cv.getMessage() + "; ");
			sb.append("\r\n------------------------------------------------\r\n");
			sb.append("Entity: " + cv.getRootBeanClass().getSimpleName() + "\r\n");
			// The violation occurred on a leaf bean
			if (cv.getLeafBean() != null && cv.getRootBean() != cv.getLeafBean())
			{
				sb.append("Embeddable: " + cv.getLeafBean().getClass().getSimpleName() + "\r\n");
			}
			sb.append("Attribute: " + cv.getPropertyPath() + "\r\n");
			sb.append("Invalid value: " + cv.getInvalidValue() + "\r\n");
		}

		return sb.toString();
	}

	/**
	 * smarkoff: Right now this method is only called by ProductServiceImpl.saveMasterLists().
	 * If it were to be called by other things then we'd refactor some of the logic
	 * in saveMasterLists().
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor=Exception.class)
	public void savePartialRequiresNew(Object o) throws Exception
	{
		savePartialEntity(o);
	}

	@Transactional(propagation = Propagation.REQUIRED, rollbackFor=Exception.class)
	public <T extends Object> T save (T object) throws PersistenceException
	{
		return getEntityManager().merge(object);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor=Exception.class)
	public <T extends Object> T saveRequiresNew (T object) throws PersistenceException
	{
		return getEntityManager().merge(object);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor=Exception.class)
	public void removeRequiresNew (Object object) throws PersistenceException
	{
		// attach the instance, to be removed
		object = getEntityManager().merge (object);
		getEntityManager().remove(object);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public int executeNativeQuery (String sqlStmt, List<?> params) {
		Query q = getEntityManager().createNativeQuery(sqlStmt);
		if (CollectionUtils.isNotEmpty(params)) {
			int i = 1;
			for (Object param : params) {
				q.setParameter(i++, param);
			}
		}
		int numRows = q.executeUpdate();
		log.debug("executeNativeQuery(): params: " + (params == null ? "null" : StringUtils.join(params, ", "))
			+ "\r\nsql: " + sqlStmt);
		log.debug("executeNativeQuery(): updated/inserted/deleted " + numRows + " rows");
		return numRows;
	}

	@SuppressWarnings("unchecked")
    @Transactional(propagation = Propagation.REQUIRED)
	public <T extends Object> T executeSingleResultNamedQuery (String namedQuery, Object[] params) {
		return (T)executeNamedQuery(namedQuery, params, true);
	}

	@SuppressWarnings("unchecked")
    @Transactional(propagation = Propagation.REQUIRED)
	public <T extends Object> T executeMultiResultNamedQuery (String namedQuery, Object[] params) {
		return (T)executeNamedQuery(namedQuery, params, false);
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> T executeNamedQuery (String namedQuery, Object[] params, boolean singleResult) {
		Query q = getEntityManager().createNamedQuery(namedQuery);
		if (null != params && params.length > 0) {
			int i = 1;
			for (Object param : params) {
				q.setParameter(i++, param);
			}
		}

		log.debug("executeNamedQuery(): params: " + (params == null ? "null" : StringUtils.join(params, ", "))
				+ "\r\nnamed query : " + namedQuery);

		if (singleResult) {
			return (T) q.getSingleResult();
		} else {
			List<?> results = q.getResultList();
			return (T) results;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public <T> void remove(Class<T> clazz, Object key) throws PersistenceException
	{
		Object object = find(clazz, key);

		// Not expected but check
		if (object == null) {
			log.warn("remove(): object not found with key [" + key + "] class [" + clazz.getSimpleName() + "]");
			return;
		}

		getEntityManager().remove(object);
	}

	public int getCount(Class<?> clazz) throws PersistenceException
	{
		try {
			Query query = getEntityManager().createQuery("select count(obj123752) from " + clazz.getName() + " obj123752");
			Number result = (Number) query.getSingleResult();
			log.debug("getCount(): found " + (result.intValue() > 0));
			return result.intValue();
		} catch (Exception e) {
			throw new PersistenceException("Failed to return count", e);
		}
	}

	public PerformanceMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(PerformanceMonitor monitor) {
		this.monitor = monitor;
	}
}
