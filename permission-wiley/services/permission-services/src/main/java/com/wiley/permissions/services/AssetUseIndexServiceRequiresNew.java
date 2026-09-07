package com.wiley.permissions.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetUseRepository;

/**
 * It is assumed that only one instance of this class will
 * be created (being a service class).

 * @since  JDK 1.6, Lucene 3.0
 * @author smarkoff
 */
public class AssetUseIndexServiceRequiresNew extends BaseService
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(AssetUseIndexServiceRequiresNew.class);

	private AssetUseRepository assetUseRepository;
	private AssetUseIndexService assetUseIndexService;


	/**
	 * Should only be called by AssetUseIndexService.
	 * (Can't make protected or Transactional annotation will be ignored.)
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Document buildDocument(Integer auId) throws PersistenceException {
		AssetUse au = assetUseRepository.loadAssetUseById(auId);
		return assetUseIndexService.buildDocument(au);
	}


	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	/**
	 * Identical to the same method in AssetUseIndexService but needed for
	 * AssetSerachSearch to call this method and get a new transaction.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AssetUse updateIndex(int auId) throws PersistenceException {
		AssetUse au = assetUseRepository.loadAssetUseById(auId);
		assetUseIndexService.updateIndex(au);
		return au;
	}
}
