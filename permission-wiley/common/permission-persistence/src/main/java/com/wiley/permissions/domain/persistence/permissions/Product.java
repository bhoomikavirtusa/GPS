package com.wiley.permissions.domain.persistence.permissions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.sf.common.config.PropertiesUtil;
import com.wiley.sf.common.io.FixWindows1252Chars;
import com.wiley.sf.common.xml.bind.BooleanXmlAdapter;

/**
 *
 * @version $Id: Product.java,v 1.88.4.1 2018-01-18 15:10:11 nchandra Exp $
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "PRODUCT")
@NamedQueries ({
	@NamedQuery(
	    name="Product.findFamilyMembers",
	    query="from Product prod join fetch prod.commonWork where " +
	    		"prod.productFamily.code = ?1 and prod.id <> ?2 order by prod.title"
	),
	@NamedQuery(
		    name="Product.findEditionMembers",
		    query="from Product prod join fetch prod.commonWork where " +
		    		"prod.productFamily.code = ?1 and prod.edition.id=?2 and prod.id <> ?3 order by prod.title"
		),
	@NamedQuery(
	    name="Product.findRelatedWorks",
	    query="from Product prod where " +
	    		"prod.commonWork.code = :workCode and prod.id <> :currentId order by prod.title"
	),
	@NamedQuery(
		    name="Product.findByTitle",
		    query="from Product p where p.title like :title "
	),
	@NamedQuery(
		    name="Product.findByIsbn10",
		    query="from Product p where p.isbn10 = :isbn "
	),
	@NamedQuery(
		    name="Product.findByPNumber",
		    query="from Product p where p.pnumber = :pnumber "
	),
	@NamedQuery(
		    name="Product.findByIsbn13",
		    query="from Product p where p.isbn13 = :isbn "
	),
	@NamedQuery(
			name="Product.findByProductPriority",
			query="from Product p where p.productPriority = :productPriority"
	),
	@NamedQuery(
		    name="Product.findProductsByCW",
		    query="from Product p where p.commonWork.id = ?1 "
	)
})
@NamedNativeQueries({
	@NamedNativeQuery(name = "Product.getTotalPrinting",
		 		  		query = "select coalesce(sum(pp.order_quantity),0) + coalesce(p.ebook_sales, 0) as count from product_printing pp, product p where pp.product_id = ? and p.id = ?",
		 		  		resultSetMapping="scalarCount"),
    @NamedNativeQuery(name="Product.findRelatedCustomPublications",
 		  			    query="select p.* from product p join product p2 on p2.cw_id = ? join relation r on p2.id = r.product_id and r.code = 'WC' where p.external_id = r.related_wid ;",
 		  			    resultClass=Product.class)
})

/*	Not available until JPA2.0 -- which we are now using -- so could review where we wanted to use this query and use it
@NamedNativeQueries({
	@NamedNativeQuery(
		    name="Product.findAllInWatchedCommonWorks",
		    query="select p.* from product p where p.cw_id in (select cw_id from watched_cw where user_id=:userId)"
	)
})
*/

public class Product extends AuditBase {
	private static final long serialVersionUID = 1L;

	private final static Log log = LogFactory.getLog(Product.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = true, unique = true)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private Integer id = null;

	// set nullable=true since won't be set until trigger
	@Column(name = "EXTERNAL_ID", nullable = false, length = 50)
	@MaterializationKey
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String externalId;

	@Column(name = "ISBN10", nullable = true, length = 20)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String isbn10 = null;

	@Column(name = "ISBN13", nullable = true, length = 20)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String isbn13 = null;

	@Column(name = "EDITOR", nullable = true, length = 10)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String editor = null;

	@Column(name = "PNUMBER", nullable = true, length = 20)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String pnumber = null;

	@Column(name = "DATA_SOURCE", nullable = false, length = 20)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String dataSource = null;

	@Transient
	// This is Boolean instead of boolean because otherwise BooleanXmlAdapter does not work
	private Boolean completeRecordSet = Boolean.FALSE;


	@Column(name = "PHOTO_ILLUS_TOTAL_COUNT")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private Integer photoIllusTotalCount = 0;

	@Column(name = "SKU", nullable = true, length = 20)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String sku = null;

	@Column(name = "TITLE", nullable = false, length = 100)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String title = null;

	@Column(name = "SHORT_TITLE", nullable = true, length = 100)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String shortTitle = null;

	@Column(name = "VOLUME", nullable = true, length = 20)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String volume = null;

	@Column(name = "COPYRIGHT_YEAR", nullable = true)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private Integer copyrightYear = null;

	@Column(name = "EBOOK_SALES", nullable = true)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private Integer ebookSales = null;

	@Column(name = "LANGUAGE_SPOKEN", nullable = true, length = 25)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String languageSpoken = null;
	
	@Column(name = "PRODUCT_PRIORITY", nullable = true, length = 10)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String productPriority = null;

	@Column(name = "SHORT_AUTHOR_NAME", nullable = true, length = 256)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String shortAuthorName = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "PRODUCTION_DATE", nullable = true)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private Date productionDate = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "PRINT_DATE", nullable = true)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private Date printDate = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CONSOLIDATED_RELEASE_DATE", nullable = true)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private Date consolidatedReleaseDate = null;

	// note this is called productionEndDate in PE but James says editorial calls it transmittalDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "TRANSMITTAL_DATE", nullable = true)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private Date transmittalDate = null;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "SUBJECT_CODE_ID")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private SubjectCode subjectCode = null;

	@ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "MEDIUM")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private Medium medium = null;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "SUBMEDIUM_ID")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private SubMedium submedium = null;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "BUSINESS_UNIT")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private BusinessUnit businessUnit = null;


	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "CW_ID")
	// smarkoff: NEVER_MERGE would be fine except for we do need JPARepository.gatherEntities() to traverse this
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	//@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private CommonWork commonWork = null;

	@Column(name="IS_CW_PRIMARY", nullable=false)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private boolean cwPrimary = false;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "EDITION")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private ProductEdition edition = null;

	@Column(name = "PREVIOUS_EDITION_WID")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private String previousEditionWID;

	@Column(name = "NEXT_EDITION_WID")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private String nextEditionWID;

	@Column(name = "EDITION_NUMBER")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private Integer editionNumber;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "PRODUCT_LINE_ID")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private ProductLine productLine = null;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "PRODUCT_FAMILY")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private ProductFamily productFamily = null;

/*	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "PRODUCT_PRIORITY")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private ProductPriority productPriorityy = null;*/
	
	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "PRODUCT_TYPE")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private ProductType productType = null;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "GEO_LOCATION")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private GeographicalLocation location = null;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "PERMISSION_PAYER")
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private PermissionPayer permissionPayer;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST,
			CascadeType.REFRESH })
	@JoinColumn(name = "PUB_STATUS")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private PublicationStatus publicationStatus = null;

	@Column(name = "process_code", nullable = true, length = 10)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String processCode;

	@OneToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH },
			fetch = FetchType.LAZY, mappedBy = "product")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT,
			collectionHandling = Merge.CollectionHandling.MERGE)
	private List<UserToRole> users = new ArrayList<UserToRole>();

	@OneToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH },
			fetch = FetchType.LAZY, mappedBy = "product")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT,
			collectionHandling = Merge.CollectionHandling.MERGE)
	private List<ProductPrinting> printings = new ArrayList<ProductPrinting>();

	@OneToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH },
			fetch = FetchType.LAZY, mappedBy = "product")
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT,
			collectionHandling = Merge.CollectionHandling.MERGE)
	private List<Relation> relations;

	@ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinTable(
			name = "PRODUCT_2_BUNDLE",
			joinColumns = @JoinColumn(name = "PRODUCT_ID"),
			inverseJoinColumns = @JoinColumn(name = "BUNDLE_CODE"))
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT,
			collectionHandling = Merge.CollectionHandling.MERGE)
	private List<Bundle> bundles;

	@Column(name = "discount_group_code", length = 10)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String discountGroupCode;

	@Column(name = "discount_sub_group_code", length = 10)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String discountSubGroupCode;

	@Column(name = "component_flag", length = 10)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private String componentFlag;


	public Product() {
		super();
	}

	@Override
	@PrePersist
	public void prePersist() {
		// Override AuditBase so we do NOT set lastUpdatedDate if it's null
		// - It should always be set to whatever date came from PE and not automatically
		if (getCreatedDate() == null) {
			setCreatedDate(new Date());
		}
	}

	@Override
	@PreUpdate
	public void preUpdate() {
		// Override AuditBase so we don't set to current date
		// - Should be set to whatever date came from PE or else not changed
	}

	/**
	 * Note if there is more than one user for the given role, this method just
	 * returns the first one. If you want all matching users, use
	 * getUsersForRole().
	 *
	 * Returns null if no such user exists.
	 */
	@Transient
	private User getUserForRole(Role role) {
		if (users == null)
			return null;

		for (UserToRole userToRole : users) {
			if (null != userToRole.getRole() && userToRole.getRole().equals(role)) {
				return userToRole.getUser();
			}
		}

		return null;
	}

	/**
	 * Note if there is more than one user for the given roles, this method just
	 * returns the first one. If you want all matching users, use
	 * getUsersForRoles().
	 *
	 * Returns null if no such user exists.
	 */
	@Transient
	private User getUserForRoles(Role [] roles) {
		if (users == null)
			return null;

		for (UserToRole userToRole : users) {
			for (Role role : roles) {
				if (null != userToRole.getRole() && userToRole.getRole().equals(role)) {
					return userToRole.getUser();
				}
			}
		}

		return null;
	}

	/**
	 * Returns an empty list if no such user exists.
	 */
	@Transient
	private List<User> getUsersForRole(Role role) {
		ArrayList<User> list = new ArrayList<User>();
		if (users == null)
			return list;

		for (UserToRole userToRole : users)	{
			if (null != userToRole.getRole() && userToRole.getRole().equals(role)) {
				list.add(userToRole.getUser());
			}
		}

		return list;
	}

	/**
	 * Returns an empty list if no such user exists.
	 */
	@Transient
	private List<User> getUsersForRoles(Role [] roles) {
		ArrayList<User> list = new ArrayList<User>();
		if (users == null)
			return list;

		for (UserToRole userToRole : users)	{
			for (Role role : roles) {
				if (null != userToRole.getRole() && userToRole.getRole().equals(role)) {
					list.add(userToRole.getUser());
					continue;
				}
			}
		}

		return list;
	}

	/**
	 * Utility method to get the Production Editor for this product
	 *
	 * @return User
	 * @see<code>com.wiley.permissions.domain.persistence.permissions.User</code>
	 */
	@Transient
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	public User getProductionEditor() {
		return getUserForRole(Role.PRODUCTION_EDITOR);
	}

	/**
	 * Returns null if no PhotoEditor exists.
	 */
	@Transient
	public User getPhotoEditor() {
		return getUserForRole(Role.PHOTO_EDITOR);
	}

	/**
	 * Utility method to get the Designer for this product
	 *
	 * @return User
	 * @see<code>com.wiley.permissions.domain.persistence.permissions.User</code>
	 */
	@Transient
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	public User getDesigner() {
		return getUserForRoles(new Role [] { Role.DESIGNER_HIGHER_ED, Role.DESIGNER_WCS });
	}

	/**
	 * Utility method to get the Photo Researcher for this product
	 *
	 * @return User
	 * @see<code>com.wiley.permissions.domain.persistence.permissions.User</code>
	 */
	@Transient
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	public User getPhotoResearcher() {
		return getUserForRoles(new Role [] { Role.PHOTO_RESEARCHER });
	}

	/**
	 * Utility method to get the Development Editor for this product
	 *
	 * @return User
	 * @see<code>com.wiley.permissions.domain.persistence.permissions.User</code>
	 */
	@Transient
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	public User getDevelopmentEditor() {
		return getUserForRole(Role.DEVELOPMENT_EDITOR);
	}

	/**
	 * Utility method to get the Acquisitions Editor for this product
	 *
	 * @return User
	 * @see<code>com.wiley.permissions.domain.persistence.permissions.User</code>
	 */
	@Transient
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	public User getAcquisitionsEditor() {
		// A. Editor is the same as just "Editor".
		return getUserForRole(Role.EDITOR_EMPLOYEE);
	}

	/**
	 * Returns a single author for this product (if there is more than one
	 * author, just returns the first one).
	 */
	@Transient
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	public User getAuthor() {
		return getUserForRole(Role.AUTHOR);
	}

	/**
	 * Convenience method for jsp to use to avoid referencing a null author User
	 * object.
	 */
	@Transient
	public String getAuthorFullName() {
		User author = getAuthor();
		if (author == null)
			return null;
		else
			return author.getFullName();
	}

	/**
	 * Returns all authors.
	 */
	@Transient
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	public List<User> getAuthors() {
		// need to use a different approach to identify authors because they come with different
		// role types than we expect from PE.

	//	return getUsersForRole(Role.AUTHOR);

		ArrayList<User> list = new ArrayList<User>();
		if (users == null)
			return list;

		for (UserToRole userToRole : users) {
			if (userToRole.getUser().getType().equals(User.Type.AUTHOR)) {
				list.add(userToRole.getUser());
			}
//			if (null != userToRole.getRole() && userToRole.getRole().equals(role))
//			{
//				list.add(userToRole.getUser());
//			}
		}

		if (CollectionUtils.isEmpty(list) && StringUtils.isNotBlank(shortAuthorName)) {
			User u = new User ();
			u.setLastName(shortAuthorName);
			list.add(u);
		}
		return list;

	}

	@Transient
	public String getAuthorsAsString() {
		List<User> list = getAuthors();
		// actually getAuthors() is currently guaranteed not to return null but no harm in checking
		if (null == list || list.size() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			User user = list.get(i);
			sb.append(user.getFullName());
			if (i < list.size() - 1)
				sb.append("; ");
		}

		return sb.toString();
	}

	@XmlElement
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlID
	@XmlElement
	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@XmlElement(name = "isbn10")
	public String getIsbn10() {
		return isbn10;
	}

	public void setIsbn10(String isbn10) {
		this.isbn10 = isbn10;
	}

	@XmlElement(name = "isbn13")
	public String getIsbn13() {
		return isbn13;
	}

	public void setIsbn13(String isbn13) {
		this.isbn13 = isbn13;
	}

	@XmlElement(name = "productPriority")
	public String getProductPriority() {
		return productPriority;
	}

	public void setProductPriority(String productPriority) {
		this.productPriority = productPriority;
	}
	@XmlElement
	public String getEditor() {
		return editor;
	}

	public void setEditor(String editor) {
		this.editor = editor;
	}

	@XmlElement(name = "pnumber")
	public String getPnumber() {
		return pnumber;
	}

	public void setPnumber(String pnumber) {
		this.pnumber = pnumber;
	}

	/**
	 * Will return ISBN13 if available, else ISBN10 else PNumber
	 * @return String
	 */
	public String getIsbn () {
		if (StringUtils.isNotBlank(getIsbn13())) {
			return getIsbn13();
		} else if (StringUtils.isNotBlank(getIsbn10())) {
			return getIsbn10();
		} else if (StringUtils.isNotBlank(getPnumber())) {
			return getPnumber();
		} else return StringUtils.EMPTY;
	}

	@XmlElement
	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	@XmlElement
	@XmlJavaTypeAdapter(BooleanXmlAdapter.class)
	public Boolean isCompleteRecordSet() {
		return completeRecordSet;
	}

	public void setCompleteRecordSet(Boolean b) {
		this.completeRecordSet = b;
	}

	@XmlElement
	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	@XmlElement
	public String getTitle() {
		// setTitle() is not called on Product load, so Fix chars here also
		title = FixWindows1252Chars.fix(title, "title", "getTitle", log);
		return title;
	}

	public void setTitle(String title) {
		this.title = FixWindows1252Chars.fix(title, "title", "setTitle", log);
	}

	@XmlElement
	public String getLanguageSpoken() {
		return languageSpoken;
	}

	public void setLanguageSpoken(String value) {
		this.languageSpoken = value.toLowerCase();
	}

	@XmlElement
	public String getShortAuthorName() {
		return shortAuthorName;
	}

	public void setShortAuthorName(String shortAuthorName) {
		this.shortAuthorName = shortAuthorName;
	}

	@XmlElement
	public String getShortTitle() {
		return shortTitle;
	}

	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	@XmlElement
	public String getVolume() {
		return volume;
	}

	public void setVolume(String volume) {
		this.volume = volume;
	}

	@XmlElement
	public Integer getCopyrightYear() {
		return copyrightYear;
	}

	public void setCopyrightYear(Integer copyrightYear) {
		this.copyrightYear = copyrightYear;
	}

	public Integer getEbookSales() {
		return ebookSales;
	}

	public void setEbookSales(Integer ebookSales) {
		this.ebookSales = ebookSales;
	}

	@XmlElement
	public Date getProductionDate() {
		return productionDate;
	}

	public void setProductionDate(Date productionDate) {
		this.productionDate = productionDate;
	}

	@XmlElement
	public Date getPrintDate() {
		return printDate;
	}

	public void setPrintDate(Date printDate) {
		this.printDate = printDate;
	}

	@XmlElement
	public Date getConsolidatedReleaseDate() {
		return consolidatedReleaseDate;
	}

	public void setConsolidatedReleaseDate(Date consolidatedReleaseDate) {
		this.consolidatedReleaseDate = consolidatedReleaseDate;
	}

	@XmlElement
	public Date getTransmittalDate() {
		return transmittalDate;
	}

	public void setTransmittalDate(Date date) {
		this.transmittalDate = date;
	}

	@XmlElement
	@XmlIDREF
	public SubjectCode getSubjectCode() {
		return subjectCode;
	}

	public void setSubjectCode(SubjectCode subjectCode) {
		this.subjectCode = subjectCode;
	}

	@XmlElement(name = "targetMedium")
	@XmlIDREF
	public Medium getMedium() {
		return medium;
	}

	public void setMedium(Medium medium) {
		this.medium = medium;
	}

	@XmlElement
	@XmlIDREF
	public BusinessUnit getBusinessUnit() {
		return businessUnit;
	}

	public void setBusinessUnit(BusinessUnit businessUnit) {
		this.businessUnit = businessUnit;
	}

	@XmlElement
	@XmlIDREF
	public CommonWork getCommonWork() {
		return commonWork;
	}

	public void setCommonWork(CommonWork value) {
		this.commonWork = value;
	}

	public boolean isCwPrimary() {
		return cwPrimary;
	}

	public void setCwPrimary(boolean b) {
		this.cwPrimary = b;
	}

	@XmlElement(name = "edition")
	@XmlIDREF
	public ProductEdition getEdition() {
		return edition;
	}

	public void setEdition(ProductEdition edition) {
		this.edition = edition;
	}

	@XmlElement
	public Integer getEditionNumber() {
		return editionNumber;
	}

	public void setEditionNumber(Integer editionNumber) {
		this.editionNumber = editionNumber;
	}

	@XmlElement
	public String getPreviousEditionWID() {
		return previousEditionWID;
	}

	public void setPreviousEditionWID(String previousEditionWID) {
		this.previousEditionWID = previousEditionWID;
	}

	@XmlElement
	public String getNextEditionWID() {
		return nextEditionWID;
	}

	public void setNextEditionWID(String nextEditionWID) {
		this.nextEditionWID = nextEditionWID;
	}

	@XmlElement
	@XmlIDREF
	public ProductLine getProductLine() {
		return productLine;
	}

	public void setProductLine(ProductLine productLine) {
		this.productLine = productLine;
	}

	@XmlElement
	@XmlIDREF
	public ProductFamily getProductFamily() {
		return productFamily;
	}

	public void setProductFamily(ProductFamily productFamily) {
		this.productFamily = productFamily;
	}

	@XmlElement
	@XmlIDREF
	public ProductType getProductType() {
		return productType;
	}

	public void setProductType(ProductType productType) {
		this.productType = productType;
	}
/*	
	@XmlElement
	@XmlIDREF
	public ProductPriority getProductPriorityy() {
		return productPriorityy;
	}

	public void setProductPriorityy(ProductPriority productPriorityy) {
		this.productPriorityy = productPriorityy;
	}
*/
	@XmlElement(name = "geographicalLocation")
	@XmlIDREF
	public GeographicalLocation getLocation() {
		return location;
	}

	public void setLocation(GeographicalLocation location) {
		this.location = location;
	}

	public PermissionPayer getPermissionPayer() {
		return permissionPayer;
	}

	public void setPermissionPayer(PermissionPayer permissionPayer) {
		this.permissionPayer = permissionPayer;
	}

	@XmlElement(name = "publicationStatus")
	public PublicationStatus getPublicationStatus() {
		return publicationStatus;
	}

	public void setPublicationStatus(PublicationStatus publicationStatus) {
		this.publicationStatus = publicationStatus;
	}

	@XmlElement
	public String getProcessCode() {
		return processCode;
	}

	public void setProcessCode(String processCode) {
		this.processCode = processCode;
	}

	@XmlElementWrapper(name = "userToProducts")
	@XmlElement(name = "userToProduct")
	public List<UserToRole> getUsers() {
		return users;
	}

	public void setUsers(List<UserToRole> users) {
		this.users = users;
	}

	@XmlElementWrapper(name = "printings")
	@XmlElement(name = "printing")
	public List<ProductPrinting> getPrintings() {
		return printings;
	}

	public void setPrintings(List<ProductPrinting> printings) {
		this.printings = printings;
	}

	@XmlElementWrapper(name = "relations")
	@XmlElement(name = "relation")
	public List<Relation> getRelations() {
		return relations;
	}

	public void setRelations(List<Relation> relations) {
		this.relations = relations;
	}

	@XmlElementWrapper(name = "bundles")
	@XmlElement(name = "bundle")
	@XmlIDREF
	public List<Bundle> getBundles() {
		return bundles;
	}

	public void setBundles(List<Bundle> bundles) {
		this.bundles = bundles;
	}

	@XmlElement
	public String getDiscountGroupCode() {
		return discountGroupCode;
	}

	public void setDiscountGroupCode(String s) {
		discountGroupCode = s;
	}

	@XmlElement
	public String getDiscountSubGroupCode() {
		return discountSubGroupCode;
	}

	public void setDiscountSubGroupCode(String s) {
		discountSubGroupCode = s;
	}

	@XmlElement
	public String getComponentFlag() {
		return componentFlag;
	}

	public void setComponentFlag(String s) {
		componentFlag = s;
	}

	@Override
	public String toString() {
		// use getters due to the way JPA works
		return super.toString() + ",\r\n" + "isbn13 = " + getIsbn13()
			+ ", external_id = " + getExternalId()
			// avoid accessing printings because causes lazy-load issues when debug.jspx enabled
			//+ ",\r\n" + ((null == this.getPrintings()) ? "no printings, "  : "printings = " + this.getPrintings().size() + ",")
			+ ",\r\ntitle = " + getTitle() + ",\r\n(other fields not shown)";
	}

	@XmlElement(name = "subMedium")
	@XmlIDREF
	public SubMedium getSubMedium() {
		return submedium;
	}

	public void setSubMedium(SubMedium submedium) {
		this.submedium = submedium;
	}

	/**
	 * We want the ISBN-10 with the first digit missing, or else
	 * the p-number if there is no ISBN.
	 * @return
	 */
	@Transient
	public String getCore9DigitIsbn() {
		String ten = getIsbn10();
		if (StringUtils.isNotBlank(ten)) {
			return ten.substring(1);
		}

		String pnumber = getPnumber();
		if (StringUtils.isBlank(pnumber)) {
			return "";
		}
		else return pnumber;
	}

	/**
	 * The Lang Code in this case is the first digit of the ISBN-10
	 * or else "P" if there is no ISBN yet.
	 */
	@Transient
	public String getCoreLangCode() {
		String ten = getIsbn10();
		if (StringUtils.isNotBlank(ten)) {
			return ten.substring(0, 1);
		}

		return "P";
	}
	
	@Transient
	private String ebookFromBIorCore = null;
	public String getEbookFromBIorCore() {
		return this.ebookFromBIorCore;
	}

	public void setEbookFromBIorCore(String ebookFromBIorCore) {
		this.ebookFromBIorCore = ebookFromBIorCore;
	}

	@Transient
	public String getBPMUrl() {
		// TODO: Later move host:port (and maybe more) to properties file
		//code change for  incident INC_70691
		String fileName = System.getenv("PERMISSIONS_HOME") + "/permissions.properties";
		PropertiesUtil pu = null;
		String url=null;
		try {
			pu = new PropertiesUtil(fileName);
			log.info("loaded properties from " + fileName);
		}
		catch (IOException e) {
			log.error(" Could not load Properties from: " + fileName);
		}

		if (pu != null) {
			try {
				url = pu.getStringNotBlank("gbpm.url");
				}
			catch (Exception ex) {
				log.error("Error reading properties: ", ex);
			}
		
		}
		//end code change for  incident INC_70691
		 url = url+"/cgi-bin/lansaweb?"
			+ "procfun+BPM04+BP4fn30+DEV+ENG+funcparms+W1AREFRSH(A0010):Y"
			+ "+PISBN(A0090):" + getCore9DigitIsbn()
			+ "+PCWIL(A0010):N"
			+ "+PLANG(A0010):" + getCoreLangCode();
		 log.debug("Url --->"+url);
		return url;

		// W1AREFRSH - pass it as Y
		// PISBN - 9 digit ISBN
		// PCWIL - Wiley code (N)
		// PLANG - Lang Code
	}

	public void setPhotoIllusTotalCount(Integer photoIllusTotalCount) {
		this.photoIllusTotalCount = photoIllusTotalCount;
	}

	@XmlElement
	public Integer getPhotoIllusTotalCount() {
		return photoIllusTotalCount;
	}

	/**
	 * Base this method only on id.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// call getId since using JPA - not sure if necessary
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());

		return result;
	}

	/**
	 * Base this method only on id.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)  return true;
		if (o == null)  return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(o instanceof Product))  return false;

		Product other = (Product) o;
		if (other.getId() == null || getId() == null) return false;
		return other.getId().equals(getId());
	}
}
