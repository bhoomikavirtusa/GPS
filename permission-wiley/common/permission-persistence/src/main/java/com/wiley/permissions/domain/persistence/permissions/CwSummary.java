package com.wiley.permissions.domain.persistence.permissions;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author smarkoff
 */
@Entity
@PersistenceUnit(unitName="permissions")
@Table(name="CW_SUMMARY")
public class CwSummary
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CwSummary.class);

	/*
	@OneToOne
	@JoinColumn(name="CW_ID")
	private CommonWork commonWork;
	*/

	@Id
	@Column(name="CW_ID")
	private Integer cwId;

	@Column(name="RESEARCH_STATUS", nullable=true)
	private Integer researchStatus;

	@Column(name = "CREDIT_LIST_SUBMITTED", nullable=false)
	private boolean creditListSubmitted = false;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="PHOTO_RESEARCH_DUE", nullable=true)
	private Date photoResearchDue;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="PERMISSIONS_DUE", nullable=true)
	private Date permissionsDue;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="AUTHOR_PERMISSIONS_DUE", nullable=true)
	private Date authorPermissionsDue;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="MANUSCRIPT_SUBMISSION_DEADLINE", nullable=true)
	private Date manuscriptSubmissionDeadline;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="PAGE_PROOF_SCHEDULE_DATE", nullable=true)
	private Date pageProofScheduleDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="RELEASE_TO_PRINTER_DATE", nullable=true)
	private Date releaseToPrinterDate;

	@Column(name = "RESEARCH_PROGRESS_NOTES", nullable=true)
	private String researchProgressNotes;

	public CwSummary()
	{

	}

	public Integer getCwId() {
		return cwId;
	}

	public void setCwId(Integer cwId) {
		this.cwId = cwId;
	}

	public Integer getResearchStatus() {
		return researchStatus;
	}

	public void setResearchStatus(Integer researchStatus) {
		this.researchStatus = researchStatus;
	}

	public boolean getCreditListSubmitted() {
		return creditListSubmitted;
	}

	public void setCreditListSubmitted(boolean creditListSubmitted) {
		this.creditListSubmitted = creditListSubmitted;
	}

	public Date getPhotoResearchDue() {
		return photoResearchDue;
	}

	public void setPhotoResearchDue(Date photoResearchDue) {
		this.photoResearchDue = photoResearchDue;
	}

	public Date getPermissionsDue() {
		return permissionsDue;
	}

	public void setPermissionsDue(Date permissionsDue) {
		this.permissionsDue = permissionsDue;
	}

	public Date getPageProofScheduleDate() {
		return pageProofScheduleDate;
	}

	public void setPageProofScheduleDate(Date pageProofScheduleDate) {
		this.pageProofScheduleDate = pageProofScheduleDate;
	}

	public Date getReleaseToPrinterDate() {
		return releaseToPrinterDate;
	}

	public void setReleaseToPrinterDate(Date releaseToPrinterDate) {
		this.releaseToPrinterDate = releaseToPrinterDate;
	}

	public String getResearchProgressNotes()
	{
		return researchProgressNotes;
	}

	public void setResearchProgressNotes(String researchProgressNotes)
	{
		this.researchProgressNotes = researchProgressNotes;
	}

	public Date getAuthorPermissionsDue()
	{
		return authorPermissionsDue;
	}

	public void setAuthorPermissionsDue(Date authorPermissionsDue)
	{
		this.authorPermissionsDue = authorPermissionsDue;
	}

	public Date getManuscriptSubmissionDeadline()
	{
		return manuscriptSubmissionDeadline;
	}

	public void setManuscriptSubmissionDeadline(Date manuscriptSubmissionDeadline)
	{
		this.manuscriptSubmissionDeadline = manuscriptSubmissionDeadline;
	}
}
