package com.wiley.sf.common.lucene;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * JUnit test class for LuceneUtil.
 *
 * @since   JDK 1.6, JUnit 4.10
 * @version $Id: LuceneUtilTest.java,v 1.4 2013-02-23 00:01:13 smarkoff Exp $
 * @author  Steve Markoff, created 3/24/2010
 */
public class LuceneUtilTest {

    @Test
    public void testParseWords() {
        parseWords2("P:", "quick brown fox", ":S", " ",
            "P:quick:SP:brown:SP:fox");
        parseWords2("P:", "quick,brown,fox", ":S", ",",
            "P:quick:SP:brown:SP:fox");

        parseWords2("P:", "(", "quick,brown,fox", ")", " AND ", ",",
            "P:(quick) AND P:(brown) AND P:(fox)");
        parseWords2("P:", "(", "quick", ")", " AND ", ",",
            "P:(quick)");
    }

    private void parseWords2(String prefix, String words, String suffix,
        String separator, String expected)
    {
        parseWords2(prefix, "", words, "", suffix, separator, expected);
    }

    private void parseWords2(String prefix, String pre, String words,
        String suf, String suffix, String separator, String expected)
    {
        String output = LuceneUtil.parseWords(prefix,
            pre, words, suf, suffix, separator);
        String msg = "pre: '" + pre + "' suf: '" + suf
            + "' prefix: '" + prefix + "' words: '" + words
            + "' suffix: '" + suffix + "' separator: '" + separator
            + "' output: '" + output + "' expected: '" + expected + "'";
        assertTrue(msg, StringUtils.equals(output, expected));
    }

    @Test
    public void parseArray() {
        String [] words = { "quick", "brown", "fox" };
        parseArray2("p:", words, " AND ", "p:quick AND p:brown AND p:fox");
        parseArray2("p:", words, " AND ", "p:quick AND p:brown AND p:fox");

        parseArray2("p:", words, "*", " AND ", "p:quick* AND p:brown* AND p:fox*");
    }

    private void parseArray2(String prefix, String [] words, String suffix,
        String expected)
    {
        parseArray2(prefix, words, "", suffix, expected);
    }

    private void parseArray2(String prefix, String [] words, String preSuffix,
        String suffix, String expected)
    {
        String output = LuceneUtil.parseArray(prefix,
            words, preSuffix, suffix);
        String msg = "prefix: '" + prefix + "'preSuffix: '" + preSuffix
            + "' suffix: '" + suffix + "' words: (array) output: '"
            + output + "' expected: '" + expected + "'";
        assertTrue(msg, StringUtils.equals(output, expected));
    }

    @Test
    public void cleanUpTerm() {
        cleanUpTerm2(null, null);
        cleanUpTerm2("", "");
        cleanUpTerm2("nothing special", "nothing special");
        cleanUpTerm2("[({ AND *:&|~! OR +- and ?blah or )}]", "blah");
    }

    private void cleanUpTerm2(String input, String expected) {
        String output = LuceneUtil.cleanUpTerm(input);
        String msg = "Input was '" + input + "', "
            + "Expected output was '" + expected + "' but got '" + output + "'";
        assertTrue(msg, StringUtils.equals(output, expected));
    }

    @Test
    public void removeStopWords() {
        removeStopWords("", "");
        removeStopWords("the quick a fox over the lazy as", "quick fox over lazy");
    }

    private void removeStopWords(String input, String expected) {
        String result = LuceneUtil.removeStopWords(input);
        String msg = "expected: " + expected + ", actual: " + result;
        assertTrue(msg, expected.equals(result));
    }
}
