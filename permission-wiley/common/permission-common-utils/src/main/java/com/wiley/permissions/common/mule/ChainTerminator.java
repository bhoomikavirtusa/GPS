package com.wiley.permissions.common.mule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;


/**
 * This is simply used to make sure that a service doesn't reply to the caller
 * if we don't want it to.
 *
 * @author ttidwell
 */
public class ChainTerminator
implements Callable
{
	private final static Log log = LogFactory.getLog(ChainTerminator.class);

	public Object onCall(MuleEventContext eventContext)
	{
		log.debug("onCall(): Terminating Event.");

		eventContext.setStopFurtherProcessing(true);

		return null;
	}
}
