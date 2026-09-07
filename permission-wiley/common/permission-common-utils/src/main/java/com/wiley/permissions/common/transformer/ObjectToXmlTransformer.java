package com.wiley.permissions.common.transformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.lifecycle.InitialisationException;

public class ObjectToXmlTransformer
extends AbstractCustomTransformer
{
    private static final Log log = LogFactory.getLog(ObjectToXmlTransformer.class);

    public ObjectToXmlTransformer() throws InitialisationException {
    	log.debug("constructor called...");
        registerSourceType(Object.class);
        setReturnClass(String.class);
    }

    @Override
	public void initialise() throws InitialisationException {
    	log.debug("initialise(): called...");

    	if (null == transformer) {
    		transformer = new ObjectToXml();
    	}
    }
}
