package com.wiley.sf.common.lucene;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiBits;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

/**
 * Provides a getIndexSearcher() method such that the caller does not have
 * to worry about closing the searcher or the underlying reader.
 *
 * @since JDK 1.8, Lucene 8.11
 */
public class IndexReadManager {

private static final Log log = LogFactory.getLog(IndexReadManager.class);
private static final long CLOSE_TIMEOUT = 30 * 1000;

private final Directory dir;
private DirectoryReader reader = null;
private IndexSearcher searcher = null;
private final ArrayDeque<CloseWrapper> closeQueue = new ArrayDeque<CloseWrapper>();

public IndexReadManager(Directory dir) {
this.dir = dir;
}

public synchronized IndexSearcher getIndexSearcher() throws IOException {
if (searcher == null) {
if (!DirectoryReader.indexExists(dir)) {
throw new IOException("The index does not exist.");
}
log.debug("getIndexSearcher(): creating new IndexSearcher...");
reader = DirectoryReader.open(dir);
searcher = new IndexSearcher(reader);
return searcher;
}

long time = System.currentTimeMillis();
checkCloseQueue(time);

if (reader.isCurrent()) {
return searcher;
}

log.debug("getIndexSearcher(): creating new IndexSearcher...");
DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
if (newReader == null) {
return searcher;
}
IndexSearcher newSearcher = new IndexSearcher(newReader);
closeQueue.add(new CloseWrapper(reader, time));
searcher = newSearcher;
reader = newReader;
return searcher;
}

public IndexInfo readIndexInfo() throws IOException {
int numDocs = 0;
int numDeletedDocs = 0;
long version = 0L;
int numFiles = 0;
long totalFileBytes = 0L;
int numSegments = 0;
boolean indexExists = DirectoryReader.indexExists(dir);
if (indexExists) {
try {
DirectoryReader localReader = (DirectoryReader) getIndexSearcher().getIndexReader();
numDocs = localReader.numDocs();
numDeletedDocs = localReader.numDeletedDocs();
version = localReader.getVersion();
String[] files = dir.listAll();
numFiles = files.length;
for (String file : files) {
totalFileBytes += dir.fileLength(file);
}
numSegments = countSegments(localReader);
} catch (IndexFormatTooOldException ex) {
// Lucene 8 cannot open Lucene 4.x on-disk indexes. Still report file stats so
// the admin page can load and the operator can run Build Index (recreate).
log.warn("readIndexInfo(): incompatible (too old) index format - rebuild required: " + ex.getMessage());
String[] files = dir.listAll();
numFiles = files == null ? 0 : files.length;
if (files != null) {
for (String file : files) {
totalFileBytes += dir.fileLength(file);
}
}
}
}
return new IndexInfo(indexExists, numDocs, numDeletedDocs, version, numFiles, totalFileBytes, numSegments);
}

/**
 * Drop cached reader/searcher so a recreated index directory can be re-opened.
 */
public synchronized void reset() {
if (reader != null) {
try {
reader.close();
} catch (Exception ex) {
log.warn("reset(): error closing IndexReader: " + ex.getMessage());
}
}
reader = null;
searcher = null;
closeQueue.clear();
}

public int countSegments(IndexReader indexReader) {
return indexReader.getContext().leaves().size();
}

public List<FieldInfo> readIndexedFieldInfo() throws IOException {
if (DirectoryReader.indexExists(dir)) {
getIndexSearcher().getIndexReader();
} else {
throw new RuntimeException("Index does not exist!");
}
HashMap<String, FieldInfo> fieldMap = new HashMap<String, FieldInfo>();
// Lucene 8: MultiFields.getFields(IndexReader) was removed; use FieldInfos + MultiTerms.
java.util.Collection<String> fieldNames = FieldInfos.getIndexedFields(reader);
if (fieldNames == null || fieldNames.isEmpty()) {
return new ArrayList<FieldInfo>(0);
}
for (String fieldName : fieldNames) {
FieldInfo info = fieldMap.get(fieldName);
if (info == null) {
info = new FieldInfo(fieldName);
fieldMap.put(fieldName, info);
}
List<String> values = info.getValues();
Terms terms = MultiTerms.getTerms(reader, fieldName);
if (terms == null) {
continue;
}
TermsEnum iterator = terms.iterator();
BytesRef byteRef = null;
while ((byteRef = iterator.next()) != null) {
info.increaseCount();
if (values.size() < 3) {
values.add(new String(byteRef.bytes, byteRef.offset, byteRef.length));
}
}
}
String[] keys = fieldMap.keySet().toArray(new String[0]);
Arrays.sort(keys);
List<FieldInfo> list = new ArrayList<FieldInfo>(keys.length);
for (String key : keys) {
list.add(fieldMap.get(key));
}
return list;
}

public List<FieldInfo> readStoredFieldInfo() throws IOException {
if (DirectoryReader.indexExists(dir)) {
getIndexSearcher().getIndexReader();
} else {
throw new RuntimeException("Index does not exist!");
}
int maxDoc = reader.maxDoc();
HashMap<String, FieldInfo> fieldMap = new HashMap<String, FieldInfo>();
Bits liveDocs = MultiBits.getLiveDocs(reader);
for (int i = 0; i < maxDoc; i++) {
if (liveDocs != null && !liveDocs.get(i)) {
continue;
}
Document doc = reader.document(i);
for (IndexableField field : doc.getFields()) {
FieldInfo info = fieldMap.get(field.name());
if (info == null) {
info = new FieldInfo(field.name());
fieldMap.put(field.name(), info);
}
info.increaseCount();
}
}
String[] keys = fieldMap.keySet().toArray(new String[0]);
Arrays.sort(keys);
List<FieldInfo> list = new ArrayList<FieldInfo>(keys.length);
for (String key : keys) {
list.add(fieldMap.get(key));
}
return list;
}

private void checkCloseQueue(long time) {
if (closeQueue.size() == 0) {
return;
}
CloseWrapper wrapper = closeQueue.peek();
if (time > wrapper.getTime() + CLOSE_TIMEOUT) {
closeQueue.remove();
wrapper.close();
checkCloseQueue(time);
}
}

protected class CloseWrapper {
private final IndexReader closeReader;
private final long time;
public CloseWrapper(IndexReader closeReader, long time) {
this.closeReader = closeReader;
this.time = time;
}
public long getTime() { return time; }
public void close() {
try {
log.debug("Closing old IndexReader...");
closeReader.close();
} catch (Exception ex) {
log.warn("Exception trying to close IndexReader:", ex);
}
}
}
}
