package com.wiley.permissions.common.transformer;

import org.mule.api.lifecycle.InitialisationException;

public class ObjectToJsonTransformer
extends AbstractCustomTransformer
{
    public ObjectToJsonTransformer() throws InitialisationException {
        registerSourceType(Object.class);
        setReturnClass(String.class);
    }

    @Override
	public void initialise() throws InitialisationException {
    	if (null == transformer) {
    		transformer = new ObjectToJson();
    	}
    }
}
