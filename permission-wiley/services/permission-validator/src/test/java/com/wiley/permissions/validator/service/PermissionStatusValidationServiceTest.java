package com.wiley.permissions.validator.service;

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.validator.service.permissionstatus.PermissionStatusValidationService;

/**
 * Tried to replace AbstractDependencyInjectionSpringContextTests with
 * AbstractJUnit4SpringContextTests or @RunWith(SpringJUnit4ClassRunner.class)
 * and @ContextConfiguration but didn't quite work - service not being injected.
 *
 * @author smarkoff
 */
public class PermissionStatusValidationServiceTest extends AbstractDependencyInjectionSpringContextTests {

    private PermissionStatusValidationService service;


    @Override
	protected String[] getConfigLocations() {
    	return new String[] {
    		"classpath*:conf/spring/global-spring-context.xml",
    		"classpath*:conf/spring/services-spring-context.xml",
    		"classpath*:conf/spring/persistence-context.xml",
    		"classpath*:conf/spring/validator/validator-spring-context.xml"
    	};
    }

	public PermissionStatusValidationService getService() {
		return service;
	}

	public void setService(PermissionStatusValidationService service) {
		this.service = service;
	}

    public void test1() {
    	// TODO: Condition-change-required
    	/*
    	Asset asset = new Asset();
    	asset.setId(-1);  // just to prevent a NullPointerException in code that will be called - doesn't match db
		asset.setDescription("Asset1");
		asset.setMediaType(MediaType.PHOTO);

		AssetUse au = new AssetUse();
		au.setUsage(Usage.FIGURE);
		au.setAsset(asset);

		CommonWork cw = new CommonWork();
		cw.setId(1);
		au.setCommonWork(cw);

		// expect MISSING_INFO because OwnerType not set on Asset
		update(1, au, PermissionStatus.MISSING_INFO);

		asset.setMediaType(null);
		asset.setOwnerType(OwnerType.PUBLIC_DOMAIN);

		// expect MISSING_INFO because MediaType not set on Asset
		update(2, au, PermissionStatus.MISSING_INFO);

		asset.setMediaType(MediaType.PHOTO);

		asset.setManaged(true);
		// expect ILLEGAL since Public Domain cannot be Managed
		update(3, au, PermissionStatus.ILLEGAL);

		asset.setManaged(false);
		asset.setFeeRequired(true);
		// expect ILLEGAL since Fair use cannot have feeRequired
		update(4, au, PermissionStatus.ILLEGAL);

		asset.setOwnerType(OwnerType.PUBLIC_DOMAIN);
		asset.setFeeRequired(false);
		asset.setRoyaltyFree(false);
		// expect ILLEGAL since !managed should imply RoyaltyFree
		update(5, au, PermissionStatus.ILLEGAL);

		asset.setRoyaltyFree(true);

		update(6, au, PermissionStatus.GRANTED_UNLIMITED);

		asset.setFeeRequired(true);
		update(7, au, PermissionStatus.UNPAID);

		asset.setOwnerType(OwnerType.THIRD_PARTY);
		asset.setManaged(false);
		asset.setFeeRequired(false);
		asset.setRoyaltyFree(true);

		update(8, au, PermissionStatus.NO_SOURCE);

		Source source1 = new Source();
		source1.setId(1);  // to prevent NullPointerException in Asset.getPurchaseOrderListForSource()
		source1.setCreditLine("Give me credit!");
		ArrayList<Source> sourceList = new ArrayList<Source>();
		sourceList.add(source1);
		asset.setSources(sourceList);

		// GRANTED expected because has MediaType + OwnerType + Source
		// and !managed and !feeRequired
		update(9, au, PermissionStatus.GRANTED_UNLIMITED);

		asset.setFeeRequired(true);
		update(10, au, PermissionStatus.UNPAID);

		asset.setManaged(true);
		update(11, au, PermissionStatus.UNREQUESTED);

		PurchaseOrder po1 = new PurchaseOrder();
		po1.setSource(source1);
		po1.setCommonWork(cw);
		ArrayList<Asset> assetList1 = new ArrayList<Asset>();
		assetList1.add(asset);
		po1.setAssets(assetList1);

		PurchaseOrder po2 = new PurchaseOrder();
		po2.setSource(source1);
		po2.setCommonWork(cw);
		ArrayList<Asset> assetList2 = new ArrayList<Asset>();
		assetList2.add(asset);
		po2.setAssets(assetList2);

		HashSet<PurchaseOrder> poSet = new HashSet<PurchaseOrder>();
		poSet.add(po1);
		poSet.add(po2);
		asset.setPurchaseOrders(poSet);

		update(12, au, PermissionStatus.PO_SENT);

		po1.setDenied(true);
		po2.setDenied(true);
		update(13, au, PermissionStatus.DENIED);

		po2.setDenied(false);
		update(14, au, PermissionStatus.PO_SENT);

		// We aren't going to do much testing of whether contract conditions
		// match the product conditions here because that is done in ContractTest.

		Condition pcPrintRun = new Condition(ConditionType.PRINT_RUN);
		pcPrintRun.setSingleValue("5000");

		Condition ccPrintRun1 = new Condition(ConditionType.PRINT_RUN);
		ccPrintRun1.setSingleValue("4000");

		Condition ccPrintRun2 = new Condition(ConditionType.PRINT_RUN);
		ccPrintRun2.setSingleValue("4000");

		List<Condition> commonworkConditionList = new ArrayList<Condition>();
		commonworkConditionList.add(pcPrintRun);

		List<Condition> contractConditionList1 = new ArrayList<Condition>();
		contractConditionList1.add(ccPrintRun1);

		List<Condition> contractConditionList2 = new ArrayList<Condition>();
		contractConditionList2.add(ccPrintRun2);

		cw.setConditions(commonworkConditionList);

		Contract contract1 = new Contract();
		//contract1.setConditions(contractConditionList);
		ContractAsset contractAsset1 = new ContractAsset();
		contractAsset1.setContract(contract1);
		contractAsset1.setAssetBase(asset);
		contractAsset1.setAssetBaseId(asset.getId());  // to prevent NullPointerException in code called
		contract1.setPurchaseOrder(po1);
		contract1.setSource(source1);

		ArrayList<ContractAsset> caList1 = new ArrayList<ContractAsset>();
		caList1.add(contractAsset1);

		Contract contract2 = new Contract();
		//contract2.setConditions(contractConditionList);
		ContractAsset contractAsset2 = new ContractAsset();
		contractAsset2.setContract(contract2);
		contractAsset2.setAssetBase(asset);
		contractAsset2.setAssetBaseId(asset.getId());  // to prevent NullPointerException in code called
		contract2.setPurchaseOrder(po2);
		contract2.setSource(source1);

		ArrayList<ContractAsset> caList2 = new ArrayList<ContractAsset>();
		caList2.add(contractAsset2);

		HashSet<ContractAsset> caSet = new HashSet<ContractAsset>();
		caSet.add(contractAsset1);
		caSet.add(contractAsset2);

		asset.setContracts(caSet);
		contract1.setAssets(caList1);
		contract2.setAssets(caList2);

		// neither contract currently has any conditions so should match product
		update(15, au, PermissionStatus.GRANTED_UNLIMITED);

		contract1.setConditions(contractConditionList1);
		// now contract1 should not match (printRun < product) but contract2 will still match
		update(16, au, PermissionStatus.GRANTED_UNLIMITED);

		contract2.setConditions(contractConditionList2);
		// now both contracts have (printRun < product)
		update(17, au, PermissionStatus.CONTRACT_INSUFFICIENT);

		ccPrintRun2.setSingleValue("5000");
		update(18, au, PermissionStatus.GRANTED_UNLIMITED);

		Source source2 = new Source();
		source1.setId(2);  // to prevent NullPointerException in Asset.getPurchaseOrderListForSource()
        sourceList.add(source2);

        update(19, au, PermissionStatus.UNREQUESTED);

        au.setCanceled(true);
        update(20, au, PermissionStatus.CANCELED);
        au.setCanceled(false);

        // test if no PO's but have contract
        poSet.clear();
        sourceList.remove(source2);  // go back to just having source1
        update(21, au, PermissionStatus.GRANTED_UNLIMITED);

        caSet.remove(contractAsset2);
        // will be insufficient because contract1 print run still 4000
        update(22, au, PermissionStatus.CONTRACT_INSUFFICIENT);

        ccPrintRun1.setSingleValue("5000");
        update(23, au, PermissionStatus.GRANTED_UNLIMITED);


        // Test a Contracts that cover an Asset related to two products
        // - not testing PO's because we don't bother to even check POs for matching
        CommonWork cw2 = new CommonWork();
        cw2.setId(2);
		AssetUse au2 = new AssetUse();
		au2.setCommonWork(cw2);
		au2.setAsset(asset);
		au2.setUsage(Usage.FIGURE);

		contract1.setCommonWork(cw);
		contract2.setCommonWork(cw);

		update(30, au2, PermissionStatus.CONTRACT_INSUFFICIENT);

		contract1.setCommonWork(null);
		contract2.setCommonWork(null);

		update(31, au2, PermissionStatus.GRANTED_UNLIMITED);

		contract1.setCommonWork(cw2);
		contract2.setCommonWork(cw2);

		update(32, au2, PermissionStatus.GRANTED_UNLIMITED);
		*/
    }


    private void update(int testNumber, AssetUse au, PermissionStatus expected) {
    	PermissionStatus result = service.updatePermissionStatus(au);
    	String msg = "#" + testNumber + ": expectedStatus: " + expected.getCode()
    	    + ", actual: " + (result == null ? "null" : result.getCode());
    	assertTrue(msg, result != null && result.equals(expected));
    }

}
