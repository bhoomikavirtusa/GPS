package com.wiley.permissions.common.transformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;


public abstract class AbstractCustomTransformer
extends AbstractTransformer
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(AbstractCustomTransformer.class);
	
	protected Transformer transformer;

	@Override
	protected Object doTransform(Object src, String encoding)
	throws TransformerException
	{
		try
		{
			initialise();

			return transformer.transform(src);
		}
		catch (TransformationException e)
		{
			throw new TransformerException(this, e);
		}
		catch (InitialisationException e)
		{
			throw new TransformerException(this, e);
		}
	}
}
