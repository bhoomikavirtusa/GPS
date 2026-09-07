package com.wiley.permissions.common.transformer;


public interface Transformer {

	public Object transform(Object src) throws TransformationException;

}
