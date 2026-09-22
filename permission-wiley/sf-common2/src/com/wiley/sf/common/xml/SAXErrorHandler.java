package com.wiley.sf.common.xml;

import java.io.File;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.wiley.sf.common.io.LineHighlight;

/**
 *
 * @since   JDK 1.5, Xerces 2.9.1
 * @version 1/1/2009
 * @author  Steve Markoff
 */
public class SAXErrorHandler implements ErrorHandler {

    // Note it would be nice to just have an InputSource which could be
    // a String or File or other underneath but this doesn't work well because
    // the XML parser uses the InputSource for the actual parsing and there
    // is no API to reset the InputSource back to the beginning for the
    // error message.

    // Only one (or none of the following) will end up being set.
    private final String xml;
    private final File xmlFile;

    public SAXErrorHandler() {
        xml = null;
        xmlFile = null;
    }

    /**
     * @param xml  Will be used to provide information in exceptions
     */
    public SAXErrorHandler(String xml) {
        this.xml = xml;
        this.xmlFile = null;
    }

    /**
     * @param xmlFile  Will be used to provide information in exceptions
     */
    public SAXErrorHandler(File xmlFile) {
        this.xml = null;
        this.xmlFile = xmlFile;
    }

    /** Implements ErrorHandler interface. */
    public void error(SAXParseException ex) throws SAXException {
        String msg = "XML error on line " + ex.getLineNumber()
            + ", column " + ex.getColumnNumber() + ":\n"
            + ex.getMessage() + "\n"
            + buildXMLWithLineNumbers(ex.getLineNumber(), ex.getColumnNumber());
        throw new SAXException(msg);
    }

    /** Implements ErrorHandler interface. */
    public void fatalError(SAXParseException ex) throws SAXException {
        // If we didn't throw an exception here, the calling code would
        // throw one but without line numbers/xml (testing with Xerces 2.9.0)
        // so throw exception here.
        String msg = "Fatal XML error on line " + ex.getLineNumber()
            + ", column " + ex.getColumnNumber() + ":\n"
            + ex.getMessage() + "\n"
            + buildXMLWithLineNumbers(ex.getLineNumber(), ex.getColumnNumber());
        throw new SAXException(msg);
    }

    /** Implements ErrorHandler interface. */
    public void warning(SAXParseException ex) throws SAXException {
        System.err.println("Warning from XML parsing (" + ex.getLineNumber()
            + ", " + ex.getColumnNumber() + "):\n" + ex.toString()
            + "\n" + buildXMLWithLineNumbers(ex.getLineNumber(), ex.getColumnNumber()));
    }

    private String buildXMLWithLineNumbers(int lineNumber, int colNumber) {
        if (xml == null && xmlFile == null)  return "[XML not provided]";

        LineHighlight lh = new LineHighlight(false, 5);
        if (xml != null) {
            return lh.highlight(xml, lineNumber, colNumber);
        }
        else {
            return lh.highlight(xmlFile, lineNumber, colNumber);
        }
    }

}
