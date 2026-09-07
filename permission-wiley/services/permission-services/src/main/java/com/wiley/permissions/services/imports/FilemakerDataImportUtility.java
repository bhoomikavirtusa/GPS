package com.wiley.permissions.services.imports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import net.sf.jxls.reader.XLSDataReadException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.PagePosition;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.persistence.PersistenceException;

public class FilemakerDataImportUtility {

	private static final Log log = LogFactory.getLog(FilemakerDataImportUtility.class);

	private DataSource dataSource;
	private ImportAssetsUtility importAssetsUtility;

	/**
	 * Imports the assets from fileMakerPro
	 *
	 * @param data
	 * @throws XLSDataReadException
	 * @throws PersistenceException
	 */
	// @Transactional(propagation = Propagation.REQUIRED)
	public void importFileMakerAssets(byte[] data) throws XLSDataReadException, PersistenceException
	{
		JdbcTemplate template = new JdbcTemplate(dataSource);
		String sql = new String(data);
		/*
		 * QUERY - will be part of the file moved under tmp/filemaker
		 *
		 * select
		 * ChapterNo,TypeSection,TypeFigure,Figure_Num,Final_Pg_No,page_pos
		 * ,Pickup_PrevTextRef,P_Num,Description,
		 * Kill,Color,BW,Pict_No,DesignSize
		 * ,PhotoSize,SourcePhotog,Credit,Camera_Copy
		 * ,Free,RoyaltyFree,ObtainedbyAuthor,
		 * ChapOpener,WorkforHire,ModelRelease
		 * ,"New",Retain,Reuse,Archive,Company,ISBN from PWList where
		 * description is not null
		 */
		log.debug(sql);
		UpdatablePreparedStatementCreator psc = new UpdatablePreparedStatementCreator (sql);
		template.query(psc, new RowMapper<ExtendedAssetUse>() {
			@Override
			public ExtendedAssetUse mapRow(ResultSet rs, int rowNum) throws SQLException
			{
				try {
					// getMonitor().startTimer("ImportAssetsUtility::mapRow");
					log.debug("---------------------------------------------");
					log.debug("Import row " + rowNum);
					rs.updateString("status", "started");
					ExtendedAssetUse assetUse = new ExtendedAssetUse();

					assetUse.setComponent(new Component());
					assetUse.setAsset(new Asset());
					assetUse.setCommonWork(new CommonWork());
					assetUse.getAsset().setSource(0, new Source());
					assetUse.setPagePosition(new PagePosition());

					assetUse.getComponent().setName(StringUtils.trim(rs.getString(1)));
					assetUse.setTypeSection(StringUtils.trim(rs.getString(2)));
					assetUse.setTypeFigure(StringUtils.trim(rs.getString(3)));
					assetUse.setPosition(StringUtils.trim(rs.getString(4)));
					assetUse.setFinalPage(StringUtils.trim(rs.getString(5)));
					assetUse.getPagePosition().setCode(StringUtils.trim(rs.getString(6)));
					assetUse.setPickupComment(StringUtils.trim(rs.getString(7)));
					assetUse.setExtSortOrder(StringUtils.trim(rs.getString(8)));
					assetUse.getAsset().setDescription(StringUtils.trim(rs.getString(9)));
					assetUse.setCanceled(StringUtils.trim(rs.getString(10)));
					assetUse.setInColor(StringUtils.trim(rs.getString(11)));
					assetUse.setBlackAndWhite(StringUtils.trim(rs.getString(12)));
					assetUse.getAsset().setVendorId(StringUtils.trim(rs.getString(13)));
					// skip 2
					assetUse.setPhotoSize(StringUtils.trim(rs.getString(15)));
					assetUse.getAsset().setArtist(StringUtils.trim(rs.getString(16)));
					assetUse.getAsset().setCreditLine(StringUtils.trim(rs.getString(17)));
					assetUse.setCameraCopy(StringUtils.trim(rs.getString(18)));
					assetUse.setFree(StringUtils.trim(rs.getString(19)));
					assetUse.setRoyaltyFree(StringUtils.trim(rs.getString(20)));
					assetUse.setObtainedByAuthor(StringUtils.trim(rs.getString(21)));
					assetUse.setChapterOpener(StringUtils.trim(rs.getString(22)));
					assetUse.setWorkForHire(StringUtils.trim(rs.getString(23)));
					assetUse.setModelRelease(StringUtils.trim(rs.getString(24)));
					assetUse.setIsExtNew(StringUtils.trim(rs.getString(25)));
					// skip 1
					assetUse.setReuse(StringUtils.trim(rs.getString(27)));
					assetUse.setIsExtArchive(StringUtils.trim(rs.getString(28)));
					assetUse.setPermissionComment(StringUtils.trim(rs.getString(29)));
					assetUse.setProductionComment(StringUtils.trim(rs.getString(30)));
					assetUse.getAsset().getSource(0).setName(StringUtils.trim(rs.getString(31)));
					assetUse.getAsset().getSource(0).setExternalId(StringUtils.trim(rs.getString(32)));
					// fake a primary product
					Product primary = new Product ();
					primary.setIsbn10(StringUtils.trim(rs.getString(33)));
					primary.setCwPrimary(true);
					assetUse.getCommonWork().getProducts().add(primary);

					// assetUse.getCommonWork().getPrimaryProduct().setIsbn10(rs.getString(30));

					log.debug("Import asset " + assetUse.getAsset().getDescription());
					rs.updateString("status", "mapped");
					// we map it here just to get the usage, but also in populateAndValidate
					assetUse.mapData();

					boolean exists = false;
					int auId = 0, oldAuId = 0;

					auId = persistFileMakerAssetUse(assetUse);
					if (exists) {
						if (oldAuId != auId) {
							rs.updateString("status", "error update existing - different ids [" + oldAuId + "," + auId + "]");
						} else {
							rs.updateString("status", "success update existing [" + auId + "]");
						}
					} else {
						rs.updateString("status", "success [" + auId + "]");
					}
					log.debug("Imported asset use id " + auId);
					// getMonitor().startTimer("ImportAssetsUtility::mapRow");
					return null;
				} catch (Exception e) {
					log.debug("mapRow(): failed to process row, but we continue", e);
					rs.updateString("status", "error");
					rs.updateString("error", e.getMessage());
					return null;
				} finally {
					rs.updateRow();
				}
			}
		});
		// getMonitor().dumpTimerStats();
	}

	public int persistFileMakerAssetUse(ExtendedAssetUse assetUse) throws Exception {
		ImportAssetsStatus status = null;
		try {
			status = populateFileMakerAssetUse(assetUse);
			if (status.getGoodRecords().size() != 1) {
				throw new Exception(status.getErrorMessage());
			}
		} catch (Exception e) {
			throw new Exception("failed to populate[" + e.getMessage() + "]", e);
		}

		List<ExtendedAssetUse> goodList = status.getGoodRecords();
		try {
			getImportAssetsUtility().persistAssetUseList(goodList, status, false); // do not calculate status
			if (goodList.size() != 1) {
				throw new Exception(status.getErrorMessage());
			}
		} catch (Exception e) {
			throw new Exception("failed to persist[" + e.getMessage() + "]");
		}
		// the list should contain one assetUse
		assetUse = goodList.get(0);
		log.debug("persistFileMakerAssetUse(): auId returned " + assetUse.getId());
		// we return the id
		return assetUse.getId();
	}

	/**
	 * Imports one asset from the FileMaker database.
	 */
	public ImportAssetsStatus populateFileMakerAssetUse(ExtendedAssetUse assetUse) {
		log.debug("populateAssetUseListFromFileMaker()...begin");
		// set the filemaker flag
		assetUse.setImportSource(ImportSource.FROM_FILEMAKER);
		assetUse.getAsset().setImportSource(ImportSource.FROM_FILEMAKER);
		assetUse.setStatus(PermissionStatus.MIGRATED_FROM_FILEMAKER);
		assetUse.getAsset().setFeeRequired(true);
		assetUse.getAsset().setRoyaltyFree(false);
		return getImportAssetsUtility().populateAssetUse(assetUse);
	}

	public ImportAssetsUtility getImportAssetsUtility() {
		return importAssetsUtility;
	}

	public void setImportAssetsUtility(ImportAssetsUtility importAssetsUtility) {
		this.importAssetsUtility = importAssetsUtility;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private class UpdatablePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {
		private final String sql;

		public UpdatablePreparedStatementCreator(String sql) {
			this.sql = sql;
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			return con.prepareStatement(this.sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		}

		@Override
		public String getSql() {
			return this.sql;
		}
	}
}
