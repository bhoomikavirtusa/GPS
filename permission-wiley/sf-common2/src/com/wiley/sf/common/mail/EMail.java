package com.wiley.sf.common.mail;

import java.util.ArrayList;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.lang.StringUtil;

/**
 * Email class that inherits from Apache Commons-Email class.
 * Provides extra functionality of having defaultEmailHost
 * and enabled properties (which are expected to be initialized
 * by Spring Framework, init Servlet, etc).
 * Also provides logging and a convenience factory method for
 * simple emails.
 * 
 * @since JDK 1.5, Apache Commons-Email 1.1
 * @author smarkoff, created 7/14/2008
 * @version $Id: EMail.java,v 1.1 2008-07-15 16:32:17 smarkoff Exp $
 */
public class EMail extends HtmlEmail {
	private static final Log log = LogFactory.getLog (EMail.class);
	
	private static String defaultEmailHost = null;
	private static boolean enabled = true;
	
	/** Should be called by Spring Framework, init Servlet, etc. */
	public static void setDefaultEmailHost(String s) {
	    defaultEmailHost = s;
	}
	
	/** Should be called by Spring Framework, init Servlet, etc. */
	public static void setEnabled(boolean b) {
	    enabled = b;
	}

	/**
	 * Create a simple email with no attachments.
	 * The email can have an html body without any embedded images.
	 * To create more fancy emails, see the full Apache Commons Email API.
	 * 
	 * @param textBody  You should always have a non-null textBody
	 * @param htmlBody  You may also have an htmlBody for clients that accept HTML email
	 * @param subject   Should be non-empty
	 * @param from      Should be non-empty
	 * @param to        May contain commas or spaces to indicate a list of addresses
	 * @throws EmailException
	 */
	public static EMail createSimple(String textBody, String htmlBody,
	        String subject, String from, String to)
	    throws EmailException
	{
	    String [] toArray = StringUtil.stringToArray(to, ", ");
	    
	    EMail email = new EMail();
	    email.setSubject(subject);
	    email.setFrom(from);
	    for (int i = 0; i < toArray.length; i++) {
	        email.addTo(toArray[i]);
	    }
	    email.setTextMsg(textBody);
	    if (htmlBody != null)  email.setHtmlMsg(htmlBody);

	    return email;
	}
	
	
	// toList is just temporary until Apache adds a getTo() API
	private ArrayList<String> toList = new ArrayList<String>();

    public EMail() {
        super();
        setHostName(defaultEmailHost);
    }
    
    /**
     * This method is just temporary until Apache adds a getTo()
     * method. (Request filed as bug EMAIL-81.)
     */
    @Override
    public Email addTo(String email) throws EmailException {
        toList.add(email);
        return super.addTo(email);
    }

    @Override
    public String send() throws EmailException {
        if (enabled) {
            log.debug("Sending email: to: "
                + StringUtil.collectionToString(toList, "; ")
                + "  subject: " + getSubject());
            return super.send();
        }
        else {
            log.debug ("Email disabled - not sending email.");
            return null;
        }
    }
}
