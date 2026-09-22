/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.mail;

import org.apache.commons.mail.EmailException;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @since JDK 1.6, JUnit 4.7
 * @version $Id: SendMailAsyncTest.java,v 1.3 2009-11-20 19:54:48 smarkoff Exp $
 */
public class SendMailAsyncTest {

    @BeforeClass
    public static void initEMail() {
        EMail.setDefaultEmailHost("xmail.wiley.com");
        EMail.setEnabled(true);
    }

    @Test
	public void textEmail() throws EmailException {
		String from = "smarkoff@wiley.com";
		String to = "smarkoff@wiley.com, nmedrano@wiley.com, lnagy@wiley.com";
		String textBody = "This is a test message from sf-common2 unit test";
		String subject = "This is a test message from sf-common2 unit test";
		EMail email = EMail.createSimple(textBody, null, subject, from, to);
		SendMailAsync.sendAsync(email);
	}

}
