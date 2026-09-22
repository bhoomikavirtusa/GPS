package com.wiley.sf.common.lucene;

import org.apache.lucene.index.IndexWriterConfig;

/**
 * Factory for fresh IndexWriterConfig instances (Lucene requires a new
 * config object for every IndexWriter).
 *
 * @since JDK 1.8, Lucene 8.11
 */
public interface IndexWriterConfigFactory {
public IndexWriterConfig newConfig();
}