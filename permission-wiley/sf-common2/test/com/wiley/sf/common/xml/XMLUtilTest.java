package com.wiley.sf.common.xml;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.text.NumberFormat;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.wiley.sf.common.lang.StringUtil;

/**
 * JUnit test class for XMLUtil.
 *
 * @since   JDK 1.6, JUnit 4.10, Xerces 2.9.1
 * @version 5/3/2012
 * @author  Steve Markoff
 */
public class XMLUtilTest {

    private final static String xmlBeg =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";

    private final static String tagWithUnicode =
        "<blah att1=\"8bit chars: \u00b1\u00b6\u00a2\""
        + " att2=\"double-byte: \u2105\u211E\u2122\" />\n";

    private final static String bodyXML =
        "<dummy>\n"
        + tagWithUnicode
        + "</dummy>\n";

    private final static String soapXML = xmlBeg
        + "<soapenv:Envelope xmlns:soapenv=\"blah\">\n"
        + "<soapenv:Header> </soapenv:Header>\n"
        + "<soapenv:Body>\n" + bodyXML + "</soapenv:Body>\n"
        + "</soapenv:Envelope>\n";

    private final static String xmlDoc2 = xmlBeg
        + "<dummy>\n"
        + "  <blah></blah>\n"
        + "  <foo />\n"
        + "</dummy>\n";


    @Test
    public void createDocument() throws Exception {
        Document doc = XMLUtil.createDocument("foo");
        createDocument(doc, "<foo/>");

        doc = XMLUtil.createDocument("foo", null, null, null);
        createDocument(doc, "<foo/>");

        doc = XMLUtil.createDocument("foo", null, null, "http://www.foo.com/foo");
        createDocument(doc, "<foo xmlns=\"http://www.foo.com/foo\"/>");

        doc = XMLUtil.createDocument("foo", "http://www.foo.com/foo.dtd",
            "foo.dtd", "http://www.foo.com/foo");
        createDocument(doc, "<!DOCTYPE foo "
            + "PUBLIC \"http://www.foo.com/foo.dtd\" \"foo.dtd\">\r\n"
            + "<foo xmlns=\"http://www.foo.com/foo\"/>");
    }

    private void createDocument(Document doc, String expected) throws Exception {
        String result = XMLUtil.nodeToString(doc, false);
        String msg = "expected: " + expected + "\r\nactual: " + result;
        assertTrue(msg, StringUtil.equalsIgnoreWhitespace(expected, result));
    }

    @Test
    public void addDocType() throws Exception {
        Document doc = XMLUtil.createDocument("HPManifest");
        XMLUtil.addDocType(doc, "HPManifest", "-//JWS//DTD Manifest File V1.0//EN", "");
        createDocument(doc, "<!DOCTYPE HPManifest PUBLIC \"-//JWS//DTD Manifest File V1.0//EN\" \"\">\r\n<HPManifest/>");
    }

    @Test
    public void extractSoapBody()
        throws Exception
    {
        String out = XMLUtil.extractSoapBody(soapXML);  // throws Exception
        // out will have "<?xml.." in the beginning so remove it before comparison
        int index = out.indexOf("<dummy");
        out = out.substring(index);
        String expected = bodyXML;
        assertTrue(StringUtil.equalsIgnoreWhitespace(out, expected));
    }

    @Test
    public void nodeToString() throws Exception {
		nodeToString(null, null);

        String docXml = xmlBeg + bodyXML;
        Document doc = XMLUtil.buildDocument(docXml, false);  // throws Exception
        Node node = doc.getElementsByTagName("blah").item(0);

        nodeToString(doc, docXml);
        nodeToString(node, tagWithUnicode);

        // test indentation - result is not quite what we expect
        // - we expect <root> to be on second line
        docXml = "<root><level1><level2 /></level1></root>";
        doc = XMLUtil.buildDocument(docXml, false);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>\r\n"
            + "    <level1>\r\n"
            + "        <level2/>\r\n"
            + "    </level1>"
            + "</root>";
        nodeToString(doc, expected);

        // Test that spaces/newlines not taken away in middle of this
        docXml = "<pre>\r\nline 1\r\n  line 2\r\n\r\n</pre>";
        doc = XMLUtil.buildDocument(docXml, false);  // throws Exception
        nodeToString2(doc, "<pre>\r\nline 1\r\n  line 2\r\n\r\n</pre>\r\n");

        // Test that spaces/newlines not added in middle of this
        // - this is ok but see following test
        docXml = "<p>H<sub>2</sub>O</p>";
        doc = XMLUtil.buildDocument(docXml, false);  // throws Exception
        nodeToString2(doc, "<p>H<sub>2</sub>O</p>\r\n");

        // Test that spaces/newlines not added in middle of this
        // - does add newline+indent after </i> - this is a bug I think
        // - same thing happens with deprecated Xerces serialization api
        // (bug exists in Xerces 2.9.1 and JDK 1.6.0_07)
        docXml = "<p>F<i>k</i><sub>a</sub></p>";
        doc = XMLUtil.buildDocument(docXml, false);  // throws Exception
        nodeToString2(doc, "<p>F<i>k</i>\r\n    <sub>a</sub>\r\n</p>\r\n");
    }

    private void nodeToString(Node node, String expected) throws Exception {
        String result = XMLUtil.nodeToString(node);  // throws Exception
        String msg = "expected: " + expected + "\r\nresult: " + result;
        assertTrue(msg, StringUtil.equalsIgnoreWhitespace(expected, result));
    }

    // says not to do xml declaration and keeps whitespace
    private void nodeToString2(Node node, String expected) throws Exception {
        String result = XMLUtil.nodeToString(node, false);  // throws Exception
        expected = expected.replace('\r', 'R');
        expected = expected.replace('\n', 'N');
        expected = expected.replace('\t', 'T');
        result = result.replace('\r', 'R');
        result = result.replace('\n', 'N');
        result = result.replace('\t', 'T');
        String msg = "expected: " + expected + "\r\nresult: " + result;
        assertTrue(msg, StringUtils.equals(expected, result));
    }

    @Test
    public void nodeListToString() throws Exception {
        nodeListToString(null, null);

        String lines = "<line att1=\"foo\" />\n"
            + "<line>blah</line>\n"
            + "<line att1=\"foo\">blah</line>\n";
        String xml = "<foo>\n" + lines + "</foo>";
        Document doc = XMLUtil.buildDocument(xml, false);  // throws various exceptions
        NodeList list = doc.getElementsByTagName("line");
        nodeListToString(list, lines);
        // test empty list
        list = doc.getElementsByTagName("blah");
        nodeListToString(list, "");
    }

    private void nodeListToString(NodeList list, String expected) throws Exception {
        String result = XMLUtil.nodeListToString(list);
        String msg = "expected: " + expected + "\r\nresult: " + result;
        assertTrue(msg, StringUtil.equalsIgnoreWhitespace(expected, result));
    }

    //@Test
    public void replaceText() {
        // fill in
    }

    //@Test
    public void getFirstElementChildNode() {
        // fill in
    }

    @Test
    public void showNodeTree() throws Exception {
        showNodeTree(null, null);

        // Attributes of an element are not considered child nodes,
        // but the showNodeTree() method treats them as children and prints them
        // out before other children - but does not include them in #children.
        String xml =
            "<person name=\"Fred\">\n"
            + "<address>\n"
            + "<zip>94103</zip>\n"
            + "textblah\n"
            + "</address>\n"
            + "</person>";
        String expected = "level 0: name = #document, type = Document, value = null, #children = 1\n"
            + "level 1: name = person, type = Element, value = null, #children = 3\n"
            + "level 2: name = name, type = Attribute, value = Fred, #children = 1\n"
            + "level 3: name = #text, type = Text, value = Fred, #children = 0\n"
            + "level 2: name = #text, type = Text, value =, #children = 0\n"
            + "level 2: name = address, type = Element, value = null, #children = 3\n"
            + "level 3: name = #text, type = Text, value =, #children = 0\n"
            + "level 3: name = zip, type = Element, value = null, #children = 1\n"
            + "level 4: name = #text, type = Text, value = 94103, #children = 0\n"
            + "level 3: name = #text, type = Text, value = textblah, #children = 0"
            + "level 2: name = #text, type = Text, value =, #children = 0";
        showNodeTree(xml, expected);
    }

    private void showNodeTree(String xml, String expected) throws Exception {
        Document doc = XMLUtil.buildDocument(xml, false);  // throws various exceptions
        String result = XMLUtil.showNodeTree(doc);
        // Turn this into separate method on StringUtil
        /*
        String e1 = StringUtil.deleteWhitespace(expected);
        String r1 = StringUtil.deleteWhitespace(result);
        if (e1 != null) {
            for (int i = 0; i < e1.length(); i++) {
                char ec = e1.charAt(i);
                char rc = r1.charAt(i);
                if (ec != rc) {
                    System.out.println("different at char " + i + " - expected [" + ec + "] result [" + rc + "]");
                    System.out.println(e1.substring(i - 5, i + 5));
                    System.out.println(r1.substring(i - 5, i + 5));
                }
            }
        }*/
        String msg = "expected: " + expected + "\nresult: " + result;
        assertTrue(msg, StringUtil.equalsIgnoreWhitespace(expected, result));
    }

    //getText

    //getTextList

    //getOptionalChildElement

    //getRequiredChildElement

    @Test
    public void checkChildElements()
        throws Exception
    {
        String xml = xmlBeg + bodyXML;
        Document doc = XMLUtil.buildDocument(xml, false);  // throws Exception
        XMLUtil.checkChildElements(doc.getDocumentElement(), "blah");
            // should not throw an exception in this case

        try {
            XMLUtil.checkChildElements(doc.getDocumentElement(), null);
                // should throw an exception in this case
            fail();
        }
        catch (RuntimeException ex) { }

        doc = XMLUtil.buildDocument(xmlDoc2, false);  // throws Exception
        try {
            XMLUtil.checkChildElements(doc.getDocumentElement(), "blah");
                // should throw an exception in this case
            fail();
        }
        catch (RuntimeException ex) { }
    }
/*
    public void checkChildElementsMulti() {
        // fill in
    }

    public void checkAttributes() {
        // fill in
    }

    public void checkAttributesMulti() {
        // fill in
    }
*/
    // a lot more methods that could be tested here

    @Test
    public void unwrapXHTMLBody() {
        String html =
            "<html>\r\n" +
            "    <body>\r\n" +
            "        <p>foo</p>\r\n" +
            "        <p>2</p>\r\n" +
            "    </body>\r\n" +
            "</html>\r\n";
        String expected = "<p>foo</p>\r\n" +
                        "<p>2</p>\r\n";
        unwrapXHTMLBody(html, expected);

        html =
            "<html>\r\n" +
            "\t<body>\r\n" +
            "    \r\n" +
            "\t\t<p>foo</p>\r\n" +
            "\t\t<p>2</p>\r\n" +
            "\t</body>\r\n" +
            "</html>\r\n";
        expected = "<p>foo</p>\r\n" +
                    "<p>2</p>\r\n";
        unwrapXHTMLBody(html, expected);

        html =
            "<html>\r\n" +
            "\t<body/>\r\n" +
            "</html>\r\n";
        expected = "";
        unwrapXHTMLBody(html, expected);
    }

    private void unwrapXHTMLBody(String html, String expected) {
        String result = XMLUtil.unwrapXHTMLBody(html, true);
        String msg = "input: " + html + "\nexpected: " + expected
            + "\nresult: " + result;
        assertTrue(msg, result.equals(expected));
    }


    /**
     * Calls memoryTest().
     */
    public static void main(String [] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: [xml file]");
            System.exit(1);
        }

        memoryTest(new File(args[0]));  // throws Exception
    }

    /**
     * Test the memory usage and garbage collection of two methods that are
     * used a lot: buildDocument() and nodeToString().
     * (Might also test validateXHTML() later.)
     *
     * Warning: buildDocument() can be very slow if the document
     * references a dtd or xml schema that is on the Internet
     * (common example = XHTML dtd)
     * because even when not validating it will try to load the
     * referenced document. So, if testing an xhtml document, just remove
     * the DOCTYPE for testing purposes.
     *
     * @param xmlFile  Must be an existing xmlFile
     */
    public static void memoryTest(File xmlFile) throws Exception {
        if (!xmlFile.exists() || !xmlFile.isFile()) {
            throw new IllegalArgumentException("xmlFile "
                + xmlFile.getPath() + " not found.");
        }

        NumberFormat intFormat = NumberFormat.getIntegerInstance();
        Runtime rt = Runtime.getRuntime();

        System.out.println("xmlFile size: "
            + intFormat.format(xmlFile.length()) + " bytes.");

        long startTime = System.currentTimeMillis();
        final int loop = 9000;

        for (int i = 0; i < loop; i++) {
            if (i % 1000 == 0) {
				System.out.print("(i = " + i + "): ");
                long freeMemory = rt.freeMemory();
                long totalMemory = rt.totalMemory();
                long usedMemory = totalMemory - freeMemory;
                System.out.println("memory used|free|total (KB): "
                    + intFormat.format(usedMemory / 1000) + " | "
                    + intFormat.format(freeMemory / 1000) + " | "
                    + intFormat.format(totalMemory / 1000));
            }

            Document doc = XMLUtil.buildDocument(xmlFile, false);
                // throws various exceptions
            XMLUtil.nodeToString(doc);
                // ignore return String
        }

        long time = System.currentTimeMillis() - startTime;
        System.out.println("total time was " + intFormat.format(time) + " ms.");
        long avgTime = time / loop;
        System.out.println("avg time per iteration was "
            + intFormat.format(avgTime) + " ms.");
    }

}

/***********
Some test results for above memoryTest:
(Tested with JDK 1.6 + Xerces 2.9.1 running completely standalone [not in Eclipse])

[Small document]
xmlFile size: 2,451 bytes.
(i = 0): memory used|free|total (KB): 424 | 4,752 | 5,177
(i = 1000): memory used|free|total (KB): 1,948 | 3,228 | 5,177
(i = 2000): memory used|free|total (KB): 1,949 | 3,228 | 5,177
(i = 3000): memory used|free|total (KB): 1,947 | 3,229 | 5,177
(i = 4000): memory used|free|total (KB): 1,949 | 3,228 | 5,177
(i = 5000): memory used|free|total (KB): 1,947 | 3,229 | 5,177
(i = 6000): memory used|free|total (KB): 1,949 | 3,228 | 5,177
(i = 7000): memory used|free|total (KB): 1,947 | 3,229 | 5,177
(i = 8000): memory used|free|total (KB): 1,949 | 3,228 | 5,177
total time was 15,015 ms.
avg time per iteration was 1 ms.

[Very large document]
xmlFile size: 618,400 bytes.
(i = 0): memory used|free|total (KB): 424 | 4,752 | 5,177
(i = 1000): memory used|free|total (KB): 13,741 | 3,916 | 17,657
(i = 2000): memory used|free|total (KB): 13,741 | 3,916 | 17,657
(i = 3000): memory used|free|total (KB): 13,740 | 3,900 | 17,641
(i = 4000): memory used|free|total (KB): 13,569 | 4,072 | 17,641
(i = 5000): memory used|free|total (KB): 13,568 | 4,089 | 17,657
(i = 6000): memory used|free|total (KB): 11,090 | 6,550 | 17,641
(i = 7000): memory used|free|total (KB): 13,737 | 1,655 | 15,392
(i = 8000): memory used|free|total (KB): 13,909 | 368 | 14,278
total time was 1,405,281 ms.
avg time per iteration was 156 ms.
***********/
