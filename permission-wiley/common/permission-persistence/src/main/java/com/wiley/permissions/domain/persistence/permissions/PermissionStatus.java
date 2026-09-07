package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "PERMISSION_STATUS")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class PermissionStatus
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// These values match what is in the database.
	public static PermissionStatus
		MISSING_INFO = new PermissionStatus("missingInfo", "Missing Info"),
		NO_SOURCE = new PermissionStatus("noSource", "Not Requested - No Source"),
		// --- UNREQUESTED
	    UNREQUESTED = new PermissionStatus("unrequested", "Not Requested"),
		NOT_REQUESTED_BE_WORKFORHIRE = new PermissionStatus("notRequestedBeWorkForHire", "Not Requested - Will be Work for Hire"),
		NOT_REQUESTED_ROYALTY_FREE = new PermissionStatus("notRequestedBeRoyaltyFree", "Not Requested - Will be Royalty Free"),
		// --- FORM SENT
		FORM_SENT = new PermissionStatus("formSent", "Request Sent"),
		FORM_SENT_BE_WORKFORHIRE = new PermissionStatus("formSentBeWorkForHire", "Request Sent - Will be Work for Hire"),
		FORM_SENT_BE_ROYALTYFREE = new PermissionStatus("formSentBeRoyaltyFree", "Request Sent - Will be Royalty Free"),
		// Not storing in DB but handling in UI based on PURCHASE_ORDER table
		FORM_WAITING_ON_INVOICE = new PermissionStatus("formWaitingOnInvoice", "Waiting on Invoice"), // Added for DM-1606
		// --- INSUFFICIENT
	    CONTRACT_INSUFFICIENT = new PermissionStatus("contractInsufficient", "Granted - Insufficient"),
		GRANTED_INSUFFICIENT_APPROVED = new PermissionStatus("grantedInsufficientApproved", "Granted - Insufficient Approved"),
		GRANTED_INSUFFICIENT_PENDING = new PermissionStatus("grantedInsufficientPending", "Granted - Insufficient Pending"),

		// --- OUT OF COMPLIANCE
		OUT_OF_COMPLIANCE_PRINT_RUN = new PermissionStatus("outOfCompliancePrintRun", "Out of Compliance - Print Run"),
		OUT_OF_COMPLIANCE_EXPIRED = new PermissionStatus("outOfComplianceExpired", "Out of Compliance - Expired"),

   		// -- GRANTED
		GRANTED_PUBLIC_DOMAIN = new PermissionStatus("grantedPublicDomain", "Granted - Public Domain"),
		GRANTED_WILEY_OWNED = new PermissionStatus("grantedWileyOwned", "Granted - Wiley Owned"),
		GRANTED_AUTHOR_CREATED = new PermissionStatus("grantedAuthorCreated", "Granted - Author Created/Owned"),
		GRANTED_FAIR_USE = new PermissionStatus("grantedFairUse", "Granted - Fair Use"),

		GRANTED_RF_UNLIMITED = new PermissionStatus("grantedRFUnlimited", "Granted - RF Unlimited Seats & Print"),
		GRANTED_ROYALTY_FREE_LIMITED_PRINT = new PermissionStatus("grantedRoyaltyFreeLimitedPrint", "Granted - RF Limited Print"),
		GRANTED_ROYALTRY_FREE_LIMITED_SEATS = new PermissionStatus("grantedRoyaltyFreeLimitedSeats", "Granted - RF Limited Seats"),

		GRANTED = new PermissionStatus("granted", "Granted"),
		GRANTED_LIMITED = new PermissionStatus("grantedLimited", "Granted - Limited"),
		GRANTED_LIMITED_PRINT = new PermissionStatus("grantedLimitedPrint", "Granted - Limited Print Run"),
		GRANTED_WORK_FOR_HIRE = new PermissionStatus("grantedWorkForHire", "Granted - Work For Hire"),
		GRANTED_STM_GUIDELINES = new PermissionStatus("grantedStmGuidelines", "Granted - STM Guidelines"),

   		// -- CANCELED
	    CANCELED = new PermissionStatus("canceled", "Cancelled"),
   		REMOVED = new PermissionStatus("removed", "Removed"),
		REPLACED = new PermissionStatus("replaced", "Replaced"),

	    ILLEGAL = new PermissionStatus("illegal", "Error"),

	    AMENDMENT_NEEDED = new PermissionStatus("amendmentNeeded", "Limitation of Liability Needed"),
		AMENDMENT_SENT = new PermissionStatus("amendmentSent", "Limitation of Liability Sent"),

		//sandhya adding for manage deals, including all the 7 conditions
		GRANTED_RF_LIMITED_PRINT_SEATS = new PermissionStatus("grantedRFlimitedprintrunseat", "Royalty Free - Limited Print Run & Seats"),
		GRANTED_RF_LIMITED_PRINTRUN = new PermissionStatus("grantedRFlimitedprintrun", "Royalty Free - Limited Print Run"),
		GRANTED_RF_LIMITED_PRINT_SUBLIC = new PermissionStatus("grantedRFlimitedprintrunSubLic", "Royalty Free  - Limited Print Run & No Sublicensing"),
		GRANTED_RF_LIMITED = new PermissionStatus("grantedRFlimitedSeats", "Royalty Free - Limited Seats"),
		GRANTED_RF_LIMITED_SEATS_SUBLIC = new PermissionStatus("grantedRFlimitedSeatsSubLic", "Royalty Free - Limited Seats & No Sublicensing"),
	    GRANTED_RF_UNLIMITED_PRINT_SUBLIC = new PermissionStatus("grantedRFunlimitedprintrunSubLic", "Royalty Free - Unlimited Print Run, Seats & Sublicensing"),
	    GRANTED_RF_LIMITED_SUBLIC = new PermissionStatus("grantedRFlimitedSubLic", "Royalty Free - No Sublicensing"),
	    GRANTED_RF_LIMITED_PRINT_SWAR_SUBLIC = new PermissionStatus("grantedRFlimitedprintseatSubLic", "Royalty Free - Limited Print Run, Seats & No Sublicensing"),



		// -- LEGACY
		LEGACY_UPLOAD_REVIEWED_UNKNOWN = new PermissionStatus("legacyUploadReviewedUnknown", "Legacy Upload - Reviewed Unknown"),
		LEGACY_UPLOAD_AUTHOR_PROVIDED = new PermissionStatus("legacyUploadAuthorProvided", "Legacy Upload - Author Provided/Unknown"),

		MIGRATED_FROM_AUSTRALIA = new PermissionStatus("grantedMigratedFromAustralia", "Migrated from Australia ePermissions"),
		MIGRATED_FROM_FILEMAKER = new PermissionStatus("grantedMigratedFromFilemaker", "Migrated from Filemaker"),
		//Start: Added a new status for DM-374
		GRANTED_MANAGER_APPROVED = new PermissionStatus("grantedAuthorSupplied", "Granted - Author Supplied");
		//End: Added a new status for DM-374

	public static PermissionStatus [] ALL_PERMISSION_STATUS_ARRAY = {
		MISSING_INFO, NO_SOURCE, UNREQUESTED, NOT_REQUESTED_BE_WORKFORHIRE, NOT_REQUESTED_ROYALTY_FREE,
		FORM_SENT, FORM_SENT_BE_WORKFORHIRE, FORM_SENT_BE_ROYALTYFREE,FORM_WAITING_ON_INVOICE,
		CONTRACT_INSUFFICIENT, GRANTED_INSUFFICIENT_APPROVED, GRANTED_INSUFFICIENT_PENDING,
		OUT_OF_COMPLIANCE_PRINT_RUN, OUT_OF_COMPLIANCE_EXPIRED,
		GRANTED_PUBLIC_DOMAIN, GRANTED_WILEY_OWNED, GRANTED_AUTHOR_CREATED, GRANTED_FAIR_USE,
		GRANTED_RF_UNLIMITED, GRANTED_ROYALTY_FREE_LIMITED_PRINT, GRANTED_ROYALTRY_FREE_LIMITED_SEATS,
		GRANTED, GRANTED_LIMITED, GRANTED_LIMITED_PRINT,
		GRANTED_WORK_FOR_HIRE, GRANTED_STM_GUIDELINES,
		CANCELED, REMOVED, REPLACED, ILLEGAL, AMENDMENT_NEEDED, AMENDMENT_SENT,
		LEGACY_UPLOAD_REVIEWED_UNKNOWN, LEGACY_UPLOAD_AUTHOR_PROVIDED,
		MIGRATED_FROM_AUSTRALIA, MIGRATED_FROM_FILEMAKER,
		GRANTED_RF_LIMITED_PRINT_SEATS, GRANTED_RF_LIMITED_PRINTRUN, GRANTED_RF_LIMITED_PRINT_SUBLIC,
		GRANTED_RF_LIMITED, GRANTED_RF_LIMITED_SEATS_SUBLIC,GRANTED_RF_UNLIMITED_PRINT_SUBLIC,
		GRANTED_RF_LIMITED_SUBLIC, GRANTED_RF_LIMITED_PRINT_SWAR_SUBLIC, GRANTED_MANAGER_APPROVED
	};

	public static PermissionStatus [] COMPLETE_GROUP = {
		CANCELED, REMOVED, REPLACED, GRANTED_PUBLIC_DOMAIN, GRANTED_WILEY_OWNED, GRANTED_AUTHOR_CREATED,
		GRANTED_FAIR_USE, GRANTED_RF_UNLIMITED,
		GRANTED, GRANTED_LIMITED, GRANTED_LIMITED_PRINT,
		GRANTED_INSUFFICIENT_APPROVED, GRANTED_STM_GUIDELINES,
		GRANTED_WORK_FOR_HIRE, GRANTED_ROYALTY_FREE_LIMITED_PRINT, GRANTED_ROYALTRY_FREE_LIMITED_SEATS,
		LEGACY_UPLOAD_REVIEWED_UNKNOWN, LEGACY_UPLOAD_AUTHOR_PROVIDED, MIGRATED_FROM_AUSTRALIA,
		GRANTED_RF_LIMITED_PRINT_SEATS, GRANTED_RF_LIMITED_PRINTRUN, GRANTED_RF_LIMITED_PRINT_SUBLIC,
		GRANTED_RF_LIMITED, GRANTED_RF_LIMITED_SEATS_SUBLIC,GRANTED_RF_UNLIMITED_PRINT_SUBLIC,
		GRANTED_RF_LIMITED_SUBLIC, GRANTED_RF_LIMITED_PRINT_SWAR_SUBLIC, GRANTED_MANAGER_APPROVED
	};

	public static PermissionStatus [] PROBLEM_GROUP = {
		ILLEGAL, OUT_OF_COMPLIANCE_PRINT_RUN, OUT_OF_COMPLIANCE_EXPIRED, CONTRACT_INSUFFICIENT, MISSING_INFO
	};

	// Doesn't seem like we are using this
	public static PermissionStatus [] WORKFLOW_GROUP = {
		MISSING_INFO, NO_SOURCE, UNREQUESTED, FORM_SENT,
		CANCELED, ILLEGAL, AMENDMENT_NEEDED,
		AMENDMENT_SENT, NOT_REQUESTED_BE_WORKFORHIRE, NOT_REQUESTED_ROYALTY_FREE, FORM_SENT, FORM_SENT_BE_WORKFORHIRE,
		FORM_SENT_BE_ROYALTYFREE, GRANTED_INSUFFICIENT_PENDING,FORM_WAITING_ON_INVOICE
	};

	public static PermissionStatus [] IN_PROGRESS_GROUP = {
		AMENDMENT_NEEDED, AMENDMENT_SENT, FORM_SENT, FORM_SENT_BE_WORKFORHIRE, FORM_SENT_BE_ROYALTYFREE, GRANTED_INSUFFICIENT_PENDING, FORM_WAITING_ON_INVOICE
	};

	public static PermissionStatus [] MIGRATED_GROUP = {
		MIGRATED_FROM_AUSTRALIA, MIGRATED_FROM_FILEMAKER
	};

	public static PermissionStatus [] GRANTED_GROUP = {
		GRANTED_PUBLIC_DOMAIN, GRANTED_WILEY_OWNED, GRANTED_AUTHOR_CREATED, GRANTED_FAIR_USE,
		GRANTED_RF_UNLIMITED, GRANTED_ROYALTY_FREE_LIMITED_PRINT, GRANTED_ROYALTRY_FREE_LIMITED_SEATS,
		GRANTED, GRANTED_LIMITED, GRANTED_LIMITED_PRINT,
		GRANTED_WORK_FOR_HIRE, GRANTED_STM_GUIDELINES, GRANTED_INSUFFICIENT_APPROVED,
		GRANTED_RF_LIMITED_PRINT_SEATS, GRANTED_RF_LIMITED_PRINTRUN, GRANTED_RF_LIMITED_PRINT_SUBLIC,
		GRANTED_RF_LIMITED, GRANTED_RF_LIMITED_SEATS_SUBLIC,GRANTED_RF_UNLIMITED_PRINT_SUBLIC,
		GRANTED_RF_LIMITED_SUBLIC, GRANTED_RF_LIMITED_PRINT_SWAR_SUBLIC, GRANTED_MANAGER_APPROVED
	};

	public static PermissionStatus [] GRANTED_ROYALTY_FREE_LIMITED_GROUP = {GRANTED_ROYALTY_FREE_LIMITED_PRINT, GRANTED_ROYALTRY_FREE_LIMITED_SEATS};

	public static PermissionStatus [] UNREQUESTED_GROUP = {UNREQUESTED, NOT_REQUESTED_BE_WORKFORHIRE, NOT_REQUESTED_ROYALTY_FREE};

	public static PermissionStatus [] INSUFFICIENT_GROUP = {CONTRACT_INSUFFICIENT, GRANTED_INSUFFICIENT_APPROVED, GRANTED_INSUFFICIENT_PENDING};

	public static PermissionStatus [] FORM_SENT_GROUP = {FORM_SENT, FORM_SENT_BE_WORKFORHIRE, FORM_SENT_BE_ROYALTYFREE,FORM_WAITING_ON_INVOICE};

	public static PermissionStatus [] MIGRATED_GROUP_FORM_SENT = {MIGRATED_FROM_AUSTRALIA, MIGRATED_FROM_FILEMAKER, FORM_SENT, FORM_SENT_BE_WORKFORHIRE, FORM_SENT_BE_ROYALTYFREE};

	public static PermissionStatus [] OUT_OF_COMPLIANCE_GROUP = {OUT_OF_COMPLIANCE_PRINT_RUN, OUT_OF_COMPLIANCE_EXPIRED};

	public static PermissionStatus [] CANCELED_GROUP = {CANCELED, REPLACED, REMOVED};

	public static PermissionStatus [] LEGACY_UPLOAD_GROUP = {LEGACY_UPLOAD_REVIEWED_UNKNOWN, LEGACY_UPLOAD_AUTHOR_PROVIDED};

	// #IM206268 - Assets list issue - Start
		//public static PermissionStatus [] REREQUEST_GROUP = { CONTRACT_INSUFFICIENT, GRANTED_INSUFFICIENT_APPROVED,
		//	GRANTED_ROYALTY_FREE_LIMITED_PRINT, GRANTED_RF_UNLIMITED, GRANTED_LIMITED,
		//	GRANTED_LIMITED_PRINT, OUT_OF_COMPLIANCE_EXPIRED, OUT_OF_COMPLIANCE_PRINT_RUN
		//};

		public static PermissionStatus [] REREQUEST_GROUP = { MIGRATED_FROM_AUSTRALIA, MIGRATED_FROM_FILEMAKER, UNREQUESTED, NOT_REQUESTED_BE_WORKFORHIRE, NOT_REQUESTED_ROYALTY_FREE, CONTRACT_INSUFFICIENT, GRANTED_INSUFFICIENT_APPROVED,
			GRANTED_ROYALTY_FREE_LIMITED_PRINT, GRANTED_RF_UNLIMITED, GRANTED_LIMITED,
			GRANTED_LIMITED_PRINT, OUT_OF_COMPLIANCE_EXPIRED, OUT_OF_COMPLIANCE_PRINT_RUN,
				GRANTED_RF_LIMITED_PRINT_SEATS, GRANTED_RF_LIMITED_PRINTRUN, GRANTED_RF_LIMITED_PRINT_SUBLIC,
		GRANTED_RF_LIMITED, GRANTED_RF_LIMITED_SEATS_SUBLIC,GRANTED_RF_UNLIMITED_PRINT_SUBLIC,
		GRANTED_RF_LIMITED_SUBLIC, GRANTED_RF_LIMITED_PRINT_SWAR_SUBLIC
		};
		// #IM206268 - Assets list issue - End

	public static PermissionStatus [] GRANTED_LIMITED_PRINT_RUN_GROUP = { GRANTED_LIMITED_PRINT,
		GRANTED_ROYALTY_FREE_LIMITED_PRINT
	};

	/**
	 * Throws an exception if the code is not valid.
	 */
	public static PermissionStatus getPermissionStatusForCode(String code) {
		for (PermissionStatus ps : ALL_PERMISSION_STATUS_ARRAY) {
			if (ps.getCode().equals(code)) return ps;
		}

		throw new IllegalArgumentException("Invalid Permission Status code: " + code);
	}

	@Id
	@Column(name = "CODE", nullable = false, length = 50)
	@MaterializationKey
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description = null;

	public PermissionStatus() {
		super();
	}

	private PermissionStatus(String code, String description) {
		super();
		this.code = code;
		this.description = description;
	}

	@XmlElement
	@XmlID
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "code = " + code + ", description = " + description;
	}

	@Transient
	public static String arrayToString (PermissionStatus [] statuses) {
		StringBuilder sb = new StringBuilder();
		for (PermissionStatus status : statuses) {
			if (sb.length() > 0) sb.append(",");
			sb.append("'").append(status.getCode()).append("'");
		}

		return sb.toString();
	}

	@Transient
	public static List<PermissionStatus> getStatuses (PermissionStatus[] statuses) {
		return Arrays.asList(statuses);
	}

	@Transient
	public static List<String> getCodes (PermissionStatus[] statuses) {
		return getCodesList(Arrays.asList(statuses));
	}

	@Transient
	public static String getSQLClause (PermissionStatus[] statuses) {
		return "'" + StringUtils.join(getCodesList(Arrays.asList(statuses)), "','") + "'";
	}

	@Transient
	/**
	 * This method assigns a point value to a status code - used in multi-source logic.
	 * Lower values take precedent.
	 */
	public int getStatusRating() {
		// don't worry about NO_SOURCE and MISSING_INFO because in order to have a source
		// we'd have to get past these statuses

		if (this.equals(PermissionStatus.ILLEGAL)) return 1;
		if (isOutOfCompliance(this)) return 2;
		if (isInsufficient(this)) return 3;
		if (isUnrequested(this)) return 4;
		if (isFormSent(this)) return 5;
		if (this.equals(PermissionStatus.AMENDMENT_NEEDED)) return 6;
		if (this.equals(PermissionStatus.AMENDMENT_SENT)) return 7;
		if (isGranted(this)) return 8;

		// return largest possible if status is not rated
		return 9;
	}

	@Transient
	public static boolean isGranted(String status) {
		return isGranted(new PermissionStatus (status, null));
	}

	@Transient
	public static boolean isGranted(PermissionStatus status) {
		return PermissionStatus.getStatuses(GRANTED_GROUP).contains(status);
	}

	@Transient
	public static boolean isLegacy(PermissionStatus status) {
		return PermissionStatus.getStatuses(LEGACY_UPLOAD_GROUP).contains(status);
	}

	@Transient
	public static boolean isCanceled(String status) {
		return isCanceled(new PermissionStatus (status, null));
	}

	@Transient
	public static boolean isCanceled(PermissionStatus status) {
		return PermissionStatus.getStatuses(CANCELED_GROUP).contains(status);
	}

	@Transient
	public static boolean isOutOfCompliance(String status) {
		return isOutOfCompliance(new PermissionStatus(status, null));
	}

	@Transient
	public static boolean isOutOfCompliance(PermissionStatus status) {
		return PermissionStatus.getStatuses(OUT_OF_COMPLIANCE_GROUP).contains(status);
	}

	@Transient
	public static boolean isComplete(String status) {
		return isComplete(new PermissionStatus (status, null));
	}

	@Transient
	public static boolean isComplete(PermissionStatus status) {
		return PermissionStatus.getStatuses(COMPLETE_GROUP).contains(status);
	}

	@Transient
	public static boolean isUnrequested(PermissionStatus status) {
		return Arrays.asList(UNREQUESTED_GROUP).contains(status);
	}

	@Transient
	public static boolean isFormSent(String status) {
		return isFormSent(new PermissionStatus (status, null));
	}

	@Transient
	public static boolean isFormSent(PermissionStatus status) {
		return PermissionStatus.getStatuses(FORM_SENT_GROUP).contains(status);
	}

	public static boolean isInsufficient(PermissionStatus status) {
		return Arrays.asList(INSUFFICIENT_GROUP).contains(status);
	}

	@Transient
	private static List<String> getCodesList (List<PermissionStatus> statuses) {
		List<String> codes = new ArrayList<String>();
		for (PermissionStatus status : statuses) {
			codes.add(status.getCode());
		}
		return codes;
	}

	// base hashCode() and equals() on just code

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;

		// special logic to allow matching with String (code) as well as PermissionStatus
		if (obj instanceof String) {
			String s = (String) obj;
			return StringUtils.equals(code, s);
		}

		if (obj instanceof PermissionStatus) {
			PermissionStatus other = (PermissionStatus) obj;
			return StringUtils.equals(code, other.getCode());
		}

		return false;
	}
}
