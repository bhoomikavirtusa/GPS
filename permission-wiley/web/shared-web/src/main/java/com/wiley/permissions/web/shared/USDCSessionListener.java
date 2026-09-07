package com.wiley.permissions.web.shared;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.security.web.UserPrincipal;

/**
 *
 * @author smarkoff
 */
public class USDCSessionListener implements HttpSessionListener
{
	private static final Log log = LogFactory.getLog(USDCSessionListener.class);

	/** Implements HttpSessionListener interface. */
	public void sessionCreated(HttpSessionEvent se) {
		HttpSession session = se.getSession();
		//if (!checkSession(session, 0)) {
			new USDCThread(session);
		//}
	}

	/** Implements HttpSessionListener interface. */
	public void sessionDestroyed(HttpSessionEvent se) {
		// do nothing
	}

	public static boolean checkSession(HttpSession session, int numSecs) {
		UserSession userSession;
		try {
			userSession = (UserSession) session.getAttribute(PermUserContext.USER_SESSION_NAME);
		}
		catch (IllegalStateException ex) {
			log.debug("checkSession(" + numSecs + " secs): session invalidated");
			return true;
		}

		if (userSession == null) {
			log.debug("checkSession(" + numSecs + " secs): userSession not set yet");
		}
		else {
			UserPrincipal currentUser = userSession.getCurrentUser();
			if (currentUser == null) {
				log.debug("checkSession(" + numSecs + " secs): currentUser not set yet");
			}
			else {
				log.debug("checkSession(" + numSecs + " secs): currentUser uniqueName = " + currentUser.getUniqueName());
				if (currentUser.getUniqueName().equals("usdchosting@wiley.com")) {
					session.setMaxInactiveInterval(10 * 60);  // 10 minutes
				}
				return true;
			}
		}

		return false;
	}
}

class USDCThread extends Thread {
	private final HttpSession session;

	public USDCThread(HttpSession session) {
		this.session = session;
		setPriority(Thread.MIN_PRIORITY);
		setDaemon(true);
		start();
	}

	/** Implements parent abstract method */
	@Override
	public void run() {
		try { Thread.sleep(20 * 1000); } catch (InterruptedException ex) { }

		USDCSessionListener.checkSession(session, 20);
	}
}
