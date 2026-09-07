package com.wiley.permissions.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.domain.persistence.permissions.EnumData;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;

/**
 * Contact Service does operations related to Contacts like create a source,
 * edit a source, find a source
 *
 * @author sputta
 */
public class EnumDataRepository extends JPARepository
{
	
	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;
	
	@Override
	protected EntityManager getEntityManager () {
		return entityManager;
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public <T extends EnumData> T findMetaDataByTypeAndCode(Class<T> type, String code)
	throws PersistenceException
	{
		T output = null;
		PersistenceUnit pu = BeanUtility.getAnnotation(PersistenceUnit.class, type);

		if (pu != null) {
			String name = pu.unitName();
			if (name.equalsIgnoreCase("permissions"))
				output = find(entityManager, type, code);
		}

		return output;
	}

	/**
	 * Loads enumdata of the specified type that match the supplied property key/value pairs
	 * @param <T>
	 * @param type
	 * @return
	 * @throws com.wiley.permissions.common.services.ServiceException
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public <T extends EnumData> List<T> getEnumDataByType(Class<T> type, Properties properties)
	throws PersistenceException
	{
		List<T> output = new ArrayList<T>();
		PersistenceUnit pu = BeanUtility.getAnnotation(PersistenceUnit.class, type);

		if (pu != null) {
			String name = pu.unitName();
			if (name.equalsIgnoreCase("permissions"))
				output = loadAll(entityManager, type, properties);
		}

		return output;
	}
}
