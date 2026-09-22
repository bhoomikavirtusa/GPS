/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.mail;

import org.apache.commons.mail.EmailException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
//import static org.junit.Assert.*;

/**
 * @since JDK 1.5, JUnit 4.4
 * @version $Id: EMailTest.java,v 1.5 2011-06-22 22:22:40 smarkoff Exp $
 */
public class EMailTest {

    @BeforeClass
    public static void initEMail() {
        EMail.setDefaultEmailHost("xmail.wiley.com");
        EMail.setEnabled(true);
    }

    // Ignore by default so doesn't send email during ant build
    @Ignore
    @Test
	public void textEmail() throws EmailException {
		String from = "smarkoff@wiley.com";
		//String to = "smarkoff@wiley.com, nmedrano@wiley.com, lnagy@wiley.com";
		String to = "smarkoff@wiley.com";
		String textBody = "This is a test message from EMailTest";
		String subject = "This is a test message";
		EMail.createSimple(textBody, null, subject, from, to).send();
	}

    // Currently htmlEmail is sending email and html is part of the
    // email but ends up as an attachment instead of the body - this
    // is a filed bug (EMAIL-80) in Apache Commons-Email 1.1 and
    // hopefully will be fixed soon

    // Ignore by default so doesn't send email during ant build
    @Ignore
    @Test
	public void htmlEmail() throws EmailException {
		String from = "smarkoff@wiley.com";
		String to = "smarkoff@wiley.com";
		String textBody = "This is a test message from EMailTest";
		StringBuilder htmlBody = new StringBuilder();
		htmlBody.append("<html><body><p>");
		htmlBody.append("This is a test message from <b>EMailTest<b><br />");
		htmlBody.append("<a href=\"www.wiley.com\">Go to wiley.com</a>");
		htmlBody.append("</p></body></html>");
		String subject = "This is a test HTML message";
		EMail.createSimple(textBody, htmlBody.toString(), subject, from, to).send();
	}

}
