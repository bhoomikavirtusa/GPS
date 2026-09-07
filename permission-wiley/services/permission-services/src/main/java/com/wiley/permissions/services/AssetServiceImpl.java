package com.wiley.permissions.services;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetToSource;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.MessageErrorOp;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.persistence.permissions.RenditionType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.services.message.CMSMessageService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.sf.common.image.ScaleImage;
import com.wiley.sf.common.lang.ResourceUtil;
import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 * lnagy - for now all methods are Transactional (SUPPORT) because of the Filter
 * (when the filter tries to persist the session we get detached entity tried to persist)
 * @author lnagy
 */
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class AssetServiceImpl extends BaseService implements AssetService
{
	private static final Log log = LogFactory.getLog(AssetService.class);

	private AssetUseService assetUseService;
	private CMSMessageService outgoingMessageService;

	private AssetRepository assetRepository;

	public void saveAssetFromMessage(Asset asset) throws Exception
	{
		getMonitor().startTimer("AssetService::saveAssetFromMessage");

		List<AssetFile> fileList = asset.getFiles();

		Asset old = assetRepository.saveAssetFromMessage(asset);
		// If we stored Asset.externalId, created_date, or last_updated_date
		// in the index then we'd want to split this method into 2 parts
		// to make sure a commit happens at this point and then reload
		// the Asset, but since we don't store any of those fields
		// in the index it doesn't matter.
		// (updateStatusForAsset will end up updating the index)
		// (updateStatusForAsset will reload the assets by id, so we will already have that)

		if (fileList != null) {
			for (AssetFile assetFile: fileList) {
				saveAssetFile(assetFile); // throws PersistenceException
			}
		}

		try {
			assetUseService.updateStatusForAsset(old);
		}
		catch (Exception ex) {
			log.error("Caught exception trying to update Permission status for Asset: ", ex);
		}
	}

	public void saveAssetFile(AssetFile file) throws PersistenceException
	{
		assetRepository.saveAssetFile(file);

		if (file.getRenditionType().equals(RenditionType.ORIGINAL)) {
			generateAndSaveThumbnail(file, RenditionType.SMALL_THUMBNAIL);
			generateAndSaveThumbnail(file, RenditionType.MEDIUM_THUMBNAIL);
			generateAndSaveThumbnail(file, RenditionType.LARGE_THUMBNAIL);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addSourceToAsset(Integer assetId, Integer sourceId)
	throws ServiceException
	{
		try {
			Asset asset = assetRepository.loadAssetById(assetId);

			assetRepository.addSourceToAsset(assetId, sourceId);

			assetUseService.updateStatusForAsset(asset);

			getOutgoingMessageService().sendUpdateAssetMessage(asset, null);

		} catch (Exception e) {
			throw new ServiceException (e);
		}
	}

	private AssetFile generateAndSaveThumbnail(AssetFile origFile, RenditionType renditionType)
	{
		try {
			Asset asset = origFile.getAsset();

			byte [] thumbData = null;

			String extension = FilenameUtils.getExtension(origFile.getObjectName());
			final String [] typesWeCanHandle = { "png", "jpg", "jpeg", "gif", "bmp", "wbmp", "tif", "tiff" };
			// TIF works when the extra jai/imageio jar files are present.
			// PSD: Tried some source code found on the web to deal with psd (Photoshop) files
			// but could not handle a simple single-layer file from CS4, although sort of
			// handled some older files with more than one layer.
			if (StringUtil.equalsAnyIgnoreCase(extension, typesWeCanHandle)) {
			    thumbData = ScaleImage.scaleToFitBox(origFile.getData(),
			    	renditionType.getMaxWidth(), renditionType.getMaxHeight(), AffineTransformOp.TYPE_BILINEAR, "png");
            	    // throws IOException
			}
			else {
				thumbData = generateThumbDataForMediaType(asset.getMediaType(), renditionType, false);
			}

			AssetFile thumbFile = new AssetFile();
			thumbFile.setAsset(asset);
			thumbFile.setData(thumbData);
			thumbFile.setFileFormat("image/png");
			thumbFile.setObjectName(renditionType.getCode() + ".png");
			thumbFile.setRenditionType(renditionType);
			saveAssetFile(thumbFile);  // throws PeristenceException

			return thumbFile;
		}
		catch (Exception ex) {
			log.error("Caught exception trying to generate/save " + renditionType.getCode() + " thumbnail", ex);
			return null;
		}
	}

	private byte [] generateThumbDataForMediaType(MediaType mediaType, RenditionType renditionType, boolean warnNoFile)
	throws FileNotFoundException, IOException
	{
		if (mediaType != null) {
			// First look for a canned image that we may have for the given mediaType.
			String cannedName = "/thumbnailsForMediaType/" + mediaType.getCode().toLowerCase() + ".png";

			if (ResourceUtil.doesResourceExist(getClass(), cannedName)) {
				byte [] thumbData = ResourceUtil.getByteArrayResource(getClass(), cannedName);
					// throws FileNotFoundException, IOException

				if (!renditionType.equals(RenditionType.LARGE_THUMBNAIL)) {
					thumbData = ScaleImage.scaleToFitBox(thumbData,
						renditionType.getMaxWidth(), renditionType.getMaxHeight(), AffineTransformOp.TYPE_BILINEAR, "png");
							// throws IOException
				}

				if (warnNoFile) {
					ByteArrayInputStream baIn = new ByteArrayInputStream(thumbData);
					BufferedImage bi = ImageIO.read(baIn);
					Graphics2D g = bi.createGraphics();
					g.setColor(Color.RED);
			        Font font = new Font(Font.DIALOG, Font.BOLD, 12);
			        g.setFont(font);
			        g.drawString("No File", 5, 15);

					ByteArrayOutputStream baOut = new ByteArrayOutputStream();
					ImageIO.write(bi, "png", baOut);  // throws IOException
					thumbData = baOut.toByteArray();
				}

				return thumbData;
			}
		}

		// Generally the following code won't get used unless mediaType is null (not common)
		// since we have canned thumbnails for all media types.

		// Generate a generic image based on the media type, checking for null.
		String mtName = "(unknown media type)";
		if (mediaType != null)  mtName = mediaType.getDescription();
		String msg = mtName;
		if (warnNoFile) msg = "No file for " + mtName;

		BufferedImage bi = generateMsgImage(msg, renditionType);
		ByteArrayOutputStream baOut = new ByteArrayOutputStream();
		ImageIO.write(bi, "png", baOut);  // throws IOException
		return baOut.toByteArray();
	}

	public BufferedImage generateMsgImage(String msg, RenditionType renditionType) {
		final int width = renditionType.getMaxWidth();
		final int height = renditionType.getMaxHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, width, height);

        Font font = new Font(Font.DIALOG, Font.BOLD, 12);
        g.setFont(font);

        g.setColor(Color.BLACK);
        int stringHeight = height / 3;
        g.drawString(String.valueOf(msg), 5, stringHeight);

        return image;
	}

	public void deleteAssetByExternalId (String externalId)
    throws ServiceException, PersistenceException, MessageException
    {
		assetRepository.deleteAssetByExternalId(externalId);
    }

	public void deleteAssetByExternalId(String externalId, boolean sendMessage)
	    throws ServiceException, PersistenceException, MessageException
	{
		Asset asset = assetRepository.loadAssetByExternalId(externalId);

		if (null == asset) return;

        if (CollectionUtils.isNotEmpty(asset.getAssetUses())) {
        	// rather then attempt a delete and maybe get a ConstraintViolationException,
        	// check for constraint violation up front b/c it's very difficult to get the nested
        	// ConstraintViolationException from the bitronix exception that is thrown
        	// in this situation
        	String replyId = null;  // TODO: Would like replyId to somehow be passed to this method
        	getOutgoingMessageService().sendErrorMessage(replyId, MessageErrorOp.CANNOT_DELETE,
				"Cannot delete the Asset with the given wid since it is referenced by an AssetUse",
				asset.getExternalId());
        }
        else {
        	assetRepository.deleteAssetAndFiles(asset.getId());

    		if (sendMessage) {
    			try {
    		        getOutgoingMessageService().sendDeleteAssetMessage(externalId);
    		            // throws MessageException
    			}
    			catch (Exception e) {
    				log.error("deleteAssetByExternalId(): Exception caught trying to send DeleteAsset message", e);
    			}
    		}
        }
	}

	public void handleGetAssetMessage(List<Reference> refs)
	    throws PersistenceException, MessageException
	{
		PerfTimer timer = getMonitor().startTimer("AssetService::handleGetAssetMessage");
		log.info("handleGetAssetMessage() called");

		//TODO: need to pass original message ID to this method somehow so can use for replyId
		String replyId = null;

		for (Reference ref: refs) {
		    Asset asset = assetRepository.loadAssetByExternalId(ref.getExternalId());
		        // throws PersistenceException
			if (asset == null) {
				getOutgoingMessageService().sendErrorMessage(replyId, MessageErrorOp.NOT_FOUND,
						"Could not find the Asset with the given wid", ref.getExternalId());
			}
			else {
				getOutgoingMessageService().sendUpdateAssetMessage(asset, replyId);
				    // throws MessageException
			}
		}
		timer.stopTimer();
	}

	public void handleDeleteAssetMessage(List<Reference> refs)
	    throws ServiceException, PersistenceException, MessageException
	{
		for (Reference ref: refs) {
			deleteAssetByExternalId(ref.getExternalId(), false);
		}
	}

	public void handleUpdateAssetMessage(List<Asset> assets) throws Exception
	{
		for (Asset asset : assets) {
			saveAssetFromMessage(asset);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Asset loadAssetByIdForManageAsset(int assetId) throws PersistenceException
	{
		Asset asset = assetRepository.loadAssetById(assetId);

		for (Source source : asset.getSourcesNotNull()) {
			source.getName();
		}

		for (AssetFile file : asset.getFilesNotNull()) {
			file.getObjectName();
		}

		return asset;
	}

	public boolean updateAssetToSourceNotes(Integer assetId, Integer sourceId, String notes) throws PersistenceException
	{
		return assetRepository.updateAssetToSourceNotes(assetId, sourceId, notes);
	}

	public void saveAssetToSource(AssetToSource a2s) throws Exception
	{
		assetRepository.saveRequiresNew(a2s);
	}

	public void saveAsset(Asset asset) throws Exception
	{
		assetRepository.saveRequiresNew(asset);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public AssetFile loadAssetFileOfRenditionType(int assetId, RenditionType renditionType) throws PersistenceException, FileNotFoundException, IOException
	{
		Asset asset = null;
		try
		{
			asset = getAssetRepository().loadAssetById( assetId);
		}
		catch (PersistenceException e)
		{
			log.debug("loadById(): failed to load by id [" + assetId + "]");
		}
	    // throws ServiceException

		if (asset == null) {
			throw new PersistenceException ("Asset not found");
		}

		AssetFile file = asset.getFileOfRenditionType(renditionType);

		if (file == null) {
			AssetFile origFile = asset.getOriginalFile();
			if (origFile != null) {
				file = generateAndSaveThumbnail(origFile, renditionType);
			}
		}

		if (file == null) {
			byte [] thumbData = generateThumbDataForMediaType(asset.getMediaType(), renditionType, true);

			file = new AssetFile();
			file.setData(thumbData);
			file.setFileFormat("image/png");
		}

		return file;
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService service) {
		assetUseService = service;
	}

	public CMSMessageService getOutgoingMessageService()
	{
		return outgoingMessageService;
	}

	public void setOutgoingMessageService(CMSMessageService outgoingMessageService)
	{
		this.outgoingMessageService = outgoingMessageService;
	}

	public AssetRepository getAssetRepository()
	{
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository)
	{
		this.assetRepository = assetRepository;
	}
}