package com.wiley.sf.common.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.wiley.sf.common.io.FileUtil;
import com.wiley.sf.common.lang.ArgUtil;

/**
 * Provides utility methods to help work with XML documents.
 *
 * @since   JDK 1.6, Xerces 2.9.1
 * @version 5/3/2012
 * @author  Steve Markoff
 */
public class XMLUtil {

    /**
     * Returns an empty document with the specified name
     * for the root element and no doctype.
     *
     * @param  qualifiedName  Should be non-blank
     */
    public static Document createDocument(String qualifiedName)
        throws ParserConfigurationException
    {
        return createDocument(qualifiedName, null, null, null);
    }

    /**
     * Returns an empty document with the specified name and
     * other optional parameters.
     * A doctype is set only if either publicId or systemId is set
     * (Can change API to support doctype without publicId or systemId
     * if needed.)
     *
     * @param  qualifiedName  Should be non-blank
     * @param  publicId       May be null
     * @param  systemId       May be null
     * @param  namespaceURI   May be null
     */
    public static Document createDocument(String qualifiedName, String publicId, String systemId,
            String namespaceURI)
        throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
            // throws ParserConfigurationException
        DOMImplementation domImpl = builder.getDOMImplementation();
        DocumentType docType = domImpl.createDocumentType(qualifiedName, publicId, systemId);
        if (publicId == null && systemId == null)  docType = null;
        return domImpl.createDocument(namespaceURI, qualifiedName, docType);
    }

    /**
     * Adds the specified DOCTYPE declaration to the document.
     *
     * @param doc - Document to process
     * @param qualifiedName - qualified name of the document root.
     * @param publicId - PUBLIC Id of the document.
     * @param systemId - SYSTEM Id of the document.
     */
    public static void addDocType(Document doc, String qualifiedName,
            String publicId, String systemId)
        throws DOMException
    {
        DOMImplementation impl = doc.getImplementation();
        DocumentType docType = impl.createDocumentType(qualifiedName, publicId, systemId);
        doc.appendChild(docType);
    }

    /**
     * Returns a DOM Document object by parsing the given XML file.
     * The XML file is expected to be UTF-8 encoded.
     *
     * @param file  If null, null will be returned
     * @param validate
     */
    public static Document buildDocument(File file, boolean validate)
        throws ParserConfigurationException, SAXException, IOException
    {
        if (file == null)  return null;

        // Can't use FileReader because is doesn't provide a way to set the
        // encoding.
        FileInputStream fin = new FileInputStream(file);
        InputStreamReader in = null;
        BufferedReader bufReader = null;

        try {
            in = new InputStreamReader(fin, "UTF-8");
            bufReader = new BufferedReader(in);
            InputSource inputSource = new InputSource(bufReader);
            // Set the system id of the input source so that if the XML specifies
            // a relative path to a DTD or XSD, there is a base URL to go off of.
            // Note: toURI() [unlike toURL()] method URL encodes characters
            // such as spaces - ex. file:/C:/dir%20with%20spaces/foo.xml
            // I tested and both toURL and toURI work but toURL() is deprecated
            inputSource.setSystemId(file.getCanonicalFile().toURI().toString());

            return buildDocument(inputSource, validate, null, new SAXErrorHandler(file));
                // throws ParserConfigurationException, SAXException, IOException
        }
        finally {
            IOUtils.closeQuietly(bufReader);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(fin);
        }
    }

    /**
     * Returns a DOM Document object by parsing the given String of XML.
     *
     * @param xml  If null, null will be returned
     */
    public static Document buildDocument(String xml, boolean validate)
        throws ParserConfigurationException, SAXException, IOException
    {
        if (xml == null)  return null;
        InputSource inputSource = new InputSource(new StringReader(xml));
        return buildDocument(inputSource, validate, null, new SAXErrorHandler(xml));
            // throws ParserConfigurationException, SAXException, IOException
    }

    /**
     * Returns a DOM Document object by parsing the XML from the given
     * InputSource.
     *
     * @param inputSource  If null, null will be returned
     */
    public static Document buildDocument(InputSource inputSource, boolean validate)
        throws ParserConfigurationException, SAXException, IOException
    {
        return buildDocument(inputSource, validate, null, new SAXErrorHandler());
    }

    /**
     * Returns a DOM Document by parsing the XML from the given file
     * and validating it using the given validateClasspath - there must
     * a DTD or XMLSchema at this location in the classpath.
     * The XML file is expected to be UTF-8 encoded.
     *
     * @param file  If null, null will be returned
     * @param validate  Whether or not the document should be fully validated
     * @param validateClasspath  Should be of the form "/package/classname" or "/package/".
     */
    public static Document buildDocument(File file, boolean validate,
            String validateClasspath)
        throws ParserConfigurationException, SAXException, IOException
    {
        if (file == null)  return null;

        // Can't use FileReader because is doesn't provide a way to set the
        // encoding.
        FileInputStream fin = new FileInputStream(file);
        InputStreamReader in = null;
        BufferedReader bufReader = null;

        try {
            in = new InputStreamReader(fin, "UTF-8");
            bufReader = new BufferedReader(in);
            InputSource inputSource = new InputSource(bufReader);

            EntityResolver entityResolver = null;
            if (validateClasspath != null) {
                entityResolver = new ClassPathEntityResolver(validateClasspath);
            }

            return buildDocument(inputSource, validate, entityResolver, new SAXErrorHandler(file));
                // throws ParserConfigurationException, SAXException, IOException
        }
        finally {
            IOUtils.closeQuietly(bufReader);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(fin);
        }
    }

    /**
     * Returns a DOM Document by parsing the given XML
     * and validating it using the given validateClasspath - there must
     * a DTD or XMLSchema at this location in the classpath.
     *
     * @param xml  If null, null will be returned
     * @param validate  Whether or not the document should be fully validated
     * @param validateClasspath  Should be of the form "/package/classname" or "/package/".
     */
    public static Document buildDocument(String xml, boolean validate,
            String validateClasspath)
        throws ParserConfigurationException, SAXException, IOException
    {
        if (xml == null)  return null;

        EntityResolver entityResolver = null;
        if (validateClasspath != null) {
            entityResolver = new ClassPathEntityResolver(validateClasspath);
        }

        InputSource inputSource = new InputSource(new StringReader(xml));
        return buildDocument(inputSource, validate, entityResolver, new SAXErrorHandler(xml));
            // throws ParserConfigurationException, SAXException, IOException
    }

    /**
     * Returns a DOM Document object by parsing the XML from the given
     * docSource. Note: One may wish to provide an EntityResolver
     * even when the document is not being validated because entities
     * are still verified even when validation is not being done (considered
     * part of the document being well-formed).
     *
     * @param docSource       If null, null will be returned
     * @param validate        Whether or not the document should be fully validated
     * @param entityResolver  If null, the default EntityResolver will be used
     * @param errorHandler    If null, the default ErrorHandler will be used
     */
    public static Document buildDocument(InputSource docSource,
            boolean validate, EntityResolver entityResolver,
            ErrorHandler errorHandler)
        throws ParserConfigurationException, SAXException, IOException
    {
        if (docSource == null)  return null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(validate);
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
            // throws ParserConfigurationException

        if (entityResolver != null) {
            builder.setEntityResolver(entityResolver);
        }

        if (errorHandler != null) {
            builder.setErrorHandler(errorHandler);
        }

        return builder.parse(docSource);
            // throws IOException, SAXException
    }

    /**
     * Takes what should be the body of an XHTML 1.0 document and wraps it
     * so that it is a complete XHTML 1.0 document.
     *
     * @param html  If null, null will be returned
     * @param strict  False means transitional
     */
    public static String wrapXHTMLBody(String html, boolean strict) {
        if (html == null)  return null;

        String fullHtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 "
            + (strict ? "Strict" : "Transitional") + "//EN\"\n"
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-"
            + (strict ? "strict" : "transitional") + ".dtd\">\n"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
            + "<head>\n"
            + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n"
            + "<title>Dummy</title>\n"
            + "</head>\n"
            + "<body>\n"
            + html
            + "</body>\n"
            + "</html>\n";

        return fullHtml;
    }

    /**
     * Takes what should be the body of an XHTML 1.0 document and wraps it
     * so that it is a complete XHTML 1.0 document.
     * Overwrites the given file with the complete document.
     *
     * @param htmlFile  Must be non-null
     * @param strict    False means transitional
     */
    public static void wrapXHTMLBody(File htmlFile, boolean strict)
        throws IOException
    {
        wrapXHTMLBody(htmlFile, strict, null);
    }

    /**
     * Takes what should be the body of an XHTML 1.0 document and wraps it
     * so that it is a complete XHTML 1.0 document.
     *
     * @param htmlFile  Must be non-null
     * @param strict    False means transitional
     * @param outFile   If null, will overwrite the htmlFile
     */
    public static void wrapXHTMLBody(File htmlFile, boolean strict, File outFile)
        throws IOException
    {
		ArgUtil.notNull(htmlFile, "htmlFile");

        String html = FileUtil.fileToString(htmlFile);  // throws IOException
        String fullHtml = wrapXHTMLBody(html, strict);

        if (outFile == null)  outFile = htmlFile;
        FileUtil.stringToFile(fullHtml, outFile);  // throws IOException
    }

    /**
     * Takes an HTML document and removes everything that is not inside
     * the body tag. If the body tag is missing, no action is taken.
     * If you know the document is nicely indented and you want to reduce
     * the indentation with the reduceIndent parameter. (Such that the
     * indentation for what inside the body will now start where the
     * html tag used to start).
     *
     * @param html  If null, null will be returned
     * @param reduceIndent
     */
    public static String unwrapXHTMLBody(String html, boolean reduceIndent) {
        if (html == null)  return null;

        int start = html.indexOf("<body");
        if (start == -1)  return html;
        start = html.indexOf('>', start);
        if (start == -1)  return html;
        if (html.charAt(start - 1) == '/') {
            // This means the document has "<body../>" - not common but
            // in particular HtmlCleaner will do this for a blank document.
            return "";
        }
        start++;

        int end = html.indexOf("</body>", start);
        if (end == -1)  return html;

        html = html.substring(start, end);

        // identify first non-whitespace char and first newline that is
        // before that char (if there is one)
        int firstChar = -1;
        int newline = -1;
        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);
            if (!Character.isWhitespace(c)) {
                firstChar = i;
                break;
            }
            if (c == '\n') {
                newline = i;
            }
        }

        if (newline != -1 && firstChar != -1) {
            html = html.substring(newline + 1);
        }

        // identify first newline after last non-whitespace char
        // (if there is one)
        newline = -1;
        for (int i = html.length() - 1; i >= 0; i--) {
            char c = html.charAt(i);
            if (!Character.isWhitespace(c))  break;
            if (c == '\n') newline = i;
        }

        if (newline != -1) {
            html = html.substring(0, newline + 1);
        }


        if (reduceIndent && firstChar != -1) {
            // Identify whitespace before first non-whitespace character.
            // We already made sure above that no newlines will be in this
            // whitespace.
            int startIndex = -1;
            for (int i = 0; i < html.length(); i++) {
                char c = html.charAt(i);
                if (!Character.isWhitespace(c)) {
                    startIndex = i;
                    break;
                }
            }

            if (startIndex > 0) {
                String remove = html.substring(0, startIndex);
                StringTokenizer tok = new StringTokenizer(html, "\r\n");
                StringBuilder builder = new StringBuilder();

                while (tok.hasMoreTokens()) {
                    String line = tok.nextToken();
                    if (line.startsWith(remove))  line = line.substring(startIndex);
                    builder.append(line);
                    builder.append("\r\n");
                }

                html = builder.toString();
            }
        }

        return html;
    }

    /**
     * Takes an complete XHTML 1.0 (either Strict or Transitional)
     * document and validates it. The document must contain a DOCTYPE
     *
     *
     * @param html    If null or blank, will be ignored
     * @throws  SAXException  If the html was not valid
     */
    public static void validateFullXHTML(String html)
        throws SAXException
    {
        if (StringUtils.isBlank(html))  return;

        try {
            //logger.debug("html = " + html);
            // Note we could validate using the public dtd (on the Internet)
            // as we specified in the DOCTYPE but this will be slow.
            // Instead we validate using a local copy that we have made
            // of the DTD.
            //buildDocument(html, true);
            buildDocument(html, true, "/com/wiley/otis/utils/xml/");
                // throws IOException, SAXException, ParserConfigurationException
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Takes an complete XHTML 1.0 (either Strict or Transitional)
     * document and validates it. The document must contain a DOCTYPE.
     *
     * @param htmlFile  Must be non-null
     * @throws  SAXException  If the html was not valid
     */
    public static void validateFullXHTML(File htmlFile)
        throws IOException, SAXException
    {
		ArgUtil.notNull(htmlFile, "htmlFile");

        try {
            // Note we could validate using the public dtd (on the Internet)
            // as we specified in the DOCTYPE but this will be slow.
            // Instead we validate using a local copy that we have made
            // of the DTD.
            //buildDocument(htmlFile, true);
            buildDocument(htmlFile, true, "/com/wiley/otis/utils/xml/");
                // throws IOException, SAXException, ParserConfigurationException
        }
        catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Takes an HTML fragment and validates it as the body of an
     * XHTML 1.0 Transitional or Strict document.
     *
     * @param html    If null or blank, will be ignored
     * @param strict  False means transitional
     * @throws  SAXException  If the html was not valid
     */
    public static void validateXHTMLBody(String html, boolean strict)
        throws SAXException
    {
        if (StringUtils.isBlank(html))  return;

        String fullHtml = wrapXHTMLBody(html, strict);
        validateFullXHTML(fullHtml);
    }

    /**
     * Takes an HTML fragment and validates it as the body of an
     * XHTML 1.0 Transitional or Strict document.
     *
     * @param htmlFile  If null or blank, will be ignored
     * @param strict  False means transitional
     * @throws  IOException   If there was a problem reading the file
     * @throws  SAXException  If the html was not valid
     */
    public static void validateXHTMLBody(File htmlFile, boolean strict)
        throws IOException, SAXException
    {
        if (htmlFile == null)  return;

        String html = FileUtil.fileToString(htmlFile);  // throws IOException
        validateXHTMLBody(html, strict);  // throws SAXException
    }

    /**
     * Takes either a full HTML document or an HTML fragment and validates
     * it as XHTML 1.0. The strict parameter is only used when validating
     * HTML fragments (the DOCTYPE is used for full documents). A document
     * is assumed to be "full" if it contains "<html". Full documents must
     * also contain a DOCTYPE or there will be a SAX error "no grammar found".
     *
     * @param html    If null or blank, will be ignored
     * @param strict  False means transitional
     * @throws SAXException  If the html was not valid
     */
    public static void validateXHTML(String html, boolean strict)
        throws SAXException
    {
        if (StringUtils.isBlank(html))  return;

        if (html.contains("<html")) {
            validateFullXHTML(html);
        }
        else {
            validateXHTMLBody(html, strict);
        }
    }

    /**
     * Takes either a full HTML document or an HTML fragment and validates
     * it as XHTML 1.0. The strict parameter is only used when validating
     * HTML fragments (the DOCTYPE is used for full documents). A document
     * is assumed to be "full" if it contains "<html". Full documents must
     * also contain a DOCTYPE or there will be a SAX error "no grammar found".
     *
     * @param htmlFile  If null or blank, will be ignored
     * @param strict  False means transitional
     * @throws  IOException   If there was a problem reading the file
     * @throws  SAXException  If the html was not valid
     */
    public static void validateXHTML(File htmlFile, boolean strict)
        throws IOException, SAXException
    {
        if (htmlFile == null)  return;

        String html = FileUtil.fileToString(htmlFile);  // throws IOException
        if (html.contains("<html")) {
            validateFullXHTML(html);
        }
        else {
            validateXHTMLBody(html, strict);
        }
    }

    /**
     * Given a Soap XML document, returns the XML inside the Soap body.
     *
     * @param xml  Must be non-null
     */
    public static String extractSoapBody(String xml)
        throws Exception
    {
        // The input xml should look something like:
        // <?xml ...?>
        // <soapenv:Envelope xmlns:soapenv="..." ...>
        //   <soapenv:Header> (note the header might be missing) </soapenv:Header>
        //   <soapenv:Body>
        //     ...(we will return the xml in here)...
        //   </soapenv:Body>
        // </soapenv:Envelop>
        //
        // Note the namespace qualifier may be something other than "soapenv".

        ArgUtil.notNull(xml, "xml");

        Document doc = buildDocument(xml, false);  // throws Exception
        Element treeRoot = doc.getDocumentElement();
        String docType = treeRoot.getTagName();
        // docType will look something like "soapenv:Envelope"
        int index = docType.indexOf(':');
        String nsColon = "";
        String tag = docType;
        if (index != -1) {
            nsColon = docType.substring(0, index + 1);
            tag = docType.substring(index + 1);
        }

        if (!tag.equals("Envelope")) {
            throw new Exception("XML document does not appear to be a SOAP Envelope: " + xml);
        }

        NodeList nodeList = treeRoot.getElementsByTagName(nsColon + "Body");
        Element body = (Element) nodeList.item(0);
        if (body == null) {
            throw new Exception("SOAP XML Envelope does not contain soap body: " + xml);
        }

        Element newDoc = getFirstElementChildNode(body);
        if (newDoc == null) {
            throw new RuntimeException("Soap body has no child element: " + xml);
        }
        return nodeToString(newDoc);  // throws ParserConfigurationException
    }

    /**
     * Given a DOM Node, returns the XML converted to plain text
     * with pretty-print formatting.
     *
     * The xml declaration (<?xml...) will be included if the given node
     * is an instance of Document.
     *
     * @param node  If null, null is returned
     */
    public static String nodeToString(Node node)
        throws TransformerException
    {
        return nodeToString(node, node instanceof Document);
    }

    /**
     * Given a DOM Node, returns the XML converted to plain text
     * with pretty-print formatting.
     *
     * @param node  If null, null is returned
     * @param xmlDeclaration  If true will include the xml declaration (<?xml...)
     */
    public static String nodeToString(Node node, boolean xmlDeclaration)
        throws TransformerException
    {
	    return nodeToString(node, xmlDeclaration, true);
    }
    /**
     * Given a DOM Node, returns the XML converted to plain text.
     *
     * @param node  If null, null is returned
     * @param xmlDeclaration  If true will include the xml declaration (<?xml...)
     * @param prettyPrint  Format with indentation
     */
    public static String nodeToString(Node node, boolean xmlDeclaration,
            boolean prettyPrint)
        throws TransformerException
    {
    	if (node == null)  return null;

        StringWriter writer = new StringWriter();
        StreamResult streamResult = new StreamResult(writer);

        nodeToStreamResult(node, streamResult, xmlDeclaration, prettyPrint);

        String s = writer.toString();
        //System.out.println("--------\r\n" + s + "---------");
        return s;
    }

    /**
     * Given a DOM Node, streams the XML converted to plain text into
     * the given File with pretty-print formatting.
     *
     * The xml declaration (<?xml...) will be included if the given node
     * is an instance of Document.
     *
     * @param node  If null, nothing is streamed
     * @param file  Must be non-null
     */
    public static void nodeToFile(Node node, File file)
        throws TransformerException
    {
        if (node == null)  return;
        ArgUtil.notNull(file, "file");

        StreamResult streamResult = new StreamResult(file);

        nodeToStreamResult(node, streamResult, node instanceof Document, true);
    }

    /**
     * Given a DOM Node, streams the XML converted to plain text into
     * the given File.
     *
     * @param node  If null, nothing is streamed
     * @param file  Must be non-null
     * @param xmlDeclaration  If true will include the xml declaration (<?xml...)
     * @param prettyPrint  Format with indentation
     */
    public static void nodeToFile(Node node, File file, boolean xmlDeclaration,
            boolean prettyPrint)
        throws TransformerException
    {
        if (node == null)  return;
        ArgUtil.notNull(file, "file");

        StreamResult streamResult = new StreamResult(file);

        nodeToStreamResult(node, streamResult, xmlDeclaration, prettyPrint);
    }

    /**
     * Given a DOM Node, streams the XML converted to plain text into
     * the given OutputStream with pretty-print formatting.
     *
     * The xml declaration (<?xml...) will be included if the given node
     * is an instance of Document.
     *
     * @param node  If null, nothing is streamed
     * @param outputStream  Must be non-null
     */
    public static void nodeToOutputStream(Node node, OutputStream outputStream)
        throws TransformerException
    {
        if (node == null)  return;
        ArgUtil.notNull(outputStream, "outputStream");

        StreamResult streamResult = new StreamResult(outputStream);

        nodeToStreamResult(node, streamResult, node instanceof Document, true);
    }

    /**
     * Given a DOM Node, streams the XML converted to plain text into
     * the given OutputStream.
     *
     * @param node  If null, nothing is streamed
     * @param outputStream  Must be non-null
     * @param xmlDeclaration  If true will include the xml declaration (<?xml...)
     * @param prettyPrint  Format with indentation
     */
    public static void nodeToOutputStream(Node node, OutputStream outputStream,
            boolean xmlDeclaration, boolean prettyPrint)
        throws TransformerException
    {
        if (node == null)  return;
        ArgUtil.notNull(outputStream, "outputStream");

        StreamResult streamResult = new StreamResult(outputStream);

        nodeToStreamResult(node, streamResult, xmlDeclaration, prettyPrint);
    }

    /**
     * Given a DOM Node, streams the XML converted to plain text into
     * the given Writer with pretty-print formatting.
     *
     * The xml declaration (<?xml...) will be included if the given node
     * is an instance of Document.
     *
     * @param node  If null, nothing is streamed
     * @param writer  Must be non-null
     */
    public static void nodeToWriter(Node node, Writer writer)
        throws TransformerException
    {
        if (node == null)  return;
        ArgUtil.notNull(writer, "writer");

        StreamResult streamResult = new StreamResult(writer);

        nodeToStreamResult(node, streamResult, node instanceof Document, true);
    }

    /**
     * Given a DOM Node, streams the XML converted to plain text into
     * the given Writer.
     *
     * @param node  If null, nothing is streamed
     * @param writer  Must be non-null
     * @param xmlDeclaration  If true will include the xml declaration (<?xml...)
     * @param prettyPrint  Format with indentation
     */
    public static void nodeToWriter(Node node, Writer writer,
            boolean xmlDeclaration, boolean prettyPrint)
        throws TransformerException
    {
        if (node == null)  return;
        ArgUtil.notNull(writer, "writer");

        StreamResult streamResult = new StreamResult(writer);

        nodeToStreamResult(node, streamResult, xmlDeclaration, prettyPrint);
    }

    /**
     * Given a DOM Node, streams the XML converted to plain text into
     * the given StreamResult.
     *
     * @param node  If null, nothing is streamed
     * @param streamResult  Must be non-null
     * @param xmlDeclaration  If true will include the xml declaration (<?xml...)
     * @param prettyPrint  Format with indentation
     */
    public static void nodeToStreamResult(Node node, StreamResult streamResult,
            boolean xmlDeclaration, boolean prettyPrint)
        throws TransformerException
    {
        if (node == null)  return;
        ArgUtil.notNull(streamResult, "streamResult");

        DOMSource domSource = new DOMSource(node);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer tf = factory.newTransformer();
            // throws TransformerConfigurationException

        // Properties always seem to start out empty
        /*
        Properties props = tf.getOutputProperties();
        String propsString = StringUtil.mapToString(props, "\r\n");
        if (props.isEmpty())  propsString = "(empty)";
        System.out.println(propsString);
        */

        // defaults (with empty properties) appear to be:
        // (testing with Xalan 2.7.1 / Xerces 2.9.0)
        // Always prints out an xml declaration like so:
        // <?xml version="1.0" encoding="UTF-8"?>
        // even when the node is not a document.
        // Never prints out a DOCTYPE even when the node is a document
        // with a DocumentType that has been set.
        // Does not do indentation (pretty-print).

        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
                xmlDeclaration ? "no" : "yes");
        tf.setOutputProperty(OutputKeys.INDENT, prettyPrint ? "yes" : "no");
        // Seems need following indent-amount also, or else indent-amount is 0
        // (but does add newlines - looks better than without indent property).
        // This indent-amount seems to work with both Xerces 2.9.0/Xalan 2.7.1
        // and just plain JDK 1.6.
        // Except at least when there is an xml-declaration, the root tag ends
        // up on the same line - seems like a bug. Example:
        // <?xml version="1.0" encoding="UTF-8"?><root>
        //     <level1>
        //        <level2/>
        //     <level1>
        // </root>
        if (prettyPrint) {
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        }
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        if (node instanceof Document) {
            Document doc = (Document) node;
            DocumentType type = doc.getDoctype();
            if (type != null) {
                String publicId = type.getPublicId();
                String systemId = type.getSystemId();

                // It seems that if a publicId is set but not a systemId,
                // then the transformer won't generate the DOCTYPE,
                // so if the publicId has been set, set the systemId
                // to the empty string if it doesn't have a value.

                if (StringUtils.isNotBlank(publicId)) {
                    tf.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicId);
                    if (StringUtils.isBlank(systemId)) {
                        systemId = "";
                    }
                }

                if (systemId != null) {
                    tf.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemId);
                }
            }
        }

        tf.transform(domSource, streamResult);
            // throws TransformerException
    }

    /**
     * Given a DOM Node, returns the XML converted to plain text.
     *
     * @param node  If null, null is returned
     * @param xmlDeclaration  If true will include the xml declaration (<?xml...)
     * @param prettyPrint  Format with indentation
     */
    public static String nodeToStringAlt(Node node, boolean xmlDeclaration,
            boolean prettyPrint)
        throws ParserConfigurationException
    {
        if (node == null)  return null;

        // Note instead of using the LSSerializer API could use javax.xml.transform
        // packages but I think the org.w3c.dom.ls package is just as good or better.

        DOMImplementationRegistry registry;

        try {
            registry = DOMImplementationRegistry.newInstance();
                // throws various Exceptions
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        DOMImplementationLS domImplLS = (DOMImplementationLS) registry.getDOMImplementation("LS");

        LSSerializer ls = domImplLS.createLSSerializer();

        // can do this if want to (what is default?)
        //ls.setNewLine(StringUtil.getLineSeparator());

        DOMConfiguration config = ls.getDomConfig();
        // xml-declaration is a standard Load/Save option but despite documentation
        // I can't seem to set this in either case so we have some workaround logic
        // further down - but retest this - maybe can only set with full documents
        if (config.canSetParameter("xml-declaration", Boolean.toString(xmlDeclaration))) {
            config.setParameter("xml-declaration", Boolean.toString(xmlDeclaration));
                // throws DOMException (Runtime)
	    }
	    else {
	        System.err.println("Couldn't set xml-declaration to "
	            + Boolean.toString(xmlDeclaration));
	    }

        // format-pretty-print is a standard Load/Save DOM option
        // - Last test listed format-pretty-print as valid param but when try
        // to set it get DOMException saying the param is not recognized.
        // - One person online had same issue and said fixed when used serializer.jar
        // from Xerces instead of Xalan (using Xerces 2.9.0 and Xalan 2.7.0)
        // but this didn't help in my testing.
        // - test if can use with both full documents and partial - and does it
        // matter if the document is validated or points to a dtd?
        // Also test what pretty-print does with something like this:
        // F<i>k</i><sub>a</sub>
        // Does it keep like this w/o adding spaces or newlines? - it should
        // and this
        // <p>line 1
        //    line 2
        // </p>
        // - Does it keep the newlines inside the paragraph?
        // But for this:
        // <p>p1</p> <p>p2</p>
        // - might be nice if it moved second paragraph to next line

        if (config.canSetParameter("format-pretty-print", Boolean.toString(prettyPrint))) {
            config.setParameter("format-pretty-print", Boolean.toString(prettyPrint));
                // throws DOMException (Runtime)
        }
        else {
	        System.err.println("Couldn't set format-pretty-print to "
	            + Boolean.toString(prettyPrint));
	    }

        String s = ls.writeToString(node);  // throws DOMException, LSException
        // logic to to remove xml-declaration if couldn't remove above
        if (!xmlDeclaration && s.startsWith("<?xml")) {
		    int end = s.indexOf('>');
		    if (end != -1)  s = s.substring(end + 1);
	    }

        // writeToString always specifies encoding="UTF-16" so change to UTF-8
        // (if keeping xml declaration)
        if (s.startsWith("<?xml")) {
            int end = s.indexOf('>');
            if (end != -1) {
                String dec = s.substring(0, end);
                dec = dec.replace("UTF-16", "UTF-8");
                s = dec + s.substring(end);
            }
        }

	    // other parameters:
	    // comments - default is true, meaning show all comments in XML
	    // - can list all possible params using DOMConfiguration.getParameterNames()
	    /*
	    DOMStringList list = config.getParameterNames();
	    System.out.println("list of valid config params:");
	    for (int i = 0; i < list.getLength(); i++) {
	        System.out.println(list.item(i));
	    }
	    */

	    // Notes on config parameters:
	    // Standard params for DOMConfiguration are listed here:
	    // http://www.w3.org/TR/DOM-Level-3-Core/core.html#DOMConfiguration
	    // and then for Load/Save are extended as listed here:
	    // http://www.w3.org/TR/2004/REC-DOM-Level-3-LS-20040407/load-save.html#LS-LSSerializer-config

	    return s;
    }

    /**
     * Given a DOM NodeList, returns the XML converted to plain text (with pretty-print).
     * This method should only be used for debugging purposes - it is probably
     * slow. (Generally it's faster to serialize a single node with many children
     * instead of doing the children individually.)
     *
     * @param nodeList  If null, null is returned. Empty string returned for 0-size list.
     * @throws TransformerException
     */
    public static String nodeListToString(NodeList nodeList) throws TransformerException {
        if (nodeList == null)  return null;
        if (nodeList.getLength() == 0)  return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < nodeList.getLength() ; i++) {
            sb.append(nodeToString(nodeList.item(i), false));
                // nodeToString() throws ParserConfigurationException
        }

        return sb.toString();
    }

    /**
     * Replace text in the given node if it is a text node, otherwise
     * look for child nodes that are text nodes and perform the replace.
     *
     * @param node  If null this method will do nothing
     * @param find  Must be non-null and of length > 0
     * @param replace  Must be non-null
     */
    public static void replaceText(Node node, String find, String replace) {
        if (node == null)  return;

        if (node.getNodeType() == Node.TEXT_NODE) {
            String value = node.getNodeValue();
            value = value.replace(find, replace);
            node.setNodeValue(value);
        }
        else {
            NodeList nodeList = node.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                replaceText(nodeList.item(i), find, replace);
            }
        }
    }

    /**
     * Returns the first child Node of the input Node that is an Element,
     * or null if no such child exists.
     *
     * @param node  Must be non-null
     */
    public static Element getFirstElementChildNode(Node node) {
        ArgUtil.notNull(node, "node");
        NodeList list = node.getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) n;
            }
        }

        return null;
    }

    /**
     * This method is intended for debugging purposes.
     * Instead of serializing the node to XML, creates a String which shows
     * the DOM tree of the node - also prints out attributes which are
     * not actually considered child nodes in the tree.
     * (Prints out attributes of an element node before it's children.)
     *
     * @param node  If null, null is returned.
     */
    public static String showNodeTree(Node node) {
        if (node == null)  return null;
        StringBuilder sb = new StringBuilder();
        showNodeTree(node, sb, 0);
        return sb.toString();
    }

    private static void showNodeTree(Node node, StringBuilder sb, int level) {
        sb.append("level " + level + ": name = " + node.getNodeName());
        sb.append(", type = " + nodeTypeToString(node.getNodeType()));
        sb.append(", value = " + node.getNodeValue());
        NodeList childList = node.getChildNodes();
        sb.append(", #children = " + childList.getLength());
        sb.append("\n");
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            NamedNodeMap map = node.getAttributes();
            for (int i = 0; i < map.getLength(); i++) {
                showNodeTree(map.item(i), sb, level + 1);
            }
        }
        for (int i = 0; i < childList.getLength(); i++) {
            showNodeTree(childList.item(i), sb, level + 1);  // recursive call
        }
    }

    public static String nodeTypeToString(int type) {
        switch (type) {
            case Node.ELEMENT_NODE: return "Element"; // 1
            case Node.ATTRIBUTE_NODE: return "Attribute"; // 2
            case Node.TEXT_NODE: return "Text"; // 3
            case Node.CDATA_SECTION_NODE: return "CDATA Section"; // 4
            case Node.ENTITY_REFERENCE_NODE: return "Entity Reference"; // 5
            case Node.ENTITY_NODE: return "Entity"; // 6
            case Node.PROCESSING_INSTRUCTION_NODE: return "Processing Instruction"; // 7
            case Node.COMMENT_NODE: return "Comment"; // 8
            case Node.DOCUMENT_NODE:  return "Document"; // 9
            case Node.DOCUMENT_TYPE_NODE: return "Document Type"; // 10
            default: return String.valueOf(type);
        }
    }

    /**
     * Returns text of the given Element, or the empty string if there is no text.
     *
     * @param element  Must be non-null
     */
    public static String getText(Element element) {
        ArgUtil.notNull(element, "element");
        NodeList list = element.getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                return node.getNodeValue();
            }
        }

        return "";
    }

    /**
     * Returns the text inside the first element with the given tagName
     * inside the given element. If no such element with the given tagName
     * exists then returns the empty string.
     *
     * @param element  Must be non-null
     * @param tagName  Must be non-null and non-blank
     */
    public static String getText(Element element, String tagName) {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(tagName, "tagName");

        NodeList list = element.getElementsByTagName(tagName);

        if (list.getLength() > 0) {
            Element e2 = (Element) list.item(0);
            return getText(e2);
        }
        else  return "";
    }

    /**
     * Returns an ArrayList<String> that corresponds to the text inside
     * all elements with the given tagName inside the given Element.
     * Such strings that are of length zero or just whitespace are ignored
     * (not included in the list returned).
     *
     * @param element  Must be non-null
     * @param tagName  Must be non-null and non-blank
     */
    public static ArrayList<String> getTextList(Element element, String tagName) {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(tagName, "tagName");

        ArrayList<String> textList = new ArrayList<String>();
        NodeList list = element.getElementsByTagName(tagName);

        for (int i = 0; i < list.getLength(); i++) {
            Element e2 = (Element) list.item(i);
            String text = getText(e2);
            if (!StringUtils.isWhitespace(text)) {
                textList.add(text);
            }
        }

        return textList;
    }

    /**
     * Use this method to get a child element where there is expected to be
     * either one or none with the given tagName. If there is none, null will
     * be returned. If there is more than one a RuntimeException will be thrown.
     *
     * @param element  The parent element. Must be non-null.
     * @param tagName  The tagName of the child to look for.
     *                 Must be non-null and non-blank.
     */
    public static Element getOptionalChildElement(Element element, String tagName) {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(tagName, "tagName");

        NodeList list = element.getElementsByTagName(tagName);
        if (list.getLength() == 0)  return null;
        else if (list.getLength() == 1) {
            Element e2 = (Element) list.item(0);
            return e2;
        }
        else {
            throw new RuntimeException("More than one " + tagName
                + " found inside " + element.getTagName());
        }
    }

    /**
     * Use this method to get a child element where there is expected to be
     * exactly one with the given tagName. A RuntimeException will be thrown
     * if there is no such child element or there is more than one.
     *
     * @param element  The parent element. Must be non-null
     * @param tagName  The tagName of the child to look for.
     *                 Must be non-null and non-blank.
     */
    public static Element getRequiredChildElement(Element element, String tagName) {
        Element e2 = getOptionalChildElement(element, tagName);
        if (e2 == null) {
            throw new RuntimeException("Required element " + tagName
                + " not found inside " + element.getTagName());
        }
        else  return e2;
    }

    /**
     * Checks that all the child elements of the input element have the
     * given tagName. Throws a RuntimeException if there is an child element
     * with a different tagName. A null for tagName means that the given
     * Element should have no sub-elements.
     *
     * @param element  Must be non-null
     * @param tagName  May be null
     */
    public static void checkChildElements(Element element, String tagName) {
        ArgUtil.notNull(element, "element");
        NodeList list = element.getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (!n.getNodeName().equals(tagName)) {
                    String msg = "illegal element '" + n.getNodeName()
                        + "' found inside element '" + element.getTagName() + "'.";
                    if (tagName == null) {
                        msg += " No sub-elements are allowed in this element.";
                    }
                    else {
                        msg += " The only sub-element allowed inside this one is '"
                            + tagName + "'.";
                    }
                    throw new RuntimeException(msg);
                }
            }
        }
    }

    // Note we can't overload checkChildElements - must use the extension
    // "Multi" below, because otherwise a class trying to call either one
    // with null as the second parameter will give a compile error.

    /**
     * Checks that all the child elements of the input element have one of the
     * given tagNames. Throws a RuntimeException if there is an child element
     * with a different tagName. A null for tagNames means that the given
     * Element should have no sub-elements.
     *
     * @param element   Must be non-null
     * @param tagNames  May be null or zero-length
     */
    public static void checkChildElementsMulti(Element element,
        String [] tagNames)
    {
        ArgUtil.notNull(element, "element");
        if (tagNames == null)  tagNames = new String[0];

        NodeList list = element.getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (!matchesOne(n.getNodeName(), tagNames)) {
                    String msg = "illegal element '" + n.getNodeName()
                        + "' found inside element '" + element.getTagName() + "'.";
                    if (tagNames.length == 0) {
                        msg += " No sub-elements are allowed in this element.";
                    }
                    else {
                        StringBuilder sb = new StringBuilder(
                            " The only sub-elements allowed inside this one are: ");
                        for (int b = 0; b < tagNames.length; b++) {
                            sb.append(tagNames[b]);
                            sb.append(' ');
                        }
                        msg += sb.toString();
                    }
                    throw new RuntimeException(msg);
                }
            }
        }
    }

    /** Helper method for checkChildElementsMulti() */
    private static boolean matchesOne(String s, String [] tagNames) {
        for (int i = 0; i < tagNames.length; i++) {
            if (s.equals(tagNames[i]))  return true;
        }
        return false;
    }

    /**
     * Checks that all the attributes of the input element have the
     * given name. Throws a RuntimeException if there is an attribute
     * with a different name. A null for name means that the given
     * Element should have no attributes.
     *
     * @param element  Must be non-null
     * @param name     May be null
     */
    public static void checkAttributes(Element element, String name) {
        ArgUtil.notNull(element, "element");
        NamedNodeMap map = element.getAttributes();

        for (int i = 0; i < map.getLength(); i++) {
            Node n = map.item(i);

            if (!n.getNodeName().equals(name)) {
                String msg = "illegal attribute '" + n.getNodeName()
                    + "' found inside element '" + element.getTagName() + "'.";
                if (name == null) {
                    msg += " No attributes are allowed for this element.";
                }
                else {
                    msg += " The only attribute allowed for this element is '"
                        + name + "'.";
                }
                throw new RuntimeException(msg);
            }
        }
    }

    /**
     * Checks that all the attributes of the input element have one of the
     * given names. Throws a RuntimeException if there is an attribute
     * with a different name. A null for names means that the given
     * Element should have no attributes.
     *
     * @param element   Must be non-null
     * @param names     May be null or zero-length
     */
    public static void checkAttributesMulti(Element element, String [] names) {
        ArgUtil.notNull(element, "element");
        if (names == null)  names = new String[0];

        NamedNodeMap map = element.getAttributes();

        for (int i = 0; i < map.getLength(); i++) {
            Node n = map.item(i);

            if (!matchesOne(n.getNodeName(), names)) {
                String msg = "illegal attribute '" + n.getNodeName()
                    + "' found inside element '" + element.getTagName() + "'.";
                if (names.length == 0) {
                    msg += " No attributes are allowed for this element.";
                }
                else {
                    StringBuilder sb = new StringBuilder(
                        " The only attributes allowed for this element are: ");
                    for (int b = 0; b < names.length; b++) {
                        sb.append(names[b]);
                        sb.append(' ');
                    }
                    msg += sb.toString();
                }
                throw new RuntimeException(msg);
            }
        }
    }

    /**
     * Returns the attibute value if the attribute was found and had a non-blank
     * value. Otherwise throws a RuntimeException.
     *
     * @param element        Must be non-null
     * @param attributeName  Must be non-null and non-blank
     */
    public static String getRequiredStringAttribute(Element element,
        String attributeName)
    {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(attributeName, "attributeName");

        String value = element.getAttribute(attributeName);

        if (StringUtils.isWhitespace(value)) {
            throw new RuntimeException("The attribute " + attributeName
                + " is required in XML element " + element.getNodeName());
        }
        else  return value;
    }

    /**
     * Returns the attribute value if the attribute was found and was true
     * or false. Otherwise throws a RuntimeException.
     *
     * @param element        Must be non-null
     * @param attributeName  Must be non-null and non-blank
     */
    public static boolean getRequiredBooleanAttribute(Element element,
        String attributeName)
    {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(attributeName, "attributeName");

        String value = element.getAttribute(attributeName);

        if (value.length() == 0) {
            throw new RuntimeException("The attribute " + attributeName
                + " is required in XML element " + element.getNodeName());
        }
        else {
            if (value.equals("true"))  return true;
            if (value.equals("false"))  return false;
            throw new RuntimeException("The attribute " + attributeName
                + " in XML element " + element.getNodeName()
                + " must be given a true or false value.");
        }
    }

    /**
     * Returns the attribute value if found and it is an integer, otherwise
     * throws a RuntimeException.
     *
     * @param element        Must be non-null
     * @param attributeName  Must be non-null and non-blank
     */
    public static int getRequiredIntAttribute(Element element,
        String attributeName)
    {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(attributeName, "attributeName");

        String value = element.getAttribute(attributeName);

        if (value.length() == 0) {
            throw new RuntimeException("The attribute " + attributeName
                + " is required in XML element " + element.getNodeName());
        }

        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ex) {
            throw new RuntimeException("The attribute " + attributeName
                + " in XML element " + element.getNodeName()
                + " must be given an integer value.");
        }
    }

    /**
     * Returns the attribute value if found and it is a non-negative integer,
     * otherwise throws a RuntimeException.
     *
     * @param element        Must be non-null
     * @param attributeName  Must be non-null and non-blank
     */
    public static int getRequiredNonNegativeIntAttribute(Element element,
        String attributeName)
    {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(attributeName, "attributeName");

        String value = element.getAttribute(attributeName);

        if (value.length() == 0) {
            throw new RuntimeException("The attribute " + attributeName
                + " is required in XML element " + element.getNodeName());
        }

        try {
            int i = Integer.parseInt(value);
            if (i < 0) {
                throw new RuntimeException("The attribute " + attributeName
                    + " in XML element " + element.getNodeName()
                    + " must be given a non-negative integer value.");
            }
            else  return i;
        }
        catch (NumberFormatException ex) {
            throw new RuntimeException("The attribute " + attributeName
                + " in XML element " + element.getNodeName()
                + " must be given an integer value.");
        }
    }

    /**
     * Returns the attribute value if found and it is an integer > 0,
     * otherwise throws a RuntimeException.
     *
     * @param element        Must be non-null
     * @param attributeName  Must be non-null and non-blank
     */
    public static int getRequiredPositiveIntAttribute(Element element,
        String attributeName)
    {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(attributeName, "attributeName");

        String value = element.getAttribute(attributeName);

        if (value.length() == 0) {
            throw new RuntimeException("The attribute " + attributeName
                + " is required in XML element " + element.getNodeName());
        }

        try {
            int i = Integer.parseInt(value);
            if (i < 1) {
                throw new RuntimeException("The attribute " + attributeName
                    + " in XML element " + element.getNodeName()
                    + " must be given an integer value greater than 0.");
            }
            else  return i;
        }
        catch (NumberFormatException ex) {
            throw new RuntimeException("The attribute " + attributeName
                + " in XML element " + element.getNodeName()
                + " must be given an integer value.");
        }
    }

    /**
     * Returns the attibute value if the attribute was found. Otherwise returns
     * the given default.
     *
     * @param element        Must be non-null
     * @param attributeName  Must be non-null and non-blank
     */
    public static String getStringAttributeWithDefault(Element element,
        String attributeName, String defaultValue)
    {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(attributeName, "attributeName");

        String value = element.getAttribute(attributeName);

        return StringUtils.isWhitespace(value) ? defaultValue : value;
    }

    /**
     * Returns the attribute value if the attribute was found and it is true
     * or false. If the attribute is missing returns the given default.
     * If the attribute is not a boolean throws a RuntimeException.
     *
     * @param element        Must be non-null
     * @param attributeName  Must be non-null and non-blank
     */
    public static boolean getBooleanAttributeWithDefault(Element element,
        String attributeName, boolean defaultValue)
    {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(attributeName, "attributeName");

        String value = element.getAttribute(attributeName);

        if (value.length() == 0) {
            return defaultValue;
        }
        else {
            if (value.equals("true"))  return true;
            if (value.equals("false"))  return false;
            throw new RuntimeException("The attribute " + attributeName
                + " in XML element " + element.getNodeName()
                + " must be given a true or false value.");
        }
    }

    /**
     * Returns the attribute value if found and it is an integer.
     * If the attribute is missing returns the given default.
     * If the attribute value is not an integer throws a RuntimeException.
     *
     * @param element        Must be non-null
     * @param attributeName  Must be non-null and non-blank
     */
    public static int getIntAttributeWithDefault(Element element,
        String attributeName, int defaultValue)
    {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(attributeName, "attributeName");

        String value = element.getAttribute(attributeName);

        if (value.length() == 0) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ex) {
            throw new RuntimeException("The attribute " + attributeName
                + " in XML element " + element.getNodeName()
                + " must be given an integer value.");
        }
    }

    /**
     * Returns the attibute value if found and it is a non-negative integer.
     * If the attribute is missing returns the given default.
     * If the attribute is negative or not an integer, throws a RuntimeException.
     *
     * @param element        Must be non-null
     * @param attributeName  Must be non-null and non-blank
     */
    public static int getNonNegativeIntWithDefault(Element element,
        String attributeName, int defaultValue)
    {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(attributeName, "attributeName");

        String value = element.getAttribute(attributeName);

        if (value.length() == 0) {
            return defaultValue;
        }

        try {
            int i = Integer.parseInt(value);
            if (i < 0) {
                throw new RuntimeException("The attribute " + attributeName
                    + " in XML element " + element.getNodeName()
                    + " must be given a non-negative integer value.");
            }
            else  return i;
        }
        catch (NumberFormatException ex) {
            throw new RuntimeException("The attribute " + attributeName
                + " in XML element " + element.getNodeName()
                + " must be given an integer value.");
        }
    }

    /**
     * Returns the attribute value if found and it is an integer > 0.
     * If the attribute is missing returns the given default.
     * If the attribute is < 1 or not an integer, throws a RuntimeException.
     *
     * @param element        Must be non-null
     * @param attributeName  Must be non-null and non-blank
     */
    public static int getPositiveIntAttributeWithDefault(Element element,
        String attributeName, int defaultValue)
    {
        ArgUtil.notNull(element, "element");
        ArgUtil.notBlank(attributeName, "attributeName");

        String value = element.getAttribute(attributeName);

        if (value.length() == 0) {
            return defaultValue;
        }

        try {
            int i = Integer.parseInt(value);
            if (i < 1) {
                throw new RuntimeException("The attribute " + attributeName
                    + " in XML element " + element.getNodeName()
                    + " must be given an integer value greater than 0.");
            }
            else  return i;
        }
        catch (NumberFormatException ex) {
            throw new RuntimeException("The attribute " + attributeName
                + " in XML element " + element.getNodeName()
                + " must be given an integer value.");
        }
    }

    /**
     * Test program that takes an xml file and calls buildDocument.
     */
    public static void main(String [] args)
        throws Exception
    {
        if (args.length < 2) {
            System.err.println("usage: <xml file> <validate(t/f)>");
            System.exit(1);
        }

        String fileName = args[0];
        File file = new File(fileName);
        if (!file.isFile()) {
            throw new RuntimeException(fileName + " is not a file.");
        }

        String tf = args[1];
        boolean validate = tf.startsWith("t") || tf.startsWith("T");

        Document doc = buildDocument(file, validate);  // throws Exception
        System.out.println("document Java class = " + doc.getClass().getName());
    }
}
