package com.wiley.sf.common.lucene;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.StringUtil;

/**
 * Utility methods to help use Lucene.
 *
 * @since JDK 1.8, Lucene 8.11
 */
public class LuceneUtil {

private static final Log log = LogFactory.getLog(LuceneUtil.class);

public static String parseWords(String prefix, String words, String suffix, String separator) {
return parseWords(prefix, "", words, "", suffix, separator);
}

public static String parseWords(String prefix, String pre, String words,
String suf, String suffix, String separator) {
if (null == words) return "";
StringBuilder result = new StringBuilder();
for (int x = words.indexOf(separator); null != words && x > -1; x = words.indexOf(separator)) {
String wWord = words.substring(0, x);
if (StringUtils.isNotBlank(wWord)) {
wWord = pre + wWord.trim() + suf;
if (result.length() > 0) result.append(suffix);
result.append(prefix);
result.append(wWord);
}
words = words.substring(x + 1, words.length());
}
if (StringUtils.isNotBlank(words)) {
if (result.length() > 0) result.append(suffix);
result.append(prefix);
result.append(pre);
result.append(words);
result.append(suf);
}
return result.toString();
}

public static String parseArray(String prefix, String[] words, String suffix) {
return parseArray(prefix, words, "", suffix);
}

public static String parseArray(String prefix, String[] words, String preSuffix, String suffix) {
if (null == words) return "";
StringBuilder result = new StringBuilder();
for (int x = 0; x < words.length; x++) {
String wWord = cleanUpTerm(words[x]);
if (StringUtils.isNotBlank(wWord)) {
wWord = wWord.trim() + preSuffix;
if (result.length() > 0) result.append(suffix);
result.append(prefix);
result.append(wWord);
}
}
return result.toString();
}

public static String cleanUpTerm(String searchTerm) {
return cleanUpTerm(searchTerm, false);
}

public static String cleanUpTerm(String searchTerm, boolean escapeHyphens) {
if (null == searchTerm) return null;
String s = StringUtil.removeIgnoreCase(searchTerm, " and ");
s = StringUtil.removeIgnoreCase(s, " or ");
s = StringUtil.removeIgnoreCase(s, " not ");
if (escapeHyphens) {
s = StringUtil.removeAny(s, "()[]{}:+!~^*?&|/\"\\");
s = s.replace("-", "\\-");
} else {
s = StringUtil.removeAny(s, "()[]{}:+-!~^*?&|/\"\\");
}
return s.trim();
}

public static String removeStopWords(String searchTerm) {
StringBuilder sb = new StringBuilder();
StringTokenizer tok = new StringTokenizer(searchTerm);
// Lucene 8 moved the default English stop set off StandardAnalyzer.
CharArraySet stopWords = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
while (tok.hasMoreTokens()) {
String word = tok.nextToken();
if (!stopWords.contains(word)) {
if (sb.length() > 0) sb.append(" ");
sb.append(word);
}
}
return sb.toString();
}

public static IndexWriter createIndexWriterWithRetryLogic(Directory dir,
IndexWriterConfig config, int maxTries, int intervalSecs) throws IOException {
ArgUtil.notNull(dir, "dir");
ArgUtil.notNull(config, "config");
ArgUtil.notLess1(maxTries, "maxTries");
ArgUtil.notLess1(intervalSecs, "intervalSecs");
int numTries = 0;
long intervalMS = 1000L * intervalSecs;
while (numTries < maxTries) {
numTries++;
try {
IndexWriter writer = new IndexWriter(dir, config);
log.debug("createIndexWriterWithRetryLogic(): Got writer on try " + numTries);
return writer;
} catch (LockObtainFailedException ex) { }
try { Thread.sleep(intervalMS); } catch (InterruptedException ex) { }
}
throw new LockObtainFailedException("Tried to get IndexWriter " + numTries + " times but still could not get a lock.");
}

public static void createIndexIfDoesNotExist(Directory dir, IndexWriterConfig config) throws IOException {
ArgUtil.notNull(dir, "dir");
ArgUtil.notNull(config, "config");
if (DirectoryReader.indexExists(dir)) return;
config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
IndexWriter writer = new IndexWriter(dir, config);
writer.close();
}

/**
 * True when there is no index, or the index can be opened with this Lucene version.
 * False when segments exist but are from an older Lucene (e.g. 4.x under Lucene 8).
 */
public static boolean canOpenIndex(Directory dir) throws IOException {
ArgUtil.notNull(dir, "dir");
if (!DirectoryReader.indexExists(dir)) {
return true;
}
DirectoryReader reader = null;
try {
reader = DirectoryReader.open(dir);
return true;
} catch (IndexFormatTooOldException ex) {
log.warn("canOpenIndex(): index format too old for this Lucene version: " + ex.getMessage());
return false;
} finally {
if (reader != null) {
try {
reader.close();
} catch (IOException ignore) {
}
}
}
}

/**
 * Deletes all files in the directory and creates a new empty index (OpenMode.CREATE).
 * Use when migrating from Lucene 4.x on-disk indexes to Lucene 8.x.
 */
public static void recreateEmptyIndex(Directory dir, IndexWriterConfig config) throws IOException {
ArgUtil.notNull(dir, "dir");
ArgUtil.notNull(config, "config");
String[] names = dir.listAll();
if (names != null) {
for (String name : names) {
try {
dir.deleteFile(name);
} catch (IOException ex) {
log.warn("recreateEmptyIndex(): could not delete [" + name + "]: " + ex.getMessage());
}
}
}
config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
IndexWriter writer = new IndexWriter(dir, config);
writer.commit();
writer.close();
log.info("recreateEmptyIndex(): created empty Lucene index compatible with current Lucene version.");
}
}
