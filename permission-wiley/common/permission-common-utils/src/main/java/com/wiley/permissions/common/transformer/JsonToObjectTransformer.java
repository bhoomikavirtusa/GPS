package com.wiley.permissions.common.transformer;

import org.mule.api.lifecycle.InitialisationException;

public class JsonToObjectTransformer
extends AbstractCustomTransformer
{

	public JsonToObjectTransformer()
	throws InitialisationException
	{
		registerSourceType(String.class);
		setReturnClass(Object.class);
	}

	@Override
	public void initialise()
	throws InitialisationException
	{
		if (null == transformer) {
			transformer = new JsonToObject(this.getReturnClass());
		}
	}
}
