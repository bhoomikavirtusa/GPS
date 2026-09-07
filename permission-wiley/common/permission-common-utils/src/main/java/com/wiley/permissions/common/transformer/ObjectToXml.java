package com.wiley.permissions.common.transformer;

import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ObjectToXml implements Transformer {

    private static final Log log = LogFactory.getLog(ObjectToXml.class);

    /**
     * This method is public and static so that it may also be used
     * inside unit tests or other debug code.
     *
     * @param obj  Must be non-null
     * @return  The XML for the object
     * @throws JAXBException
     */
    public static String objectToXml(Object obj) throws JAXBException, XMLStreamException {
    	return objectToXml1(obj);
		// smarkoff: Use objectToXml2() only for debugging
    	// - it prints out a lot and the formatted output does not work
        //return objectToXml2(obj);
    }

	private static String objectToXml1(Object obj)
        throws JAXBException, XMLStreamException
    {
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContextProvider.getContext(obj.getClass()); //JAXBContext.newInstance(obj.getClass());
            // throws JAXBException
        Marshaller m = context.createMarshaller();
            // throws JAXBException
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            // throws PropertyException
        m.marshal(obj, writer);
            // throws JAXBException

        return writer.toString();
    }


	@SuppressWarnings("unused")
	private static String objectToXml2(Object obj)
        throws JAXBException, XMLStreamException
    {
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContextProvider.getContext(obj.getClass()); //JAXBContext.newInstance(obj.getClass());
            // throws JAXBException

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
            // throws FactoryConfigurationError
        final XMLEventWriter ew = factory.createXMLEventWriter(writer);
	        // throws XMLStreamException
		XMLEventWriter ew2 = (XMLEventWriter) Proxy.newProxyInstance(
				ObjectToXml.class.getClassLoader(),
				new Class[] { XMLEventWriter.class }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						Object returnVal = method.invoke(ew, args);

						if (method.getName().equals("add")) {
							XMLEvent evt = (XMLEvent) args[0];
							if (evt.isStartElement()) {
								StartElement startElem = evt.asStartElement();
								log.debug("objectToXml(): startElem = " + startElem.getName());
							}
						}

						return returnVal;
					}
				});

        Marshaller m = context.createMarshaller();
            // throws JAXBException
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            // throws PropertyException
        m.marshal(obj, ew2);
            // throws JAXBException

        return writer.toString();
    }


    public ObjectToXml() {
		super();
	}

    /** Implements permissions Transformer interface method */
    @Override
	public Object transform(Object src)
	    throws TransformationException
    {
		try {
			String xml = objectToXml(src);  // throws JAXBException, XMLStreamException
			log.debug("Marshaled To:\n" + xml);
			return xml;
		}
		catch (Exception e) {
			throw new TransformationException("Could Not Transform Object", e);
		}
    }
}
