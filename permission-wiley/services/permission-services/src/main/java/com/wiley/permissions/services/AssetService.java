package com.wiley.permissions.services;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetToSource;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.persistence.permissions.RenditionType;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.services.util.ServiceException;

public interface AssetService {

	/**
	 * Important: This method is only designed to be called
	 * from handling an updateAsset message - could be new or existing asset.
	 */
	public void saveAssetFromMessage(Asset asset) throws Exception;

	public void saveAssetFile(AssetFile file) throws PersistenceException;

	public void addSourceToAsset(Integer assetId, Integer sourceId)
			throws ServiceException;

	public BufferedImage generateMsgImage(String msg, RenditionType renditionType);

	public void deleteAssetByExternalId(String externalId)
			throws ServiceException, PersistenceException, MessageException;

	/**
	 * Used by messages - please use the above one if regular delete operation
	 * @param externalId
	 * @param sendMessage
	 * @throws ServiceException
	 * @throws PersistenceException
	 * @throws MessageException
	 */
	public void deleteAssetByExternalId(String externalId, boolean sendMessage)
			throws ServiceException, PersistenceException, MessageException;

	/**
	 * MESSAGES HANDLERS
	 */
	public void handleGetAssetMessage(List<Reference> refs)
			throws PersistenceException, MessageException;

	public void handleDeleteAssetMessage(List<Reference> refs)
			throws ServiceException, PersistenceException, MessageException;

	public void handleUpdateAssetMessage(List<Asset> assets) throws Exception;

	/**
	 *
	 * @param assetId  Must be non-null
	 * @param sourceId  Must be non-null
	 * @param notes  May be null
	 * @return true if a row was updated
	 */
	public boolean updateAssetToSourceNotes(Integer assetId, Integer sourceId, String notes) throws PersistenceException;

	public void saveAssetToSource(AssetToSource a2s) throws Exception;

	public void saveAsset(Asset asset) throws Exception;

	public Asset loadAssetByIdForManageAsset(int assetId) throws PersistenceException;

	public AssetFile loadAssetFileOfRenditionType(int assetId, RenditionType renditionType) throws PersistenceException, FileNotFoundException, IOException;
}
