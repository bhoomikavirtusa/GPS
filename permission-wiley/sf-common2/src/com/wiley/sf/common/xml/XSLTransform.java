package com.wiley.sf.common.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.wiley.sf.common.io.FileUtil;
import com.wiley.sf.common.lang.ResourceUtil;

/**
 * Utility class to help with XSL Transforms.
 *
 * How to use this class:
 *
 * Use one instance for a single transform (due to complications of the use
 * of streams/readers that would have to be reset for multiple transforms).
 *
 * Generally you create an instance, call one of the setXml methods, one
 * of the setXsl methods, and then one of the transform methods.
 * Optional the setProperties/Parameters methods can also be used.
 *
 * Certain default properties are built-in (prettyPrint, utf-8) unless you
 * call setOutputProperties() to override.
 *
 * @since   JDK 1.6, Xalan 2.7.1
 * @version 4/2/2012
 * @author  smarkoff
 */
public class XSLTransform {
	private Source xmlSource = null;
	private Source xslSource = null;

	private HashMap<String, Object> parameters = null;
	private Properties outputProperties = null;

    public XSLTransform() {

    }

    public void setParameters(HashMap<String, Object> parameters) {
    	this.parameters = parameters;
    }

    public void setOutputProperties(Properties p) {
    	this.outputProperties = p;
    }

    public void setXsl(String s) {
		InputSource inSource = new InputSource(new StringReader(s));
		xslSource = new SAXSource(inSource);
    }

    public void setXsl(File file) {
    	xslSource = new StreamSource(file);
    }

    public void setXsl(Document doc) {
        xslSource = new DOMSource(doc);
    }

    public void setXsl(Reader r) {
        xslSource = new StreamSource(r);
    }

    public void setXsl(InputStream is) {
    	xslSource = new StreamSource(is);
    }

    public void setXsl(Class<?> cl, String name)
        throws IOException, FileNotFoundException
    {
    	InputStream is = ResourceUtil.getResourceInputStream(cl, name);
    	    // throws IOException, FileNotFoundException
    	xslSource = new StreamSource(is);
    }


    public void setXml(String s) {
		InputSource inSource = new InputSource(new StringReader(s));
		xmlSource = new SAXSource(inSource);
    }

    public void setXml(File file) {
    	xmlSource = new StreamSource(file);
    }

    public void setXml(Document doc) {
        xmlSource = new DOMSource(doc);
    }

    public void setXml(Reader r) {
        xmlSource = new StreamSource(r);
    }

    public void setXml(InputStream is) {
    	xmlSource = new StreamSource(is);
    }

    public void setXml(Class<?> cl, String name)
        throws IOException, FileNotFoundException
    {
    	InputStream is = ResourceUtil.getResourceInputStream(cl, name);
    	    // throws IOException, FileNotFoundException
    	xmlSource = new StreamSource(is);
    }


    public String transformToString()
        throws TransformerConfigurationException, TransformerException
    {
    	StringWriter sw = new StringWriter();
    	transformTo(new StreamResult(sw));
    	return sw.toString();
    }

    public void transformTo(File file)
        throws TransformerConfigurationException, TransformerException
    {
        transformTo(new StreamResult(file));
    }

    public Document transformToDocument() throws TransformerConfigurationException, TransformerException,
        ParserConfigurationException, SAXException, IOException
    {
    	// Either need to know root element name to create Document object or have xml
    	// (see XMLUtil methods). It's easier to create from xml.
        //Result result = new DOMResult(doc); -- you need to already have a Document for this
        //return transformTo(doc);
    	String xml = transformToString();  // throws TransformerConfigurationException, TransformerException
    	Document doc = XMLUtil.buildDocument(xml, false);  // don't validate
    	    // throws ParserConfigurationException, SAXException, IOException
    	return doc;
    }

    public void transformTo(Writer w)
        throws TransformerConfigurationException, TransformerException
    {
        Result result = new StreamResult(w);
        transformTo(result);
    }

    public void transformTo(OutputStream os)
        throws TransformerConfigurationException, TransformerException
    {
        Result result = new StreamResult(os);
        transformTo(result);
    }

    public void transformTo(Result result)
        throws TransformerConfigurationException, TransformerException
    {
    	TransformerFactory tFactory = TransformerFactory.newInstance();

        Transformer tf = tFactory.newTransformer(xslSource);
            // throws TransformerConfigurationException

        if (parameters != null) {
        	Set<String> keySet = parameters.keySet();
        	for (String key: keySet) {
        		tf.setParameter(key, parameters.get(key));
        	}
        }

        if (outputProperties != null) {
            tf.setOutputProperties(outputProperties);
        }
        else {
        	// see comments about output properties in XMLUtil.nodeToStreamResult()

        	// leave this out since default is "no"
        	//tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            //tf.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicId);
            //tf.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemId);
        }

        tf.transform(xmlSource, result);
            // throws TransformerException
    }

	/** Test program */
    public static void main(String [] args)
    	throws Exception
    {
		if (args.length < 2) {
			System.err.println("usage: <xsl file> <xml file> [output file]");
			System.exit(1);
		}

		File xslFile = FileUtil.checkFile(args[0]);
		File xmlFile = FileUtil.checkFile(args[1]);
		String outName = "output.xml";
		if (args.length > 3) {
			outName = args[2];
		}
		File outFile = new File(outName);

		XSLTransform transform = new XSLTransform();
		transform.setXsl(xslFile);
		transform.setXml(xmlFile);
		transform.transformTo(outFile);  // throws TransformerConfigurationException, TransformerException
		System.out.println("resulting xml saved to: " + outFile.getName());

		TransformerFactory tFactory = TransformerFactory.newInstance();
		System.out.println("TransformerFactory = " + tFactory.getClass().getName());
	}
}
