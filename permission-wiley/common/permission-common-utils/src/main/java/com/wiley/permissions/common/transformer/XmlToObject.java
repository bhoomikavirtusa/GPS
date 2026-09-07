package com.wiley.permissions.common.transformer;

import java.io.InputStream;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

import com.sun.xml.bind.IDResolver;
import com.wiley.sf.common.xml.bind.CustomIDResolver;


public class XmlToObject
implements Transformer
{
	private static final Log log = LogFactory.getLog(XmlToObject.class);

	private Class<?> returnClass;

    public XmlToObject() {
	}

	public XmlToObject(Class<?> returnClass) {
		this.returnClass = returnClass;
	}

	/** Implements JavaTransformer interface method */
	@Override
	public Object transform(Object src)
	    throws TransformationException
	{
		try {
			log.debug("transform(): about to unmarshall this XML to object:\n" + src);
			return xmlToObject2(returnClass, (String) src);
		}
		catch (Exception e) {
			log.error("Error Transforming", e);
			throw new TransformationException("Could Not Transform XML To Object", e);
		}
	}

	/**
	 * This method is good for debug and also is used in JUNIT tests
	 * @param clazz
	 * @param xml
	 * @return
	 * @throws JAXBException
	 * @throws ClassNotFoundException
	 */
	public static Object xmlToObject(Class<?> clazz, String xml) throws JAXBException, ClassNotFoundException
	{
		JAXBContext jaxbContext = JAXBContextProvider.getContext(clazz); //JAXBContext.newInstance(clazz);
			// throws JAXBException
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();  // throws JAXBException
		// we need the customIDResolver because of same IDREF values and IDREFs missing full objects
		unmarshaller.setProperty(IDResolver.class.getName(), new CustomIDResolver("com.wiley.permissions.domain.persistence.permissions.Bundle"));
			// throws JAXBException, ClassNotFoundException
		Object obj = unmarshaller.unmarshal(new InputSource(new StringReader(xml)));
			// throws JAXBException
		return obj;
	}

	//Start: Added for Build Ticket DM-292
	public static Object xmlInputStreamToObject(Class<?> clazz, InputStream in) throws JAXBException, ClassNotFoundException
	{
		JAXBContext jaxbContext = JAXBContextProvider.getContext(clazz);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		Object obj = unmarshaller.unmarshal(in);
		return obj;
	}
	//End: Added for Build Ticket DM-292

	public static Object xmlToObject2(Class<?> clazz, String xml) throws JAXBException, XMLStreamException, ClassNotFoundException {
		JAXBContext jaxbContext = JAXBContextProvider.getContext(clazz); //JAXBContext.newInstance(clazz);
	    	// throws JAXBException

		XMLInputFactory factory = XMLInputFactory.newInstance();
	    	// throws FactoryConfigurationError

		XMLEventReader er = factory.createXMLEventReader(new StringReader(xml));
	    	// throws XMLStreamException

		XMLEventReader erd = new EventReaderDelegate(er) {

			@Override
			public XMLEvent nextEvent() throws XMLStreamException {
				try {
					XMLEvent event = super.nextEvent();
					return event;
				}
				catch (Exception e) {
					XMLEventFactory ef = XMLEventFactory.newInstance();
						// throws FactoryConfigurationError
					Characters comment = ef.createCharacters("XML ERROR: invalid XML characters");
					return comment;
				}
			}
		};

		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();  // throws JAXBException

		// we need the customIDResolver because of same IDREF values and IDREFs missing full objects
		unmarshaller.setProperty(IDResolver.class.getName(), new CustomIDResolver("com.wiley.permissions.domain.persistence.permissions.Bundle"));
	    	// throws JAXBException, ClassNotFoundException

		Object obj = unmarshaller.unmarshal(erd);  // throws JAXBException
		return obj;
	}

	// smarkoff: This method is currently only used in the PEMessageTransformTest
	// (not in actual production use)
	/**
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    public static String stripNonValidXMLCharacters(String in) {
    	if (StringUtils.isEmpty(in)) return "";
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < in.length(); i++) {
            char current = in.charAt(i);
            if ((current == 0x9) ||
                (current == 0xA) ||
                (current == 0xD) ||
                ((current >= 0x20) && (current <= 0xD7FF)) ||
                ((current >= 0xE000) && (current <= 0xFFFD)) ||
                ((current >= 0x10000) && (current <= 0x10FFFF))) {
                out.append(current);
            }
            else {
            	log.info("stripNonValidXMLCharacters(): Ignoring char " + i + "(dec " + current + ") of XML");
            }
        }

        return out.toString();
    }

	public Class<?> getReturnClass() {
		return returnClass;
	}

	public void setReturnClass(Class<?> returnClass) {
		this.returnClass = returnClass;
	}
}
