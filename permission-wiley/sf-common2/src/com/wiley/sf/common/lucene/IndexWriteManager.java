package com.wiley.sf.common.lucene;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Background write queue + occasional rebuild/optimize for a Lucene index.
 *
 * @since JDK 1.8, Lucene 8.11
 */
public class IndexWriteManager implements Runnable
{
private static final Log log = LogFactory.getLog(IndexWriteManager.class);

private final Directory dir;
private final IndexWriterConfigFactory configFactory;
private final long intervalMS;
private final ArrayDeque<UpdateWrapper> queue = new ArrayDeque<UpdateWrapper>();
private int obtainLockFailCount = 0;
private int otherFailCount = 0;
private volatile boolean rebuild = false;
private volatile boolean optimize = false;
private IndexRebuilder rebuilder = null;

public IndexWriteManager(Directory dir, IndexWriterConfigFactory configFactory, int intervalSecs) {
ArgUtil.notNull(dir, "dir");
ArgUtil.notNull(configFactory, "configFactory");
ArgUtil.notLess1(intervalSecs, "intervalSecs");
this.dir = dir;
this.configFactory = configFactory;
this.intervalMS = intervalSecs * 1000L;
Thread thread = new Thread(this);
thread.setDaemon(true);
thread.start();
}

public void setRebuilder(IndexRebuilder rebuilder) {
this.rebuilder = rebuilder;
}

public void updateDocument(Document doc, Term deleteTerm) {
if (doc != null || deleteTerm != null) {
synchronized (queue) {
queue.add(new UpdateWrapper(doc, deleteTerm));
}
}
}

public void updateDocument(UpdateWrapper wrapper) {
ArgUtil.notNull(wrapper, "wrapper");
synchronized (queue) {
queue.add(wrapper);
}
}

public boolean updateDocumentNow(Document doc, Term deleteTerm) throws IOException {
if (doc == null && deleteTerm == null) {
throw new IllegalArgumentException("doc and deleteTerm cannot both be null.");
}
try {
IndexWriterConfig config = configFactory.newConfig().setOpenMode(IndexWriterConfig.OpenMode.APPEND);
IndexWriter writer = new IndexWriter(dir, config);
log.debug("updateDocumentNow(): index write lock obtained.");
internalUpdateDocument(writer, doc, deleteTerm);
writer.close();
return true;
} catch (LockObtainFailedException ex1) {
log.debug("updateDocumentNow(): Could not obtain index write lock.");
updateDocument(doc, deleteTerm);
return false;
}
}

public boolean updateDocumentsNow(Collection<UpdateWrapper> wrappers) throws IOException {
try {
IndexWriterConfig config = configFactory.newConfig().setOpenMode(IndexWriterConfig.OpenMode.APPEND);
IndexWriter writer = new IndexWriter(dir, config);
log.debug("updateDocumentsNow(): index write lock obtained.");
int count = 0;
for (UpdateWrapper wrapper : wrappers) {
internalUpdateDocument(writer, wrapper.getDocument(), wrapper.getDeleteTerm());
count++;
if (count % 10000 == 0) {
writer.commit();
}
}
writer.close();
return true;
} catch (LockObtainFailedException ex1) {
log.debug("updateDocumentsNow(): Could not obtain index write lock.");
for (UpdateWrapper wrapper : wrappers) {
updateDocument(wrapper);
}
return false;
}
}

public boolean optimizeNow(int maxNumSegments) throws IOException {
ArgUtil.notLess1(maxNumSegments, "maxNumSegments");
try {
IndexWriterConfig config = configFactory.newConfig().setOpenMode(IndexWriterConfig.OpenMode.APPEND);
IndexWriter writer = new IndexWriter(dir, config);
log.debug("optimizeNow(): index write lock obtained.");
writer.forceMerge(maxNumSegments);
writer.close();
return true;
} catch (LockObtainFailedException ex1) {
log.debug("optimizeNow(): Could not obtain index write lock.");
return false;
}
}

public void optimize() { optimize = true; }
public void rebuild() { rebuild = true; }

public void run() {
while (true) {
try { Thread.sleep(intervalMS); } catch (InterruptedException ex) { }
checkQueue();
checkRebuildAndOptimize();
}
}

private void checkRebuildAndOptimize() {
if (!rebuild && !optimize) return;
if (rebuilder == null && !optimize) {
log.warn("checkRebuild(): rebuild requested but no rebuilder set.");
return;
}
try {
IndexWriterConfig config = configFactory.newConfig();
config.setOpenMode(rebuild ? IndexWriterConfig.OpenMode.CREATE : IndexWriterConfig.OpenMode.APPEND);
IndexWriter writer = new IndexWriter(dir, config);
log.debug("checkRebuildAndOptimize(): index write lock obtained.");
obtainLockFailCount = 0;
if (rebuild && rebuilder != null) {
rebuild = false;
rebuilder.rebuild(writer);
}
optimize = false;
writer.forceMerge(1);
writer.close();
otherFailCount = 0;
} catch (LockObtainFailedException ex1) {
obtainLockFailCount++;
log.debug("checkRebuildAndOptimize(): Could not obtain index write lock (obtainLockFailCount=" + obtainLockFailCount + ").");
} catch (Exception ex2) {
otherFailCount++;
log.error("checkRebuildAndOptimize(): Exception rebuilding index (otherFailCount=" + otherFailCount + "): ", ex2);
}
}

private void checkQueue() {
synchronized (queue) {
if (queue.size() == 0) return;
try {
IndexWriterConfig config = configFactory.newConfig().setOpenMode(IndexWriterConfig.OpenMode.APPEND);
IndexWriter writer = new IndexWriter(dir, config);
log.debug("checkQueue(): index write lock obtained.");
obtainLockFailCount = 0;
UpdateWrapper item;
while ((item = queue.poll()) != null) {
internalUpdateDocument(writer, item.getDocument(), item.getDeleteTerm());
}
writer.close();
otherFailCount = 0;
} catch (LockObtainFailedException ex1) {
obtainLockFailCount++;
log.debug("checkQueue(): Could not obtain index write lock (obtainLockFailCount=" + obtainLockFailCount + ").");
} catch (Exception ex2) {
otherFailCount++;
log.error("checkQueue(): Could not write delta (otherFailCount=" + otherFailCount + "): ", ex2);
}
}
}

private void internalUpdateDocument(IndexWriter writer, Document doc, Term deleteTerm) throws IOException {
if (deleteTerm != null) {
writer.deleteDocuments(deleteTerm);
}
if (doc != null) {
writer.addDocument(doc);
}
}
}
