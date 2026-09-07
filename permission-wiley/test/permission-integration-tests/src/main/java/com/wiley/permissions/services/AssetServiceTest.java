package com.wiley.permissions.services;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleException;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.common.dispatcher.ServiceDispatcher;
import com.wiley.permissions.common.transformer.TransformationException;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.it.base.BaseTestCase;
import com.wiley.sf.common.monitor.PerformanceMonitor;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 * Unit test for simple AssetService.
 * @author lnagy
 * @version $Id: AssetServiceTest.java,v 1.16.4.1 2017-10-09 07:05:42 sdevadasan Exp $
 */
public class AssetServiceTest extends BaseTestCase
{
	private static final Log logPerformance = LogFactory.getLog(PerformanceMonitor.class);

	// wire with spring
	private ServiceDispatcher dispatcher;
	private final int INSERT_COUNT = 1000;
	private final int LOAD_COUNT = 10;


	/*protected String[] getConfigLocations() {
        return new String[] { "conf/spring/services-spring-context.xml" };
    }*/

    public void setDispatcher(ServiceDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	public ServiceDispatcher getDispatcher() {
		return dispatcher;
	}

    /**
     * reads Assets from xml or creates new Asset and persist it thru the service
     * @throws JAXBException
     * @throws TransformerException
     * @throws IOException
     * @throws MuleException
     * @throws TransformationException
     * @throws DispatcherException
     */
    public void testPersistAssets() throws TransformationException, JAXBException, TransformerException, IOException, MuleException, DispatcherException {
    	for (int i = 0; i < INSERT_COUNT; i++) {
    		PerfTimer timer = PerformanceMonitor.getInstance().startTimer("testPersistAsset");
    		logPerformance.debug("persist #" + i);
    		Asset asset = loadAsset();
			asset = (Asset)getDispatcher().send("vm://update.asset?method=updateAsset", asset, null);
		//	assertNull (message.getExceptionPayload());
			//assertNotSame (message.getPayload().getClass(),  NullPayload.class);
			// assertEquals(asset.getExternalId(), "asset0.8767826872796652");
			timer.stopTimer();
			logPerformance.debug(PerformanceMonitor.getInstance().dumpStats());
    	}
    }

    /**
     * loads all Assets
     * @throws DispatcherException
     */
    @SuppressWarnings("unchecked")
	public void testLoadAssets() throws MuleException, DispatcherException {
    	List<Asset> assets = null;
    	for (int i = 0; i < LOAD_COUNT; i++) {
    		PerfTimer timer = PerformanceMonitor.getInstance().startTimer("testLoadAssets");
    		logPerformance.debug("#" + i);
    		assets = (List<Asset>)getDispatcher().send("vm://load.assets?method=loadAll", Asset.class,
					null, 3000000);
			//assertTrue(assets.size() > 0);
			timer.stopTimer();
			logPerformance.debug(PerformanceMonitor.getInstance().dumpStats());
    	}
    }

    /**
     * tests the deletion of an asset using services
     * @throws JAXBException
     * @throws TransformerException
     * @throws IOException
     * @throws DispatcherException
     */
    public void testDeleteAsset() throws TransformationException, JAXBException, TransformerException, IOException, MuleException, DispatcherException {
    	Asset asset = loadAsset();

    	getDispatcher().send("vm://delete.asset?method=deleteAsset", asset.getExternalId(),
				null);
    }

    /**
     * returns an Asset object
     * @param fromXML
     * @return Asset
     * @throws JAXBException
     * @throws TransformerException
     * @throws IOException
     */
    public Asset loadAsset()
    		throws JAXBException, TransformerException, TransformationException, IOException {
    	Asset asset = new Asset ();
        String extId = "asset" + Math.random();
        asset.setExternalId(extId);
    	asset.setDescription("test");
    	/*asset.setParentId(null);
    	asset.setOwnerType("wiley");
    	asset.setType("photo");
    	asset.setSourceId(1);
    	asset.setLastUpdatedUserId(1);*/
		return asset;
    }
}
