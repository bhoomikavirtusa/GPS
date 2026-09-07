package com.wiley.permissions.common.dispatcher;

import java.util.Date;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class StdioDate extends AbstractTransformer {

	@Override
	protected Object doTransform(Object arg0, String arg1) throws TransformerException
	{
		return "** Date : " + (new Date()).toString() + "\n";
	}

}
