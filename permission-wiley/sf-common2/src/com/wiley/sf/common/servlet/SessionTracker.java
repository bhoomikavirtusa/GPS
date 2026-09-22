package com.wiley.sf.common.servlet;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * To use, specify this class as a listener in the web.xml file.
 * Optionally also specify (in web.xml) a context parameter for
 * com.wiley.sf.common.servlet.SessionTracker.userAttributeName. The value should
 * be a user-identifying object in the session.
 *
 * Then you can access "sessionTracker.sessionInfoList" in a jsp
 * to show current session info. If you specified the userAttributeName
 * param in web.xml then you can access sessionInfo.userAttribute for
 * each member of sessionInfoList in addition to the other attributes
 * (creationTime, lastAccessedTime, and maxInactiveInterval).
 *
 * You can also access sessionTracker.maxSessionCount in a jsp.
 *
 * This class is threadsafe. Because methods are synchronized this class
 * should only be used as an administrative utility (by a small number of users)
 * and not by many simultaneous users.
 *
 * @since   JDK 1.6, Servlet API 2.5
 * @version $Id: SessionTracker.java,v 1.5 2013-03-08 19:44:46 smarkoff Exp $
 * @author  Steve Markoff
 */
public class SessionTracker implements HttpSessionListener {
	private static final Log log = LogFactory.getLog(SessionTracker.class);

    private final Set<HttpSession> sessions = new HashSet<HttpSession>();
    private String userAttributeName;
    private int maxSessionCount = 0;


    public SessionTracker() {
    }

    public synchronized void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        sessions.add(session);

        session.setAttribute("sessionTracker", this);

        // userAttributeName will be unnecessarily set many times
        // (every time a new session is created) but that's ok
        userAttributeName = event.getSession().getServletContext().getInitParameter(
        	"com.wiley.sf.common.servlet.SessionTracker.userAttributeName");

        if (sessions.size() > maxSessionCount) {
            maxSessionCount = sessions.size();
        }
    }

    public synchronized void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        sessions.remove(session);

        session.removeAttribute("sessionTracker");
    }

    public synchronized List<SessionInfo> getSessionInfoList() {
    	ArrayList<SessionInfo> list = new ArrayList<SessionInfo>(sessions.size());

    	for (HttpSession session : sessions) {
    		try {
    			list.add(new SessionInfo(
    				session.getAttribute(userAttributeName),
    				session.getCreationTime(),
    				session.getLastAccessedTime(),
    				session.getMaxInactiveInterval()));
    		}
    		catch (Exception ex) {
    		    log.debug("getSessionInfoList(): caught exception: ", ex);
    		}
    	}

    	return list;
    }

    public synchronized int getMaxSessionCount() {
        return maxSessionCount;
    }

    public class SessionInfo {  // class must be public for JSTL to read properties
    	private final Object userAttribute;
    	private final Date creationTime;
    	private final Date lastAccessedTime;
    	private final int maxInactiveInterval;  // in secs
    	private final int minsToTimeout;

    	public SessionInfo(Object userAttribute, long creationTime, long lastAccessedTime, int maxInactiveInterval) {
    		this.userAttribute = userAttribute;
    		this.creationTime = new Date(creationTime);
    		this.lastAccessedTime = new Date(lastAccessedTime);
    		this.maxInactiveInterval = maxInactiveInterval;

    		if (maxInactiveInterval < 1) {
    		    minsToTimeout = -1;
    		}
    		else {
    		    final int minSinceLastAccessedTime = (int) ((System.currentTimeMillis() - lastAccessedTime) / 1000 / 60);
    		    minsToTimeout = (maxInactiveInterval / 60) - minSinceLastAccessedTime;
    		}
    	}

    	public Object getUserAttribute() { return userAttribute; }
    	public Date getCreationTime() { return creationTime; }
    	public Date getLastAccessedTime() { return lastAccessedTime; }
    	public int getMaxInactiveInterval() { return maxInactiveInterval; }
    	public int getMinsToTimeout() { return minsToTimeout; }
    }
}
