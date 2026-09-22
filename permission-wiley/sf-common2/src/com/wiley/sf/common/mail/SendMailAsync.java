package com.wiley.sf.common.mail;

import org.apache.commons.mail.Email;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Sends any email based on the Apache Commons-Email Email class
 * asynchronously.
 * 
 * @since JDK 1.5, Apache Commons-Email 1.1
 * @author smarkoff, created 7/15/2008
 * @version $Id: SendMailAsync.java,v 1.3 2008-07-16 12:26:34 smarkoff Exp $
 */
public class SendMailAsync implements Runnable {
	private static final Log log = LogFactory.getLog(SendMailAsync.class);

	/**
	 * Send the given email in a separate thread which logs any exception.
	 * 
	 * @param email  Must be non-null
	 */
	public static void sendAsync(Email email) {
	    ArgUtil.notNull(email, "email");
	    new SendMailAsync(email);
	}


	private final Email email;
	
    private SendMailAsync(Email email) {
        this.email = email;
        new Thread(this).start();
    }
    
    /** Implements Runnable interface. */
    public void run() {
        try {
            //log.debug("sending mail async...");
            email.send();
        }
        catch (Exception ex) {
            log.error("Error sending email: ", ex);
        }
    }
}
