package com.wiley.permissions.web.shared.controllers.asset;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.RenditionType;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.services.AssetService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author smarkoff
 */
@Controller
public class AssetThumbnailController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(AssetThumbnailController.class);

	private AssetService assetService = null;
	private AssetRepository assetRepository;

	@RequestMapping("/product/assetThumbnail")
	public void serveThumbnail(
			@RequestParam("size") String size,
			@RequestParam(value = "assetId", required=false) Integer assetId,
			@RequestParam(value = "assetUseId", required=false) Integer auId,
			HttpServletResponse response)
	throws ServiceException, IOException
	{
		log.debug("serveThumbnail(): entered, assetId = " + assetId + ", auId = " + auId);

		if (null != auId) {
			try {
				AssetUse au =  assetRepository.lazyLoad(AssetUse.class, auId, new String[] {"asset"});
				assetId = au.getAsset().getId();
			} catch (Exception f) {
				log.debug(f.getMessage());
			}
		}

        final RenditionType renditionType = RenditionType.getRenditionTypeForString(size);

        try {
        	AssetFile file = getAssetService().loadAssetFileOfRenditionType (assetId, renditionType);
    		response.setContentType(file.getFileFormat());
            response.setContentLength(file.getData().length);

            OutputStream out = response.getOutputStream();  // throws IOException
            out.write(file.getData());  // throws IOException
        }
        catch (PersistenceException pe) {
			serveMessageImage(response, "Asset not found", renditionType);
        }
	}

	private void serveMessageImage(HttpServletResponse response, String msg, RenditionType renditionType) throws IOException {
	    // Note must set all response headers before write to response
        // OutputStream.  But we will do both at very end.

        BufferedImage image = getAssetService().generateMsgImage(msg, renditionType);

        // Convert image to png and send.
        // Write to intermediate stream first so we can determine size.
        // (Although for a small image we don't have to set the size
        // and Tomcat will because it will fit inside the 8K buffer.)

        ByteArrayOutputStream tempOut = new ByteArrayOutputStream();

        response.setContentType("image/png");

        @SuppressWarnings("unused")
        boolean ok = ImageIO.write(image, "png", tempOut);  // throws IOException

        response.setContentLength(tempOut.size());

        OutputStream out = response.getOutputStream();  // throws IOException
        tempOut.writeTo(out);
	}

	public AssetService getAssetService() {
		return assetService;
	}

	public void setAssetService(AssetService assetService) {
		this.assetService = assetService;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}
}
