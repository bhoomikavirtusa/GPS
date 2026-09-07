package com.wiley.permissions.services;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.wiley.permissions.domain.persistence.permissions.EnumUsageCondition;
import com.wiley.permissions.domain.persistence.permissions.Size;
import com.wiley.permissions.domain.persistence.permissions.UsageConditionSize;
import com.wiley.permissions.repositories.ConditionRepository;

//import edu.emory.mathcs.backport.java.util.Collections;

/**
 *
 * @since  JUnit 4.11, JDK 6
 * @author smarkoff
 */
public class ConditionRepositoryTest
{
	@Test
	public void eliminateRedundancies() {
		List<UsageConditionSize> input = new ArrayList<UsageConditionSize>();
		input.add(new UsageConditionSize(null, Size.NA));
		List<UsageConditionSize> expected = new ArrayList<UsageConditionSize>();
		expected.add(new UsageConditionSize(null, Size.NA));
		eliminateRedundancies(input, expected);

		input.add(new UsageConditionSize(null, Size.FULL_PAGE));
		eliminateRedundancies(input, expected);

		input.clear();
		// do same test as above with different order
		input.add(new UsageConditionSize(null, Size.FULL_PAGE));
		input.add(new UsageConditionSize(null, Size.NA));
		eliminateRedundancies(input, expected);

		input.clear();
		input.add(new UsageConditionSize(null, Size.FULL_PAGE));
		input.add(new UsageConditionSize(null, Size.HALF_PAGE));
		expected.clear();
		expected.add(new UsageConditionSize(null, Size.FULL_PAGE));
		eliminateRedundancies(input, expected);

		input.add(new UsageConditionSize(EnumUsageCondition.FRONT_COVER, Size.ONE_AND_12_PAGE));
		input.add(new UsageConditionSize(EnumUsageCondition.FRONT_COVER, Size.DOUBLE_PAGE));
		expected.add(new UsageConditionSize(EnumUsageCondition.FRONT_COVER, Size.DOUBLE_PAGE));
		eliminateRedundancies(input, expected);

		input.clear();
		expected.clear();
		// now do similar to last test except have Size values for FRONT_COVER all smaller than
		// the largest size value for Usage NA so that the FRONT_COVER usage is totally redundant
		input.add(new UsageConditionSize(null, Size.FULL_PAGE));
		input.add(new UsageConditionSize(null, Size.QUARTER_PAGE));
		input.add(new UsageConditionSize(EnumUsageCondition.FRONT_COVER, Size.HALF_PAGE));
		input.add(new UsageConditionSize(EnumUsageCondition.FRONT_COVER, Size.THREE_QUARTER_PAGE));
		expected.add(new UsageConditionSize(null, Size.FULL_PAGE));
		eliminateRedundancies(input, expected);

		input.clear();
		expected.clear();
		input.add(new UsageConditionSize(EnumUsageCondition.FRONT_COVER, Size.HALF_PAGE));
		input.add(new UsageConditionSize(EnumUsageCondition.FRONT_COVER, Size.THREE_QUARTER_PAGE));
		input.add(new UsageConditionSize(EnumUsageCondition.FRONT_COVER, Size.SPOT));
		input.add(new UsageConditionSize(EnumUsageCondition.BACK_COVER, Size.ONE_AND_14_PAGE));
		input.add(new UsageConditionSize(EnumUsageCondition.BACK_COVER, Size.ONE_EIGHTH));
		expected.add(new UsageConditionSize(EnumUsageCondition.FRONT_COVER, Size.THREE_QUARTER_PAGE));
		expected.add(new UsageConditionSize(EnumUsageCondition.BACK_COVER, Size.ONE_AND_14_PAGE));
		eliminateRedundancies(input, expected);
	}

	private void eliminateRedundancies(List<UsageConditionSize> input, List<UsageConditionSize> expected) {
		List<UsageConditionSize> output = ConditionRepository.eliminateRedundancies(input);

		// sort output and expected same way (by Usage, which should be unique in each list)
		// so we can avoid not matching due to different orders
		java.util.Collections.sort(output);
		java.util.Collections.sort(expected);

		String msg = "Output list size was " + output.size() + " instead of expected size " + expected.size()
				+ "\r\noutput:\r\n" + StringUtils.join(output, "\r\n")
				+ "\r\nexpected:\r\n" + StringUtils.join(expected, "\r\n");
		assertTrue(msg, output.size() == expected.size());

		for (int i = 0; i < output.size(); i++) {
			UsageConditionSize outputPair = output.get(i);
			UsageConditionSize expectedPair = expected.get(i);
			msg = "pair " + i + " did not match:\r\noutput:\r\n" + StringUtils.join(output, "\r\n")
				+ "\r\nexpected:\r\n" + StringUtils.join(expected, "\r\n");
			assertTrue(msg, outputPair.equals(expectedPair));
		}
	}
}
