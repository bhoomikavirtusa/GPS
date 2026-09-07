package com.wiley.permissions.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Wrapper around TopDocs that provides JSTL access and also contains other information.
 *
 * @author smarkoff
 */
public class AssetUseSearchResults
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(AssetUseSearchResults.class);


	private final TopDocs hits;
	private final IndexSearcher searcher;
	private final List<SearchCriterion> criteria;  // to show users
	private Set<Integer> assetIdSetForCurrentCW;

	/**
	 *
	 * @param hits  Must be non-null
	 * @param searcher  Must be non-null
	 * @param criteria  May be null
	 */
	public AssetUseSearchResults(TopDocs hits, IndexSearcher searcher, List<SearchCriterion> criteria) {
		ArgUtil.notNull(hits, "hits");
		ArgUtil.notNull(searcher, "searcher");

		this.hits = hits;
		this.searcher = searcher;
		this.criteria = criteria;
	}

    public int getTotalHits() {
    	return hits.totalHits;
    }

    public int getDocumentCount() {
    	return hits.scoreDocs.length;
    }

    public List<SearchCriterion> getCriteria() {
    	return criteria;
    }

    public void setAssetIdSetForCurrentCW(Set<Integer> assetIdSet) {
    	assetIdSetForCurrentCW = assetIdSet;
    }

    /**
     * Returns all documents. If you don't intend to display all documents
     * then use getDocuments(startIndex, endIndex).
     * @throws IOException
     * @throws CorruptIndexException
     */
    public ArrayList<AssetUseSearchResult> getDocuments()
        throws CorruptIndexException, IOException
    {
    	if (hits.scoreDocs.length == 0)  return new ArrayList<AssetUseSearchResult>(0);
    	else  return getDocuments(0, hits.scoreDocs.length - 1);
    }

    /**
     * Returns all documents that contain non no-fly sources.
     *
     * @throws IOException
     * @throws CorruptIndexException
     */
    public ArrayList<AssetUseSearchResult> getDocumentsExcludingNoflySources()
        throws CorruptIndexException, IOException
    {
    	if (hits.scoreDocs.length == 0)  return new ArrayList<AssetUseSearchResult>(0);
    	else  {
    		ArrayList<AssetUseSearchResult> temp = getDocuments(0, hits.scoreDocs.length - 1);
    		for (int x = 0; x < temp.size(); x++) {
    			if (temp.get(x).isSourceNofly()) {
    				temp.remove(x);
    				x--;
    			}
    		}
    		return temp;
    	}
    }

    /**
     * This method should not be called if totalHits == 0.
     *
     * @param startIndex  must be >= 0 and < getDocumentCount()
     * @param endIndex  must be >= 0 and < getDocumentCount() and >= endIndex
     */
    public ArrayList<AssetUseSearchResult> getDocuments(int startIndex, int endIndex)
        throws CorruptIndexException, IOException
    {
    	if (startIndex > endIndex) {
    		throw new IllegalArgumentException("startIndex cannot be greater than endIndex");
    	}
    	ArgUtil.inRange(startIndex, "startIndex", 0, hits.scoreDocs.length - 1);
    	ArgUtil.inRange(endIndex, "endIndex", 0, hits.scoreDocs.length - 1);

    	ArrayList<AssetUseSearchResult> list = new ArrayList<AssetUseSearchResult>(endIndex - startIndex + 1);

    	for (int i = startIndex; i <= endIndex; i++) {
    	    ScoreDoc scoreDoc = hits.scoreDocs[i];
    	    Document document = searcher.doc(scoreDoc.doc);
    	        // throws CorruptIndexException, IOException
    	    boolean assetUseForCurrentCW = false;
    	    if (assetIdSetForCurrentCW != null) {
    	    	Integer assetId = new Integer(document.get(AssetUseIndexService.ASSET_ID));
    	    	assetUseForCurrentCW = assetIdSetForCurrentCW.contains(assetId);
    	    }

    	    AssetUseSearchResult result = new AssetUseSearchResult(document, assetUseForCurrentCW);
    	    list.add(result);
    	}

    	return list;
    }
}
