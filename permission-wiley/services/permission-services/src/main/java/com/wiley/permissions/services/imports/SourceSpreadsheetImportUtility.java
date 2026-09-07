package com.wiley.permissions.services.imports;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.AddressType;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Country;
import com.wiley.permissions.domain.persistence.permissions.PermissionType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceAddress;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.SourceService;
import com.wiley.sf.common.excel.ReadExcel;
import com.wiley.sf.common.excel.SimpleExcelData;

/**
 * This is the class used to import Source Data from a Given XLS Sheet.
 *
 * Don't think we are using this class anymore - but might be useful for
 * future reference if we ever need to do this type of thing again.
 */
// TODO: We Need to move these utility classes to a Client Jar.
public class SourceSpreadsheetImportUtility {

	private static final Log log = LogFactory.getLog(SourceSpreadsheetImportUtility.class);

	private static volatile boolean alreadyCalled = false;

	private SourceService sourceService;
	private SourceRepository sourceRepository;

	public SourceSpreadsheetImportUtility() {
	}

	// TODO SOURCE
/*	*//**
	 * This method Imports data from Excel and updates the Permissions database
	 * using Mule Server Instance. And it returns all the failed Source
	 * records as a List
	 */
	public List<Source> importFromExcel(InputStream is) throws IOException, PersistenceException
	{
		log.debug("importFromExcel(): entered...");

		// smarkoff: fix to ensure this is not called more than once
		// (why being called more than once?)
		if (alreadyCalled) {
			log.debug("importFromExcel(): exiting since already called.");
			return null;
		}
		alreadyCalled = true;

		final int FAIL_LIMIT = 100000; // move to config file
		List<Source> failedData = new ArrayList<Source>();
		int blankNameCount = 0;
		int alreadyExistsCount = 0;
		int duplicateNameCount = 0;

		HashMap<String, Country> countryMap = loadCountryMap();
		// throws PersistenceException
		HashMap<String, Source> sourceMap = loadSourceMap();
		// throws PersistenceException

		HSSFWorkbook wb = new HSSFWorkbook(is);
		HSSFSheet sheet = wb.getSheetAt(0);

		// use map to check for duplicate names
		HashSet<String> nameSet = new HashSet<String>();

		int lastRowNum = sheet.getLastRowNum();
		log.debug("lastRowNum = " + lastRowNum);

		// first row is 0 but skip since heading row
		for (int i = 1; i <= lastRowNum; i++) {
			boolean existing = false;
	//		log.debug("Row number / # failures so far = " + i + " / " + failedData.size());
			log.debug("blankNameCount / duplicateNameCount so far = " + blankNameCount + " / "
					+ duplicateNameCount);
			log.debug("alreadyExsistsCount so far = " + alreadyExistsCount);
			HSSFRow row = sheet.getRow(i);
			Source source = new Source();
			Address address = new Address();

			try {
				String name = getStringCell(row, 4);
				if (StringUtils.isBlank(name)) {
					blankNameCount++;
					log.debug("Won't import source because name is blank... just continue");
					continue;
				}

				// no longer need to add prefix to external id it should now
				// come in the spreadsheet
				// int recId = (int) row.getCell(0).getNumericCellValue();
				// String externalId = "perm.source.FileMaker." + recId;
				String externalId = getStringCell(row, 0);
				String contact_name = getStringCell(row, 40);
				String contact_phone = getStringCell(row, 41);
				String contact_fax = getStringCell(row, 42);
				String contact_email = getStringCell(row, 43);
				String source_phone = getStringCell(row, 27);
				String source_fax = getStringCell(row, 28);

				if (sourceMap.get(externalId) != null) {
					alreadyExistsCount++;
					log.debug("Won't import source because externalId [" + externalId
						+ "] already exsists... will only add address if it does not have one");
					existing = true;
					source = sourceRepository.loadSourceByExternalId(externalId);
					source =  sourceRepository.lazyLoad (Source.class, source.getId(), new String[] {"addresses"});
					if (null == source.getAddresses() || source.getAddresses().size()> 0) continue;
					// continue;
				}

			if (!existing) {
				source.setExternalId(externalId);
				log.debug("externalId: " + externalId);

				// trim name because a few have a leading space
				name = name.trim();

				log.debug("Name: " + name);
				// lnagy - we do not truncate here anymore...we save the whole
				// name in the source table and just
				// the truncated version in the enterprise object

				if (name.length() > 200) {
					log.info("Name is > 200 chars - truncating");
					name = name.substring(0, 200);
				}

				// check for duplicate name
				if (nameSet.contains(name)) {
					duplicateNameCount++;
					log.info("Name [" + name + "] is a duplicate (first 50 chars at least)... will continue");
					continue;
				}
				else {
					nameSet.add(name);
				}
				source.setName(name);
				if (null != source_phone && source_phone.length() > 0) {
					source.setPhoneNumber(source_phone);
				}
				if (null != source_fax && source_fax.length() > 0) {
					source.setFaxNumber(source_fax);
				}


				// import VendorNo
				String vendorID = getStringCell(row, 35);
				if (StringUtils.isNotBlank(vendorID) && vendorID.length() > 7) {
					log.info("Vendor ID is > 7 chars - set to null");
					vendorID = null;
				}
				source.setJdeVendorNumber(vendorID);

				// default it to REUSE_PO
				source.setPermissionType(PermissionType.REUSE_PO.getCode());
				source.setWebsite(StringUtils.EMPTY);
			}

				String addressLines = getStringCell(row, 2);
				log.debug("Address lines: " + addressLines);
				String[] addresses = null;
				if (addressLines != null) {
					addresses = addressLines.split("\n");

					for (int count = 0; count < addresses.length; count++) {
						if (count == 0)
							address.setLineOne(addresses[0]);
						if (count == 1)
							address.setLineTwo(addresses[1]);
						if (count >= 2) {
							address.setLineThree((null == address.getLineThree() ? "" : address
									.getLineThree()) + " " + addresses[count]);
						}
					}
				}

				log.debug("Address line 1: " + address.getLineOne());
				log.debug("Address line 2: " + address.getLineTwo());
				log.debug("Address line 3: " + address.getLineThree());

				String addressLineOne = address.getLineOne();
				if (addressLineOne != null && addressLineOne.length() > 45) {
					log.info("AddressLineOne > 45 chars - truncating");
					address.setLineOne(addressLineOne.substring(0, 45));
				}

				String addressLineTwo = address.getLineTwo();
				if (addressLineTwo != null && addressLineTwo.length() > 45) {
					log.info("AddressLineTwo > 45 chars - truncating");
					address.setLineTwo(addressLineTwo.substring(0, 45));
				}

				String addressLineThree = address.getLineThree();
				if (addressLineThree != null && addressLineThree.length() > 45) {
					log.info("AddressLineThree > 45 chars - truncating");
					address.setLineThree(addressLineThree.substring(0, 45));
				}

				String city = getStringCell(row, 3);
				log.debug("City: " + city);
				address.setCity(city);

				// see comments about countryMap above
				String countryName = getStringCell(row, 5);
				if (StringUtils.isNotBlank(countryName)) {
					countryName = countryName.trim();
					countryName = countryName.replaceAll("\\.", "");
					countryName = countryName.replaceAll("\n", "");
					log.debug("Country: " + countryName);
					Country country = countryMap.get(countryName.toUpperCase());
					if (country == null) {
						log.warn("Cound not find country code for name = " + countryName);
					}
					else {
						address.setCountry(country);
						source.setCountry(country);
					}
				}

				String province = getStringCell(row, 6);
				log.debug("Province: " + province);
				address.setProvince(province);
				String postalCode = getStringCell(row, 28);
				log.debug("PostalCode: " + postalCode);
				if (postalCode != null && postalCode.length() > 10) {
					log.info("PostalCode is > 10 chars - truncating");
					postalCode = postalCode.substring(0, 10);
				}
				address.setPostalCode(postalCode);

				// skipMessage = true since CMS will import the spreadsheet on
				// their own
				log.debug("DEV_TEMP: calling createSource()...");

				if (!existing) {
					source = sourceRepository.saveSource(source);
				}

				log.debug("DEV_TEMP: createSource() done.");

				source = sourceRepository.saveSource(source);

				SourceAddress sourceAddress = new SourceAddress();

				address.setType(AddressType.MAIN);
				if (null == address.getLineOne()) {
					log.debug("******* No address line1 found will ignore address");
					continue;
				}
				address = sourceRepository.save(address);

				sourceAddress.setAddress(address);
				sourceAddress.setSource(source);

				sourceRepository.save(sourceAddress);

				if (null != contact_name && contact_name.length() > 0) {
					source =  sourceRepository.lazyLoad(Source.class, source.getId(), new String[] {"contacts"});
					if (null == source.getContacts() || source.getContacts().size() < 1) {
						contact_name = contact_name.trim().replaceAll("\n", "");
						String[] names = null;
						names = contact_name.split(" ");
						Contact contact = new Contact();
						if (names.length == 2) {
							contact.setFirstName(names[0]);
							contact.setLastName(names[1]);
						} else {
							contact.setFirstName(contact_name);
						}

						contact.setAddress(address);
						contact.setEmail(contact_email);
						contact.setFax(contact_fax);
						contact.setPhone(contact_phone);

						contact.setSource(source);
						contact = sourceRepository.save(contact);
						log.info("Added contact: " + contact);
					}
				}

				log.info("Added Source: " + source);
			}
			catch (Exception e) {
				log.error("Exception: ", e);

				failedData.add(source);
				if (failedData.size() >= FAIL_LIMIT) {
					log.error("Stopping WinTouch import since reached " + FAIL_LIMIT + " failures.");
					break;
				}
			}
		}

		log.info("total failures = " + failedData.size());
		log.info("final blankNameCount = " + blankNameCount);
		log.info("final alreadyExistsCount = " + alreadyExistsCount);
		log.info("final duplicateNameCount = " + duplicateNameCount);

		if (failedData.size() == 0)
			failedData = null;

		return failedData;
	}

	/**
	 * Return a null cell as null string, but avoid the NullPointerException
	 * from calling getStringCellValue() on the null cell.
	 */
	private static String getStringCell(HSSFRow row, int col)
	{
		HSSFCell cell = row.getCell(col);

		if (cell == null)
			return null;

		if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
			return cell.getNumericCellValue() + "";
		}
		else {
			return cell.getStringCellValue();
		}
	}

	private HashMap<String, Country> loadCountryMap() throws PersistenceException
	{
		// create lookup of country codes for country names
		// (since spreadsheet has name instead of code)
		// Also since spreadsheet has country names in all uppercase, unlike db,
		// take care of that problem by putting names in map as uppercase.
		List<Country> countryList = sourceRepository.loadCountryList();
		// throws PersistenceException
		HashMap<String, Country> countryMap = new HashMap<String, Country>();
		for (Country c : countryList) {
			countryMap.put(c.getDescription().trim().toUpperCase(), c);
		}

		// Also add "U.S.A" (spreadsheet has) since db has "USA"
		Country usa = countryMap.get("USA");
		if (usa != null) {
			countryMap.put("U.S.A.", usa);
		}

		// now load by country code
		for (Country c : countryList) {
			countryMap.put(c.getCode().toUpperCase(), c);
		}

		return countryMap;
	}


	private HashMap<String, Source> loadSourceMap() throws PersistenceException {
		// create lookup of Source based on externalId
		List<Source> sourceList = sourceRepository.loadSourceList();
		// throws PersistenceException
		HashMap<String, Source> sourceMap = new HashMap<String, Source>();
		for (Source s : sourceList) {
			sourceMap.put(s.getExternalId(), s);
		}

		return sourceMap;
	}

	/**
	 * This method takes in a File as Resource and returns Data Grid.If
	 * something goes wrong returns null.
	 *
	 * @param source
	 * @return
	 */
	private SimpleExcelData readFromExcel(InputStream is) {
		try {
			SimpleExcelData data = ReadExcel.parse(is);
			return data;
		}
		catch (Exception e) {
			log.error("Error Reading Excel", e);
			return null;
		}
	}

	public SourceService getSourceService() {
		return sourceService;
	}

	public void setSourceService(SourceService sourceService) {
		this.sourceService = sourceService;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}
}
