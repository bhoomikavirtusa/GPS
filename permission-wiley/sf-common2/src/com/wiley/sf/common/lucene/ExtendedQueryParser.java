package com.wiley.sf.common.lucene;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * Enhances the standard QueryParser so numeric range queries on known int/long
 * point fields become IntPoint/LongPoint range queries instead of TermRangeQuery.
 *
 * Lucene 8 no longer has NumericRangeQuery; callers must declare the numeric
 * field names via setIntFieldNames()/setLongFieldNames().
 *
 * @since JDK 1.8, Lucene 8.11
 */
public class ExtendedQueryParser extends QueryParser {

private static final Log log = LogFactory.getLog(ExtendedQueryParser.class);

private Set<String> intFieldNames = null;
private Set<String> longFieldNames = null;

public ExtendedQueryParser(String f, Analyzer a) {
super(f, a);
}

/**
 * Call soon after construction (before parsing) to use IntPoint ranges.
 * @param names may be null
 */
public void setIntFieldNames(String[] names) {
intFieldNames = stringArrayToSet(names);
}

/**
 * Call soon after construction (before parsing) to use LongPoint ranges.
 * @param names may be null
 */
public void setLongFieldNames(String[] names) {
longFieldNames = stringArrayToSet(names);
}

private HashSet<String> stringArrayToSet(String[] array) {
if (array == null || array.length == 0) {
return null;
}
HashSet<String> set = new HashSet<String>(array.length);
for (String s : array) {
set.add(s);
}
return set;
}

@Override
protected Query getRangeQuery(String field, String part1, String part2,
boolean startInclusive, boolean endInclusive) throws ParseException {

if (intFieldNames != null && intFieldNames.contains(field)) {
log.debug("getRangeQuery(): returning IntPoint range for int field: " + field);
int lower = Integer.parseInt(part1);
int upper = Integer.parseInt(part2);
if (!startInclusive) {
lower++;
}
if (!endInclusive) {
upper--;
}
return IntPoint.newRangeQuery(field, lower, upper);
}

if (longFieldNames != null && longFieldNames.contains(field)) {
log.debug("getRangeQuery(): returning LongPoint range for long field: " + field);
long lower = Long.parseLong(part1);
long upper = Long.parseLong(part2);
if (!startInclusive) {
lower++;
}
if (!endInclusive) {
upper--;
}
return LongPoint.newRangeQuery(field, lower, upper);
}

return super.getRangeQuery(field, part1, part2, startInclusive, endInclusive);
}
}
