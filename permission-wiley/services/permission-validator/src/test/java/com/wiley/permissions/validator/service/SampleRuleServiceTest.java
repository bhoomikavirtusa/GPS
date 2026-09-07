package com.wiley.permissions.validator.service;

import junit.framework.TestCase;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.wiley.permissions.domain.persistence.permissions.Asset;

public class SampleRuleServiceTest extends TestCase {

    private ApplicationContext context = null;

    public SampleRuleServiceTest(String testName) {
		super(testName);
		context = new ClassPathXmlApplicationContext(
				"classpath*:conf/spring/validator/validator-spring-context.xml");
	}

    @Test
	public void testSampleRuleServiceWithAgendaGroup() {

		Asset asset = new Asset();
		asset.setDescription("Asset0001");

		SampleRuleService ruleService = (SampleRuleService) context
				.getBean("com.wiley.permissions.sampleRuleService");

		ruleService.validateAssetWithAgendaGroup(asset);

		assertNotNull(asset);
	}

    public void testSampleRuleServiceWithoutAgendaGroup() {
		Asset asset = new Asset();
		asset.setDescription("Asset0001");

		SampleRuleService ruleService = (SampleRuleService) context
				.getBean("com.wiley.permissions.sampleRuleService");
		ruleService.validateAssetWithoutAgendaGroup(asset);
	}
}
