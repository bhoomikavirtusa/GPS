package com.wiley.permissions.services;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import com.wiley.permissions.common.transaction.TransactionManagerLookupUtility;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.it.base.BaseTestCase;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.services.message.CMSMessageService;

/**
 * Unit test for simple AssetService.
 * @author lnagy
 * @version $Id: CMSMessagesTest.java,v 1.21 2011-05-05 22:47:30 lnagy Exp $
 */
public class CMSMessagesTest extends BaseTestCase
{
	// wire with spring
	private CMSMessageService outService;
	private AssetService assetService;
	private AssetUseRepository assetUseRepository;


	@Override
	protected String[] getConfigLocations() {
        return new String[] { "classpath*:conf/spring/*.xml" };
    }

	public CMSMessageService getUtility()
	{
		return outService;
	}

	public void setOutgoingMessageService(CMSMessageService outService)
	{
		this.outService = outService;
	}


	public void testSendGetAssetMessage() {
		// System.setProperty("jaxb.debug", "true");
		try
		{
			TransactionManagerLookupUtility lookup = new TransactionManagerLookupUtility();

			TransactionManager transactionManager = lookup.getTransactionManager();

			transactionManager.begin();
			Transaction t = transactionManager.getTransaction();
			AssetUse au = assetUseRepository.find(AssetUse.class, new Integer(43));
			if (au == null) throw new PersistenceException("AssetUse not found.");
			au.setCanceled(true);
/*
			AssetUse au = new AssetUse();
			au.setAsset(new Asset());
			au.getAsset().setDescription("momo1");
			au.getAsset().setExternalId("perm.asset_base.5");
			au.getAsset().setFeeRequired(false);
			au.setProduct(new Product());
			au.getProduct().setExternalId("CORE.001.PROD.0000043321");
*/
			List<AssetUse> list = new ArrayList<AssetUse>();
			list.add(au);
			// assetUseService.updateAssetUse(au, false, false);
			// assetUseService.handleUpdateAssetUseMessage(list);
			getUtility().sendUpdateAssetUseMessage(au, null);

/*
			// System.out.println(JavaObjectToXml.objectToXml(au));
			Product p = au.getProduct();
			System.out.println (p.getExternalId());
			AssetUse au2 = new AssetUse();
			au2.setId(new Integer(242));
			au2.setExternalId("perm242");
			Product p2 = new Product();
			p2.setId(new Integer(28));
			p2.setExternalId("foo ext");
			au2.setProduct(p);
			System.out.println(au.getProduct().getId());
			System.out.println(au.getProduct().getExternalId());
			// au = au2;
*/
			System.out.println(au);
			// outService.sendGetAssetMessage("perm.asset_use.5");
			// outService.sendUpdateAssetUseMessage (au, null);

			lookup.dumpCurrentState ("END");
			System.out.println(t.getStatus());
			transactionManager.commit();

			// outService.sendGetAssetMessage("dummy ext id");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

/*
	public void testMuleDispather () throws DispatcherException {

			outService.getMessageDispatcher().dispatch("vm://assetUseService?method=handleUpdateAssetUseMessage",
					new ArrayList<AssetUse>(),null);
	}
*/
	public AssetService getAssetService()
	{
		return assetService;
	}

	public void setAssetService(AssetService assetService)
	{
		this.assetService = assetService;
	}

	public AssetUseRepository getAssetUseRepository()
	{
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository)
	{
		this.assetUseRepository = assetUseRepository;
	}
}
