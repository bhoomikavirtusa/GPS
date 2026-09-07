package com.wiley.permissions.common.transformer;

import java.io.IOException;
import java.text.DateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.ser.StdSerializerProvider;
import org.codehaus.jackson.map.util.JSONPObject;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.mule.api.lifecycle.InitialisationException;

/**
 * Converts a java object to a JSON encoded object that can be consumed by other languages such as Javascript
 * or Ruby.
 * <p/>
 * The JSON engine can be configured using the jsonConfig attribute. This is an object reference to an
 * instance of: {@link net.sf.json.JsonConfig}. This can be created as a spring bean.
 * <p/>
 * Users can configure a comma-separated list of property names to exclude or include i.e.
 * excludeProperties="address,postcode".
 * <p/>
 * The returnClass for this transformer is always java.lang.String, there is no need to set this.
 */
public class ObjectToJson implements Transformer {
	/**
	 * logger used by this class
	 */
	protected transient final Log logger = LogFactory.getLog(ObjectToJson.class);

	private ObjectMapper mapper;

	private String callback;

	public ObjectToJson() throws InitialisationException {
		if (mapper == null) {
			StdSerializerProvider sp = new StdSerializerProvider();
	        sp.setNullValueSerializer(new NullSerializer());
			mapper = new ObjectMapper();
	        mapper.setSerializerProvider(sp);
			AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
			// make deserializer use JAXB annotations (only)
			mapper.getDeserializationConfig().setAnnotationIntrospector(introspector);
			// make serializer use JAXB annotations (only)
			mapper.getSerializationConfig().setAnnotationIntrospector(introspector);
			mapper.getSerializationConfig().disable(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS);
			mapper.getSerializationConfig().setDateFormat(DateFormat.getDateInstance(DateFormat.MEDIUM));
		}
	}

	public Object transform(Object src) throws TransformationException {
		try {
			if (StringUtils.isBlank(callback))
				return getMapper().writeValueAsString(src);
			else
				return getMapper().writeValueAsString(new JSONPObject (callback, src));
		}
		catch (IOException e) {
			throw new TransformationException(e);
		}
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	public void setMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}

	public static String doTransform(Object src, String callback)
			throws TransformationException, InitialisationException
	{
		ObjectToJson transformer = new ObjectToJson();
		transformer.setCallback(callback);
		return (String) transformer.transform(src);
	}
}
