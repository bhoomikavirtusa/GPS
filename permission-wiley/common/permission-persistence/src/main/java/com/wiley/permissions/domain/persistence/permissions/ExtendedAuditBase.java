package com.wiley.permissions.domain.persistence.permissions;


import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.security.web.ThreadLocalUser;
import com.wiley.permissions.security.web.UserPrincipal;

@MappedSuperclass
public abstract class ExtendedAuditBase
extends AuditBase
{
	private static final long serialVersionUID = 1L;

	private final static Log log = LogFactory.getLog(ExtendedAuditBase.class);

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "LAST_UPDATED_USER_ID")
	private User lastUpdatedUser;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "CREATED_USER_ID")
	private User createdUser;

	public ExtendedAuditBase() {
		super();
	}

	/**
	 * smarkoff: About PrePersist / PreUpdate: (testing with Hibernate 3.6.10)
	 * If you have a base class with a method called prePersist() annotated with @PrePersist
	 * and then a child class overrides this method AND has the same annotation, only the child
	 * method will be called, so the child generally should call super.prePersist().
	 * 
	 * Would like to retest at some point:
	 * What if you have base class method called prePersist and annotated,
	 * and child class with a diffrent method name, also annotated.
	 * And child class method does not call super method.
	 * Would both annotated methods be called or just the child?
	 */
	@Override
	@PrePersist
	public void prePersist() {
		log.debug("prePersist() called for class " + getClass().getSimpleName());
		reconcileUsers();
		updateTimestamps();
		super.prePersist();
	}

	@Override
	@PreUpdate
	public void preUpdate() {
		//log.debug("preUpdate() called for class " + getClass().getSimpleName());
		reconcileUsers();
		updateTimestamps();
		super.preUpdate();
	}

	/**
	 * We're basically using this to make sure that there is a last updated user
	 * in the audit trail.  It tries to find one from the transaction resources
	 * and if it can't, it defaults to the system permissions user.
	 */
	public void reconcileUsers() {
		if (lastUpdatedUser == null || createdUser == null) {
			try {
				User user = new User();
				UserPrincipal userPrincipal = (UserPrincipal)ThreadLocalUser.get();
				Integer userId = null;
				if (null != userPrincipal) {
					userId = userPrincipal.getId();
				}
				log.debug("reconcileUsers(): ThreadLocalUser.get() = " + userId);
				log.debug("reconcileUsers(): currentThread.id = " + Thread.currentThread().getId());

				if (userId == null) {
					user.setId(1);  // Permissions System user
				}
				else {
					user.setId(userId);
				}

				if (lastUpdatedUser == null) setLastUpdatedUser(user);
				if (createdUser == null) setCreatedUser(user);
			}
			catch (Exception e) {
				log.error("Error reconciling user: ", e);
			}
		}
	}

	/**
	 * adds today timestamp to dates that have empty timestamps.
	 * Note for Contract.date this is adjusted before getting to this method in
	 * in ContractServiceImpl.adjustContractAndOrPODate() (and there is additional logic there).
	 */
	public void updateTimestamps() {
        Field[] fields = this.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Temporal timestampAnnotation = fields[i].getAnnotation(Temporal.class);
            if (timestampAnnotation != null) {
                try{
                    fields[i].setAccessible(true);
                    Object value = fields[i].get(this);
                    if (value == null) {
                    	// if null value, do nothing
                    } else {
                    	if (value instanceof Date) {
                    		Date date = (Date) value;
                    		Calendar cCal = Calendar.getInstance();
                    		cCal.setTime(date);
                    		// if no timestamp, we set the time to be todays time
                    		if (cCal.get(Calendar.HOUR_OF_DAY) == 0 && cCal.get(Calendar.MINUTE) == 0 && cCal.get(Calendar.SECOND) == 0) {
                    			Calendar today = Calendar.getInstance();
                    			cCal.set(Calendar.HOUR_OF_DAY, today.get(Calendar.HOUR_OF_DAY));
                    			cCal.set(Calendar.MINUTE, today.get(Calendar.MINUTE));
                    			cCal.set(Calendar.SECOND, today.get(Calendar.SECOND));
                        		fields[i].set(this, cCal.getTime());
                    		} else {
                    			// the date has a timestamp
                    		}
                    	} else {
                    		log.debug("Incorrect Timestamp annotation on a non Date field");
                    	}
                    }
                } catch (Exception ex) {
                	log.error("Error updating timestamps: ", ex);
                }
            }
        }
	}

	public User getLastUpdatedUser() {
		return lastUpdatedUser;
	}

	public void setLastUpdatedUser(User lastUpdatedUser) {
		this.lastUpdatedUser = lastUpdatedUser;
	}

	public User getCreatedUser() {
		return createdUser;
	}

	public void setCreatedUser(User createdUser) {
		this.createdUser = createdUser;
	}

	@Override
	public String toString() {
		// use getters due to the way JPA works
		return super.toString();
		// don't include user because causes lazy-load issues when debug.jspx used
		    //+ ", lastUpdatedUser = " + getLastUpdatedUser();
	}
}
