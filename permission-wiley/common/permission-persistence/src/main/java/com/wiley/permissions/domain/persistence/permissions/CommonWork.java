package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
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
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Formula;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@NamedNativeQueries({
	@NamedNativeQuery(	name = "CommonWork.getTotalAssetCount",
		 		  		query = "select coalesce(count(distinct(au.asset_id)),0) as count from asset_use au where au.cw_id = ?",
		 		  		resultSetMapping="scalarCount")
})

@SqlResultSetMappings({
	// scalarDouble is for CommonWorkRepository.getTotalEstimatedCost() and other methods
	// "double" is a mysql keyword but "double_value" fine
	@SqlResultSetMapping (name="scalarDouble", columns = @ColumnResult(name = "double_value") )
})
@Table(name = "COMMON_WORK")
public class CommonWork
		extends DomainObject {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CommonWork.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", unique = true, nullable = false)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private Integer id = null;

	// Most codes are less than 20 chars but when we have to generate our
	// own dummy codes (not the usual case) they are longer - so 64 chars.
	@Column(name = "CODE", unique = true, nullable = false, length = 64)
	@MaterializationKey(ignoreCase = true, alwaysTrim = true)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String code;

	@Column(name = "NAME", nullable = true, length = 100)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String name;

	@Column(name = "NOTES", nullable = true, length = 500)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String notes;

	@Column(name="CONDITIONS_INIT", nullable=false)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private boolean conditionsInit = false;

	// Actually this is OneToMany but it's not the usual kind
	@ManyToMany
	@JoinTable(
			name = "CW_2_CONDITION",
			joinColumns = @JoinColumn(name = "CW_ID"),
			inverseJoinColumns = @JoinColumn(name = "CONDITION_ID"))
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private List<Condition> conditions;

	@ManyToMany
	@JoinTable(name = "AUTHOR_2_CW", joinColumns = @JoinColumn(name = "CW_ID"), inverseJoinColumns = @JoinColumn(name = "USER_ID"))
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private List<User> authors = new ArrayList<User>();

	// added never merge and StopGather annotations because it was creating problem with Product Engineering message processor
	@OneToMany(mappedBy = "commonWork")
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private List<Product> products = new ArrayList<Product>();

	@Formula("(select count(*) from product p where p.cw_id = id)")
	@Basic(fetch = FetchType.LAZY)
	private int productCount;

	// smarkoff: 2/25/2013
	// Up until this date the fields allWiley, permissionComplete, noCoverArt,
	// (allWiley, permissionComplete, noCoverArt now replaced by interiorCWStatus
	// and coverCWStatus) and minGrantYears were missing the
	// @Merge - NEVER_MERGE annotation and I confirmed that the MasterList
	// update for CommonWork was overriding these fields with their default
	// values (false, 0). Fixed in a build on all environments on this
	// date but certainly some data was probably lost (altered) beforehand.
	// Actually seems like photoCountEstimate (removed now) was not affected
	// (but minGrantYears was)
	// (- has something to do with photoCountEstimate being nullable Integer while
	// minGrantYears non-nullable int?)
	// Anyway, James says having lost some of this data not a big deal (but
	// definitely want fixed now).

	@Column(name = "MIN_GRANT_YEARS", nullable = false)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private int minGrantYears = 10; // default value and can't be changed

	@ManyToOne
	@JoinColumn(name = "INTERIOR_CW_STATUS", nullable = false)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private CommonWorkStatus interiorCWStatus = CommonWorkStatus.IN_PROGRESS;

	@ManyToOne
	@JoinColumn(name = "COVER_CW_STATUS", nullable = false)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private CommonWorkStatus coverCWStatus = CommonWorkStatus.IN_PROGRESS;


	@Formula("(select count(*) from payment_request pr, contract c " +
			"where c.cw_id = id and pr.contract_id = c.id)")
	@Basic(fetch = FetchType.LAZY)
	private int paymentCount;

	@Formula("(select count(*) from contract c where c.cw_id = id)")
	@Basic(fetch = FetchType.LAZY)
	private int contractCount;

	@Formula("(select count(*) from purchase_order po where po.cw_id = id)")
	@Basic(fetch = FetchType.LAZY)
	private int poCount;

	// For some reason "is false" does not work in Formula (but "= 0" works)
	@Formula("(select count(*) from asset_use au where au.cw_id = id and au.is_canceled = 0)")
	@Basic(fetch = FetchType.LAZY)
	private int auCountNotCanceled;

	// James says to use Component Category to determine if Cover, not usage_type
	// For some reason "is false" does not work in Formula (but "= 0" works)
	@Formula("(select count(*) from asset_use au, component c where au.cw_id = id and au.is_canceled = 0 and au.component_id = c.id and c.category = 'CVW')")
	@Basic(fetch = FetchType.LAZY)
	private int coverCountNotCanceled;

	// James says to use Component Category to determine if Cover, not usage_type
	// No/null component counts as interior
	// nonCoverCountNotCanceled - should be same as (auCountNotCanceled - coverCountNotCanceled)
	@Formula("(select count(*) from asset_use au left join component c on c.id = au.component_id where au.cw_id = id and au.is_canceled = 0 and (c.category is null or c.category != 'CVW'))")
	@Basic(fetch = FetchType.LAZY)
	private int nonCoverCountNotCanceled;

	@Formula("(select count(*) from asset_use au where au.cw_id = id and (au.permission_status is null or au.permission_status IN ('illegal','contractInsufficient', "
			+ "'missingInfo', 'outOfCompliancePrintRun', 'outOfComplianceExpired')))")
	@Basic(fetch = FetchType.LAZY)
	private int statusNotOkCount;

	@Formula("(select max(h.last_updated_date) from cw_history h where h.cw_id = id)")
	@Basic(fetch = FetchType.LAZY)
	private Date lastWorkedOnDate;  // may be null


	public CommonWork() {
		super();
	}

	public CommonWork(String code, String name) {
		this.code = code;
		this.name = name;
	}

	// Don't need prePersist because ProductRepository will take care of creating
	// the identifier when needed (in savePartialProductInner()).
	//@PrePersist
	//public void prePersist() {
	//	if (StringUtils.isBlank(getCode())) {
	//		setCode(UniqueIdentifierGenerator.getNextIdentifier("perm.cw."));
	//	}
	//}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElement
	@XmlID
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Transient
	public boolean isDummy() {
		return getCode().startsWith("perm.cw.");
	}

	@XmlElement(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String value) {
		this.notes = value;
	}

	public boolean isConditionsInit() {
		return conditionsInit;
	}

	public void setConditionsInit(boolean conditionsInit) {
		this.conditionsInit = conditionsInit;
	}

	public CommonWorkStatus getInteriorCWStatus() {
		return interiorCWStatus;
	}

	public void setInteriorCWStatus(CommonWorkStatus status) {
		this.interiorCWStatus = status;
	}

	// method used by assets.jspx assetTabButtons.jspx - may change logic in jsp later
	@Transient
	public boolean isPermissionComplete() {
		return CommonWorkStatus.COMPLETE.equals(getCoverCWStatus())
			&& CommonWorkStatus.COMPLETE.equals(getInteriorCWStatus());
	}

	public CommonWorkStatus getCoverCWStatus() {
		return coverCWStatus;
	}

	public void setCoverCWStatus(CommonWorkStatus status) {
		this.coverCWStatus = status;
	}

	public int getMinGrantYears() {
		return minGrantYears;
	}

	public void setMinGrantYears(int minGrantYears) {
		this.minGrantYears = minGrantYears;
	}

	public List<Condition> getConditions() {
		return conditions;
	}

	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}

	@Transient
	public List<Condition> getConditionsNotNull() {
		if (conditions == null) {
			conditions = new ArrayList<Condition>();
		}
		return conditions;
	}

	/**
	 * This is just for the Rules Engine which doesn't handle generics yet.
	 * Will return an empty list instead of null.
	 */
	@Transient
	public ConditionList getConditionList() {
		List<Condition> list = getConditionsNotNull();

		ConditionList list2 = new ConditionList(list.size());
		list2.addAll(list);
		return list2;
	}

	@Transient
	public Condition getConditionByType(ConditionType type) {
		if (getConditions() == null)
			return null;

		for (Condition c : getConditions()) {
			if (c.getType().equals(type)) {
				return c;
			}
		}
		return null;
	}

	@Transient
	public static Condition getConditionByType(List<Condition> list, ConditionType type) {
		if (list == null)
			return null;

		for (Condition c : list) {
			if (c.getType().equals(type)) {
				return c;
			}
		}
		return null;
	}

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}

	@Transient
	public int getProductCount() {
		return productCount;
	}

	// @XmlElement (name="product")
	/**
	 * the @XmlElement might be good for the CMS messages (any other usages ?) The CMS messages will need
	 * re-factoring, so for now we just do not add the @XMLElement because that throws exception when trying
	 * to XSL transform an Update Product REPLY message from PE
	 */
	@Transient
	public Product getPrimaryProduct() {
		// for safety, check that there is exactly one primary product
		Product primary = null;
		List<Product> products = getProducts();

		for (Product p : products) {
			if (p.isCwPrimary()) {
				if (primary != null) {
					throw new IllegalStateException("More than one primary product found! (cwId = " + getId() + ")");
				}
				else {
					primary = p;
				}
			}
		}

		if (primary == null) {
			throw new IllegalStateException("No primary product! (cwId = " + getId() + ")");
		}

		return primary;
	}

	/**
	 * This is a special method used only for the XSL transform during FilemakerAssetImport. Don't use for
	 * other purposes - it removes any existing primary product from the product collection rather than just
	 * make it non-primary.
	 */
	@Transient
	public void setPrimaryProduct(Product primary) {
		// smarkoff: check for null otherwise can cause problem with CustomIDResolver which tries
		// to copy properties from one object to another not looking at Transient
		// (and for a few products primary is null, not sure why - dealing with messages so this
		// may be fine)
		if (primary == null) return;

		primary.setCwPrimary(true);
		List<Product> products = getProducts();
		for (Product p : products) {
			if (p.isCwPrimary()) {
				products.remove(p);
			}
		}
		products.add(primary);
	}

	public List<User> getAuthors() {
		return authors;
	}

	public void setAuthors(List<User> authors) {
		this.authors = authors;
	}

	@Transient
	public int getContractCount() {
		return contractCount;
	}

	@Transient
	public int getPoCount() {
		return poCount;
	}

	@Transient
	public int getPaymentCount() {
		return paymentCount;
	}

	@Transient
	public int getAUCountNotCanceled() {
		return auCountNotCanceled;
	}

	@Transient
	public int getCoverCountNotCanceled() {
		return coverCountNotCanceled;
	}

	@Transient
	public int getNonCoverCountNotCanceled() {
		return nonCoverCountNotCanceled;
	}

	@Transient
	public int getStatusNotOkCount() {
		return statusNotOkCount;
	}

	/**
	 * May return null
	 */
	@Transient
	public Date getLastWorkedOnDate() {
		return lastWorkedOnDate;
	}

	@Transient
	public ComplianceStatus getComplianceStatus() {
		if (getStatusNotOkCount() > 0) return ComplianceStatus.PROBLEM;

		if (getInteriorCWStatus().equals(CommonWorkStatus.COMPLETE)
			|| getInteriorCWStatus().equals(CommonWorkStatus.NO_PERMISSIONS_REQUIRED)) {
			return ComplianceStatus.COMPLETE;
		}
		else if (getCoverCWStatus().equals(CommonWorkStatus.COMPLETE_NO_3RD_PARTY_ASSETS)) {
			return ComplianceStatus.COMPLETE_NO_3RD_PARTY;
		}
		else if (getNonCoverCountNotCanceled() > 0) {
			return ComplianceStatus.IN_PROCESS;
		}
		else return ComplianceStatus.NOT_STARTED;
	}

	/**
	 * NOT USED - used it for excludeMedium report but I need all the Medium values returns a distinct list of
	 * mediums that are available for the CommonWork
	 *
	 * @return List
	 */
	@Transient
	public List<Medium> getMediumList() {
		List<Product> products = getProducts();
		Set<Medium> mediums = new HashSet<Medium>();

		for (Product product : products) {
			mediums.add(product.getMedium());
		}
		return new ArrayList<Medium>(mediums);
	}

	@Override
	public String toString() {
		// use getters due to the way JPA works
		return "id = " + getId()
				+ ", code = " + getCode()
				+ ", name = " + getName()
				+ ", notes = " + getNotes();
	}

	/**
	 * Base on code (XmlID).
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof CommonWork)) return false;

		CommonWork other = (CommonWork) obj;
		if (code == null) {
			if (other.code != null) return false;
		}
		else if (!code.equals(other.code)) return false;

		return true;
	}

	/**
	 * Base on code (XmlID).
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());

		return result;
	}
}

