package com.wiley.permissions.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiBits;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.ComplianceStatus;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.Medium;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;	// Added for DM-1606
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.lucene.FieldInfo;
import com.wiley.sf.common.lucene.IndexInfo;
import com.wiley.sf.common.lucene.IndexReadManager;
import com.wiley.sf.common.lucene.IndexWriteManager;
import com.wiley.sf.common.lucene.IndexWriterConfigFactory;
import com.wiley.sf.common.lucene.LuceneUtil;
import com.wiley.sf.common.lucene.UpdateWrapper;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;
import com.wiley.sf.common.text.TimeFormat;

/**
 * It is assumed that only one instance of this class will
 * be created (being a service class).
 *
 * @since  JDK 1.8, Lucene 8.11
 * @author smarkoff
 */
public class AssetUseIndexService extends BaseService implements IndexWriterConfigFactory
{
	private static final Log log = LogFactory.getLog(AssetUseIndexService.class);

	private static final String PERMISSIONS_HOME = "PERMISSIONS_HOME";
	private static final String INDEX_DIR = "assetUseIndex";
	private static final String OLD_INDEX_DIR = "assetSearchIndex";
	//Made changes as per RN#
	private static final int MAX_RESULTS = 10000;
	// 100,000 is simply a number greater than the max number of asset uses
	// we expect to be assigned to any single CommonWork - just saying we want all the results
	private static final int CW_MAX_RESULTS = 100000;

	// constants for product search field names
	public static final String COMMON_WORK_ID = "commonWorkId";
	public static final String PRODUCT_ID = "productId";
	public static final String INTERIOR_CW_STATUS = "interior_cw_status";  // don't think actually being used
	public static final String COVER_CW_STATUS = "cover_cw_status";  // ditto
	public static final String AUTHOR_NAME = "author_name";
	public static final String TITLE = "title";
	public static final String TITLE_SORT = "title_sort";
	public static final String ISBN13 = "isbn13";//updated for DM-280
	public static final String ISBN10 = "isbn10";//Added for DM-280
	public static final String COPYRIGHT_YEAR = "copyrightYear";
	public static final String USER_NAME = "userName";
	public static final String PUBLICATION_STATUS = "publicationStatus";
	public static final String PUBLICATION_STATUS_DISPLAY = "publicationStatus_display";
	//Start: Added for DM-280
	public static final String PNUMBER = "pNumber";
	public static final String EDITION_NUMBER = "editionNumber";
	public static final String BUSINESS_UNIT_CODE = "businessUnitCode";
	public static final String B_UNIT_CODE = "businessUnitCode";
	public static final String PRODUCT_PRIORITY = "productPriority";
	public static final String MEDIUM_CODE = "mediumCode";
	public static final String DATA_SOURCE = "dataSource";
	public static final String SHORT_AUTHOR_NAME = "shortAuthorName";
	public static final String EDITOR_CODE = "editorCode";
	public static final String EDITOR_NAME = "editorName";
	public static final String COMPLIANCE_STATUS = "complianceStatus";
	public static final String AU_COUNT_NOT_CANCELED = "auCountNotCancelled";
	public static final String COVER_COUNT_NOT_CANCELED = "coverCountNotCancelled";
	public static final String STATUS_NOT_OK_COUNT = "statusNotOkCount";
	public static final String PHOTO_ILLUS_TOTAL_COUNT = "photsIllusCount";
	public static final String PHOTO_EDITOR_LAST_NAME = "photoEditorLN";
	public static final String PHOTO_EDITOR_FIRST_NAME = "photoEditorFN";
	public static final String PROD_LAST_UPDATED_DATE = "prodLastUpdatedDate";
	public static final String CONSOLIDATED_RELEASE_DATE = "consolidateReleaseDate";
	public static final String TRANSMITTAL_DATE = "transmittalDate";
	//End: Added for DM-280

	// constants for asset search field names
	public static final String ASSET_ID = "assetId";  // for link to "Add to current"
	public static final String SOURCE_ID = "sourceId";
	public static final String SOURCE_NAME = "sourceName";
	public static final String SOURCE_ID_NOFLY_NAME = "sourceIdNoflyName";
	public static final String SOURCE_NOFLY = "isSourceNofly";
	public static final String MEDIA_TYPE = "mediaType";
	public static final String MEDIA_TYPE_DISPLAY = "mediaType_display";
	public static final String DESCRIPTION = "description";
	public static final String KEYWORDS = "keywords";
	public static final String DESCRIPTION_SORT = "description_sort";
	public static final String WORK_FOR_HIRE = "workForHire";
	public static final String OWNER_TYPE = "ownerType";
	public static final String OWNER_TYPE_CODE = "ownerType_code";
	public static final String OWNER_TYPE_DESCRIPTION = "ownerType_description";
	public static final String SOURCE_REF = "sourceRef";
	public static final String SOURCE_REF_DISPLAY = "sourceRef_display";
	public static final String ARTIST = "artist";
	public static final String MANAGED = "managed";
	public static final String FEE_REQUIRED = "feeRequired";
	public static final String ROYALTY_FREE = "royaltyFree";
	public static final String RESTRICTED_USE = "restrictedUse";
	public static final String MUST_DISPLAY_CREDIT = "mustDisplayCredit";
	public static final String OBTAINED_BY_AUTHOR = "obtainedByAuthor";
	public static final String MODEL_RELEASE = "modelRelease";
	public static final String PROPERTY_RELEASE = "propertyRelease";
	public static final String ACTIVE = "active";
	public static final String ARCHIVE = "archive";
	public static final String SORT_ORDER = "sort_order";
	public static final String HAS_ASSET_FILES = "has_asset_files";
	public static final String REVIEWED_OR_AUTHOR_UNKNOWN = "reviewedOrAuthorUnknown";

	// constants for asset_use search field names
	public static final String ASSET_USE_ID = "assetUseId";
	public static final String RESTRICTED_USE_APPROVED = "restrictedUseApproved";
	public static final String PRINT_RUN_WARNING = "printRunWarning";
	public static final String PERMISSION_STATUS_CODE = "permissionStatus_code";
	public static final String PERMISSION_STATUS_CODE_NOT_NORM = "permissionStatus_code_not_norm";
	public static final String CANCELED = "canceled";
	public static final String REPLACED = "replaced";
	public static final String REMOVED = "removed";
	public static final String REPLACEMENT_ID = "replacementId";
	public static final String CREDIT_LINE = "creditLine";
	public static final String USAGE = "usage";
	public static final String PAGE_POSITION_CODE = "pagePosition_code";
	public static final String PAGE_POSITION_DISPLAY = "pagePosition_display";
	public static final String USAGE_DISPLAY = "usage_display";
	public static final String POSITION = "position";
	public static final String POSITION_DISPLAY = "position_display";
	public static final String POSITION_SORT = "position_sort";
	public static final String FOUND_ON = "foundOn";
	public static final String COMPONENT_ID = "componentId";
	public static final String COMPONENT_NAME = "componentName";
	// COMPONENT_NAME_SORT stored the same as DISPLAY would be so also use for DISPLAY
	public static final String COMPONENT_NAME_SORT = "componentName_sort";
	public static final String COMPONENT_CATEGORY_CODE = "component_category_code";
	public static final String MANUSCRIPT_PAGE = "manuscriptPage";
	public static final String FINAL_PAGE = "finalPage";
	public static final String COLOR = "color";
	public static final String SIZE = "size";
	public static final String SIZE_DISPLAY = "size_display";
	public static final String CAPTION = "caption";
	public static final String PERMISSION_COMMENT = "permissionComment";
	public static final String PRODUCTION_COMMENT = "productionComment";
	public static final String REUSE = "reuse";
	public static final String PICKUP = "pickup";
	public static final String MEDIAMANAGER = "mediaManager";
	public static final String NEED_PAYMENT_REQUEST = "needPaymentRequest";
	public static final String PAID = "paid";
	public static final String SENT_TO_PRODUCTION = "sentToProduction";
	public static final String CAMERA_COPY_TO_COME = "cameraCopyToCome";
	public static final String CUSTOM = "custom";
	public static final String MEDIA_RETURN_REQUEST = "mediaReturnRequest";
	public static final String LAST_UPDATED_DATE = "lastUpdatedDate";
	public static final String NEED_TO_CONFIRM_CANCELS = "needToConfirmCancels";
	public static final String INDEX_DATE = "index_date";
	public static final String HAS_FILES = "hasFiles";
	public static final String CREATED_USER_ID = "createdUserId";
	public static final String CREATED_GROUP_ID = "createdGroupId";
	public static final String AU_USER_GROUP_ID = "AuUserGroupId";
	public static final String LATEST_CONTRACT_ID = "latestContractId";
	public static final String ESTIMATED_COST = "estimatedCost";
	public static final String GBPM_CATEGORY = "gbpmCategory";
	public static final String IMPORT_SOURCE_CODE ="importSourceCode";
	public static final String NOFLY_MATCH_APPROVED = "noFlyMatchApproved";
	public static final String CREATED_PO = "createdPO";	// Added for DM-1606


	private Directory indexDirectory;  // set by constructor

	private AssetUseService assetUseService;  // set by Spring config
	private AssetUseRepository assetUseRepository;  // set by Spring config
	private ProductRepository productRepository; // wired by Spring //Added for DM-280
	private AssetUseIndexServiceRequiresNew assetUseIndexServiceRequiresNew;  // set by Spring config

	private final IndexWriteManager writeManager;  // init in constructor
	private final IndexReadManager readManager;  // init in constructor

	// overridden by Spring properties
	private int primeTimeStartHour = 8;
	private int primeTimeStopHour = 19;  // 19 = 7pm
	private AssetRepository assetRepository;	// Added for DM-1606


	public AssetUseIndexService() throws IOException {
		this(false);
	}

	/**
	 * For unit testing, useRAMDirectory should be true, otherwise false.
	 */
	public AssetUseIndexService(boolean useRAMDirectory) throws IOException {
		if (useRAMDirectory) {
			indexDirectory = new RAMDirectory();
		}
		else {
			String path = System.getenv(PERMISSIONS_HOME);
			if (path == null) {
				throw new RuntimeException(PERMISSIONS_HOME + " env variable not defined.");
			}

			File homeDir = new File(path);
			if (!homeDir.exists()) {
				throw new FileNotFoundException(PERMISSIONS_HOME + " [" + path + "] directory does not exist.");
			}

			File indexDir = new File(homeDir, INDEX_DIR);
			if (!indexDir.exists()) {
				// temporary code to rename OLD_INDEX_DIR
				File oldIndexDir = new File(homeDir, OLD_INDEX_DIR);
				if (oldIndexDir.exists()) {
					oldIndexDir.renameTo(indexDir);
				}
				else {
					// permanent code section
					boolean ok = indexDir.mkdir();
					if (!ok) {
						throw new IOException("Could not create index dir: " + indexDir.getAbsolutePath());
					}
				}
			}

			 indexDirectory = FSDirectory.open(indexDir.toPath());  // throws IOException
		}

		// delay creation of searcher in case the index does not exist yet

		writeManager = new IndexWriteManager(indexDirectory, this, 10);

		readManager = new IndexReadManager(indexDirectory);
	}

	/** Implements IndexWriterConfigFactory */
	@Override
	public IndexWriterConfig newConfig() {
		return new IndexWriterConfig(new StandardAnalyzer());
		// return new IndexWriterConfig( new StandardAnalyzer());
	}

	public IndexInfo readIndexInfo() throws IOException {
		return readManager.readIndexInfo();
	}

	public List<FieldInfo> readIndexedFieldInfo() throws IOException {
		return readManager.readIndexedFieldInfo();
	}

	public List<FieldInfo> readStoredFieldInfo() throws IOException {
		return readManager.readStoredFieldInfo();
	}

	/**
	 * Lucene 8 cannot open Lucene 4.x on-disk segments. Wipe and create an empty
	 * Lucene 8 index so incremental rebuild / writes can proceed.
	 */
	private void ensureIndexReadableOrRecreate() throws IOException {
		if (LuceneUtil.canOpenIndex(indexDirectory)) {
			LuceneUtil.createIndexIfDoesNotExist(indexDirectory, newConfig());
			return;
		}
		log.warn("ensureIndexReadableOrRecreate(): Asset Use index is Lucene-format-incompatible; recreating empty Lucene 8 index under "
				+ INDEX_DIR + ". Operators must rebuild (per CW or full).");
		readManager.reset();
		LuceneUtil.recreateEmptyIndex(indexDirectory, newConfig());
		readManager.reset();
	}

    /**
     * Called by Mule / Quartz.
     */
	@Transactional(propagation = Propagation.SUPPORTS)
	public void buildIndex() throws Exception {
		buildIndex(null, false);
	}

	/**
	 * Called by Admin UI.
	 *
	 * @param cwId  null means rebuild entire index
	 */
	public void buildIndex(Integer cwId, boolean overridePrimeTime) throws Exception {
		log.debug("buildIndex() called...");

		// temporary code for memory debug
		/*
		Runtime runtime = Runtime.getRuntime();
		runtime.gc();
		log.debug("buildIndex(): memory before start = " + MemoryStats.getCurrentAsString());
		*/

		long startTime = System.currentTimeMillis();

		// Lucene 4.x on-disk indexes must be replaced before incremental rebuild can run.
		ensureIndexReadableOrRecreate();

		//buildIndexByIdFromScratch();  // throws Exception
	    buildIndexByIdIncremental(cwId, overridePrimeTime);  // throws Exception

		long time = System.currentTimeMillis() - startTime;
		TimeFormat timeFormat = new TimeFormat(true);
		log.debug("buildIndex(): took " + timeFormat.formatMS(time, true) + " to build index");
		if (cwId == null) {
			getMonitor().addTime("AssetUseIndexService::buildIndex::(all)", time);
		}

		// temporary code for memory debug
		/*
		log.debug("buildIndex(): total memory after = " + MemoryStats.getCurrentAsString());
		runtime.gc();
		log.debug("buildIndex(): total memory after gc = " + MemoryStats.getCurrentAsString());
		*/
	}

	/**
	 * @param cwId  null means rebuild entire index
	 */
	public void buildIndexByIdIncremental(Integer cwId, boolean overridePrimeTime) throws PersistenceException, IOException, ParseException {
		ensureIndexReadableOrRecreate();

		long startTime = System.currentTimeMillis();
		String where = (cwId == null) ? null : "cw_id = " + cwId;
		List<Integer> idsFromDbList = assetUseRepository.loadAssetUseIds(where);
		long time = System.currentTimeMillis() - startTime;
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		log.debug("buildIndexByIdIncremental(): loaded " + intFormat.format(idsFromDbList.size())
				+ " ids from db in " + time + " ms. cwId [" + cwId + "]");

		ArrayList<AuIdAndIndexDate> idsFromIndexList = new ArrayList<AuIdAndIndexDate>();
		Set<Integer> idsFromIndex = getAssetUseIdsFromIndex(cwId, idsFromIndexList);
		log.debug("buildIndexByIdIncremental(): #ids in index = "
				+ intFormat.format(idsFromIndex.size()) + " cwId [" + cwId + "]");

		Set<Integer> idsOnlyInDb = new HashSet<Integer>(idsFromDbList.size());
		idsOnlyInDb.addAll(idsFromDbList);
		idsOnlyInDb.removeAll(idsFromIndex);
		log.debug("buildIndexByIdIncremental(): idsOnlyInDb.size() = "
				+ intFormat.format(idsOnlyInDb.size()) + " cwId [" + cwId + "]");

		// create Set because makes operations below (removeAll, retainAll) much faster than dealing with List
		Set<Integer> idsFromDbSet = new HashSet<Integer>();
		idsFromDbSet.addAll(idsFromDbList);

		Set<Integer> idsOnlyInIndex = new HashSet<Integer>(idsFromIndex.size());
		idsOnlyInIndex.addAll(idsFromIndex);
		idsOnlyInIndex.removeAll(idsFromDbSet);
		log.debug("buildIndexByIdIncremental(): idsOnlyInIndex.size() = "
				+ intFormat.format(idsOnlyInIndex.size()) + " cwId [" + cwId + "]");

		idsFromIndex.retainAll(idsFromDbSet);
		Set<Integer> commonIds = idsFromIndex;
		log.debug("buildIndexByIdIncremental(): commonIds.size() = "
				+ intFormat.format(commonIds.size()) + " cwId [" + cwId + "]");

		// created list of commonIds sorted by indexDate
		ArrayList<AuIdAndIndexDate> sortedList = new ArrayList<AuIdAndIndexDate>(idsFromIndexList.size());
		for (AuIdAndIndexDate object : idsFromIndexList) {
			if (commonIds.contains(object.getId())) {
				sortedList.add(object);
			}
		}
		Collections.sort(sortedList);

		// delete idsOnlyInIndex from index
		for (Integer id : idsOnlyInIndex) {
			deleteFromIndex(id);
		}
		log.debug("buildIndexByIdIncremental(): deleted " + intFormat.format(idsOnlyInIndex.size()) + " index records.");

		for (Integer id : idsOnlyInDb) {
			assetUseIndexServiceRequiresNew.updateIndex(id);
		}
		log.debug("buildIndexByIdIncremental(): inserted " + intFormat.format(idsOnlyInDb.size()) + " index records.");

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		// we keep track of update count because if stopped early for "prime time" may be less than sortedList.size()
		int updateCount = 0;
		for (int i = 0; i < sortedList.size(); i++) {
			if (i % 1000 == 0) {
				log.debug("buildIndexByIdIncremental(): " + intFormat.format(i)
					+ " out of " + intFormat.format(sortedList.size()) + " updated, oldest index_date now "
					+ dateFormat.format(sortedList.get(i).getDate()));

				if (!overridePrimeTime && cwId == null && duringPrimeTime()) {
					log.info("buildIndexByIdIncremental(): stopping because it's 'prime time' (M-F 8am-7pm).");
					break;
				}
			}
			AuIdAndIndexDate object = sortedList.get(i);
			assetUseIndexServiceRequiresNew.updateIndex(object.getId());
			updateCount++;
		}
		log.debug("buildIndexByIdIncremental(): updated "
			+ intFormat.format(updateCount) + " index records.");
	}

	private boolean duringPrimeTime() {
		Calendar cal = Calendar.getInstance();
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) return false;
		int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
		return (hourOfDay >= primeTimeStartHour && hourOfDay < primeTimeStopHour);
	}

	/**
	 * This method is no longer used - buildIndexByIdIncremental() above used instead.
	 *
	 * This version to be called by the application (not by unit test).
	 * Loads each AssetUse separately rather than taking a list of
	 * objects since all objects may not fit in memory at once.
	 * Commits the transaction every so often (calls REQUIRES_NEW method)
	 * so that memory can be collected.
	 *
	 * @param cwIdList  Must be non-null
	 * @throws Exception
	 */
	public void buildIndexByIdFromScratch() throws Exception {
		List<Integer> auIdList = assetUseRepository.loadAssetUseIds(null);

		IndexWriterConfig config = newConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		IndexWriter writer = LuceneUtil.createIndexWriterWithRetryLogic(indexDirectory, config, 50, 60);

		int count = 0;
		int failureCount = 0;
		NumberFormat intFormat = NumberFormat.getIntegerInstance();

	    for (Integer id: auIdList) {
	    	try {
	    		assetUseService.addAssetUseByIdToIndex(id, writer);
	    		// above method will call addToIndex()
	    		//addToIndex(au, writer);  // throws IOException
	    	}
	    	catch (Exception ex) {
	    		failureCount++;
	    		log.error("failed to build index for AssetUse id " + id, ex);
	    	}

	    	count++;
	    	if (count % 1000 == 0) {
	    		log.debug("buildIndexByIdFromScratch(): reached count "
	    			+ intFormat.format(count) + " out of "
	    			+ intFormat.format(auIdList.size()) + " | failureCount = " + failureCount);
	    	}
	    }

	    writer.close();
	}

	/**
	 * This is ONLY intended to be called with a hard-coded auList for unit testing.
	 * Not currently designed to be called from the application since it doesn't
	 * contain any retry logic for when the index is already being written to.
	 *
	 * @param auList  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void buildIndexByWhole(List<AssetUse> auList) throws IOException {
		ArgUtil.notNull(auList, "auList");

		IndexWriterConfig config = newConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE);
	    IndexWriter writer = new IndexWriter(indexDirectory, config);

	    for (AssetUse au: auList) {
	    	addToIndex(au, writer);  // throws IOException
	    }

	    writer.close();
	}

	public void deleteFromIndex(int assetUseId) {
		Term deleteTerm = new Term(ASSET_USE_ID, String.valueOf(assetUseId));
		writeManager.updateDocument(null, deleteTerm);
	}

	public void deleteFromIndexNow(int assetUseId) throws IOException {
		Term deleteTerm = new Term(ASSET_USE_ID, String.valueOf(assetUseId));
		writeManager.updateDocumentNow(null, deleteTerm);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AssetUse updateIndex(int auId) throws PersistenceException {
		AssetUse au = assetUseRepository.loadAssetUseById(auId);
		updateIndex(au);
		return au;
	}

	/**
	 * Returns true if the index was successfully updated right now.
	 * False means the index could not be updated right now but was scheduled
	 * for the background thread.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AssetUse updateIndexNow(int auId) throws PersistenceException, IOException {
		AssetUse au = assetUseRepository.loadAssetUseById(auId);
		updateIndexNow(au);  // ignore return value, throws IOException
		return au;
	}

	public boolean updateIndexNow(Collection<Integer> auIds) throws PersistenceException, IOException {
		ArrayList<UpdateWrapper> wrapperList = new ArrayList<UpdateWrapper>();

		for (Integer auId : auIds) {
			Document doc = assetUseIndexServiceRequiresNew.buildDocument(auId);
			Term deleteTerm = new Term(ASSET_USE_ID, String.valueOf(auId));
			wrapperList.add(new UpdateWrapper(doc, deleteTerm));
		}
		try{
		return writeManager.updateDocumentsNow(wrapperList);
		}
		catch (Exception e) {
			// TODO: handle exception
			log.error("inside catch wrapperList  updateIndexNow(Collection<Integer> auIds) ",e);
			return false;
		}
	}

	public void updateIndex(AssetUse au) {
		Document doc = buildDocument(au);
		Term deleteTerm = new Term(ASSET_USE_ID, String.valueOf(au.getId()));
		writeManager.updateDocument(doc, deleteTerm);
	}

	/**
	 * Returns true if the index was successfully updated right now.
	 * False means the index could not be updated right now but was scheduled
	 * for the background thread.
	 */
	public boolean updateIndexNow(AssetUse au) throws IOException {
		Document doc = buildDocument(au);
		Term deleteTerm = new Term(ASSET_USE_ID, String.valueOf(au.getId()));
		return writeManager.updateDocumentNow(doc, deleteTerm);  // throws IOException
	}
/*
	public boolean updateIndexNow(Collection<AssetUse> auCollection) {
		ArrayList<UpdateWrapper> wrapperList = new ArrayList<UpdateWrapper>();

		for (AssetUse au : auCollection) {
			assetUseRepository.setNeedsConfirmationFlag(au);
			Document doc = buildDocument(au);
			Term deleteTerm = new Term(ASSET_USE_ID, String.valueOf(au.getId()));
			wrapperList.add(new UpdateWrapper(doc, deleteTerm));
		}

		return writeManager.updateDocumentsNow(wrapperList);
	}*/

	/**
	 * This method should be called when a source is disabled or re-enabled
	 * but not when other source changes are made that aren't saved in the index
	 * (would be a waste of time).
	 */
	public void updateIndexForSourceInSeparateThread(String sourceExtId) {
		new SourceUpdateThread(sourceExtId).start();
	}

	class SourceUpdateThread extends Thread {
		private final String sourceExtId;

		public SourceUpdateThread(String sourceExtId) {
			this.sourceExtId = sourceExtId;
		}

	    @Override
	    public void run() {
	    	try {
	    		assetUseService.updateIndexForSource(sourceExtId);
	    	}
	    	catch (Exception ex) {
	    		log.error("Exception from updateIndexForSource(sourceExtId): ", ex);
	    	}
	    }
	}

	/** Only public so can be called by AssetUseService.loadAssetUseByIdForIndex(). */
	public void addToIndex(AssetUse au, IndexWriter writer) throws IOException {
		Document doc = buildDocument(au);
		writer.addDocument(doc);
	}

	/**
	 * Only to be called by AssetUseIndexServiceRequiresNew.
	 */
	protected Document buildDocument(AssetUse au) {
        CommonWork commonWork = au.getCommonWork();
        Asset asset = au.getAsset();
        Document doc = new Document();

        // product stuff

        // Use StringField instead of IntField because we only need to lookup by exact match
        // (we don't need a range query)
        doc.add(new StringField(COMMON_WORK_ID, commonWork.getId().toString(), Field.Store.YES));

        for (Product product : commonWork.getProducts()) {
        	buildDocument(doc, product);
        }

        // no need to index - won't search on this
        doc.add(new StoredField(INTERIOR_CW_STATUS, commonWork.getInteriorCWStatus().getCode()));
        doc.add(new StoredField(COVER_CW_STATUS, commonWork.getCoverCWStatus().getCode()));

        // asset stuff

        // index as well as store so we can lookup for delete
        doc.add(new StringField(ASSET_ID, String.valueOf(asset.getId()), Field.Store.YES));

        for (Source source: asset.getSources()) {
        	doc.add(new StringField(SOURCE_ID, String.valueOf(source.getId()), Field.Store.YES));

		    doc.add(new TextField(SOURCE_NAME, QueryBuilder.normalizeAnalyzed(source.getName()),
		        Field.Store.YES));

		    String value = source.getId() + ":" + source.isNofly() + ":" + source.getName();
		    doc.add(new StoredField(SOURCE_ID_NOFLY_NAME, value));
        }

		doc.add(new StringField(SOURCE_NOFLY, String.valueOf(asset.isSourceNofly()), Field.Store.YES));

        if (asset.getMediaType() != null) {
            doc.add(new StringField(MEDIA_TYPE, QueryBuilder.normalizeCode(asset.getMediaType().getCode()),
            	Field.Store.NO));
            doc.add(new StoredField(MEDIA_TYPE_DISPLAY, asset.getMediaType().getDescription()));
        }

        doc.add(new TextField(DESCRIPTION, QueryBuilder.normalizeAnalyzed(asset.getDescription()),
            Field.Store.YES));
        doc.add(new StringField(DESCRIPTION_SORT, asset.getDescription(), Field.Store.YES));

        if (StringUtils.isNotBlank(asset.getKeywords())) {
        	doc.add(new TextField(KEYWORDS, QueryBuilder.normalizeAnalyzed(asset.getKeywords()),
                Field.Store.YES));
        }

        doc.add(new StringField(WORK_FOR_HIRE, String.valueOf(asset.isWorkForHire()), Field.Store.YES));

        if (asset.getOwnerType() != null) {
            doc.add(new StringField(OWNER_TYPE, QueryBuilder.normalizeCode(asset.getOwnerType().getCode()),
            	Field.Store.NO));
            doc.add(new StoredField(OWNER_TYPE_CODE, asset.getOwnerType().getCode()));
            doc.add(new StoredField(OWNER_TYPE_DESCRIPTION, asset.getOwnerType().getDescription()));
        }

		if (StringUtils.isNotBlank(asset.getVendorId())) {
		    doc.add(new StringField(SOURCE_REF, QueryBuilder.normalizeCode(asset.getVendorId()),
		    	Field.Store.NO));
		    doc.add(new StoredField(SOURCE_REF_DISPLAY, asset.getVendorId()));
		}

		if (StringUtils.isNotBlank(asset.getArtist())) {
		    doc.add(new TextField(ARTIST, QueryBuilder.normalizeAnalyzed(asset.getArtist()),
		    	Field.Store.YES));
		}

		doc.add(new StringField(MANAGED, String.valueOf(asset.isManaged()), Field.Store.YES));
		doc.add(new StringField(FEE_REQUIRED, String.valueOf(asset.isFeeRequired()), Field.Store.YES));
		doc.add(new StringField(ROYALTY_FREE, String.valueOf(asset.isRoyaltyFree()), Field.Store.YES));
		doc.add(new StringField(RESTRICTED_USE, String.valueOf(asset.isRestrictedUse()), Field.Store.YES));
		doc.add(new StringField(ACTIVE, String.valueOf(asset.isActive()), Field.Store.YES));
		doc.add(new StringField(ARCHIVE, String.valueOf(asset.isArchive()), Field.Store.YES));
		doc.add(new StringField(MUST_DISPLAY_CREDIT, String.valueOf(asset.isMustDisplayCredit()), Field.Store.YES));
		doc.add(new StringField(OBTAINED_BY_AUTHOR, String.valueOf(asset.isObtainedByAuthor()), Field.Store.YES));
		doc.add(new StringField(MODEL_RELEASE, String.valueOf(asset.getModelRelease().getCode()), Field.Store.YES));
		doc.add(new StringField(PROPERTY_RELEASE, String.valueOf(asset.isPropertyRelease()), Field.Store.YES));

		int assetFileCount = ((Number)(assetUseRepository.executeSingleResultNamedQuery(
				"Asset.countFiles", new Object [] { asset.getId() }))).intValue();
    	doc.add(new StoredField(HAS_ASSET_FILES, String.valueOf(assetFileCount > 0)));

		doc.add(new StringField(REVIEWED_OR_AUTHOR_UNKNOWN, String.valueOf(asset.isReviewedUnknown() || asset.isAuthorProvidedUnknown()),
				Field.Store.YES));


		// asset_use stuff

		// index as well as store so we can lookup for delete
        doc.add(new StringField(ASSET_USE_ID, String.valueOf(au.getId()), Field.Store.YES));

        //Adding AssetUse User Group ID
        //doc.add(new StringField(COMMON_WORK_ID, commonWork.getId().toString(), Field.Store.YES));

        doc.add(new StoredField(RESTRICTED_USE_APPROVED, String.valueOf(au.isRestrictedUseApproved())));
        doc.add(new StoredField(PRINT_RUN_WARNING, String.valueOf(au.isPrintRunWarning())));

        if (StringUtils.isNotBlank(au.getSortOrder())) {
        	doc.add(new StringField(SORT_ORDER, String.valueOf(au.getSortOrder()), Field.Store.YES));
        }

         // Store code not normalized to be used by AssetUseService().getPermissionStatusSet()
    	// which is used to built the task bar for the landing page.
        // Start : Added for DM-1606
        boolean createdPO = false ;
        if (au.getStatus() != null) {
        	String permStatus =  au.getStatus().getCode();
        	if(permStatus.equalsIgnoreCase(PermissionStatus.FORM_SENT.getCode()))
        	{
        		createdPO = assetRepository.checkForPoCreation(asset.getId());
        	//	boolean createdPO = true;
    			log.debug("AssetUseTableRowView() PO created "+createdPO+" for asset ID "+asset.getId());
    			// if any PO created for an asset in Purchase_order table then display waiting on invoice else display form sent
    		}
			doc.add(new StringField(PERMISSION_STATUS_CODE, QueryBuilder.normalizeCode(permStatus),
            	Field.Store.NO));
            doc.add(new StoredField(PERMISSION_STATUS_CODE_NOT_NORM, permStatus));
        }
        doc.add(new StringField(CREATED_PO, String.valueOf(createdPO), Field.Store.YES));
        // End : Added for DM-1606
        // if the asset use has files or the latest contract has files, we set a flag
        boolean hasAssetUseFiles = assetUseRepository.doesAssetUseHaveFiles(au.getId());
        boolean latestContractHasFile = assetUseRepository.doesLatestContractHaveFile(au.getId());
    	boolean hasFiles = hasAssetUseFiles || latestContractHasFile;
    	doc.add(new StoredField(HAS_FILES, String.valueOf(hasFiles)));

		doc.add(new StringField(CANCELED, String.valueOf(au.isCanceled()), Field.Store.YES));

		doc.add(new StringField(REPLACED, String.valueOf(au.isCanceled() && au.getCancelReplacement() != null),
			    Field.Store.YES));
		doc.add(new StoredField(NOFLY_MATCH_APPROVED, String.valueOf(au.isNoFlyMatchApproved())));

		if (null != au.getCancelReplacement()) {
			// won't search on so don't bother to index
			doc.add(new StoredField(REPLACEMENT_ID, String.valueOf(au.getCancelReplacement().getId())));
		}

		if (StringUtils.isNotBlank(au.getCreditLine())) {
		    doc.add(new TextField(CREDIT_LINE, QueryBuilder.normalizeAnalyzed(au.getCreditLine()),
		    	Field.Store.YES));
		}

        if (au.getUsage() != null) {
            doc.add(new StringField(USAGE, QueryBuilder.normalizeCode(au.getUsage().getCode()),
            	Field.Store.NO));
            doc.add(new StoredField(USAGE_DISPLAY, au.getUsage().getDescription()));
        }

		if (StringUtils.isNotBlank(au.getPosition())) {
		    doc.add(new TextField(POSITION, au.getPosition(), Field.Store.NO));
		    doc.add(new StringField(POSITION_DISPLAY, au.getPosition(), Field.Store.YES));
		    doc.add(new StringField(POSITION_SORT, QueryBuilder.normalizeAnalyzed(au.getPosition()),
			    Field.Store.YES));
		}

		if (StringUtils.isNotBlank(au.getFoundOn())) {
		    doc.add(new TextField(FOUND_ON, QueryBuilder.normalizeAnalyzed(au.getFoundOn()),
		    	Field.Store.YES));
		}

		if (au.getPagePosition() != null) {
			doc.add(new StringField(PAGE_POSITION_CODE, String.valueOf(au.getPagePosition().getCode()),
	    		    Field.Store.YES));
			doc.add(new StoredField(PAGE_POSITION_DISPLAY, String.valueOf(au.getPagePosition().getDescription())));
		}

		if (au.getComponent() != null) {
			doc.add(new StringField(COMPONENT_ID, String.valueOf(au.getComponent().getId()), Field.Store.YES));
		}

		if (StringUtils.isNotBlank(au.getComponentName())) {
		    doc.add(new TextField(COMPONENT_NAME, QueryBuilder.normalizeAnalyzed(au.getComponentName()),
			    Field.Store.YES));  // store only for possible debugging
		    doc.add(new StringField(COMPONENT_NAME_SORT, au.getComponentName(), Field.Store.YES));

		    Component component = au.getComponent();
            doc.add(new StringField(COMPONENT_CATEGORY_CODE, QueryBuilder.normalizeCode(component.getCategory().getCode()),
                	Field.Store.NO));
		}

		if (StringUtils.isNotBlank(au.getManuscriptPage())) {
		    doc.add(new TextField(MANUSCRIPT_PAGE, QueryBuilder.normalizeAnalyzed(au.getManuscriptPage()),
			    Field.Store.YES));
		}

		if (StringUtils.isNotBlank(au.getFinalPage())) {
		    doc.add(new TextField(FINAL_PAGE, QueryBuilder.normalizeAnalyzed(au.getFinalPage()),
			    Field.Store.YES));
		}

		doc.add(new StringField(COLOR, String.valueOf(au.isColor()), Field.Store.YES));

		doc.add(new StringField(CUSTOM, String.valueOf(au.isCustom()), Field.Store.YES));

        if (au.getSize() != null) {
            doc.add(new StringField(SIZE, QueryBuilder.normalizeCode(au.getSize().getCode()),
            	Field.Store.NO));
            doc.add(new StoredField(SIZE_DISPLAY, au.getSize().getDescription()));
        }

		if (StringUtils.isNotBlank(au.getCaption())) {
		    doc.add(new TextField(CAPTION, QueryBuilder.normalizeAnalyzed(au.getCaption()), Field.Store.YES));
		}

		if (StringUtils.isNotBlank(au.getPermissionComment())) {
		    doc.add(new TextField(PERMISSION_COMMENT, QueryBuilder.normalizeAnalyzed(au.getPermissionComment()),
		    	Field.Store.YES));
		}

		if (StringUtils.isNotBlank(au.getProductionComment())) {
		    doc.add(new TextField(PRODUCTION_COMMENT, QueryBuilder.normalizeAnalyzed(au.getProductionComment()),
		    	Field.Store.YES));
		    // currently we don't support search by production comment in Advanced asset search
		    // but maybe in future so leave as an indexed field
		}

		doc.add(new StringField(REUSE, String.valueOf(au.isReusedFromPreviousEdition()), Field.Store.YES));
		doc.add(new StringField(PICKUP, String.valueOf(au.isPickup()), Field.Store.YES));
		doc.add(new StringField(MEDIAMANAGER, String.valueOf(au.isMediaManager()), Field.Store.YES)); //SR_301213

		// won't search on so don't bother to index
		doc.add(new StoredField(NEED_PAYMENT_REQUEST, String.valueOf(au.isNeedPaymentRequest())));
		doc.add(new StoredField(PAID, String.valueOf(au.isPaid())));

		doc.add(new StringField(SENT_TO_PRODUCTION, String.valueOf(au.isSentToProduction()), Field.Store.YES));
		doc.add(new StringField(CAMERA_COPY_TO_COME, String.valueOf(au.isCameraCopyToCome()), Field.Store.YES));
		doc.add(new StringField(MEDIA_RETURN_REQUEST, String.valueOf(au.isMediaReturnRequested()), Field.Store.YES));

		// won't search on so don't bother to index
		doc.add(new StoredField(LAST_UPDATED_DATE, String.valueOf(au.getLastUpdatedDate().getTime())));

		// won't search on so don't bother to index
		doc.add(new StoredField(NEED_TO_CONFIRM_CANCELS, String.valueOf(au.isNeedToConfirmCancels())));

		// currently only use this field for searching but store as well for debugging and possible future use
		doc.add(new StringField(CREATED_USER_ID, String.valueOf(au.getCreatedUser().getId()), Field.Store.YES));

		//Adding AssetUse User Group ID
        doc.add(new StringField(AU_USER_GROUP_ID, String.valueOf(au.getUserGroup().getId()), Field.Store.YES));

		// Use the group at the time of the creation, not the current group (may be different)
		//UserGroup group = au.getCreatedUser().getGroup();
		UserGroup group = au.getUserGroup();
		if (group != null) {  // in the future this will be non-nullable but check for null for now
			// currently only use this field for searching but store as well for debugging and possible future use
			doc.add(new StringField(CREATED_GROUP_ID, String.valueOf(group.getId()), Field.Store.YES));
		}

    	List<Integer> contractIds = assetUseRepository.getLatestContractIds(au.getId());
        for (Integer contractId : contractIds) {
	        doc.add(new StringField(LATEST_CONTRACT_ID, String.valueOf(contractId), Field.Store.YES));
        }

        // won't search on so don't bother to index
		doc.add(new StoredField(ESTIMATED_COST, String.valueOf(au.getEstimatedCost())));

        if (au.getGbpmCategory() != null) {
            doc.add(new StringField(GBPM_CATEGORY, String.valueOf(au.getGbpmCategory()), Field.Store.YES));
        }

        if (au.getImportSource() != null) {
        	doc.add(new StringField(IMPORT_SOURCE_CODE, String.valueOf(au.getImportSource()), Field.Store.YES));
        }

        doc.add(new StoredField(INDEX_DATE, String.valueOf(System.currentTimeMillis())));

        //printDocument(doc);

		return doc;
	}

	private void buildDocument(Document doc, Product product) {
        // Use StringField instead of IntField because we only need to lookup by exact match
        // (we don't need a range query)
		// store just for possible reference
        doc.add(new StringField(PRODUCT_ID, product.getId().toString(),
        		Field.Store.YES));

        List<User> authors = product.getAuthors();
        if (!CollectionUtils.isEmpty(authors)) {
        	for (User author : authors) {
        		doc.add(new TextField(AUTHOR_NAME, author.getFullName(), Field.Store.YES));
        	}
        }

        doc.add(new TextField(TITLE, QueryBuilder.normalizeAnalyzed(product.getTitle()), Field.Store.YES));
        doc.add(new StringField(TITLE_SORT, product.getTitle(), Field.Store.YES));

        if (StringUtils.isNotBlank(product.getIsbn13())) {//updated for DM-280
            // since not stored in db with hyphens, don't have separate ISBN_DISPLAY
            doc.add(new StringField(ISBN13, QueryBuilder.normalizeCode(product.getIsbn13()), Field.Store.YES));
        }

        if (product.getCopyrightYear() != null) {
            doc.add(new StringField(COPYRIGHT_YEAR, String.valueOf(product.getCopyrightYear()),
                Field.Store.YES));
        }

        List<UserToRole> userList = product.getUsers();
        for (UserToRole u2p: userList) {
            if (u2p.hasNonAuthorRole()) {
            	doc.add(new TextField(USER_NAME, QueryBuilder.normalizeAnalyzed(u2p.getUser().getFullName()),
            		Field.Store.NO));
            }
        }

        if (product.getPublicationStatus() != null) {
            doc.add(new StringField(PUBLICATION_STATUS, QueryBuilder.normalizeCode(product.getPublicationStatus().getCode()),
            	Field.Store.NO));
            // While Publication Status description should not be null it is possible so check for it
            String description = product.getPublicationStatus().getDescription();
            if (description == null) {
            	description = product.getPublicationStatus().getCode();
            }
            doc.add(new StoredField(PUBLICATION_STATUS_DISPLAY, description));
        }

      //Start: Added for DM-280
        String pnumber = product.getPnumber();
        Integer editionNumber = product.getEditionNumber();
        String businessUnitCode = product.getBusinessUnit().getCode();
        String bUnitCode = product.getBusinessUnit().getCode();
        String productPriority = product.getProductPriority();
    	Medium medium = product.getMedium();
    	String mediumCode = (medium == null) ? null : medium.getCode();
    	String dataSource = product.getDataSource();
    	String shortAuthorName = product.getShortAuthorName();
    	String editorCode = product.getEditor();
		String editorName = null;
		if (StringUtils.isNotBlank(editorCode)) {
			try {
				editorName = productRepository.lookupEditorNameForCode(editorCode, dataSource);
			} catch (PersistenceException pe) {
				pe.printStackTrace();
			}

		}
		CommonWork cw = product.getCommonWork();
		ComplianceStatus complianceStatus = cw.getComplianceStatus();
    	Integer auCountNotCanceled = cw.getAUCountNotCanceled();
    	Integer coverCountNotCanceled =  cw.getCoverCountNotCanceled();
    	Integer statusNotOkCount = cw.getStatusNotOkCount();
		Integer photoIllusTotalCount = product.getPhotoIllusTotalCount();
		User photoEditor = product.getPhotoEditor();
		String photoEditorLastName = photoEditor == null ? null : photoEditor.getLastName();
		String photoEditorFirstName = photoEditor == null ? null : photoEditor.getFirstName();
		String lastUpdatedDate = null;
		String consolidatedReleaseDate = null;
		String transmittalDate = null;
		Date tempDate = null;
		tempDate = product.getLastUpdatedDate();
		lastUpdatedDate = getDateAsMMddyyyy(tempDate);
		if (StringUtils.isNotBlank(lastUpdatedDate)) {
			doc.add(new StringField(PROD_LAST_UPDATED_DATE, lastUpdatedDate, Field.Store.YES));
        }
		tempDate = product.getConsolidatedReleaseDate();
		consolidatedReleaseDate = getDateAsMMddyyyy(tempDate);
		if (StringUtils.isNotBlank(consolidatedReleaseDate)) {
			doc.add(new StringField(CONSOLIDATED_RELEASE_DATE, consolidatedReleaseDate, Field.Store.YES));
        }
		tempDate = product.getTransmittalDate();
		transmittalDate = getDateAsMMddyyyy(tempDate);
		if (StringUtils.isNotBlank(transmittalDate)) {
			doc.add(new StringField(TRANSMITTAL_DATE, transmittalDate, Field.Store.YES));
        }
    	if (StringUtils.isNotBlank(pnumber)) {
            doc.add(new StringField(PNUMBER, QueryBuilder.normalizeCode(pnumber), Field.Store.YES));
        }
        if (editionNumber != null) {
        	doc.add(new StringField(EDITION_NUMBER, String.valueOf(editionNumber), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(businessUnitCode)) {
        	doc.add(new StringField(BUSINESS_UNIT_CODE, QueryBuilder.normalizeCode(businessUnitCode),
                Field.Store.YES));
        }
        if (StringUtils.isNotBlank(product.getIsbn10())) {
            doc.add(new StringField(ISBN10, QueryBuilder.normalizeCode(product.getIsbn10()), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(bUnitCode)) {
        	doc.add(new StoredField(B_UNIT_CODE, bUnitCode));
        }
        if (StringUtils.isNotBlank(productPriority)) {
        	doc.add(new StringField(PRODUCT_PRIORITY, productPriority, Field.Store.YES));
        }
        if (StringUtils.isNotBlank(mediumCode)) {
        	doc.add(new StringField(MEDIUM_CODE, QueryBuilder.normalizeCode(mediumCode), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(dataSource)) {
        	doc.add(new StringField(DATA_SOURCE, QueryBuilder.normalizeCode(dataSource), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(shortAuthorName)) {
        	doc.add(new TextField(SHORT_AUTHOR_NAME, QueryBuilder.normalizeAnalyzed(shortAuthorName), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(editorCode)) {
        	doc.add(new StringField(EDITOR_CODE, QueryBuilder.normalizeCode(editorCode), Field.Store.YES));
        }

        if (StringUtils.isNotBlank(editorName)) {
        	doc.add(new TextField(EDITOR_NAME, QueryBuilder.normalizeAnalyzed(editorName), Field.Store.YES));
        }
        if (complianceStatus != null) {
        	doc.add(new StringField(COMPLIANCE_STATUS, QueryBuilder.normalizeCode(complianceStatus.getCode()),
            		Field.Store.YES));
        }
        if (auCountNotCanceled != null) {
        	doc.add(new StringField(AU_COUNT_NOT_CANCELED, QueryBuilder.normalizeCode(String.valueOf(auCountNotCanceled)), Field.Store.YES));
        }

        if (coverCountNotCanceled != null) {
        	doc.add(new StringField(COVER_COUNT_NOT_CANCELED, QueryBuilder.normalizeCode(String.valueOf(coverCountNotCanceled)), Field.Store.YES));
        }

        if (statusNotOkCount != null) {
        	doc.add(new StringField(STATUS_NOT_OK_COUNT, QueryBuilder.normalizeCode(String.valueOf(statusNotOkCount)), Field.Store.YES));
        }

        if (photoIllusTotalCount != null) {
        	doc.add(new StringField(PHOTO_ILLUS_TOTAL_COUNT, QueryBuilder.normalizeCode(String.valueOf(photoIllusTotalCount)), Field.Store.YES));
        	//doc.add(new StoredField(PHOTO_ILLUS_TOTAL_COUNT, String.valueOf(photoIllusTotalCount)));
        }
        if (StringUtils.isNotBlank(photoEditorLastName)) {
        	doc.add(new TextField(PHOTO_EDITOR_LAST_NAME,  QueryBuilder.normalizeAnalyzed(photoEditorLastName), Field.Store.YES));
        }

        if (StringUtils.isNotBlank(photoEditorFirstName)) {
        	doc.add(new TextField(PHOTO_EDITOR_FIRST_NAME, QueryBuilder.normalizeAnalyzed(photoEditorFirstName),Field.Store.YES));
        }

      //End: Added for DM-280
	}

	//Start: Added for DM-280
	private String getDateAsMMddyyyy(Date date) {
		if(null != date) {
			SimpleDateFormat formatter = new SimpleDateFormat("MMddyyyy");
			return formatter.format(date);
		}
		return "";
	}
	//End: Added for DM-280

	/** For debugging. */
	@SuppressWarnings("unused")
	private void printDocument(Document doc) {
		List<IndexableField> fieldList = doc.getFields();
		for (IndexableField f: fieldList) {
			log.debug(f.name() + " = " + f.stringValue());
		}
	}

	/**
	 * This method is used for AssetUse Search.
	 */
	public AssetUseSearchResults searchIndex(AssetSearchForm form) throws ParseException, IOException {
		List<SearchCriterion> criteria = new ArrayList<SearchCriterion>();

		String queryString = buildQueryString(form, criteria);

		return searchIndex(queryString, MAX_RESULTS, criteria);
	}

	/**
	 * This method is used to build the table on the CW landing page.
	 */
	public AssetUseSearchResults searchIndex(int cwId, LandingFilterForm filterForm, boolean includeCovers, boolean includeCanceled) throws ParseException, IOException {
		List<SearchCriterion> criteria = new ArrayList<SearchCriterion>();

		// filterForm may be null
		String queryString = buildQueryString(cwId, filterForm, criteria, includeCovers, includeCanceled);

		return searchIndex(queryString, CW_MAX_RESULTS, criteria);
	}

	public AssetUseSearchResults getCWAssetUsesFromIndex(int cwId, boolean includeCovers, boolean includeCanceled) throws ParseException, IOException {
		return searchIndex(cwId, null, includeCovers, includeCanceled);
	}

	//Start: Added for DM-534
			public AssetUseSearchResults getCWAssetUsesFromIndexForExcel(int cwId, boolean includeCanceled, boolean includeRemoved, boolean includeReplaced) throws ParseException, IOException {
				List<SearchCriterion> criteria = new ArrayList<SearchCriterion>();
				QueryBuilder qb = new QueryBuilder();
				qb.addInt(cwId, COMMON_WORK_ID, "Common Work Id");

				if (!includeCanceled) {
					qb.addBoolean(false, CANCELED, "Canceled");
				}

				if (!includeReplaced) {
					qb.addBoolean(false, REPLACED, "replaced");
				}
				// Commented As we are not indexing anywhere with REMOVED status
				/*if (!includeRemoved) {
					qb.addBoolean(false, REMOVED, "removed");
					log.debug(" qb.getQueryString(); REMOVED "+qb.getQueryString());
				}*/

				String queryString = qb.getQueryString();
				log.debug("buildQueryString(): queryString = " + queryString);
				return searchIndex(queryString, CW_MAX_RESULTS, criteria);
			}

			//End: Added for DM-534

	//Code Change for SS Task 3 - Added new method to include userGroupId in the query
	/**
	 * @param id
	 * @return  The AssetSearchResults or null if not found
	 */
	public AssetUseSearchResults getCWAssetUsesFromIndexWithUserGroupId(int cwId, int userGroupId,boolean includeCovers, boolean includeCanceled) throws ParseException, IOException {
		List<SearchCriterion> criteria = new ArrayList<SearchCriterion>();
		QueryBuilder qb = new QueryBuilder();
		qb.addInt(cwId, COMMON_WORK_ID, "Common Work Id");
		if(userGroupId>0){
		qb.addInt(userGroupId, AU_USER_GROUP_ID, "Asset Use Group Id");
		}

		if (!includeCovers) {
			qb.addNotCode(ComponentCategory.COVER.getCode(), COMPONENT_CATEGORY_CODE, "!" + ComponentCategory.COVER.getDescription());
		}

		if (!includeCanceled) {
			qb.addBoolean(false, CANCELED, "Canceled");
		}

		String queryString = qb.getQueryString();
		log.debug("buildQueryString(): queryString = " + queryString);
		return searchIndex(queryString, CW_MAX_RESULTS, criteria);
	}


	/**
	 * @param id
	 * @return  The AssetSearchResult or null if not found
	 */
	public AssetUseSearchResult searchIndexByAssetUseId(int id) throws ParseException, IOException {
		QueryBuilder qb = new QueryBuilder();
		qb.addInt(id, ASSET_USE_ID, "Asset Use Id");
		String queryString = qb.getQueryString();
		//log.debug("searchByAssetUseId(): queryString = " + queryString);

		AssetUseSearchResults results = searchIndex(queryString, 1, null);
		List<AssetUseSearchResult> list = results.getDocuments();
		//log.debug("searchByAssetUseId(): found = " + (list.size() > 0));

		if (list.size() == 0)  return null;
		else {
			if (list.size() > 1) {
				log.error("searchByAssetUseId(): more than one (" + list.size() + ") entry in index for id [" + id + "]");
			}
			return list.get(0);
		}
	}

	/**
	 * returns a Set of distinct PermissionsStatus for a list of AssetUse (excluding any null statuses)
	 * @param assetUses
	 * @return Set<PermissionStatus>
	 * @throws IOException
		 */
	public Set<PermissionStatus> getPermissionStatusSet(AssetUseSearchResults results) throws IOException
	{
		Set<PermissionStatus> set = new HashSet<PermissionStatus>();

		// getDocuments() throws IOException
		for (AssetUseSearchResult result : results.getDocuments()) {
			// add the asset only if has at least one Source (bug #0001945)
			if (result.exclude()) {
				continue;
			}
			PermissionStatus status = result.getPermissionStatus();
			// check for null status - normally not expected but possible
			if (status != null) {
				set.add(status);
			}
		}

		log.debug("getPermissionStatusSet(): Distinct Permissions Statuses are: " + set);

		return set;
	}

	private AssetUseSearchResults searchIndex(String queryString, int maxResults, List<SearchCriterion> criteria) throws ParseException, IOException {
		long startTime = System.currentTimeMillis();

         QueryParser qp = new QueryParser(DESCRIPTION, new StandardAnalyzer());
		// QueryParser qp = new QueryParser(DESCRIPTION, new StandardAnalyzer());
        Query query = qp.parse(queryString);  // throws ParseException
        log.debug("searchIndex(): query.toString(): " + query.toString());

        // -- No longer need to sort since the table does it.
        // This sort order is the same as the initial sort on the Product Landing page,
        // except adding the Product Title first.
        //SortField [] sortFields = {
        //	new SortField(TITLE_SORT, SortField.STRING),
        //	new SortField(COMPONENT_NAME_SORT, SortField.STRING),
        //	new SortField(POSITION_SORT, SortField.STRING),
        //	new SortField(DESCRIPTION_SORT, SortField.STRING)
        //};
        //Sort sort = new Sort(sortFields);

        // using readManager.getIndexSearcher() means we don't have to worry about closing the searcher
        IndexSearcher searcher = readManager.getIndexSearcher();
        //TopDocs hits = searcher.search(query, null, maxResults, sort);  // throws IOException
        TopDocs hits = searcher.search(query, maxResults);  // throws IOException

        long time = System.currentTimeMillis() - startTime;
        log.debug("searchIndex(): query parse + search took " + time + " ms.");

        return new AssetUseSearchResults(hits, searcher, criteria);
	}

	private Set<Integer> getAssetUseIdsFromIndex(Integer cwId, ArrayList<AuIdAndIndexDate> list) throws IOException, ParseException {
		if (cwId == null) return getAllAssetUseIdsFromIndex(list);
		else return getCWAssetUseIdsFromIndex(cwId, list);
	}

	/**
	 * Returns Set<Integer> containing AssetUse ids and fills up an empty
	 * List<AuIdAndIndexDate> (which is the same ids plus indexDate for each).
	 */
	private Set<Integer> getAllAssetUseIdsFromIndex(ArrayList<AuIdAndIndexDate> list) throws IOException {
		PerfTimer timer = getMonitor().startTimer("AssetUseIndexService::getAllAssetUseIdsFromIndex");

		// using readManager.getIndexSearcher() means we don't have to worry about closing the searcher or reader
		IndexReader reader = readManager.getIndexSearcher().getIndexReader();  // throws IOException
		int numDocs = reader.numDocs();  // number of non-deleted documents
		// maxDoc - 1 is the number of documents including deleted ones
		int maxDoc = reader.maxDoc();

		Set<Integer> set = new HashSet<Integer>(numDocs);
		list.ensureCapacity(numDocs);

		// liveDocs contains doc indexes of not-deleted docs
        Bits liveDocs = MultiBits.getLiveDocs(reader);

		for (int i = 0; i < maxDoc; i++) {
			if (liveDocs != null && !liveDocs.get(i)) continue;

			Document document = reader.document(i);  // throws IOException
			int id = Integer.parseInt(document.get(ASSET_USE_ID));
			long date = Long.parseLong(document.get(INDEX_DATE));
			set.add(new Integer(id));
			list.add(new AuIdAndIndexDate(id, date));
		}

		long time = timer.stopTimer();
		log.debug("getAllAssetUseIdsFromIndex(): took " + time + " ms.");

		return set;
	}

	private Set<Integer> getCWAssetUseIdsFromIndex(int cwId, ArrayList<AuIdAndIndexDate> list) throws ParseException, IOException {
		AssetUseSearchResults results = getCWAssetUsesFromIndex(cwId, true, true);  // throws IOException
		List<AssetUseSearchResult> resultList = results.getDocuments();
		Set<Integer> set = new HashSet<Integer>(resultList.size());
		list.ensureCapacity(resultList.size());

		for (AssetUseSearchResult result : resultList) {
			int auId = result.getAssetUseId();
			set.add(auId);
			list.add(new AuIdAndIndexDate(auId, result.getIndexDate().getTime()));
		}

		return set;
	}

	class AuIdAndIndexDate implements Comparable<AuIdAndIndexDate> {
		private final int id;
		private final long date;

		public AuIdAndIndexDate(int id, long date) {
			this.id = id;
			this.date = date;
		}

		public int getId() { return id; }
		public long getDate() { return date; }

		@Override
		public int compareTo(AuIdAndIndexDate o) {
			return o.getDate() == this.getDate() ? 0 : (this.getDate() > o.getDate() ? 1 : -1);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			AuIdAndIndexDate other = (AuIdAndIndexDate) obj;
			if (id != other.id) return false;
			return true;
		}
	}

	/**
	 * public only for unit testing
	 */
	public String buildQueryString(AssetSearchForm form, List<SearchCriterion> criteria) {
		QueryBuilder qb = new QueryBuilder(criteria);

		// product stuff
		if (form.getIncludeCommonWorkId()) {
			qb.addInt(form.getCommonWorkId(), COMMON_WORK_ID, "Common Work Id");
		}
		if (form.getIncludeEnteredCwID()) {
			qb.addInt(form.getEnteredCwID(), COMMON_WORK_ID, "Common Work Id");
		}
		if (form.getIncludeAuthor()) {
			qb.addAnalyzedWithoutCleanup(form.getAuthor(), AUTHOR_NAME, "Author Name");
		}
		if (form.getIncludeTitle()) {
			// Removed as it is not searching for titles with hyphen
			// qb.addAnalyzed(form.getTitle(), TITLE, "Title Name");

			// Added to search for the titles includes hyphen
			qb.addAnalyzedWithoutCleanup(form.getTitle(), TITLE,"Title Name");
		}

		// Removed ISBN13 and Isbn10 as Tim suggested not a show-stopper but confusing
		/*if (form.getIncludeIsbn13()) {
			qb.addCode(form.getIsbn13(), ISBN13, "ISBN13");
		}*/

		if (form.getIncludeCopyrightYear()) {
			qb.addCode(form.getCopyrightYear(), COPYRIGHT_YEAR, "Copyright Year");
		}
		if (form.getIncludeUserName()) {
			qb.addAnalyzedWithoutCleanup(form.getUserName(), USER_NAME, "User Name");
		}
		if (form.getIncludePublicationStatus() && form.getPublicationStatus() != null) {
			qb.addCode(form.getPublicationStatus().getCode(),
					form.getPublicationStatus().getDescription(), PUBLICATION_STATUS, "Publication Status");
		}
		//Start: Added for DM-280
		if(form.isIncludeProductNumber()) {
			qb.addAnalyzed(form.getProductNumber(), PNUMBER, "Product Number");
		}
		if(form.isIncludeEditionNumber()) {
			qb.addCode(form.getEditionNumber(), EDITION_NUMBER, "Edition Number");
		}
		if (form.isIncludeBusinessUnit() && form.getBusinessUnit() != null) {
			qb.addCode(form.getBusinessUnit().getCode(),
					form.getBusinessUnit().getName(), BUSINESS_UNIT_CODE, "Business Unit Name");
		}
		if(form.isIncludeBunitCode()) {
			qb.addInt(form.getBunitCode(), B_UNIT_CODE, "Business Unit Code");
		}

		// Removed ISBN13 and Isbn10 as Tim suggested not a show-stopper but confusing
		/*if (form.getIncludeIsbn10()) {
			qb.addCode(form.getIsbn10(), ISBN10, "ISBN10");
		}*/

		if(form.isIncludeProductPriority()) {
			qb.addCode(form.getProductPriority(), PRODUCT_PRIORITY, "Product Priority");
		}
		if (form.isIncludeMedium() && form.getMedium() != null) {
			qb.addCode(form.getMedium().getCode(),
					form.getMedium().getName(), MEDIUM_CODE, "Medium Code");
		}
		if (form.isIncludeDataSource()) {
			qb.addAnalyzed(form.getDataSource(), DATA_SOURCE, "Data Source");
		}
		if (form.isIncludeShortAuthorName()) {
			qb.addAnalyzedWithoutCleanup(form.getShortAuthorName(), SHORT_AUTHOR_NAME, "Short Author Name");
		}
		if (form.isIncludeEditorCode()) {
			qb.addCode(form.getEditorCode(), EDITOR_CODE, "Editor Code");
		}
		if (form.isIncludeEditorName()) {
			qb.addAnalyzedWithoutCleanup(form.getEditorName(), EDITOR_NAME, "Editor Name");
		}
		if (form.isIncludeComplianceStatus()) {
			qb.addAnalyzed(form.getComplianceStatus(), COMPLIANCE_STATUS, "Compliance Status");
		}
		if (form.isIncludeAUNotCancelCount()) {
			qb.addCode(form.getAuCountNotCanceledCnt(), AU_COUNT_NOT_CANCELED, "Asset Use Not Cancelled Count");
		}
		if (form.isIncludeCoverCountNotCanceled()) {
			qb.addCode(form.getCoverCountNotCanceled(), COVER_COUNT_NOT_CANCELED, "Cover Count Not Cancelled");
		}
		if (form.isIncludeStatusNotOkCount()) {
			qb.addCode(form.getStatusNotOkCount(), STATUS_NOT_OK_COUNT, "Status Not Ok Count");
		}
		if (form.isIncludePhotoIllusTotalCount()) {
			qb.addCode(form.getPhotoIllusTotalCount(), PHOTO_ILLUS_TOTAL_COUNT, "Photo Illustration Total Count");
		}
		if (form.isIncludePhotoEditorLastName()) {
			qb.addAnalyzedWithoutCleanup(form.getPhotoEditorLastName(), PHOTO_EDITOR_LAST_NAME, "Photo Editor Last Name");
		}
		if (form.isIncludePhotoEditorFirstName()) {
			qb.addAnalyzedWithoutCleanup(form.getPhotoEditorFirstName(), PHOTO_EDITOR_FIRST_NAME, "Photo Editor First Name");
		}
		if (form.isIncludeLastUpdatedDate()) {
			qb.addAnalyzed(form.getLastUpdatedDate(), PROD_LAST_UPDATED_DATE, "Last Updated Date");
		}
		if (form.isIncludeConsolidatedReleaseDate()) {
			qb.addAnalyzed(form.getConsolidatedReleaseDate(), CONSOLIDATED_RELEASE_DATE, "Consolidated Release Date");
		}
		if (form.isIncludeTransmittalDate()) {
			qb.addAnalyzed(form.getTransmittalDate(), TRANSMITTAL_DATE, "Transmittal Date");
		}
		//End: Added for DM-280

		// asset stuff
		if (form.getIncludeSourceName()) {
			qb.addAnalyzed(form.getSourceName(), SOURCE_NAME, "Source Name");
		}
		if (form.getIncludeMediaType() && form.getMediaType() != null) {
			qb.addCode(form.getMediaType().getCode(),
				form.getMediaType().getDescription(), MEDIA_TYPE, "Media Type");
		}
		if (form.getIncludeDescription()) {
			qb.addAnalyzed(form.getDescription(), DESCRIPTION, "Description");
		}
		if (form.getIncludeKeywords()) {
			qb.addAnalyzed(form.getKeywords(), KEYWORDS, "Keywords");
		}
		if (form.getIncludeWorkForHire()) {
			qb.addBoolean(form.isWorkForHire(), WORK_FOR_HIRE, "Work For Hire");
		}
		if (form.getIncludeOwnerType() && form.getOwnerType() != null) {
			qb.addCode(form.getOwnerType().getCode(),
				form.getOwnerType().getDescription(), OWNER_TYPE, "Owner Type");
		}
		if (form.getIncludeSourceRef()) {
			qb.addCode(form.getSourceRef(), SOURCE_REF, "Source Ref");
		}
		if (form.getIncludeArtist()) {
			qb.addAnalyzed(form.getArtist(), ARTIST, "Artist/Photographer");
		}
		if (form.getIncludeManaged()) {
			qb.addBoolean(form.isManaged(), MANAGED, "Managed");
		}
		if (form.getIncludeFeeRequired()) {
			qb.addBoolean(form.isFeeRequired(), FEE_REQUIRED, "Fee Required");
		}
		if (form.getIncludeRoyaltyFree()) {
			qb.addBoolean(form.isRoyaltyFree(), ROYALTY_FREE, "Royalty Free");
		}
		if (form.getIncludeRestrictedUse()) {
			qb.addBoolean(form.isRestrictedUse(), RESTRICTED_USE, "Restricted Use");
		}
		if (form.getIncludeActive()) {
			qb.addBoolean(form.isActive(), ACTIVE, "Active");
		}
		if (form.getIncludeArchive()) {
			qb.addBoolean(form.isArchive(), ARCHIVE, "Archive");
		}
		if (form.getIncludeMustDisplayCredit()) {
			qb.addBoolean(form.isMustDisplayCredit(), MUST_DISPLAY_CREDIT, "Must Display Credit");
		}
		if (form.getIncludeObtainedByAuthor()) {
			qb.addBoolean(form.isObtainedByAuthor(), OBTAINED_BY_AUTHOR, "Obtained By Author");
		}
		if (form.getIncludeModelRelease()) {
			qb.addCode(form.getModelRelease().getCode(),
					form.getModelRelease().getName(), MODEL_RELEASE, "Model Release");
		}
		if (form.getIncludePropertyRelease()) {
			qb.addBoolean(form.isPropertyRelease(), PROPERTY_RELEASE, "Property Release");
		}

		// asset_use stuff
		if (form.getIncludePermissionStatus() && form.getPermissionStatus() != null) {
			// Start : Updated for DM-1606
			String permStatusCode = form.getPermissionStatus().getCode() ;
			if(permStatusCode.equalsIgnoreCase(PermissionStatus.FORM_WAITING_ON_INVOICE.getCode()))
			{
				qb.addBoolean(true, CREATED_PO, "createdPO");
				qb.addCode(PermissionStatus.FORM_SENT.getCode(),
						PermissionStatus.FORM_WAITING_ON_INVOICE.getDescription(), PERMISSION_STATUS_CODE, "Permission Status");
			}
			else if(permStatusCode.equalsIgnoreCase(PermissionStatus.FORM_SENT.getCode()))
			{
				qb.addBoolean(false, CREATED_PO, "createdPO");
				qb.addCode(form.getPermissionStatus().getCode(),
						form.getPermissionStatus().getDescription(), PERMISSION_STATUS_CODE, "Permission Status");
			}
			else
			{
				qb.addCode(form.getPermissionStatus().getCode(),
						form.getPermissionStatus().getDescription(), PERMISSION_STATUS_CODE, "Permission Status");
			}
			// End : Updated for DM-1606

		}
		if (form.getIncludeCanceled()) {
			qb.addBoolean(form.isCanceled(), CANCELED, "Canceled");
		}
		if (form.getIncludeReplaced()) {
			qb.addBoolean(form.isReplaced(), REPLACED, "Replaced");
		}
		if (form.getIncludeCreditLine()) {
			qb.addAnalyzed(form.getCreditLine(), CREDIT_LINE, "Credit Line");
		}
		if (form.getIncludeUsage() && form.getUsage() != null) {
			qb.addCode(form.getUsage().getCode(),
				form.getUsage().getDescription(), USAGE, "Usage");
		}
		if (form.getIncludePosition()) {
			qb.addAnalyzed(form.getPosition(), POSITION, "Position", true);
		}
		if (form.getIncludeFoundOn()) {
			qb.addAnalyzed(form.getFoundOn(), FOUND_ON, "Found On", true);
		}
		if (form.getIncludeComponentName()) {
			qb.addAnalyzed(form.getComponentName(), COMPONENT_NAME, "Component Name");
		}
		if (form.getIncludeManuscriptPage()) {
			qb.addAnalyzed(form.getManuscriptPage(), MANUSCRIPT_PAGE, "Manuscript Page");
		}
		if (form.getIncludeFinalPage()) {
			qb.addAnalyzed(form.getFinalPage(), FINAL_PAGE, "Final Page");
		}
		if (form.getIncludeColor()) {
			qb.addBoolean(form.isColor(), COLOR, "Color");
		}
		if (form.getIncludeSize() && form.getSize() != null) {
			qb.addCode(form.getSize().getCode(),
				form.getSize().getDescription(), SIZE, "Size");
		}
		if (form.getIncludeCaption()) {
			qb.addAnalyzed(form.getCaption(), CAPTION, "Caption");
		}
		if (form.getIncludePermissionComment()) {
			qb.addAnalyzed(form.getPermissionComment(), PERMISSION_COMMENT, "Permission Comment");
		}
		if (form.getIncludeReuse()) {
			qb.addBoolean(form.isReuse(), REUSE, "Reused from Previous Edition");
		}
		if (form.getIncludePickup()) {
			qb.addBoolean(form.isPickup(), PICKUP, "Pickup from Another Title");
		}

		//SR_301213 starts
		if (form.getIncludeMediaManager()) {
			qb.addBoolean(form.isMediaManager(), MEDIAMANAGER, "Media Manager");
		}
		//SR_301213 ends


		if (form.getIncludeCameraCopyToCome()) {
			qb.addBoolean(form.isCameraCopyToCome(), CAMERA_COPY_TO_COME, "Camera Copy To Come");
		}
		if (form.getIncludeSentToProduction()) {
			qb.addBoolean(form.isSentToProduction(), SENT_TO_PRODUCTION, "Sent To Production");
		}
		if (form.getIncludeMediaReturnRequest()) {
			qb.addBoolean(form.isMediaReturnRequest(), MEDIA_RETURN_REQUEST, "Media Return Request");
		}

		String queryString = qb.getQueryString();
		log.debug("buildQueryString(): queryString = " + queryString);
		return queryString;
	}

	/**
	 * LandingFilterForm may be null
	 */
	public String buildQueryString(int cwId, LandingFilterForm form, List<SearchCriterion> criteria, boolean includeCovers, boolean includeCanceled) {
		QueryBuilder qb = new QueryBuilder(criteria);

		qb.addInt(cwId, COMMON_WORK_ID, "Common Work Id");


		if (form != null) {
			qb.addIntsOrBetween(form.getComponentIds().toArray(new Integer[]{}), COMPONENT_ID, "Component Id");
			qb.addCodesOrBetween(form.getMediaTypeCodes(), MEDIA_TYPE, "Media Type Code");
			qb.addCodesOrBetween(form.getUsageCodes(), USAGE, "Usage Code");
			qb.addCodesOrBetween(form.getOwnerTypeCodes(), OWNER_TYPE, "Owner Type Code");
			qb.addIntsOrBetween(form.getSourceIds().toArray(new Integer[]{}), SOURCE_ID, "Source Id");
			if (form.isUserSelected() && form.getUserId() != null) {
				qb.addInt(form.getUserId(), CREATED_USER_ID, "Created User Id");
			}
			if (form.isGroupSelected() && form.getGroupId() != null) {
				qb.addInt(form.getGroupId(), CREATED_GROUP_ID, "Created Group Id");
			}
			qb.addCodesOrBetween(form.getStatusCodes(), PERMISSION_STATUS_CODE, "Permission Status Id");
			if (StringUtils.isNotBlank(form.getCustomFilter())) {
				if (form.getCustomFilter().equals(UserDefaults.CUSTOM_FILTER))
					qb.addBoolean(true, CUSTOM, "Is Custom");
				else if (form.getCustomFilter().equals(UserDefaults.INTERNAL_FILTER))
					qb.addBoolean(false, CUSTOM, "Is Custom");
			}
		}

		if (!includeCovers) {
			qb.addNotCode(ComponentCategory.COVER.getCode(), COMPONENT_CATEGORY_CODE, "!" + ComponentCategory.COVER.getDescription());
		}

		if (!includeCanceled) {
			qb.addBoolean(false, CANCELED, "Canceled");
		}

		String queryString = qb.getQueryString();
		log.debug("buildQueryString(): queryString = " + queryString);
		return queryString;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	//Start: Added for DM-280
	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}
	//End: Added for DM-280

	public AssetUseIndexServiceRequiresNew getAssetUseIndexServiceRequiresNew() {
		return assetUseIndexServiceRequiresNew;
	}

	public void setAssetUseIndexServiceRequiresNew(
			AssetUseIndexServiceRequiresNew assetUseIndexServiceRequiresNew) {
		this.assetUseIndexServiceRequiresNew = assetUseIndexServiceRequiresNew;
	}

	public void setPrimeTimeStartHour(int hour) {
		this.primeTimeStartHour = hour;
	}

	public void setPrimeTimeStopHour(int hour) {
		this.primeTimeStopHour = hour;
	}

	//	Start : Added for DM-1606
	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}
	//	End : Added for DM-1606
}

class QueryBuilder {
	private final StringBuilder sb;
	private final List<SearchCriterion> criteria;
	private final String AND_SEP = " AND ";
	private final String OR_SEP = " OR ";
	private String SEP = AND_SEP;

	public QueryBuilder() {
		this(null, null);
	}

	public QueryBuilder(List<SearchCriterion> criteria) {
		this(criteria, null);
	}

	public QueryBuilder(List<SearchCriterion> criteria, StringBuilder sb) {
		if (criteria == null) {
			criteria = new ArrayList<SearchCriterion>();
		}
		if (sb == null) {
			sb = new StringBuilder();
		}
		this.sb = sb;
		this.criteria = criteria;
	}

	/**
	 * This method should only be used in simple situations where all query elements
	 * are codes - have not tested in all situations yet - probably need to modify
	 * code to work in all situations. (In some situations, params need to be added
	 * around the AND for analyzed phrases.)
	 */
	public void switchToOr() {
		SEP = OR_SEP;
	}

	public void addAnalyzed(String s, String key, String displayName) {
		addAnalyzed(s, key, displayName, false);
	}

	public void addAnalyzed(String s, String key, String displayName, boolean leaveHyphens) {
		if (StringUtils.isBlank(s))  return;

		if (sb.length() > 0)  sb.append(SEP);
		String s2 = LuceneUtil.removeStopWords(s.toLowerCase().trim());
		s2 = LuceneUtil.cleanUpTerm(s2, leaveHyphens);
		s2 = normalizeAnalyzed(s2);
		s2 = StringUtils.replace(s2, ",", " ");
		s2 = StringUtils.replace(s2, "_", " ");
		s2 = StringUtils.replace(s2, "-", " ");
		String s3 = LuceneUtil.parseWords(key + ":", s2 + "*", "* AND ", " ");
		sb.append('(').append(s3).append(')');
		//if (!s2.equals(s)) {
		//	s = s + " --> " + s2;
		//}
		criteria.add(new SearchCriterion(displayName, s));
	}

	//added without cleanUpTerm eg. cast-iron after lucene cleanUpTerm system is taking as castiron

	public void addAnalyzedWithoutCleanup(String s, String key, String displayName) {
		if (StringUtils.isBlank(s))  return;
		if (sb.length() > 0)  sb.append(SEP);
		String s2 = LuceneUtil.removeStopWords(s.toLowerCase().trim());

		// Commented as entered title name  eg. cast-iron after lucene cleanUpTerm system is taking as castiron
		//s2 = LuceneUtil.cleanUpTerm(s2, leaveHyphens);

		s2 = normalizeAnalyzed(s2);
		s2 = replaceExtraCharacters(s2);
		s2 = StringUtils.replace(s2, "_", " ");
		s2 = StringUtils.replace(s2, "-", " ");
		s2 = StringUtils.replace(s2, "\"", " ");
		s2 = s2.trim();
		String s3 = LuceneUtil.parseWords(key + ":", s2 + "*", "* AND ", " ");
		sb.append('(').append(s3).append(')');
		//if (!s2.equals(s)) {
		//	s = s + " --> " + s2;
		//}
		criteria.add(new SearchCriterion(displayName, s));
	}

	public void addCodesOrBetween(String [] array, String key, String displayName) {
		if (ArrayUtils.isEmpty(array))  return;

		if (sb.length() > 0) sb.append(SEP);

		StringBuilder sbCriteria = new StringBuilder();

		sb.append("(");

		for (int i = 0; i < array.length; i++) {
			String code = array[i];
			if (i > 0) {
				sb.append(OR_SEP);
				sbCriteria.append(OR_SEP);
			}
			sb.append(key).append(':');
			sb.append(normalizeCode(code));
			sbCriteria.append(code);
		}

		sb.append(")");
		criteria.add(new SearchCriterion(displayName, sbCriteria.toString()));
	}

	public void addCode(String code, String key, String displayName) {
		addCode(code, code, key, displayName);
	}

	public void addNotCode(String code, String key, String displayName) {
		addCode(code, code, key, displayName, true);
	}

	public void addCode(String code, String display, String key, String displayName) {
		addCode(code, display, key, displayName, false);
	}

	public void addCode(String code, String display, String key, String displayName, boolean not) {
		if (StringUtils.isBlank(code))  return;

		if (sb.length() > 0) sb.append(SEP);
		if (not) sb.append('!');
		sb.append(key).append(':');
		sb.append(normalizeCode(code));
		criteria.add(new SearchCriterion(displayName, display));
	}

	// Don't use addNotAnalyzed because if using StandardAnalyzer will
	// still lower case.
	/*
	public void addNotAnalyzed(String s, String key, String displayName) {
		addNotAnalyzed(s, s, key, displayName);
	}

	public void addNotAnalyzed(String s, String display, String key, String displayName) {
		if (StringUtils.isBlank(s))  return;

		if (sb.length() > 0) sb.append(SEP);
		sb.append(key).append(':');
		sb.append(s);
		criteria.add(new SearchCriterion(displayName, display));
	}*/

	public void addStartPhrase(String startPhrase, String key, String displayName) {
		if (StringUtils.isBlank(startPhrase))  return;

		if (sb.length() > 0) sb.append(SEP);
		sb.append(key).append(':');
		sb.append(normalizeStartPhrase(startPhrase));
		sb.append('*');
		criteria.add(new SearchCriterion(displayName, startPhrase));
	}

	/**
	 * Only use this method for adding complicated subqueries that are not
	 * covered by the other methods. You must manually normalize terms, etc
	 * yourself.
	 * @param s  Must be non-null
	 */
	public void addCustom(String s) {
		sb.append(s);
	}

	/**
	 * public so can be used for indexing also
	 */
	public static String normalizeCode(String code) {
		// lowercase, remove whitespace, and hyphens
		return StringUtil.removeAny(code.toLowerCase(), " \t\r\n-");
	}

	/**
	 * public so can be used for indexing also
	 */
	public static String normalizeAnalyzed(String s) {
		// remove single quotes, parentheses, and backslashes
		return StringUtil.removeAny(s, "'()\\");
	}

	public static String replaceExtraCharacters(String origString) {
		// remove end characters dots, comma, and colon
		String finalString = "";
		String[] splitString = origString.trim().split(" ");
		for (String splitStrings : splitString) {
			try {
				if (splitStrings != "" || splitStrings != null) {
					String subString = splitStrings.trim().substring(splitStrings.length() - 1, splitStrings.length());
					if (subString.equals(".") || subString.equals(",") || subString.equals(":"))
						// if(splitStrings.substring(splitStrings.length()-1,splitStrings.length()).matches("."))
						finalString = finalString + " " + splitStrings.substring(0, splitStrings.length() - 1);
					else
						finalString = finalString + " " + splitStrings;
				}
			} catch (Exception ex) {
				continue;
			}
		}
		return finalString;
	}



	/**
	 * public so can be used for indexing also
	 */
	public static String normalizeStartPhrase(String s) {
		s = s.toLowerCase();
		s = LuceneUtil.removeStopWords(s);
		// smarkoff: 11/2013: added colon to be removed since query including one
		// causes an exception from org.apache.lucene.queryParser.QueryParser
		// example that causes exception: title_start_phrase:creative:foo*
		s = StringUtil.removeAny(s, " \t\r\n-'()\\:");
		return s;
	}

	public void addBoolean(boolean b, String key, String displayName) {
		if (sb.length() > 0) sb.append(SEP);
		sb.append(key).append(':');
		sb.append(b);
		if(key.equalsIgnoreCase("createdPO"))
		{
			// do nothing
		}
		else
		{
			criteria.add(new SearchCriterion(displayName, String.valueOf(b)));
		}
	}

	public void addIntsOrBetween(Integer [] array, String key, String displayName) {
		if (ArrayUtils.isEmpty(array))  return;

		if (sb.length() > 0) sb.append(SEP);

		StringBuilder sbCriteria = new StringBuilder();

		sb.append("(");

		for (int i = 0; i < array.length; i++) {
			int value = array[i];
			if (i > 0) {
				sb.append(OR_SEP);
				sbCriteria.append(OR_SEP);
			}
			sb.append(key).append(':');
			sb.append(value);
			sbCriteria.append(value);
		}

		sb.append(")");
		criteria.add(new SearchCriterion(displayName, sbCriteria.toString()));
	}

	public void addInt(int i, String key, String displayName) {
		if (sb.length() > 0) sb.append(SEP);
		sb.append(key).append(':');
		sb.append(i);
		criteria.add(new SearchCriterion(displayName, String.valueOf(i)));
	}

	/**
	 * This is inclusive (square brackets instead of curly).
	 * The field must have been stored as a NumericField for a range query
	 * to work (I think).
	 */
	public void addIntRange(int from, int to, String key, String displayName) {
		if (sb.length() > 0) sb.append(SEP);
		sb.append(key).append(":[");
		sb.append(from).append(" TO ").append(to);
		sb.append(']');
		criteria.add(new SearchCriterion(displayName, "[" + from + " to " + to + "]"));
	}

	/**
	 * Same as addIntRange() except for taking long parameters.
	 */
	public void addLongRange(long from, long to, String key, String displayName) {
		if (sb.length() > 0) sb.append(SEP);
		sb.append(key).append(":[");
		sb.append(from).append(" TO ").append(to);
		sb.append(']');
		criteria.add(new SearchCriterion(displayName, "[" + from + " to " + to + "]"));
	}

	public String getQueryString() {
		return sb.toString();
	}
}
