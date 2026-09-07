package com.wiley.permissions.common.transformer;

import java.io.InputStream;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.IOUtils;

/**
 * A transformer that will convert a JSON encoded object graph to a java object. The object type is determined
 * by the 'returnClass' attribute. Note that this transformers supports Arrays and Lists. For example, to
 * convert a JSON string to an array of org.foo.Person, set the the returnClass=[Lorg.foo.Person;.
 * <p/>
 * The JSON engine can be configured using the jsonConfig attribute. This is an object reference to an
 * instance of: {@link net.sf.json.JsonConfig}. This can be created as a spring bean.
 */
public class JsonToObject implements Transformer {

	private ObjectMapper mapper;
	private Class<?> returnClass;

	public JsonToObject(Class<?> returnClass) throws InitialisationException {
		this();
		this.returnClass = returnClass;
	}

	public JsonToObject() throws InitialisationException {
		if (mapper == null) {
			mapper = new ObjectMapper();
			AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
			// make deserializer use JAXB annotations (only)
			mapper.getDeserializationConfig().setAnnotationIntrospector(introspector);
			// make serializer use JAXB annotations (only)
			mapper.getSerializationConfig().setAnnotationIntrospector(introspector);
		}
	}

	@Override
	public Object transform(Object src)
			throws TransformationException
	{
		Object returnValue;
		InputStream is = null;

		try {
			returnValue = getMapper().readValue((String) src, returnClass);
			return returnValue;
		}
		catch (Exception e) {
			throw new TransformationException(e);
		}
		finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	public void setMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public Class<?> getReturnClass() {
		return returnClass;
	}

	public void setReturnClass(Class<?> returnClass) {
		this.returnClass = returnClass;
	}

	public static Object doTransform(String src, Class<?> returnClass)
			throws TransformationException, InitialisationException
	{
		JsonToObject transformer = new JsonToObject(returnClass);
		return transformer.transform(src);
	}
}
