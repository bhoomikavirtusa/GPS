package com.wiley.sf.common.lucene;

import org.apache.lucene.index.IndexWriter;

/**
 * To be used with IndexWriteManager.
 *
 * @since JDK 1.8, Lucene 8.11
 */
public interface IndexRebuilder {
public void rebuild(IndexWriter writer) throws Exception;
}
