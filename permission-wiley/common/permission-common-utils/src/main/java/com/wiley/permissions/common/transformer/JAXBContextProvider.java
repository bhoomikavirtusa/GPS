package com.wiley.permissions.common.transformer;

import java.io.StringWriter;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;



import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class JAXBContextProvider {

    private static final Log log = LogFactory.getLog(JAXBContextProvider.class);

	private static Map<Class, JAXBContext> jaxBContexts = new HashMap<Class, JAXBContext>();
	
	
    /**
     *
     * @param className  Must be non-null
     * @return  The JAXBContext for the class
     * @throws JAXBException
     */
    public static synchronized JAXBContext getContext(Class className) throws JAXBException {
		  if (jaxBContexts.get(className) == null){
			  log.trace("Creating new JAXBContext for class:" + className.getName());
			  jaxBContexts.put(className, JAXBContext.newInstance(className));
		  }
		  log.trace("Getting JAXBContext for class:" + className.getName());
		  return jaxBContexts.get(className);
    }
}
