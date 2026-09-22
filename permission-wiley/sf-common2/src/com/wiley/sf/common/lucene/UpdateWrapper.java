package com.wiley.sf.common.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;

/**
 * Instances of this class are used by IndexWriteManager, and must be passed
 * into updateDocumentsNow().
 *
 * @since JDK 1.8, Lucene 8.11
 */
public class UpdateWrapper {
private final Document doc;
private final Term deleteTerm;

/**
 * One parameter may be null but the other (or both) must be non-null.
 * @param doc may be null
 * @param deleteTerm may be null
 */
public UpdateWrapper(Document doc, Term deleteTerm) {
if (doc == null && deleteTerm == null) {
throw new IllegalArgumentException("doc and deleteTerm cannot both be null.");
}
this.doc = doc;
this.deleteTerm = deleteTerm;
}

public Document getDocument() {
return doc;
}

public Term getDeleteTerm() {
return deleteTerm;
}
}
