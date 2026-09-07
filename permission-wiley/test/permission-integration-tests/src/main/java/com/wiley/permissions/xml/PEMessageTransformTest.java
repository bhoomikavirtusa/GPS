package com.wiley.permissions.xml;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;

import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.common.transformer.ObjectToXml;
import com.wiley.permissions.common.transformer.XmlToObject;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.Message.MessageEntity;
import com.wiley.permissions.domain.message.Message.MessageType;
import com.wiley.permissions.domain.message.MessageOperation;
import com.wiley.permissions.domain.message.pe.ProductSearchMessage;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;
import com.wiley.sf.common.io.FileUtil;
import com.wiley.sf.common.lang.ResourceUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;
import com.wiley.sf.common.xml.XSLTransform;

/**
 * @author lnagy
 */
public class PEMessageTransformTest {

	public PEMessageTransformTest()
	{
	}

	@BeforeClass
	public static void setUpClass() throws Exception
	{
	}

	@AfterClass
	public static void tearDownClass() throws Exception
	{
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

	@Test
	public void peXmlToMessage() throws Exception {
		//peXmlFileToMessage("/UpdateProduct.xml", "UpdateProduct");
		peXmlFileToMessage("/MasterListUpdateNotification.xml", "MasterListUpdateNotification");
	}

	@Test
	public void testRequestUpdateProduct() throws Exception {
		String externalId = "CORE.001.PROD.0000175111";
		Message message = new Message();
		message.setId(UniqueIdentifierGenerator.getNextIdentifier());
		message.setType(MessageType.REQUEST);
		message.setSource(MessageEntity.PERMISSIONS);
		message.getTargets().add(MessageEntity.PRODUCT_ENGINEERING);
		MessageOperation op = message.createMessageOperation(OperationType.PRODUCT_UPDATE);

		/*
		if (dataSource != null) {
			MessageOperationParameter param = op.createMessageOperationParameter("dataSource", dataSource.name());
			op.getParameters().add(param);
		}*/

		Product tmp = new Product();

		tmp.setExternalId(externalId);
		tmp.setDataSource(DataSource.US.getCode());

		List<Object> products = new ArrayList<Object>();
		products.add(tmp);
		op.setItems(products);
		message.getOperations().add(op);
		peMessageToXml(message);
	}

	//@Test
	public void testXmlToProductSearchMessage1() throws Exception {
		InputStream input = ResourceUtil.getResourceInputStream(this.getClass(), "/products.xml");

		JAXBContext jaxbContext = JAXBContext.newInstance(ProductSearchMessage.class);

		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader r = factory.createXMLEventReader(input);

		XMLEventReader fr = new EventReaderDelegate(r) {

			@Override
			public XMLEvent nextEvent() throws XMLStreamException {
				try {
					XMLEvent event = super.nextEvent();

					if (event.isCharacters()) {
						XMLEventFactory ef = XMLEventFactory.newInstance();
						Characters comment = ef.createCharacters(XmlToObject
								.stripNonValidXMLCharacters(event.asCharacters().getData()));
						// System.out.println(comment.getData());
						return comment;
					}
					return event;
				}
				catch (Exception e) {
					XMLEventFactory ef = XMLEventFactory.newInstance();
					Characters comment = ef.createCharacters("INVALID char");
					return comment;
				}
			}
		};

		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		@SuppressWarnings("unused")
		ProductSearchMessage myObject = (ProductSearchMessage) unmarshaller.unmarshal(fr);
		OutputStreamWriter writer = new OutputStreamWriter(System.out);
		try {
			while (fr.hasNext()) {
				XMLEvent event = fr.nextEvent();
				event.writeAsEncodedUnicode(writer);
			}
		}
		finally {
			fr.close();
			r.close();
		}
	}

	// @Test
	public void testXmlToProductSearchMessage() throws Exception {
		InputStream input = ResourceUtil.getResourceInputStream(this.getClass(), "/products.xml");
		String xmlMessage = FileUtil.toString(input);
		//xmlMessage = StringEscapeUtils.unescapeXml(xmlMessage);
		//System.out.println(xmlMessage);
		xmlMessage = XmlToObject.stripNonValidXMLCharacters(xmlMessage);
		JAXBContext jaxbContext = JAXBContext.newInstance(ProductSearchMessage.class);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

		for (int i = 0; i < 100; i++) {
			PerfTimer timer = PerformanceMonitor.getInstance().startTimer("testTransformer");

			@SuppressWarnings("unused")
			ProductSearchMessage msg = (ProductSearchMessage) unmarshaller.unmarshal(new InputSource(
					new StringReader(xmlMessage)));

			timer.stopTimer();
			System.out.println(PerformanceMonitor.getInstance().dumpStats());
		}
	}

	private static void peMessageToXml(Message msg) throws Exception {
		String internalXml = ObjectToXml.objectToXml(msg);
		// throws JAXBException
		System.out.println("------ Permissions Message to internal format ----------");
		System.out.println(internalXml);

		String type = "NO OPERATION";
		try {
			type = msg.getOperations().get(0).getOperationType().name();
		}
		catch (Exception e) { }
		peXmlToMessage(internalXml, type);
	}

	public static void peXmlFileToMessage(String xmlResourceName, String type) throws Exception {
		String externalXml = ResourceUtil.getStringResource(PEMessageTransformTest.class, xmlResourceName);
		peXmlToMessage(externalXml, type);
	}

	public static void peXmlToMessage(String externalXml, String type) throws Exception {
		XSLTransform tf = new XSLTransform();
		tf.setXml(externalXml);
		tf.setXsl(PEMessageTransformTest.class, "/xsl/pe-notification-to-permissions.xsl");
		// throws IOException, FileNotFoundException
		String internalXml = tf.transformToString();
		// throws TransformerConfigurationException, TransformerException

		System.out.println("------ " + type + ": Permissions internal format ----------");
		System.out.println(internalXml);

		Message msg = (Message) XmlToObject.xmlToObject(Message.class, internalXml); // throws JAXBException

		internalXml = ObjectToXml.objectToXml(msg);

		System.out.println("------ " + type + ": Permissions internal format (again) ----------");
		System.out.println(internalXml);

		tf = new XSLTransform();
		tf.setXml(internalXml);

		// smarkoff - we don't have the file permissions-to-pe.xsl anymore -- nor do we need
		// to bother to create one for notifications -- only need for request
		tf.setXsl(PEMessageTransformTest.class, "/xsl/permissions-to-pe.xsl");
		// throws IOException, FileNotFoundException

		externalXml = tf.transformToString();
		// throws TransformerConfigurationException, TransformerException

		System.out.println("------ " + type + ": Permissions external format ----------");
		System.out.println(externalXml);
	}

	public static void main(String[] args) throws Exception {
		// just an easy way to run just one of the tests
		// (another way is to use @Ignore on the other tests)

		PEMessageTransformTest test = new PEMessageTransformTest();
		test.peXmlToMessage();
		//test.updateComponentMessageToXml();
	}
}