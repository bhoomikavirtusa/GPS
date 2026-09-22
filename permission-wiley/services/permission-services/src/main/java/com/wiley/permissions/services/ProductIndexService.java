package com.wiley.permissions.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiBits;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.domain.persistence.permissions.Bundle;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.ComplianceStatus;
import com.wiley.permissions.domain.persistence.permissions.Medium;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.persistence.permissions.Relation;
import com.wiley.permissions.domain.persistence.permissions.RelationCode;
import com.wiley.permissions.domain.persistence.permissions.SubMedium;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.services.message.PEMessageService;
import com.wiley.sf.common.io.CSVTabParser;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.lucene.ExtendedQueryParser;
import com.wiley.sf.common.lucene.FieldInfo;
import com.wiley.sf.common.lucene.IndexInfo;
import com.wiley.sf.common.lucene.IndexReadManager;
import com.wiley.sf.common.lucene.IndexWriteManager;
import com.wiley.sf.common.lucene.IndexWriterConfigFactory;
import com.wiley.sf.common.lucene.LuceneUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;
import com.wiley.sf.common.text.TimeFormat;

/**
 * It is assumed that only one instance of this class will
 * be created (being a service class).
 *
 * @since  JDK 1.8, Lucene 8.11
 * @author smarkoff
 */
public class ProductIndexService extends BaseService implements IndexWriterConfigFactory
{
	private static final Log log = LogFactory.getLog(ProductIndexService.class);

	private static final String PERMISSIONS_HOME = "PERMISSIONS_HOME";
	private static final String INDEX_DIR = "productIndex";
		// constants for product search field names
	// this first group of fields we will have for all products (available on notification messages or calculated from)
	public static final String WID = "wid";
	public static final String WID_DISPLAY = "wid_display";
	public static final String CW_CODE = "cw_code";  // cw codes all digits so norm/display are same
	public static final String LAST_UPDATED_DATE = "last_updated_date";
	public static final String ISBN13 = "isbn13";
	public static final String ISBN13_DISPLAY = "isbn13_display";
	public static final String ISBN10 = "isbn10";
	public static final String ISBN10_DISPLAY = "isbn10_display";
	public static final String ISBN_LANGUAGE_CODE = "isbn_language_code";
	public static final String ISBN_LANGUAGE_CODE_DISPLAY = "isbn_language_code_display";
	public static final String PNUMBER = "pnumber";
	public static final String TITLE = "title";
	public static final String TITLE_START_PHRASE = "title_start_phrase";
	public static final String TITLE_DISPLAY = "title_display";
	public static final String TITLE_SORT = "title_sort";
	public static final String SHORT_AUTHOR_NAME = "short_author_name";
	public static final String AUTHOR_NAME = "author_name";
	public static final String EDITION_NUMBER = "edition_number";
	public static final String PREVIOUS_EDITION_WID = "previous_edition_wid";
	public static final String NEXT_EDITION_WID = "next_edition_wid";
	public static final String PUB_STATUS = "pub_status";
	public static final String PUB_STATUS_DISPLAY = "pub_status_display";
	public static final String BUSINESS_UNIT_CODE = "business_unit_code";  // bu codes are digits so norm/display are same
	public static final String BUSINESS_UNIT_NAME = "business_unit_name";
	public static final String DATA_SOURCE = "data_source";
	public static final String DATA_SOURCE_DISPLAY = "data_source_display";
	public static final String COPYRIGHT_YEAR = "copyright_year";
	public static final String MEDIUM_CODE = "medium_code";
	public static final String MEDIUM_CODE_DISPLAY = "medium_code_display";
	public static final String INDEX_DATE = "index_date";  // for debugging only
	public static final String INDEX_VERSION = "index_version";  // for debugging / fixing stuff
	public static final int CURRENT_INDEX_VERSION = 1;

	// these fields will only be present for Products that we have imported
	public static final String PRODUCT_LINE_CODE = "product_line_code";
	public static final String PRODUCT_LINE_CODE_DISPLAY = "product_line_code_display";
	public static final String EDITOR_CODE = "editor_code";
	public static final String EDITOR_CODE_DISPLAY = "editor_code_display";
	public static final String EDITOR_NAME_DISPLAY = "editor_name_display";
	public static final String PHOTO_EDITOR_LAST_NAME = "photo_editor_last_name";
	public static final String HAS_PHOTO_EDITOR_LAST_NAME = "has_photo_editor_last_name";
	public static final String PHOTO_EDITOR_LAST_NAME_DISPLAY = "photo_editor_last_name_display";
	public static final String PHOTO_EDITOR_FIRST_NAME_DISPLAY = "photo_editor_first_name_display";
	public static final String CON_RELEASE_DATE_DAY = "con_release_date_day";
	public static final String CON_RELEASE_DATE_MS = "con_release_date_ms";
	public static final String TRANSMITTAL_DATE_DAY = "transmittal_date_day";
	public static final String TRANSMITTAL_DATE_MS = "transmittal_date_ms";
	public static final long MS_TO_DAY_DIVIDE = 1000L * 60L * 60L * 24L;  // converts MS to day
	public static final String SUB_MEDIUM_CODE = "sub_medium_code";
	public static final String SUB_MEDIUM_CODE_DISPLAY = "sub_medium_code_display";
	public static final String PROCESS_CODE = "process_code";
	public static final String PROCESS_CODE_DISPLAY = "process_code_display";
	public static final String BUNDLE_CODE = "bundle_code";
	public static final String BUNDLE_CODE_DISPLAY = "bundle_code_display";
	public static final String DISCOUNT_GROUP_CODE = "discount_group_code";
	public static final String DISCOUNT_GROUP_CODE_DISPLAY = "discount_group_code_display";
	public static final String DISCOUNT_SUB_GROUP_CODE = "discount_sub_group_code";
	public static final String DISCOUNT_SUB_GROUP_CODE_DISPLAY = "discount_sub_group_code_display";
	public static final String HAS_RELATION_CW = "has_relation_cw";
	public static final String COMPONENT_FLAG = "component_flag";
	public static final String COMPONENT_FLAG_DISPLAY = "component_flag_display";
	// ---- computed fields (not part of PE data)
	public static final String CW_PRIMARY = "cw_primary";
	public static final String CW_ID = "cw_id";
	public static final String COMPLIANCE_STATUS = "compliance_status";
	public static final String LAST_WORKED_ON_DATE_MS = "last_worked_on_date_ms";
	public static final String AU_COUNT_NOT_CANCELED = "au_count_not_canceled";
	public static final String COVER_COUNT_NOT_CANCELED = "cover_count_not_canceled";
	public static final String STATUS_NOT_OK_COUNT = "status_not_ok_count";
	// (photo_illus_total_count is not a computed field but directly on Product)
	public static final String PHOTO_ILLUS_TOTAL_COUNT = "photo_illus_total_count";
	public static final String PRODUCT_PRIORITY = "product_priority";
	public static final String MS_TO_COMP_DISPLAY = "ms_to_comp_display";  // from cw_photo_estimate table
	public static final String FINAL_COST_DISPLAY = "final_cost_display";
	public static final String TOTAL_PHOTOS_DISPLAY = "total_photos_display";
	public static final String PHOTO_COVER_PICKUP_COUNT = "photo_cover_pickup_count";
	public static final String PHOTO_INTERNAL_FREE_COUNT = "photo_internal_free_count";
	public static final String PHOTO_INTERNAL_ROYALTY_FREE_COUNT = "photo_internal_royalty_free_count";


	private Directory indexDirectory;  // set by constructor


	private final IndexWriteManager writeManager;  // init in constructor
	private final IndexReadManager readManager;  // init in constructor

	private PEMessageService peMessageService;  // wired by Spring
	private ProductService productService;  // wired by Spring
	private ProductRepository productRepository; // wired by Spring
	private CommonWorkRepository cwRepository;  // wired by Spring


	public ProductIndexService() throws IOException {
		this(false);
	}

	/**
	 * For unit testing, useRAMDirectory should be true, otherwise false.
	 */
	public ProductIndexService(boolean useRAMDirectory) throws IOException {
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
		//	Path indexPath = FileSystems.getDefault().getPath("path", "productIndex"); //
			if (!indexDir.exists()) {
				boolean ok = indexDir.mkdir();
				if (!ok) {
					throw new IOException("Could not create index dir: " + indexDir.getAbsolutePath());
				}
			}

			indexDirectory = FSDirectory.open(indexDir.toPath());
			//indexDirectory = FSDirectory.open(indexDir.toPath());  // throws IOException

			// Do not create-if-missing here if a Lucene 4 index already exists —
			// ensureIndexReadableOrRecreate() handles both empty and too-old cases.
			if (!DirectoryReader.indexExists(indexDirectory)) {
				LuceneUtil.createIndexIfDoesNotExist(indexDirectory, newConfig());
			}
		}

		// delay creation of searcher in case the index does not exist yet

		writeManager = new IndexWriteManager(indexDirectory, this, 10);

		readManager = new IndexReadManager(indexDirectory);
	}

	/** Implements IndexWriterConfigFactory */
	@Override
	public IndexWriterConfig newConfig() {
		return new IndexWriterConfig(new StandardAnalyzer());
		//return new IndexWriterConfig(new StandardAnalyzer());
	}

	/**
	 * Lucene 8 cannot open Lucene 4.x on-disk segments. Wipe and create an empty
	 * Lucene 8 index so product index updates can proceed.
	 */
	private void ensureIndexReadableOrRecreate() throws IOException {
		if (LuceneUtil.canOpenIndex(indexDirectory)) {
			LuceneUtil.createIndexIfDoesNotExist(indexDirectory, newConfig());
			return;
		}
		log.warn("ensureIndexReadableOrRecreate(): Product index is Lucene-format-incompatible; recreating empty Lucene 8 index under "
				+ INDEX_DIR + ". Operators must rebuild (per CW or full).");
		readManager.reset();
		LuceneUtil.recreateEmptyIndex(indexDirectory, newConfig());
		readManager.reset();
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
	 * @param wid Should be non-blank
	 */
	public void deleteFromIndexByWID(String wid) {
		Term deleteTerm = new Term(WID, QueryBuilder.normalizeCode(wid));
		writeManager.updateDocument(null, deleteTerm);
	}

	/**
	 *
	 * @param wid  Must be non-blank
	 * @param cwCode  May be null/blank
	 * @param lastUpdatedDate  Should be non-null - warning logged if null
	 * @param isbn13  May be null/blank
	 * @param isbn10  May be null/blank
	 * @param pnumber May be null/blank
	 * @param title  Should be non-blank - warning logged if it is blank
	 * @param shortAuthorName May be null/blank
	 * @param authorNames  May be null/empty
	 * @param editionNumber May be null/blank
	 * @param previousEditionWID  May be null/blank
	 * @param nextEditionWID  May be null/blank
	 * @param publicationStatus  May be null/blank
	 * @param businessUnitCode  May be null/blank
	 * @param businessUnitName  May be null/blank
	 * @param dataSource  May be null/blank
	 * @param copyrightYear  May be null
	 * @param productLineCode  May be null/blank
	 * @param editorCode  May be null/blank
	 * @param editorName  May be null/blank
	 * @param photoEditorLastName  May be null/blank
	 * @param photoEditorFirstName  May be null/blank
	 * @param consolidatedReleaseDate  May be null
	 * @param transmittalDate  May be null
	 * @param mediumCode  May be null
	 * @param subMediumCode  May be null
	 * @param processCode  May be null
	 * @param bundleList  May be null or empty
	 * @param discountGroupCode  May be null
	 * @param discountSubGroupCode  May be null
	 * @param hasRelationCW  May be null
	 * @param componentFlag  May be null
	 * @param complianceStatus  May be null
	 * @param lastWorkedOnDate  May be null
	 * @param assetUseCountNotCanceled  May be null
	 * @param coverCountNotCanceled  May be null
	 * @param statusNotOkCount  May be null
	 * @param photoIllusTotalCount  May be null
	 * @param productPriority  May be null
	 * @param msToComp  May be null
	 * @param finalCost  May be null
	 * @param totalPhotos  May be null
	 * @param photoCoverPickupCount  May be null
	 * @param photoInternalFreeCount  May be null
	 * @param photoInternalRoyaltyFreeCount  May be null
	 */
	public void updateIndex(String wid, String cwCode, Boolean isCwPrimary, Integer cwId,
			Date lastUpdatedDate, String isbn13, String isbn10, String pnumber,
			String title, String shortAuthorName, List<String> authorNames, Integer editionNumber, String previousEditionWID, String nextEditionWID,
			String publicationStatus, String businessUnitCode, String businessUnitName, String dataSource,
			Integer copyrightYear, String productLineCode, String editorCode, String editorName, String photoEditorLastName, String photoEditorFirstName,
			Date consolidatedReleaseDate, Date transmittalDate, String mediumCode, String subMediumCode, String processCode, List<Bundle> bundleList,
			String discountGroupCode, String discountSubGroupCode, Boolean hasRelationCW, String componentFlag,
			ComplianceStatus complianceStatus, Date lastWorkedOnDate,
			Integer auCountNotCanceled, Integer coverCountNotCanceled, Integer statusNotOkCount, Integer photoIllusTotalCount,String productPriority,
			Double msToComp, Double finalCost, Integer totalPhotos,
			Integer photoCoverPickupCount, Integer photoInternalFreeCount, Integer photoInternalRoyaltyFreeCount)
	{
		try {
			ensureIndexReadableOrRecreate();
		} catch (IOException e) {
			throw new RuntimeException("Product index could not be opened or recreated for Lucene 8", e);
		}
		log.debug("updateIndex() called:\r\nwid = " + wid + ", cwCode = " + cwCode
				+ ", isCwPrimary = " + isCwPrimary
				+ ", cwId = " + cwId
				+ ", lastUpdatedDate = " + lastUpdatedDate
				+ ",\r\ndataSource = " + dataSource
				+ ", isbn13 = " + isbn13 + ", isbn10 = " + isbn10 + ", pnumber = " + pnumber
				+ ",\r\ntitle = " + title
				+ ",\r\nshortAuthorName = " + shortAuthorName
				+ ", authorNames = " + StringUtils.join(authorNames, ", ")
				+ ",\r\neditionNumber = " + editionNumber
				+ ", previousEditionWID = " + previousEditionWID
				+ ", nextEditionWID = " + nextEditionWID
				+ ", publicationStatus = " + publicationStatus
				+ ", businessUnitCode/Name = " + businessUnitCode + "/" + businessUnitName
				+ ",\r\ncopyrightYear = " + copyrightYear
				+ ", productLineCode = " + productLineCode
				+ ", editorCode = " + editorCode
				+ ", editorName = " + editorName
				+ ", photoEditorLastName = " + photoEditorLastName
				+ ", photoEditorFirstName = " + photoEditorFirstName
				+ ", consolidatedReleaseDate = " + consolidatedReleaseDate
				+ ", transmittalDate = " + transmittalDate
				+ ", mediumCode = " + mediumCode
				+ ", subMediumCode = " + subMediumCode
				+ ",\r\nprocessCode = " + processCode
				+ ", bundleList = (skip for now)"
				+ ", discountGroupCode = " + discountGroupCode
				+ ", discountSubGroupCode = " + discountSubGroupCode
				+ ", hasRelationCW" + hasRelationCW
				+ ", componentFlag" + componentFlag
				+ ", complianceStatus = " + complianceStatus
				+ ", lastWorkedOnDate = " + lastWorkedOnDate
				+ ",\r\nauCountNotCanceled = " + auCountNotCanceled
				+ ", coverCountNotCanceled = " + coverCountNotCanceled
				+ ", statusNotOkCount = " + statusNotOkCount
				+ ", photoIllusTotalCount = " + photoIllusTotalCount
				+ ", productPriority = " + productPriority
				+ ", msToComp = " + msToComp
				+ ",\r\nfinalCost = " + finalCost
				+ ", totalPhotos = " + totalPhotos
				+ ", photoCoverPickupCount = " + photoCoverPickupCount
				+ ", photoInternalFreeCount = " + photoInternalFreeCount
				+ ", photoInternalRoyaltyFreeCount = " + photoInternalRoyaltyFreeCount);

		ArgUtil.notBlank(wid, "wid");
		//ArgUtil.notBlank(title, "title");
		if (StringUtils.isBlank(title)) {
			log.warn("updateIndex(): title for wid [" + wid + "] is blank");
		}

		if (lastUpdatedDate == null) {
			log.warn("updateIndex(): lastUpdatedDate for wid [" + wid + "] is null");
		}

		Document doc = buildDocument(wid, cwCode, isCwPrimary, cwId, lastUpdatedDate, isbn13, isbn10, pnumber,
				title, shortAuthorName, authorNames, editionNumber, previousEditionWID, nextEditionWID,
				publicationStatus, businessUnitCode, businessUnitName, dataSource,
				copyrightYear, productLineCode, editorCode, editorName, photoEditorLastName, photoEditorFirstName,
				consolidatedReleaseDate, transmittalDate, mediumCode, subMediumCode, processCode, bundleList,
				discountGroupCode, discountSubGroupCode, hasRelationCW, componentFlag, complianceStatus, lastWorkedOnDate,
				auCountNotCanceled, coverCountNotCanceled, statusNotOkCount, photoIllusTotalCount, productPriority,
				msToComp, finalCost, totalPhotos, photoCoverPickupCount, photoInternalFreeCount, photoInternalRoyaltyFreeCount);
		Term deleteTerm = new Term(WID, QueryBuilder.normalizeCode(wid));
		writeManager.updateDocument(doc, deleteTerm);
	}

	/**
	 * Note calling the above version of this method is preferable if you already have all
	 * the data (since this method will hit the database for some fields).
	 *
	 * @param product  Must be non-null
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public void updateIndex(Product product) throws PersistenceException {
		ArgUtil.notNull(product, "product");
		PerfTimer timer = getMonitor().startTimer("ProductIndexService::updateIndex(Product)");

		String wid = product.getExternalId();
		String cwCode = product.getCommonWork().getCode();
		boolean isCwPrimary = product.isCwPrimary();
		Integer cwId = product.getCommonWork().getId();
		Date lastUpdatedDate = product.getLastUpdatedDate();
		String isbn13 = product.getIsbn13();
		String isbn10 = product.getIsbn10();
		String pnumber = product.getPnumber();
		String title = product.getTitle();
		String shortAuthorName = product.getShortAuthorName();

		// Don't use product.getAuthorsAsString() or User.getFullName() because
		// these give "Last, First" instead of "First Last".
		List<User> authors = product.getAuthors();
		List<String> authorNames = new ArrayList<String>(authors.size());
		for (User author : authors) {
			authorNames.add(author.getFirstName() + " " + author.getLastName());
		}

		Integer editionNumber = product.getEditionNumber();
		String previousEditionWID = product.getPreviousEditionWID();
		String nextEditionWID = product.getNextEditionWID();
		String publicationStatus = product.getPublicationStatus().getCode();
		String businessUnitCode = product.getBusinessUnit().getCode();
		String businessUnitName = product.getBusinessUnit().getName();
		String dataSource = product.getDataSource();
		Integer copyrightYear = product.getCopyrightYear();
		String productPriority = product.getProductPriority();
		String productLineCode = null;
		if (product.getProductLine() != null) productLineCode = product.getProductLine().getCode();
		String editorCode = product.getEditor();
		String editorName = null;
		if (StringUtils.isNotBlank(editorCode)) {
			editorName = productRepository.lookupEditorNameForCode(editorCode, dataSource);
		}
		User photoEditor = product.getPhotoEditor();
		String photoEditorLastName = photoEditor == null ? null : photoEditor.getLastName();
		String photoEditorFirstName = photoEditor == null ? null : photoEditor.getFirstName();
		Date consolidatedReleaseDate = product.getConsolidatedReleaseDate();
		Date transmittalDate = product.getTransmittalDate();
		Medium medium = product.getMedium();
		String mediumCode = (medium == null) ? null : medium.getCode();
		SubMedium subMedium = product.getSubMedium();
		String subMediumCode = (subMedium == null) ? null : subMedium.getCode();
		String processCode = product.getProcessCode();
		List<Bundle> bundleList = product.getBundles();
		String discountGroupCode = product.getDiscountGroupCode();
		String discountSubGroupCode = product.getDiscountSubGroupCode();
		boolean hasRelationCW = false;
		List<Relation> relations = product.getRelations();
		if (relations != null) {
			for (Relation r : relations) {
				if (RelationCode.CW.equals(r.getCode())) {
					hasRelationCW = true;
					break;
				}
			}
		}
		String componentFlag = product.getComponentFlag();

		CommonWork cw = product.getCommonWork();
		ComplianceStatus complianceStatus = cw.getComplianceStatus();
		Date lastWorkedOnDate = cw.getLastWorkedOnDate();
		int auCountNotCanceled = cw.getAUCountNotCanceled();
		int coverCountNotCanceled =  cw.getCoverCountNotCanceled();
		int statusNotOkCount = cw.getStatusNotOkCount();
		Integer photoIllusTotalCount = product.getPhotoIllusTotalCount();
		Double msToComp = cwRepository.getMSToComp(cw.getId());
		double finalCost = cwRepository.getFinalCost(cw.getId());
		int totalPhotos = cwRepository.getTotalPhotoCount(cw.getId());
		int photoCoverPickupCount = cwRepository.getPhotoCoverPickupAssetCount(cw.getId());
		int photoInternalFreeCount = cwRepository.getPhotoInternalFreeAssetCount(cw.getId());
		int photoInternalRoyaltyFreeCount = cwRepository.getPhotoInternalRoyaltyFreeAssetCount(cw.getId());

		updateIndex(wid, cwCode, isCwPrimary, cwId, lastUpdatedDate, isbn13, isbn10, pnumber, title,
				shortAuthorName, authorNames, editionNumber, previousEditionWID, nextEditionWID,
				publicationStatus, businessUnitCode, businessUnitName, dataSource,
				copyrightYear, productLineCode, editorCode, editorName, photoEditorLastName, photoEditorFirstName,
				consolidatedReleaseDate, transmittalDate, mediumCode, subMediumCode, processCode, bundleList,
				discountGroupCode, discountSubGroupCode, hasRelationCW, componentFlag, complianceStatus, lastWorkedOnDate,
				auCountNotCanceled, coverCountNotCanceled, statusNotOkCount, photoIllusTotalCount, productPriority,
				msToComp, finalCost, totalPhotos, photoCoverPickupCount, photoInternalFreeCount, photoInternalRoyaltyFreeCount);
		timer.stopTimer();
	}

	/**
	 * For every document in the index with an old INDEX_VERSION, updates the document
	 * by using the product data in the database (if the product is in our database)
	 * or else sending a product update request to PE (for products not in our database).
	 * @throws Exception
	 */
	public void updateIndexBasedOnVersion() throws Exception {
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		List<String []> list = searchByOldVersion();
		log.debug("updateIndexBasedOnVersion(): old version count = " + intFormat.format(list.size()));
		List<String[]> requestList = new ArrayList<String[]>();

		// save requests for the end because they take longer

		for (String [] array : list) {
			String wid = array[0];
			String dataSource = array[1];
			Product product = productRepository.loadByExternalIdForIndex(wid);  // throws Exception
			if (product == null) {
				log.debug("updateIndexBasedOnVersion(): wid not in db, will request: " + wid);
				requestList.add(new String [] { wid, dataSource });
			}
			else {
				log.debug("updateIndexBasedOnVersion(): updating for wid (in db): " + wid);
				updateIndex(product);  // throws PersistenceException
			}
		}

		List<Product> productList = new ArrayList<Product>();
		for (String [] request : requestList) {
			Product product = new Product();
			product.setExternalId(request[0]);
			product.setDataSource(request[1]);
			productList.add(product);
		}
		peMessageService.requestProductUpdate(productList, null);  // throws MessageException
	}

	private Document buildDocument(String wid, String cwCode, Boolean isCwPrimary, Integer cwId,
			Date lastUpdatedDate, String isbn13, String isbn10, String pnumber,
			String title, String shortAuthorName, List<String> authorNames, Integer editionNumber, String previousEditionWID, String nextEditionWID,
			String publicationStatus, String businessUnitCode, String businessUnitName, String dataSource,
			Integer copyrightYear, String productLineCode, String editorCode, String editorName,
			String photoEditorLastName, String photoEditorFirstName, Date consolidatedReleaseDate,
			Date transmittalDate, String mediumCode, String subMediumCode, String processCode, List<Bundle> bundleList,
			String discountGroupCode, String discountSubGroupCode, Boolean hasRelationCW, String componentFlag,
			ComplianceStatus complianceStatus, Date lastWorkedOnDate,
			Integer auCountNotCanceled, Integer coverCountNotCanceled, Integer statusNotOkCount, Integer photoIllusTotalCount, String productPriority,
			Double msToComp, Double finalCost, Integer totalPhotos,
			Integer photoCoverPickupCount, Integer photoInternalFreeCount, Integer photoInternalRoyaltyFreeCount)
	{
		Document doc = new Document();

		// store this because we use for delete term lookup in fix() / copyOrRenameField()
        doc.add(new StringField(WID, QueryBuilder.normalizeCode(wid), Field.Store.YES));

        doc.add(new StoredField(WID_DISPLAY, wid));

        if (StringUtils.isNotBlank(cwCode)) {
        	doc.add(new StringField(CW_CODE, QueryBuilder.normalizeCode(cwCode), Field.Store.YES));
        }

        // we don't know the value of isCwPrimary when we get a notification message or when
        // we update from the dump that Bruce gave me (in these cases isCwPrimary should be null)
        if (isCwPrimary != null) {
        	doc.add(new StringField(CW_PRIMARY, String.valueOf(isCwPrimary), Field.Store.YES));
        }

        // we don't know the value of cwId when we get a notification message or when
        // we update from the dump that Bruce gave me (in these cases cwId should be null)
        if (cwId != null) {
        	// Use StringField instead of IntField because we only might want to lookup by exact match
            // (we don't need a range query)
        	doc.add(new StringField(CW_ID, String.valueOf(cwId), Field.Store.YES));
        }

        if (lastUpdatedDate != null) {
        	doc.add(new StoredField(LAST_UPDATED_DATE, String.valueOf(lastUpdatedDate.getTime())));
        }

        if (StringUtils.isNotBlank(isbn13)) {
            doc.add(new StringField(ISBN13, QueryBuilder.normalizeCode(isbn13), Field.Store.NO));
            doc.add(new StoredField(ISBN13_DISPLAY, isbn13));
        }

        if (StringUtils.isNotBlank(isbn10)) {
            doc.add(new StringField(ISBN10, QueryBuilder.normalizeCode(isbn10), Field.Store.NO));
            doc.add(new StoredField(ISBN10_DISPLAY, isbn10));
        }

        char isbnLanguageCode = 'P';  // pnumber
        if (StringUtils.isNotBlank(isbn10)) {
        	isbnLanguageCode = isbn10.charAt(0);
        }
        doc.add(new StringField(ISBN_LANGUAGE_CODE, QueryBuilder.normalizeCode(String.valueOf(isbnLanguageCode)),
        		Field.Store.NO));
        doc.add(new StoredField(ISBN_LANGUAGE_CODE_DISPLAY, String.valueOf(isbnLanguageCode)));

        if (StringUtils.isNotBlank(pnumber)) {
            doc.add(new StringField(PNUMBER, QueryBuilder.normalizeCode(pnumber), Field.Store.YES));
        }

        if (StringUtils.isNotBlank(title)) {
        	doc.add(new TextField(TITLE, QueryBuilder.normalizeAnalyzed(title), Field.Store.NO));
        	doc.add(new StringField(TITLE_START_PHRASE, QueryBuilder.normalizeStartPhrase(title),
                Field.Store.NO));
            doc.add(new StoredField(TITLE_DISPLAY, title));
            doc.add(new StringField(TITLE_SORT, title.toLowerCase(), Field.Store.NO));
        }

        if (StringUtils.isNotBlank(shortAuthorName)) {
        	doc.add(new StringField(SHORT_AUTHOR_NAME, shortAuthorName, Field.Store.YES));
        }

        if (CollectionUtils.isNotEmpty(authorNames)) {
        	for (String name : authorNames) {
        		doc.add(new TextField(AUTHOR_NAME, QueryBuilder.normalizeAnalyzed(name), Field.Store.YES));
        	}
        }

        if (editionNumber != null) {
        	doc.add(new StoredField(EDITION_NUMBER, String.valueOf(editionNumber)));
        }

        if (StringUtils.isNotBlank(previousEditionWID)) {
        	doc.add(new StoredField(PREVIOUS_EDITION_WID, previousEditionWID));
        }

        if (StringUtils.isNotBlank(nextEditionWID)) {
        	doc.add(new StoredField(NEXT_EDITION_WID, nextEditionWID));
        }

        if (StringUtils.isNotBlank(productPriority)) {
        	doc.add(new StoredField(PRODUCT_PRIORITY, productPriority));
        }

        if (StringUtils.isNotBlank(publicationStatus)) {
        	doc.add(new StringField(PUB_STATUS, QueryBuilder.normalizeCode(publicationStatus),
                Field.Store.NO));
        	doc.add(new StoredField(PUB_STATUS_DISPLAY, publicationStatus));
        }

        if (StringUtils.isNotBlank(businessUnitCode)) {
        	doc.add(new StringField(BUSINESS_UNIT_CODE, QueryBuilder.normalizeCode(businessUnitCode),
                Field.Store.YES));
        }

        if (StringUtils.isNotBlank(businessUnitName)) {
        	doc.add(new StoredField(BUSINESS_UNIT_NAME, businessUnitName));
        }

        if (StringUtils.isNotBlank(dataSource)) {
        	doc.add(new StringField(DATA_SOURCE, QueryBuilder.normalizeCode(dataSource),
                    Field.Store.NO));
        	doc.add(new StoredField(DATA_SOURCE_DISPLAY, dataSource));
        }

        if (copyrightYear != null) {
        	 doc.add(new IntPoint(COPYRIGHT_YEAR, copyrightYear));
        doc.add(new StoredField(COPYRIGHT_YEAR, copyrightYear));
        }

        if (StringUtils.isNotBlank(productLineCode)) {
        	doc.add(new StringField(PRODUCT_LINE_CODE, QueryBuilder.normalizeCode(productLineCode),
        		Field.Store.NO));
        	doc.add(new StoredField(PRODUCT_LINE_CODE_DISPLAY, productLineCode));
        }

        if (StringUtils.isNotBlank(editorCode)) {
        	doc.add(new StringField(EDITOR_CODE, QueryBuilder.normalizeCode(editorCode), Field.Store.NO));
        	doc.add(new StoredField(EDITOR_CODE_DISPLAY, editorCode));
        }

        if (StringUtils.isNotBlank(editorName)) {
        	doc.add(new StoredField(EDITOR_NAME_DISPLAY, editorName));
        }

        if (StringUtils.isNotBlank(photoEditorLastName)) {
        	doc.add(new StringField(PHOTO_EDITOR_LAST_NAME, photoEditorLastName, Field.Store.NO));
        	doc.add(new StringField(HAS_PHOTO_EDITOR_LAST_NAME, "true", Field.Store.NO));
        	doc.add(new StoredField(PHOTO_EDITOR_LAST_NAME_DISPLAY, photoEditorLastName));
        }

        if (StringUtils.isNotBlank(photoEditorFirstName)) {
        	doc.add(new StoredField(PHOTO_EDITOR_FIRST_NAME_DISPLAY, photoEditorFirstName));
        }

        if (consolidatedReleaseDate != null) {
        	// Since we don't need millisecond precision for searching, using the day instead
        	// saves space in the index and is more efficient to search
        	final long day = consolidatedReleaseDate.getTime() / MS_TO_DAY_DIVIDE;
        	 doc.add(new LongPoint(CON_RELEASE_DATE_DAY, day));
        doc.add(new StoredField(CON_RELEASE_DATE_DAY, day));

        	// Store the millisecond precision in a separate field (which is not indexed)
        	doc.add(new StoredField(CON_RELEASE_DATE_MS, String.valueOf(consolidatedReleaseDate.getTime())));
        }

        if (transmittalDate != null) {
        	// Since we don't need millisecond precision for searching, using the day instead
        	// saves space in the index and is more efficient to search
        	final long day = transmittalDate.getTime() / MS_TO_DAY_DIVIDE;
        	doc.add(new LongPoint(TRANSMITTAL_DATE_DAY, day));
        doc.add(new StoredField(TRANSMITTAL_DATE_DAY, day));

        	// Store the millisecond precision in a separate field (which is not indexed)
        	doc.add(new StoredField(TRANSMITTAL_DATE_MS, String.valueOf(transmittalDate.getTime())));
        }

        if (StringUtils.isNotBlank(mediumCode)) {
        	doc.add(new StringField(MEDIUM_CODE, QueryBuilder.normalizeCode(mediumCode), Field.Store.NO));
        	doc.add(new StoredField(MEDIUM_CODE_DISPLAY, mediumCode));
        }

        if (StringUtils.isNotBlank(subMediumCode)) {
        	doc.add(new StringField(SUB_MEDIUM_CODE, QueryBuilder.normalizeCode(subMediumCode),
                    Field.Store.NO));
        	doc.add(new StoredField(SUB_MEDIUM_CODE_DISPLAY, subMediumCode));
        }

        if (StringUtils.isNotBlank(processCode)) {
        	doc.add(new StringField(PROCESS_CODE, QueryBuilder.normalizeCode(processCode), Field.Store.NO));
        	doc.add(new StoredField(PROCESS_CODE_DISPLAY, processCode));
        }

        if (CollectionUtils.isNotEmpty(bundleList)) {
        	for (Bundle b : bundleList) {
        		doc.add(new StringField(BUNDLE_CODE, QueryBuilder.normalizeCode(b.getCode()),
        			Field.Store.NO));
            	doc.add(new StoredField(BUNDLE_CODE_DISPLAY, b.getCode()));
        	}
        }

        if (StringUtils.isNotBlank(discountGroupCode)) {
        	doc.add(new StringField(DISCOUNT_GROUP_CODE, QueryBuilder.normalizeCode(discountGroupCode),
                Field.Store.NO));
        	doc.add(new StoredField(DISCOUNT_GROUP_CODE_DISPLAY, discountGroupCode));
        }

        if (StringUtils.isNotBlank(discountSubGroupCode)) {
        	doc.add(new StringField(DISCOUNT_SUB_GROUP_CODE, QueryBuilder.normalizeCode(discountSubGroupCode),
                Field.Store.NO));
        	doc.add(new StoredField(DISCOUNT_SUB_GROUP_CODE_DISPLAY, discountSubGroupCode));
        }

        // we don't know the value of hasRelationCW when we get a notification message or when
        // we update from the dump that Bruce gave me (in these cases should be null)
        if (hasRelationCW != null) {
        	doc.add(new StringField(HAS_RELATION_CW, String.valueOf(hasRelationCW), Field.Store.YES));
        }

        if (StringUtils.isNotBlank(componentFlag)) {
        	doc.add(new StringField(COMPONENT_FLAG, QueryBuilder.normalizeCode(componentFlag),
                    Field.Store.NO));
            doc.add(new StoredField(COMPONENT_FLAG_DISPLAY, componentFlag));
        }

        if (complianceStatus != null) {
        	doc.add(new StringField(COMPLIANCE_STATUS, QueryBuilder.normalizeCode(complianceStatus.getCode()),
            		Field.Store.YES));
        }

        if (lastWorkedOnDate != null) {
        	doc.add(new StoredField(LAST_WORKED_ON_DATE_MS, String.valueOf(lastWorkedOnDate.getTime())));
        }

        if (auCountNotCanceled != null) {
        	doc.add(new StoredField(AU_COUNT_NOT_CANCELED, String.valueOf(auCountNotCanceled)));
        }

        if (coverCountNotCanceled != null) {
        	doc.add(new StoredField(COVER_COUNT_NOT_CANCELED, String.valueOf(coverCountNotCanceled)));
        }

        if (statusNotOkCount != null) {
        	doc.add(new StoredField(STATUS_NOT_OK_COUNT, String.valueOf(statusNotOkCount)));
        }

        if (photoIllusTotalCount != null) {
        	doc.add(new StoredField(PHOTO_ILLUS_TOTAL_COUNT, String.valueOf(photoIllusTotalCount)));
        }

        if (msToComp != null) {
        	doc.add(new StoredField(MS_TO_COMP_DISPLAY, String.valueOf(msToComp)));
        }

        if (finalCost != null) {
        	doc.add(new StoredField(FINAL_COST_DISPLAY, String.valueOf(finalCost)));
        }

        if (totalPhotos != null) {
        	doc.add(new StoredField(TOTAL_PHOTOS_DISPLAY, String.valueOf(totalPhotos)));
        }

        if (photoCoverPickupCount != null) {
        	doc.add(new StoredField(PHOTO_COVER_PICKUP_COUNT, String.valueOf(photoCoverPickupCount)));
        }

        if (photoInternalFreeCount != null) {
        	doc.add(new StoredField(PHOTO_INTERNAL_FREE_COUNT, String.valueOf(photoInternalFreeCount)));
        }

        if (photoInternalRoyaltyFreeCount != null) {
        	doc.add(new StoredField(PHOTO_INTERNAL_ROYALTY_FREE_COUNT, String.valueOf(photoInternalRoyaltyFreeCount)));
        }

        // for debugging / fixing stuff

        doc.add(new StoredField(INDEX_DATE, String.valueOf(System.currentTimeMillis())));

		doc.add(new IntPoint(INDEX_VERSION, CURRENT_INDEX_VERSION));
		doc.add(new StoredField(INDEX_VERSION, CURRENT_INDEX_VERSION));

        return doc;
	}

	/**
	 * Important: Although this method returns List<ProductSearchResult>
	 * like ProductService.searchProducts() does, it does NOT behave the same
	 * - it searches on the start of the title ([title]*) instead of anywhere in the title (*[title]*).
	 *
	 * @param title  Must be non-null
	 * @param maxResults
	 */
	public List<ProductSearchResult> searchByTitleStart(String title, int maxResults) throws IOException, ParseException {
		QueryBuilder qb = new QueryBuilder();
		qb.addStartPhrase(title, TITLE_START_PHRASE, "Title");
		// including CW primary in the query does two things
		// 1) ensures that multiple results not given for when multiple products are in the same Common Work
		// 2) means that only products that are also in our database will be returned
		//    because the CW primary field is not set for other products
		//    - actually I'm not sure this side-effect is desirable in all cases - should review what methods
		//    call this one and if they desire to get products which are in the index but not in our db
		qb.addBoolean(true, CW_PRIMARY, "CW primary");
		String queryString = qb.getQueryString();
		log.debug("searchByTitleStart(): queryString = " + queryString);

        SortField [] sortFields = {
            new SortField(TITLE_SORT, SortField.Type.STRING),
        };

        return search(queryString, maxResults, sortFields);
	}

	public Set<String> getAllWIDs() throws IOException {
		ArrayList<WidAndIndexDate> list = new ArrayList<WidAndIndexDate>();
		return getAllWIDs(list);
	}

	/**
	 * Returns Set<String> containing WIDs and fills up an empty
	 * List<WidAndIndexDate> (which is the same WIDs plus indexDate for each).
	 */
	private Set<String> getAllWIDs(ArrayList<WidAndIndexDate> list) throws IOException {
		PerfTimer timer = getMonitor().startTimer("ProductIndexService::getAllWIDs");

		// using readManager.getIndexSearcher() means we don't have to worry about closing the searcher or reader
		IndexReader reader = readManager.getIndexSearcher().getIndexReader();  // throws IOException
		int numDocs = reader.numDocs();  // number of non-deleted documents
		// maxDoc - 1 is the number of documents including deleted ones
		int maxDoc = reader.maxDoc();

		Set<String> set = new HashSet<String>(numDocs);
		list.ensureCapacity(numDocs);

        // liveDocs contains doc indexes of not-deleted docs
        Bits liveDocs = MultiBits.getLiveDocs(reader);

		for (int i = 0; i < maxDoc; i++) {
			if (liveDocs != null && !liveDocs.get(i)) continue;

			Document doc = reader.document(i);  // throws IOException
			String wid = doc.get(WID_DISPLAY);
			long date = Long.parseLong(doc.get(INDEX_DATE));
			set.add(wid);
			list.add(new WidAndIndexDate(wid, date));
		}

		long time = timer.stopTimer();
		log.debug("getAllWIDs(): took " + time + " ms.");

		return set;
	}

	class WidAndIndexDate implements Comparable<WidAndIndexDate> {
		private final String wid;
		private final long date;

		public WidAndIndexDate(String wid, long date) {
			this.wid = wid;
			this.date = date;
		}

		public String getWid() { return wid; }
		public long getDate() { return date; }

		@Override
		public int compareTo(WidAndIndexDate o) {
			return o.getDate() == this.getDate() ? 0 : (this.getDate() > o.getDate() ? 1 : -1);
		}

		@Override
		public int hashCode() {
			return wid.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			WidAndIndexDate other = (WidAndIndexDate) obj;
			return wid.equals(other.wid);
		}
	}

	/**
	 * @param wid  Must be non-blank
	 * @return WID of the previous edition or null if either the product was not found or had no previous edition
	 */
	public String getPreviousEditionByWID(String wid) throws ParseException, IOException {
		ProductSearchResult result = searchByWID(wid);
		if (result == null) return null;
		else return result.getPreviousEditionWID();
	}

	public Integer getPreviousEditionByCwId(int cwId) throws ParseException, IOException {
		QueryBuilder qb = new QueryBuilder();
		qb.addInt(cwId, CW_ID, "Common Work id");
		qb.addBoolean(true, CW_PRIMARY, "CW primary");
		String queryString = qb.getQueryString();
		//log.debug("getPreviousEditionByCwId(): queryString = " + queryString);

		List<ProductSearchResult> list = search(queryString, 2, null);
		//log.debug("getPreviousEditionByCwId(): found = " + (list.size() > 0));

		String previousEditionWID = null;
		Integer previousEditionCWId = null;
		if (list.size() == 0)  return null;
		else {
			if (list.size() > 1) {
				log.error("getPreviousEditionByCwId(): more than one entry in index for cwId (and primary) [" + cwId + "]");
			}
			ProductSearchResult searchResult = list.get(0);
			previousEditionWID = searchResult.getPreviousEditionWID();
			previousEditionCWId = searchResult.getCwId();
			// normally should not happen but check anyway (did cause issue at one point)
			if (StringUtils.isBlank(previousEditionWID)) return null;
			if (cwId == previousEditionCWId) {
				// smarkoff: You'd think this would never happen but it does with WID core.001.prod.0000007289
				// (the previousEdition for this WID is not the same product but put in same common work - wrong)
				// and so unless return null here we'd go into an endless loop.
				log.warn("getPreviousEditionByCWId(): Previous Edition for cwId " + cwId + " is same CW! - will treat as if has no previousEdition");
				return null;
			}
		}

		ProductSearchResult result = searchByWID(previousEditionWID);
		if (result == null) return null;
		else return result.getCwId();
	}

	/**
	 * @param wid  Must be non-blank
	 * @return WID of the next edition or null if either the product was not found or had no next edition
	 */
	public String getNextEditionByWID(String wid) throws ParseException, IOException {
		ProductSearchResult result = searchByWID(wid);
		if (result == null) return null;
		else return result.getNextEditionWID();
	}

	/**
	 * smarkoff: I don't think this method is quite right (a null pointer check missing)
	 *  - but also not being used at the moment it seems.
	 * - Also should log when previousEditionWID not blank but that WID not found.
	 * Search the product index for all previous editions of a product given an external_id (WID).
	 */
	public List<ProductSearchResult> searchPreviousEditionsByWID(String wid) throws ParseException, IOException {
		ProductSearchResult result = searchByWID(wid);
		List<ProductSearchResult> editions = new ArrayList<ProductSearchResult>();
		if (result == null) return null;

		String prevEdition = result.getPreviousEditionWID();
		if (StringUtils.isBlank(prevEdition)) return null;

		while (prevEdition != null) {
			result = searchByWID(prevEdition);
			if (result != null) {
				editions.add(result);
				prevEdition = result.getPreviousEditionWID();
				if (prevEdition.length() < 1) {
					prevEdition = null;
				}
			}
		}

		if (editions.size() < 1) return null;
		return editions;
	}

	/**
	 * @param wid  Must be non-blank
	 * @return  The ProductSearchResult or null if not found
	 */
	public ProductSearchResult searchByWID(String wid) throws ParseException, IOException {
		ArgUtil.notBlank(wid, "wid");
		QueryBuilder qb = new QueryBuilder();
		qb.addCode(wid, WID, "Product WID");
		String queryString = qb.getQueryString();
		//log.debug("searchByWID(): queryString = " + queryString);

		List<ProductSearchResult> list = search(queryString, 2, null);
		//log.debug("searchByWID(): found = " + (list.size() > 0));

		if (list.size() == 0)  return null;
		else {
			if (list.size() > 1) {
				log.error("searchByWID(): more than one entry in index for wid [" + wid + "]");
			}
			return list.get(0);
		}
	}

	/**
	 * @param isbn  Must be non-blank
	 * @return  The ProductSearchResult or null if not found
	 */
	public ProductSearchResult searchByISBN(String isbn) throws ParseException, IOException {
		ArgUtil.notBlank(isbn, "isbn");
		QueryBuilder qb = new QueryBuilder();
		qb.switchToOr();
		qb.addCode(isbn, ISBN13, "ISBN 13");
		qb.addCode(isbn, ISBN10, "ISBN 10");
		String queryString = qb.getQueryString();
		log.debug("searchByISBN(): queryString = " + queryString);

		List<ProductSearchResult> list = search(queryString, 2, null);
		//log.debug("searchByISBN(): found = " + (list.size() > 0));

		if (list.size() == 0)  return null;
		else {
			if (list.size() > 1) {
				log.error("searchByISBN(): more than one entry in index for isbn [" + isbn + "]");
			}
			return list.get(0);
		}
	}

	/**
	 * @param widOrIsbn  Must be non-blank
	 * @return  The ProductSearchResult or null if not found
	 */
	public ProductSearchResult searchByWIDOrISBN(String widOrIsbn) throws ParseException, IOException {
		ArgUtil.notBlank(widOrIsbn, "widOrIsbn");
		QueryBuilder qb = new QueryBuilder();
		qb.switchToOr();
		qb.addCode(widOrIsbn, WID, "Product WID");
		qb.addCode(widOrIsbn, ISBN13, "ISBN 13");
		qb.addCode(widOrIsbn, ISBN10, "ISBN 10");
		String queryString = qb.getQueryString();
		log.debug("searchByWIDOrISBN(): queryString = " + queryString);

		List<ProductSearchResult> list = search(queryString, 2, null);
		//log.debug("searchByWIDOrISBN(): found = " + (list.size() > 0));

		if (list.size() == 0)  return null;
		else {
			if (list.size() > 1) {
				log.error("searchByWIDOrISBN(): more than one entry in index for widOrIsbn [" + widOrIsbn + "]");
			}
			return list.get(0);
		}
	}

	/**
	 * Returns all products in the index having a Publication Status of PRE_CONTRACT, EDITORIAL, IN_PRODUCTION,
	 * or (PUBLISHED and copyrightYear >= PUBLISHED_START_YEAR).
	 */
	public List<ProductSearchResult> searchByPrimaryAndPubStatusPEIN() throws ParseException, IOException {
		QueryBuilder qb = new QueryBuilder();
		qb.addBoolean(true, CW_PRIMARY, "CW primary");
		// ideally should wrap the following with () but think ok the way it is
		// -should be primary AND (big or statement)
		final String [] codes = {
				PublicationStatus.PRE_CONTRACT.getCode(), PublicationStatus.EDITORIAL.getCode(),
				PublicationStatus.IN_PRODUCTION.getCode()};
		qb.addCodesOrBetween(codes, PUB_STATUS, "Publication Status");
		qb.addCustom(" or (" + PUB_STATUS + ":" + PublicationStatus.PUBLISHED.getCode()
				+ " AND " + COPYRIGHT_YEAR + ":[" + PublicationStatus.PUBLISHED_START_YEAR + " TO 9999])");
		String queryString = qb.getQueryString();
		log.debug("searchByPubStatusPreAndInProduction(): queryString = " + queryString);
		List<ProductSearchResult> list = search(queryString, Integer.MAX_VALUE, null);  // throws ParseException, IOException
		return list;
	}

	public List<ProductSearchResult> photoCopyrightReport(int copyrightYear) throws ParseException, IOException {
		QueryBuilder qb = new QueryBuilder();

		qb.addBoolean(true, CW_PRIMARY, "CW primary");
		qb.addBoolean(true, HAS_PHOTO_EDITOR_LAST_NAME, "Has Photo Editor Last Name");
		// since copyright year indexed as numeric need to use range query (regular does not work)
		//qb.addInt(copyrightYear, COPYRIGHT_YEAR, "Copyright Year");
		qb.addIntRange(copyrightYear, copyrightYear, COPYRIGHT_YEAR, "Copyright Year");

		String queryString = qb.getQueryString();
		log.debug("photoCopyrightReport(): queryString = " + queryString);

        SortField [] sortFields = {
            new SortField(PHOTO_EDITOR_LAST_NAME, SortField.Type.STRING)
        };

		return search(queryString, Integer.MAX_VALUE, sortFields);
	}

	public ComplianceCount complianceCount(ComplianceForm form) throws ParseException, IOException {
		if (form.getDateCriterion().equals("copyrightYear")) {
			log.debug("complianceCount(): fromYear / toYear = " + form.getFromDateYear() + " / " + form.getToDateYear());
		}
		else {
			log.debug("complianceCount(): fromDate / toDate = " + form.getFromDate() + " / " + form.getToDate());
		}

		QueryBuilder qb = buildComplianceQuery(form);

		String queryString = qb.getQueryString();
		log.debug("complianceCount(): queryString = " + queryString);

		long startTime = System.currentTimeMillis();

		// customized search just for this method so we don't have to save all the results in memory
		// (we just want the counts)
        QueryParser qp = createQueryParser();
	    Query query = qp.parse(queryString);  // throws ParseException

	    // using readManager.getIndexSearcher() means we don't have to worry about closing the searcher
	    IndexSearcher searcher = readManager.getIndexSearcher();
	    TopDocs hits = searcher.search(query, Integer.MAX_VALUE);  // throws IOException

		int notStartedCount = 0;
		int inProcessCount = 0;
		int completeCount = 0;
		int problemCount = 0;
		int completeNo3rdPartyCount = 0;

	    for (int i = 0; i < hits.scoreDocs.length; i++) {
	       	ScoreDoc scoreDoc = hits.scoreDocs[i];
	       	Document doc = searcher.doc(scoreDoc.doc);

	       	String wid = doc.get(WID_DISPLAY);
	       	ComplianceStatus status = ComplianceStatus.normalizedCodeToValue(doc.get(COMPLIANCE_STATUS));
	    	if (status == null) {
				log.warn("chart(): complianceStatus null for wid " + wid);
			}
			else if (status.equals(ComplianceStatus.NOT_STARTED))  notStartedCount++;
			else if (status.equals(ComplianceStatus.IN_PROCESS))  inProcessCount++;
			else if (status.equals(ComplianceStatus.COMPLETE))  completeCount++;
			else if (status.equals(ComplianceStatus.COMPLETE_NO_3RD_PARTY))  completeNo3rdPartyCount++;
			else if (status.equals(ComplianceStatus.PROBLEM))  problemCount++;
			else {
				log.warn("complianceCount(): complianceStatus not expected: " + status);
			}
		}

		long time = System.currentTimeMillis() - startTime;
		log.debug("complianceCount(): index search time was " + time + " ms.");

		log.debug("complianceCount(): notStarted [" + notStartedCount + "] inProcess ["
				+ inProcessCount + "] complete [" + completeCount + "] problem [" + problemCount + "]");

		return new ComplianceCount(notStartedCount, inProcessCount, completeCount, completeNo3rdPartyCount, problemCount);
	}

	public List<ProductSearchResult> complianceList(ComplianceForm form, String complianceStatus) throws ParseException, IOException {
		QueryBuilder qb = buildComplianceQuery(form);
		qb.addCode(complianceStatus, COMPLIANCE_STATUS, "complianceStatus");
		String queryString = qb.getQueryString();
		log.debug("complianceList(): queryString = " + queryString);

		return search(queryString, Integer.MAX_VALUE, null);
	}

	private QueryBuilder buildComplianceQuery(ComplianceForm form) {
		QueryBuilder qb = new QueryBuilder();

		qb.addBoolean(true, CW_PRIMARY, "CW primary");

		if (form.isInProduction()) {
			//qb.addCode(PublicationStatus.IN_PRODUCTION.getCode(), PUB_STATUS, "PublicationStatus");
			qb.addCustom(" and (" + PUB_STATUS + ":" + PublicationStatus.IN_PRODUCTION.getCode() + " or (" + PUB_STATUS
				+ ":" + PublicationStatus.PUBLISHED.getCode()
				+ " AND " + COPYRIGHT_YEAR + ":[" + PublicationStatus.PUBLISHED_START_YEAR + " TO 9999]))");
		}
		else {
			final String [] codes = { PublicationStatus.PRE_CONTRACT.getCode(), PublicationStatus.EDITORIAL.getCode() };
			qb.addCodesOrBetween(codes, PUB_STATUS, "Publication Status");
		}

		// following year fields stores as NumericField so can use NumericRangeQuery
		if (form.getToDateYear() >= form.getFromDateYear() && form.getToDateYear() > 0) {
			if ("CRD".equals(form.getDateCriterion())) {
				long fromDateDay = form.getFromDateMS() / MS_TO_DAY_DIVIDE;
				long toDateDay = form.getToDateMS() / MS_TO_DAY_DIVIDE;
				qb.addLongRange(fromDateDay, toDateDay, CON_RELEASE_DATE_DAY, "CRD");
			}
			else if ("transmittalDate".equals(form.getDateCriterion())) {
				long fromDateDay = form.getFromDateMS() / MS_TO_DAY_DIVIDE;
				long toDateDay = form.getToDateMS() / MS_TO_DAY_DIVIDE;
				qb.addLongRange(fromDateDay, toDateDay, TRANSMITTAL_DATE_DAY, "Transmittal Date");
			}
			else if ("copyrightYear".equals(form.getDateCriterion())) {
				qb.addIntRange(form.getFromDateYear(), form.getToDateYear(), COPYRIGHT_YEAR, "Copyright Year");
			}
		}

		if (StringUtils.isNotBlank(form.getBusinessUnitCode())) {
			qb.addCode(form.getBusinessUnitCode(), BUSINESS_UNIT_CODE, "Business Unit Code");
		}

		if (StringUtils.isNotBlank(form.getProductLineCode())) {
			qb.addCode(form.getProductLineCode(), PRODUCT_LINE_CODE, "Product Line Code");
		}

		if (StringUtils.isNotBlank(form.getEditorCode())) {
			qb.addCode(form.getEditorCode(), EDITOR_CODE, "Editor Code");
		}

		if (form.isUseFilter()) {
			addComplianceFilter(qb);
		}

		return qb;
	}

	private void addComplianceFilter(QueryBuilder qb) {
		qb.addCustom("\r\n");  // for easier debugging

		qb.addNotCode("P", ISBN_LANGUAGE_CODE, "ISBN Language Code");
		qb.addNotCode("K", ISBN_LANGUAGE_CODE, "ISBN Language Code");
		qb.addNotCode("G", ISBN_LANGUAGE_CODE, "ISBN Language Code");
		qb.addNotCode("E", ISBN_LANGUAGE_CODE, "ISBN Language Code");

		qb.addCustom("\r\n");  // for easier debugging

		qb.addNotCode(Medium.DISPLAY.getCode(), MEDIUM_CODE, "Medium Code");
		qb.addNotCode(Medium.EBOOK.getCode(), MEDIUM_CODE, "Medium Code");
		qb.addNotCode(Medium.GAYLORD.getCode(), MEDIUM_CODE, "Medium Code");

		qb.addCustom("\r\n");  // for easier debugging

		final String NORM_PRE_CONTRACT =
			QueryBuilder.normalizeCode(PublicationStatus.PRE_CONTRACT.getCode());
		final String NORM_EDITORIAL =
			QueryBuilder.normalizeCode(PublicationStatus.EDITORIAL.getCode());

		String custom = " AND !((" + PROCESS_CODE + ":ca OR " + PROCESS_CODE
			+ ":d OR " + PROCESS_CODE + ":di) AND (" + PUB_STATUS + ":"
			+ NORM_PRE_CONTRACT + " OR " + PUB_STATUS + ":" + NORM_EDITORIAL
			+ "))";
		qb.addCustom(custom);

		qb.addCustom("\r\n");  // for easier debugging

		custom = " AND !(" + MEDIUM_CODE + ":o AND " + SUB_MEDIUM_CODE + ":as)"
			+ " AND !(" + MEDIUM_CODE + ":h AND " + SUB_MEDIUM_CODE + ":as)"
			+ " AND !(" + MEDIUM_CODE + ":h AND " + SUB_MEDIUM_CODE + ":ok)";
		qb.addCustom(custom);

		qb.addCustom("\r\n");  // for easier debugging

		qb.addNotCode("WIE", BUNDLE_CODE, "Bundle Code");
		qb.addNotCode("CA", DISCOUNT_SUB_GROUP_CODE, "Discount SubGroup Code");

		qb.addBoolean(false, HAS_RELATION_CW, "Has Relation CW");
		qb.addNotCode("S", COMPONENT_FLAG, "Component Flag");

		// could do a bit more filtering but this may be enough
	}

	/**
	 * Returns a list of {wid, dataSource} that represent all documents
	 * in the index where INDEX_VERSION is either missing or less than CURRENT_INDEX_VERSION.
	 */
	public List<String []> searchByOldVersion() throws ParseException, IOException {
		// using readManager.getIndexSearcher() means we don't have to worry about closing the searcher or reader
		IndexReader reader = readManager.getIndexSearcher().getIndexReader();  // throws IOException
		//int numDocs = reader.numDocs();  // number of non-deleted documents
		// maxDoc - 1 is the number of documents including deleted ones
		int maxDoc = reader.maxDoc();
		List<String []> list = new ArrayList<String []>();

        // liveDocs contains doc indexes of not-deleted docs
        Bits liveDocs = MultiBits.getLiveDocs(reader);

		for (int i = 0; i < maxDoc; i++) {
			if (liveDocs != null && !liveDocs.get(i)) continue;

			Document doc = reader.document(i);  // throws IOException
			String version = doc.get(INDEX_VERSION);
			String wid = doc.get(WID_DISPLAY);
			String dataSource = doc.get(DATA_SOURCE_DISPLAY);
			if (StringUtils.isBlank(version)) {
				list.add(new String [] { wid, dataSource });
			}
			else {
				int v = Integer.parseInt(version);
				if (v < CURRENT_INDEX_VERSION) list.add(new String [] { wid, dataSource });
			}
		}

		return list;
	}

	/**
	 * @param cwCode  Must be non-blank
	 * @return  The previousEditionCWCode or null if not found
	 */
	/*
	public String getPreviousEditionCWCodeForCWCode(String cwCode) throws ParseException, IOException {
		QueryBuilder qb = new QueryBuilder();
		qb.addCode(cwCode, CW_CODE, "CommonWork");
		String queryString = qb.getQueryString();
		log.debug("getPreviousEditionCWCodeForCWCode(): queryString = " + queryString);

		List<ProductSearchResult> list = search(queryString, 100, null);
		log.debug("getPreviousEditionCWCodeForCWCode(): result list size = " + list.size());

		// If there are multiple products for this common work then we will
		// probably (we should) get back different previousEditonWID's for
		// each one -- but those different previousEditionWID's should all
		// have the same commonWork.

		String previousCWCode = null;
		for (ProductSearchResult result : list) {
			// temp work around for error in data
			if (cwCode.equals("1000058051") && StringUtils.isBlank(result.getPreviousEditionWID())) {
				log.debug("getPreviousEditionCWCodeForCWCode(): data issue workaround");
				result.setPreviousEditionWID("CORE.001.PROD.0000117129");
			}
			if (StringUtils.isNotBlank(result.getPreviousEditionWID())) {
				log.debug("getPreviousEditionCWCodeForCWCode(): previousEditionID = " + result.getPreviousEditionWID());
				ProductSearchResult result2 = searchByProductWID(result.getPreviousEditionWID());

				if (result2 != null) {
					if (result2.getCommonWork() == null && result.getPreviousEditionWID().equals("CORE.001.PROD.0000117129")) {
						log.debug("getPreviousEditionCWCodeForCWCode(): data issue workaround #2");
						result2.setCommonWork("perm.cw.5450");
					}

					if (previousCWCode != null && !previousCWCode.equals(result2.getCommonWork())) {
						log.warn("getPreviousEditionCWCodeForCWCode(): There is more than one previous CW for CWCode = " + cwCode);
					}
					previousCWCode = result2.getCommonWork();
				}
			}
		}

		return previousCWCode;
	}
	*/

	/**
	 * (Not returning the CW titles because we don't have them in the index and the
	 * product title(s) could be different from the CW title.)
	 *
	 * @param cwCode  Must be non-blank
	 * @return  A List of cwCode where the immediate previous edition comes first.
	 * 		List will be zero-size if there is no previous edition.
	 */
	/*
	public List<String> getPreviousEditionCWListForCWCode(String cwCode) throws ParseException, IOException {
		List<String> list = new ArrayList<String>(0);

		String previousCode = getPreviousEditionCWCodeForCWCode(cwCode);
		while (previousCode != null) {
			list.add(previousCode);
			previousCode = getPreviousEditionCWCodeForCWCode(previousCode);
		}

		return list;
	}*/

	private QueryParser createQueryParser() {
		ExtendedQueryParser qp = new ExtendedQueryParser(WID, new StandardAnalyzer());
	    qp.setIntFieldNames(new String [] { COPYRIGHT_YEAR });
	    qp.setLongFieldNames(new String [] { CON_RELEASE_DATE_DAY, TRANSMITTAL_DATE_DAY });
	    return qp;
	}

	/**
	 *
	 * @param queryString  Must be non-blank
	 * @param maxResults  Should be 1+
	 * @param sortFields  May be null
	 */
	private List<ProductSearchResult> search(String queryString, int maxResults, SortField [] sortFields) throws ParseException, IOException {
		ArgUtil.notBlank(queryString, "queryString");
		PerfTimer timer = getMonitor().startTimer("ProductIndexService::search");

        QueryParser qp = createQueryParser();
        log.debug("search(): queryString: " + queryString);
        Query query = qp.parse(queryString);  // throws ParseException
        log.debug("search(): query.toString(): " + query.toString());

        // using readManager.getIndexSearcher() means we don't have to worry about closing the searcher
        IndexSearcher searcher = readManager.getIndexSearcher();

        Sort sort = null;
        if (sortFields != null) {
        	sort = new Sort(sortFields);
        }

        TopDocs hits;
        // Calling searcher.search() with null Sort causes NullPointerException
        if (sort == null) {
        	hits = searcher.search(query, maxResults);  // throws IOException
        }
        else {
        	hits = searcher.search(query, maxResults, sort);  // throws IOException
        }

        List<ProductSearchResult> results = null;
    	if (hits.scoreDocs.length == 0) {
    		results = new ArrayList<ProductSearchResult>(0);
    	}
    	else {
    		results = new ArrayList<ProductSearchResult>();

    		for (int i = 0; i < hits.scoreDocs.length; i++) {
    			ScoreDoc scoreDoc = hits.scoreDocs[i];
    			Document doc = searcher.doc(scoreDoc.doc);
    			ProductSearchResult result = new ProductSearchResult();
    			result.setWid(doc.get(WID_DISPLAY));
    			result.setCommonWorkCode(doc.get(CW_CODE));
    			result.setTitle(doc.get(TITLE_DISPLAY));
    			result.setIsbn13(doc.get(ISBN13_DISPLAY));
    			result.setIsbn10(doc.get(ISBN10_DISPLAY));
    			result.setPnumber(doc.get(PNUMBER));
    			result.setStatus(doc.get(PUB_STATUS_DISPLAY));
    			result.setBusinessUnitCode(doc.get(BUSINESS_UNIT_CODE));
    			result.setBusinessUnitName(doc.get(BUSINESS_UNIT_NAME));
    			result.setCopyrightYear(doc.get(COPYRIGHT_YEAR));
    			result.setEditionNumber(doc.get(EDITION_NUMBER));
    			result.setPreviousEditionWID(doc.get(PREVIOUS_EDITION_WID));
    			result.setNextEditionWID(doc.get(NEXT_EDITION_WID));
    			result.setProductPriority(doc.get(PRODUCT_PRIORITY));
    			result.setMediumCode(doc.get(MEDIUM_CODE_DISPLAY));
    			result.setLastUpdatedDate(getDate(doc, LAST_UPDATED_DATE));
    			//DataSource dataSource = DataSource.getDataSourceForCode(document.get(DATA_SOURCE_DISPLAY));
    			result.setDataSource(doc.get(DATA_SOURCE_DISPLAY));

    			result.setShortAuthorName(doc.get(SHORT_AUTHOR_NAME));

    			List<String> authorNameList = null;
    			String [] authorNameArray = doc.getValues(AUTHOR_NAME);
    			// isNotEmpty won't compile with clean build
    			if (!ArrayUtils.isEmpty(authorNameArray)) {
    				authorNameList = new ArrayList<String>(authorNameArray.length);
    				for (String s : authorNameArray) {
    					authorNameList.add(s);
    				}
    			}

    			result.setAuthors(authorNameList);
    			results.add(result);

    		    result.setConsolidatedReleaseDate(getDate(doc, CON_RELEASE_DATE_MS));
    		    result.setTransmittalDate(getDate(doc, TRANSMITTAL_DATE_MS));
    			result.setIndexDate(getDate(doc, INDEX_DATE));

    			String versionString = doc.get(INDEX_VERSION);
    			try {
    				result.setIndexVersion(Integer.parseInt(versionString));
    			}
    			catch (NumberFormatException ex) {
    				result.setIndexVersion(0);
    			}

    			String s = doc.get(COMPLIANCE_STATUS);
    			if (s != null) {
    				result.setComplianceStatus(ComplianceStatus.normalizedCodeToValue(s));
    			}

    			result.setLastWorkedOnDate(getDate(doc, LAST_WORKED_ON_DATE_MS));

    			// these fields will only be present in the index for products
    			// that we imported (because these fields are not in the notification message)
    			result.setProductLine(doc.get(PRODUCT_LINE_CODE_DISPLAY));
    			result.setEditor(doc.get(EDITOR_CODE_DISPLAY));
    			result.setEditorName(doc.get(EDITOR_NAME_DISPLAY));

    			result.setCwId(getInteger(doc, CW_ID));
    			result.setAuCountNotCanceled(getInteger(doc, AU_COUNT_NOT_CANCELED));
    			result.setCoverCountNotCanceled(getInteger(doc, COVER_COUNT_NOT_CANCELED));
    			result.setStatusNotOkCount(getInteger(doc, STATUS_NOT_OK_COUNT));
    			result.setPhotoIllusTotalCount(getInteger(doc, PHOTO_ILLUS_TOTAL_COUNT));

    			result.setPhotoEditorLastName(doc.get(PHOTO_EDITOR_LAST_NAME_DISPLAY));
    			result.setPhotoEditorFirstName(doc.get(PHOTO_EDITOR_FIRST_NAME_DISPLAY));
    			result.setMsToComp(doc.get(MS_TO_COMP_DISPLAY));
    			result.setFinalCost(getDouble(doc, FINAL_COST_DISPLAY));
    			result.setTotalPhotos(getInteger(doc, TOTAL_PHOTOS_DISPLAY));
    			result.setPhotoCoverPickupCount(getInteger(doc, PHOTO_COVER_PICKUP_COUNT));
    			result.setPhotoInternalFreeCount(getInteger(doc, PHOTO_INTERNAL_FREE_COUNT));
    			result.setPhotoInternalRoyalityFreeCount(getInteger(doc, PHOTO_INTERNAL_ROYALTY_FREE_COUNT));
    		}
    	}

        timer.stopTimer();

        return results;
	}

	/**
	 * Get a date stored in the index as milliseconds.
	 * Returns null if for some reason the date is not in the index or cannot be parsed as a Long integer.
	 * @param doc  Must be non-null
	 * @param key  Must be non-blank
	 */
	private Date getDate(Document doc, String key) {
		try {
			String s = doc.get(key);
			if (s == null)  return null;
			return new Date(Long.parseLong(s));
		}
		catch (NumberFormatException ex1) {
			log.warn("getDate(): unparsable " + key + ": " + doc.get(key));
			return null;
		}
	}

	private Integer getInteger(Document doc, String key) {
		String s = doc.get(key);
		if (s == null)  return null;
		return Integer.parseInt(s);
	}

	private Double getDouble(Document doc, String key) {
		String s = doc.get(key);
		if (s == null)  return null;
		return Double.parseDouble(s);
	}

	public void fix() throws Exception {
		long startTime = System.currentTimeMillis();
		TimeFormat timeFormat = new TimeFormat();

		checkMissingFromIndex();
		updateIndexBasedOnVersion();  // throws Exception

		long time = System.currentTimeMillis() - startTime;
		log.debug("fix(): took " + timeFormat.formatMS(time));
	}

	/**
	 * Updates index for all the products in the common work
	 * @param cwId
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public void updateProductsIndexForCWID(int cwId) throws Exception {
		log.debug("Enter: updateProductsIndexForCWID() - with cwId: " + cwId);
		ensureIndexReadableOrRecreate();
		CommonWork cw = cwRepository.loadByIdForProductIndex(cwId);
		if(null != cw) {
			for (Product p : cw.getProducts()) {
				updateIndex(p);
			}
			log.debug("Exit: updateProductsIndexForCWID() - update products index completed for cwId: " + cwId);
		}
		else {
			log.debug("Common Work Id " + cwId + " not found!");
		}
	}

	/**
	 * Makes sure that all products in our database are also in the index
	 * (adds to index if missing).
	 * @throws Exception
	 */
	public void checkMissingFromIndex() throws Exception {
		Set<String> dbSet = productRepository.loadExternalIds(null);
		Set<String> indexSet = getAllWIDs();
		dbSet.removeAll(indexSet);
		log.debug("checkMissingFromIndex(): # products in db that are missing ");

		for (String externalId : dbSet) {
			log.debug("checkMissingFromIndex(): wid is missing (will fix): " + externalId);
			Product product = productRepository.loadByExternalIdForIndex(externalId);  // throws Exception
			updateIndex(product);
		}
	}

	/**
	 * This no-argument version of the method only exists for the cron job to call
	 * since I'm not sure how to specify an argument in cron-mule-config.xml.
	 */
	public void updateComputedFields() throws Exception {
		updateComputedFields(null);
	}

	/**
	 * @param widInput  May be null/blank
	 * @throws Exception
	 */
	public void updateComputedFields(String widInput) throws Exception {
		log.debug("updateComputedFields(): entered...widInput [" + widInput + "]");
		if (StringUtils.isNotBlank(widInput)) {
			updateComputedFieldsSingleWid(widInput); // throws Exception
			return;
		}
		PerfTimer timer = getMonitor().startTimer("ProductIndexService::updateComputedFields");

		// we want to update wids that are in db but not in index first,
		// then update wids that are in both -- ordered by previous indexDate ascending

		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

		Set<String> dbSet = productRepository.loadExternalIdsWherePrimaryAndPubStatusPEIN();
		log.debug("updateComputedFields(): dbSet.size() = " + intFormat.format(dbSet.size()));
		ArrayList<WidAndIndexDate> indexList = new ArrayList<WidAndIndexDate>();
		Set<String> indexSet = getAllWIDs(indexList);
		// remove all that are not also in db
		indexSet.retainAll(dbSet);
		for (int i = 0; i < indexList.size(); i++) {
			WidAndIndexDate w = indexList.get(0);
			if (!dbSet.contains(w.getWid())) {
				indexList.remove(i);
				i--;
			}
		}
		// after this dbSet will contain wids ONLY in db but missing from index
		dbSet.removeAll(indexSet);
		log.debug("updateComputedFields(): wids only in db: " + intFormat.format(dbSet.size()));
		// add DB only wids into list with date 1970 (0)
		for (String wid : dbSet) {
			indexList.add(new WidAndIndexDate(wid, 0));
		}
		Collections.sort(indexList);
		int successCount = 0;
		int errorCount = 0;

		for (WidAndIndexDate w : indexList) {
			Date indexDate = new Date(w.getDate());
			log.debug("updateComputedFields(): wid = " + w.getWid() + ", indexDate = " + dateFormat.format(indexDate));
			// normally we don't expect any exceptions
			try {
				Product product = productRepository.loadByExternalIdForIndex(w.getWid());  // throws PersistenceException
				// normally product shouldn't be null but sometimes is (means we have product in index that is not in db)
				// - for the purpose of this method we will just ignore null products
				if (product != null) {
					updateIndex(product);
					successCount++;
				}
				else {
					log.warn("updateComputedFields(): product [" + w.getWid() + "] in index but not db");
					errorCount++;
				}
			}
			catch (Exception ex) {
				log.error("updateComputedFields(): caught exception in updateComputedFields(): ", ex);
				errorCount++;
			}
		}

		long time = timer.stopTimer();
		TimeFormat timeFormat = new TimeFormat();
		log.debug("updateComputedFields(): took " + timeFormat.formatMS(time));
		log.debug("updateComputedFields(): successCount = " + intFormat.format(successCount)
			+ ", errorCount = " + intFormat.format(errorCount));
	}

	private void updateComputedFieldsSingleWid(String wid) throws Exception {
		Product product = productRepository.loadByExternalIdForIndex(wid);  // throws Exception
		if (product == null) {
			log.warn("updateComputedFieldsSingleWid(): product [" + wid + "] is not in db");
		}
		else {
			updateIndex(product);
			log.info("updateComputedFieldsSingleWid(): product [" + wid + "] updated successfully");
		}
	}

	// Need to ask for transaction in this method otherwise transaction ends after calling
	// loadAll for Products and lazy load for children of Product fails.
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public void compareDBToIndex() throws PersistenceException, ParseException, IOException {
		// this is kind of a memory hog - may be better to load products one at a time instead of all at once
		// - first load list of all wids, then load each product by wid

		List<Product> list = productRepository.loadAll(Product.class);  // throws PersistenceException

		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		log.info("compareDBToIndex(): " + intFormat.format(list.size()) + " products in DB");
		int notFoundCount = 0;
		int mismatchCount = 0;

		for (Product product : list) {
			String wid = product.getExternalId();
			ProductSearchResult result = searchByWID(wid);  // throws IOException
			boolean mismatch = false;
			if (result == null) {
				log.info("compareDBToIndex(): product in db not found in index, wid = " + wid);
				notFoundCount++;
			}
			else {
				if (!product.getCommonWork().getCode().equals(result.getCommonWorkCode())) {
					mismatch = true;
					log.info("compareDBToIndex(): CW.code does not match for wid ["
						+ wid + "] - DB [" + product.getCommonWork().getCode() + "] index [" + result.getCommonWorkCode() + "]");
				}
				if (!product.getDataSource().equals(result.getDataSource())) {
					mismatch = true;
					log.info("compareDBToIndex(): dataSource does not match for wid ["
						+ wid + "] - DB [" + product.getDataSource() + "] index [" + result.getDataSource() + "]");
				}
				if (!StringUtils.equalsIgnoreCase(product.getPnumber(), result.getPnumber())) {
					mismatch = true;
					log.info("compareDBToIndex(): pnumber does not match for wid ["
							+ wid + "] - DB [" + product.getPnumber() + "] index [" + result.getPnumber() + "]");
				}
				if (!StringUtils.equalsIgnoreCase(product.getIsbn13(), result.getIsbn13())) {
					mismatch = true;
					log.info("compareDBToIndex(): isbn13 does not match for wid ["
							+ wid + "] - DB [" + product.getIsbn13() + "] index [" + result.getIsbn13() + "]");
				}
				if (!product.getTitle().equals(result.getTitle())) {
					mismatch = true;
					log.info("compareDBToIndex(): title does not match for wid ["
							+ wid + "] - DB [" + product.getTitle() + "] index [" + result.getTitle() + "]");
				}
				if (!product.getPublicationStatus().getCode().equals(result.getStatus())) {
					mismatch = true;
					log.info("compareDBToIndex(): pubStatus does not match for wid ["
							+ wid + "] - DB [" + product.getPublicationStatus().getCode() + "] index [" + result.getStatus() + "]");
				}
				if (!product.getBusinessUnit().getCode().equals(result.getBusinessUnitCode())) {
					mismatch = true;
					log.info("compareDBToIndex(): businessUnit does not match for wid ["
							+ wid + "] - DB [" + product.getBusinessUnit().getCode() + "] index [" + result.getBusinessUnitCode() + "]");
				}
				if (!StringUtils.equalsIgnoreCase(product.getShortAuthorName(), result.getShortAuthorName())) {
					mismatch = true;
					log.info("compareDBToIndex(): shortAuthorName does not match for wid ["
							+ wid + "] - DB [" + product.getShortAuthorName() + "] index [" + result.getShortAuthorName() + "]");
				}
				if (!StringUtils.equalsIgnoreCase(product.getMedium().getCode(), result.getMediumCode())) {
					mismatch = true;
					log.info("compareDBToIndex(): mediumCode does not match for wid ["
							+ wid + "] - DB [" + product.getMedium().getCode() + "] index [" + result.getMediumCode() + "]");
				}
				// When fully on Java 7 use !Objects.equals(...) from JDK instead of ObjectUtils from Apache Commons
				// - will fix these deprecation warning
				if (!ObjectUtils.equals(product.getEditionNumber(), result.getEditionNumber())) {
					mismatch = true;
					log.info("compareDBToIndex(): editionNumber does not match for wid ["
							+ wid + "] - DB [" + product.getEditionNumber() + "] index [" + result.getEditionNumber() + "]");
				}

				if (!StringUtils.equalsIgnoreCase(product.getPreviousEditionWID(), result.getPreviousEditionWID())) {
					mismatch = true;
					log.info("compareDBToIndex(): previousEditionWID does not match for wid ["
							+ wid + "] - DB [" + product.getPreviousEditionWID() + "] index [" + result.getPreviousEditionWID() + "]");
				}

				if (!StringUtils.equalsIgnoreCase(product.getNextEditionWID(), result.getNextEditionWID())) {
					mismatch = true;
					log.info("compareDBToIndex(): nextEditionWID does not match for wid ["
							+ wid + "] - DB [" + product.getNextEditionWID() + "] index [" + result.getNextEditionWID() + "]");
				}

				// not comparing (full) authors for now

				// other things to add once added to notification messages and index:
				// (actually Bruce is not willing to send this info in the notification message)
				// productLine
				// editor code(s)
			}

			if (mismatch) {
				mismatchCount++;

				// TEMPORARY CODE
				try {
					// Following products give an exception when trying to add from requesting and getting
					// product update message.
					// CORE.001.PROD.0000121228
					// CORE.001.PROD.0000102510
					if (!wid.equals("CORE.001.PROD.0000007605")) {
						//Thread.sleep(2000);
						//productService.refreshProduct(wid, product.getDataSource(), true);
					}
				}
				catch (Exception ex) {
					log.warn("compareDBToIndex(): caught exception calling refreshProduct(): ", ex);
				}
			}
		}

		log.info("compareDBToIndex(): # products in db not found in index: " + intFormat.format(notFoundCount));
		log.info("compareDBToIndex(): # products in db not matching index: " + intFormat.format(mismatchCount));
	}

	// we don't want one big transaction but this method will call
	// others that are REQUIRED so sub-methods will have transaction
	@Transactional(propagation = Propagation.NEVER)
	public void indexProductData(InputStream is) throws IOException, PersistenceException {
		PerfTimer timer = getMonitor().startTimer("ProductIndexService::indexProductData");
		final NumberFormat intFormat = NumberFormat.getIntegerInstance();
		Set<String> widsInIndex = getAllWIDs();
		Set<String> widsInDB =  productRepository.loadExternalIds(null);  // throws PersistenceException
		log.debug("indexProductData(): " + widsInIndex.size() + " wids currently in index");
		int skipCount = 0;
		int totalCount = 0;
		int updateCount = 0;
		int errorCount = 0;
		int lastPosition = productRepository.readProductIndexFilePosition();
		CSVTabParser parser = new CSVTabParser('\t', is, true);
		List<String> list = parser.parseNextLine();

		while (list != null) {
			if (totalCount < lastPosition) {
				skipCount++;
				totalCount++;
				log.debug("indexProductData(): skipping row " + totalCount);
				continue;
			}
			// only write to db every 1000 records to save time (maybe doesn't save much)
			else if (totalCount > 0 && totalCount % 1000 == 0) {
				productRepository.writeProductIndexFilePosition(totalCount);
			}
			totalCount++;

			PerfTimer loopTimer = getMonitor().startTimer("ProductIndexService::indexProductData::loop");
			try {
				String dataSource = list.get(0);
				String wid = list.get(1);
				String isbn10 = list.get(2);
				String isbn13 = list.get(3);
				String pnumber = list.get(4);
				String title = list.get(5);
				//String shortTitle = list.get(6);
				String shortAuthorName = list.size() > 7 ? list.get(7) : null;
				String businessUnitCode = list.size() > 8 ? list.get(8) : null;
				String cwCode = list.size() > 9 ? list.get(9) : null;
				String publicationStatus = list.size() > 10 ? list.get(10) : null;
				String previousEditionWID = list.size() > 11 ? list.get(11) : null;
				String nextEditionWID = list.size() > 12 ? list.get(12) : null;
				String authorCodes = list.size() > 13 ? list.get(13) : null;
				String updateTime = list.size() > 14 ? list.get(14) : null;
				String productLineCode = list.size() > 15 ? list.get(15) : null;
				String editorCode = list.size() > 16 ? list.get(16) : null;
				String copyrightYearString = list.size() > 17 ? list.get(17) : null;
				String crdString = list.size() > 18 ? list.get(18) : null;
				String mediumCode = list.size() > 19 ? list.get(19) : null;
				String subMediumCode = list.size() > 20 ? list.get(20) : null;
				String processCode = list.size() > 21 ? list.get(21) : null;
				String bundleCodes = list.size() > 22 ? list.get(22) : null;
				String discountGroupCode = list.size() > 23 ? list.get(23) : null;
				String discountSubGroupCode = list.size() > 24 ? list.get(24) : null;
				String transmittalDateString = list.size() > 25 ? list.get(25) : null;  // Bruce calls production end date
				String photoIllusTotalCountString = list.size() > 26 ? list.get(26) : null;
				String editionNumberString = list.size() > 27 ? list.get(27) : null;
				String componentFlag = list.size() > 28 ? list.get(28) : null;
				String productPriority = list.size() > 29 ? list.get(29) : null;

				Date lastUpdatedDate = null;
				if (StringUtils.isNotBlank(updateTime)) {
					try {
						long ms = Long.parseLong(updateTime);
						lastUpdatedDate = new Date(ms);
					}
					catch (NumberFormatException ex) {
						log.warn("unable to parse updateTime [" + updateTime + "]");
					}
				}

				Integer copyrightYear = parseInteger(copyrightYearString, "copyrightYear", wid);
				Integer editionNumber = parseInteger(editionNumberString, "editionNumber", wid);

				Date consolidatedReleaseDate = parseDate(crdString, "CRD", wid);
				Date transmittalDate = parseDate(transmittalDateString, "transmittalDate", wid);

				List<String> authorCodeList = StringUtil.stringToArrayList(authorCodes, ",");
				List<String> authorNames = peMessageService.authorCodesToNames(authorCodeList);

				String businessUnitName = null;
				if (businessUnitCode != null) {
					businessUnitName = BusinessUnit.forCode(businessUnitCode).getName();
				}

				List<String> bundleCodeList = StringUtil.stringToArrayList(bundleCodes, ",");
				if (bundleCodeList == null)  bundleCodeList = new ArrayList<String>(0);
				List<Bundle> bundleList = new ArrayList<Bundle>(bundleCodeList.size());
				for (String bc : bundleCodeList) {
					bundleList.add(new Bundle(bc, null));
				}

				Integer photoIllusTotalCount = parseInteger(photoIllusTotalCountString, "photoIllusTotalCount", wid);

				// for now don't skip any product
				if (widsInIndex.contains(wid) && System.currentTimeMillis() < 0) {
					log.debug("indexProductData(): skipping wid [" + wid + "] since already in index");
					skipCount++;
				}
				else {
					String editorName = null;
					// - I will skip the editorName lookup because I also don't lookup the last 5 fields (pass null below)
					// and I think my idea was always to run updateComputedFields() after running this.
					//if (StringUtils.isNotBlank(editorCode)) {
					//	editorName = productRepository.lookupEditorNameForCode(editorCode, dataSource);
					//}

					updateIndex(wid, cwCode, null, null, lastUpdatedDate, isbn13, isbn10, pnumber, title, shortAuthorName, authorNames,
						editionNumber, previousEditionWID, nextEditionWID, publicationStatus,
						businessUnitCode, businessUnitName, dataSource,
						copyrightYear, productLineCode, editorCode, editorName, null, null,  // null, null is photoEditor First/Last Name
						consolidatedReleaseDate, transmittalDate, mediumCode, subMediumCode, processCode, bundleList,
						discountGroupCode, discountSubGroupCode, null, componentFlag, null, null, null, null, null, photoIllusTotalCount,productPriority,
						null, null, null, null, null, null);
					updateCount++;

					// temporary
					if (widsInDB.contains(wid)) {
						//if (StringUtils.isNotBlank(previousEditionWID) || StringUtils.isNotBlank(nextEditionWID)) {
						//	productRepository.updatePreviousNextEditionWIDs(wid, previousEditionWID, nextEditionWID);
						//}

						//if (StringUtils.isNotBlank(componentFlag)) {
						//	productRepository.updateComponentFlag(wid, componentFlag);
						//}
					}
				}
			}
			catch (Exception ex) {
				errorCount++;
				log.error("Caught exception processing row: " + StringUtils.join(list, "\t"), ex);
			}
			finally {
				loopTimer.stopTimer();
			}
			list = parser.parseNextLine();
		} // end while

		productRepository.writeProductIndexFilePosition(0);

		long time = timer.stopTimer();
		TimeFormat timeFormat = new TimeFormat();
		log.debug("indexProductData(): took " + timeFormat.formatMS(time));
		log.debug("indexProductData(): totalCount = " + intFormat.format(totalCount)
			+ ", updateCount = " + intFormat.format(updateCount)
			+ ", errorCount = " + intFormat.format(errorCount)
			+ ", skipCount = " + intFormat.format(skipCount));
	}

	/*
	 *
	 * @param dateString  If null or blank then null is returned
	 */
	private Date parseDate(String dateString, String fieldName, String wid) {
		if (StringUtils.isBlank(dateString))  return null;
		Date date = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		try {
			date = dateFormat.parse(dateString);
		}
		catch (java.text.ParseException ex) {
			log.error("indexProductData(): caught ParseException trying to read " + fieldName + "[" + dateString + "] where wid = " + wid);
		}
		return date;
	}

	private Integer parseInteger(String value, String fieldName, String wid) {
		if (StringUtils.isBlank(value)) return null;
		Integer result = null;
		try {
			result = Integer.parseInt(value);
		}
		catch (NumberFormatException ex) {
			log.warn("unable to parse " + fieldName + " [" + value + "] for wid = " + wid);
		}
		return result;
	}

	/**
	 * Check that all products with pub status in (P, E, I, N) in index are also in the database.
	 * Request missing products from PE.
	 */
	public void checkPreAndInProduction() throws ParseException, IOException, MessageException, PersistenceException {
		List<ProductSearchResult> list = searchByPrimaryAndPubStatusPEIN();  // throws ParseException, IOException
		final NumberFormat intFormat = NumberFormat.getIntegerInstance();
		log.debug("checkPreAndInProduction(): # primary products in index in (P, E, I, N): " + intFormat.format(list.size()));

		Set<String> dbSet = productRepository.loadExternalIdsWherePrimaryAndPubStatusPEIN();
		log.debug("checkPreAndInProduction(): # primary products in DB in (P, E, I, N): " + intFormat.format(dbSet.size()));

		int difference = list.size() - dbSet.size();
		log.debug("estimate for # products we need to import (difference): " + intFormat.format(difference));

		List<Product> productList = new ArrayList<Product>();
		for (ProductSearchResult result : list) {
			if (!dbSet.contains(result.getWid())) {
				Product product = new Product();
				product.setExternalId(result.getWid());
				product.setDataSource(result.getDataSource());
				log.debug("checkPreAndInProduction(): requesting (delayed) " + result.getWid());
				productList.add(product);
			}
		}
		int bunchLimit = peMessageService.getDefaultBunchLimit();
		if (productList.size() < 5000) bunchLimit = 1;
		peMessageService.requestProductUpdate(productList, null, bunchLimit);  // throws MessageException
		log.debug("checkPreAndInProduction(): # products in index in (P, E, I, N) that were not in db: "
			+ intFormat.format(productList.size()));
	}


	public void setPeMessageService(PEMessageService peMessageService) {
		this.peMessageService = peMessageService;
	}

	public PEMessageService getPeMessageService() {
		return peMessageService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	public ProductService getProductService() {
		return productService;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public class ComplianceCount {
		final private int notStartedCount;
		final private int inProcessCount;
		final private int completeCount;
		final private int completeNo3rdPartyCount;
		final private int problemCount;

		public ComplianceCount(int notStartedCount, int inProcessCount, int completeCount, int completeNo3rdPartyCount, int problemCount) {
			this.notStartedCount = notStartedCount;
			this.inProcessCount = inProcessCount;
			this.completeCount = completeCount;
			this.completeNo3rdPartyCount = completeNo3rdPartyCount;
			this.problemCount = problemCount;
		}

		public int getNotStartedCount() { return notStartedCount; }
		public int getInProcessCount() { return inProcessCount; }
		public int getCompleteCount() { return completeCount; }
		public int getCompleteNo3rdPartyCount() { return completeNo3rdPartyCount; }
		public int getProblemCount() { return problemCount; }
	}
}