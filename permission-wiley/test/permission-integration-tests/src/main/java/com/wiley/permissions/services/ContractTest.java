package com.wiley.permissions.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Condition;
import com.wiley.permissions.domain.persistence.permissions.ConditionMatchResult;
import com.wiley.permissions.domain.persistence.permissions.ConditionType;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.repositories.ConditionRepository;

/**
 * Test the matches(Contract, AssetUse) method on ConditionRepository.
 * 
 * This class is very out of date.
 *
 * smarkoff: Currently when I try to run this in Eclipse (Run As - JUnit Test) I get
 * java.lang.NoSuchMethodError: org.slf4j.spi.LocationAwareLogger.log...
 * - This might be because we have slf4j 1.5 for Mule and 1.6 for Hibernate and
 * these versions conflict.
 *
 * TODO: // add tests where the AssetUse has some medium exclusions
 *
 * @version $Id: ContractTest.java,v 1.14 2014-03-22 01:58:55 smarkoff Exp $
 * @since JDK 1.6, JUnit 4.9
 * @author smarkoff
 */
public class ContractTest {

	private static ConditionRepository conditionRepository;


	@BeforeClass
	public static void init() {
		conditionRepository = new ConditionRepository();
	}

	@Test
	public void matchesCW_GlobalConditions() {
		// dealing with only 1 condition at a time

		List<Condition> cwConditionList = new ArrayList<Condition>();
		List<Condition> contractConditionList = new ArrayList<Condition>();

		CommonWork commonWork = new CommonWork();
		commonWork.setId(1);
		commonWork.setConditions(cwConditionList);

		AssetUse au = new AssetUse();
		au.setCommonWork(commonWork);

		Contract contract = new Contract();
		contract.setCommonWork(commonWork);
		// if permissionForm is true then contract conditions don't need to be checked
		contract.setPermissionForm(false);

		ConditionMatchResult matchResult = conditionRepository.matches(contract, au);
		assertTrue("Contract should have matched CW - both have no conditions", matchResult.isMatch());

		// TODO: this need to be changed! - print run now has a mini-tree structure
		Condition cwPrintRun = new Condition(ConditionType.PRINT_RUN, "5000");
		cwConditionList.add(cwPrintRun);

		matchResult = conditionRepository.matches(contract, au);
		assertTrue("Contract should have matched CW - CW had 1 condition, Contract had no conditions",
			matchResult.isMatch());

		// TODO: this need to be changed! - print run now has a mini-tree structure
		Condition ccPrintRun = new Condition(ConditionType.PRINT_RUN, "5000");
		contractConditionList.add(ccPrintRun);

		matchResult = conditionRepository.matches(contract, au);
		assertTrue("Contract should have matched CW - both have the same single conditions",
		    matchResult.isMatch());

		ccPrintRun.setValue("4000");

		matchResult = conditionRepository.matches(contract, au);
		assertFalse("Contract [Print Run: 4000] should NOT have matched CW [Print Run: 5000]",
		    matchResult.isMatch());

		contract.setPermissionForm(true);
		matchResult = conditionRepository.matches(contract, au);
		assertTrue("Contract [Print Run: 4000] should have matched CW [Print Run: 5000] - since contract isPermissionForm",
			    matchResult.isMatch());
		contract.setPermissionForm(false);

		ccPrintRun.setValue("9000");

		matchResult = conditionRepository.matches(contract, au);
		assertTrue("Contract [Print Run: 9000] should have matched CW [Print Run: 5000]",
			matchResult.isMatch());

		cwConditionList.clear();
		contractConditionList.clear();

		Condition cwLanguageEng = new Condition(ConditionType.LANGUAGE_ENGLISH, "true");
		Condition cwLanguageFre = new Condition(ConditionType.LANGUAGE_FRENCH, "true");

		Condition ccLanguageEng = new Condition(ConditionType.LANGUAGE_ENGLISH, "true");
		Condition ccLanguageFre = new Condition(ConditionType.LANGUAGE_FRENCH, "true");
		Condition ccLanguageDeu = new Condition(ConditionType.LANGUAGE_GERMAN, "true");

		cwConditionList.add(cwLanguageEng);
		cwConditionList.add(cwLanguageFre);

		contractConditionList.add(ccLanguageDeu);
		contractConditionList.add(ccLanguageFre);
		contractConditionList.add(ccLanguageEng);

		matchResult = conditionRepository.matches(contract, au);
		assertTrue("Contract [Language: deu, fre, eng] should have matched CW [Language: eng, fre]",
			matchResult.isMatch());

		contractConditionList.remove(ccLanguageEng);  // so now has Deu and Fre left

		matchResult = conditionRepository.matches(contract, au);
		assertFalse("Contract [Language: deu, fre] should NOT have matched CW [Language: eng, fre]",
			matchResult.isMatch());

		// TODO: add test for matching [All] European against a subset of Europe, etc.

		// TODO: convert below to use new conditions
/*
		// multiple conditions

		pcAuthorPays.setSingleValue("false");

		cwConditionList.add(pcLanguage);
		cwConditionList.add(pcPrintRun);

		assertTrue("Contract [only has AuthorPays condition] should have matched CW [that + 2 other conditions]",
			contract.matches(au).isMatch());

		cwConditionList.clear();
		contractConditionList.clear();

		Condition pcMedium = new Condition(ConditionType.MEDIUM);
		pcMedium.setSingleValue("ALLER");

		Condition ccMedium = new Condition(ConditionType.MEDIUM);
		ccMedium.setSingleValue("CD");

		cwConditionList.clear();
		contractConditionList.clear();
		cwConditionList.add(pcMedium);
		contractConditionList.add(ccMedium);

		assertFalse("Contract [only has CD condition] should NOT have matched CW [has All Electronic]",
				contract.matches(au).isMatch());

		ccMedium.setValues(new String [] { "CD", "DVD", "O", "W" } );

		assertTrue("Contract [has CD, DVD, O, W] should have matched CW [has All Electronic]",
				contract.matches(au).isMatch());

		// reverse situation
		pcMedium.setSingleValue("CD");
		ccMedium.setSingleValue("ALLER");

		assertTrue("Contract [has All Electronic condition] should have matched CW [has CD]",
				contract.matches(au).isMatch());

		pcMedium.setValues(new String [] { "CD", "DVD" });

		assertTrue("Contract [has All Electronic condition] should have matched CW [has CD, DVD]",
				contract.matches(au).isMatch());

		pcMedium.setValues(new String [] { "CD", "DVD", "P" });

		assertFalse("Contract [has All Electronic condition] should NOT have matched CW [has CD, DVD, P]",
				contract.matches(au).isMatch());

		pcMedium.setSingleValue("CD");
		ccMedium.setExclusion(true);

		assertFalse("Contract [all Except - All Electronic condition] should NOT have matched CW [has CD]",
				contract.matches(au).isMatch());
		*/
	}

	@Test
	public void matchesProduct_AssetCases() {
		// TODO: Condition-change-required
		/*
		Condition pcPrintRun = new Condition(ConditionType.PRINT_RUN);
		pcPrintRun.setSingleValue("5000");

		Condition ccPrintRun = new Condition(ConditionType.PRINT_RUN);
		ccPrintRun.setSingleValue("4000");

		Condition ccaPrintRun = new Condition();
		Asset asset = new Asset();
		asset.setId(1);
		ccaPrintRun.setAssetBase(asset);
		ccaPrintRun.setType(ConditionType.PRINT_RUN);
		ccaPrintRun.setSingleValue("6000");

		CommonWork commonWork = new CommonWork();
		commonWork.setId(1);

		AssetUse au = new AssetUse();
		au.setCommonWork(commonWork);

		List<Condition> cwConditionList = new ArrayList<Condition>();
		cwConditionList.add(pcPrintRun);
		commonWork.setConditions(cwConditionList);

		List<Condition> contractConditionList = new ArrayList<Condition>();
		contractConditionList.add(ccPrintRun);
		contractConditionList.add(ccaPrintRun);

		Contract contract = new Contract();
		List<ContractAsset> contractAssetList = new ArrayList<ContractAsset>();
		// The assetBaseId here matters (must match asset.setId() above)
		// but the contractId does not matter (for this test)
		contractAssetList.add(new ContractAsset(1, 1, 0.0));
		contract.setAssets(contractAssetList);

		contract.setConditions(contractConditionList);

		assertTrue("Contract should have matched CW - asset Print Run overrides global Print Run",
		    contract.matches(au).isMatch());

		// reverse values
		ccPrintRun.setSingleValue("6000");
		ccaPrintRun.setSingleValue("4000");

		assertFalse("Contract should NOT have matched CW - asset condition not good enough",
			    contract.matches(au).isMatch());
		 */
	}
}
