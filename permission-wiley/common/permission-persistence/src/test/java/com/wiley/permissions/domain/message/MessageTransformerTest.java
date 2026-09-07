package com.wiley.permissions.domain.message;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;

import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.domain.message.Message.MessageEntity;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.DeliveryMethod;
import com.wiley.permissions.domain.persistence.permissions.GeographicalLocation;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.Medium;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.PermissionPayer;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.ProductEdition;
import com.wiley.permissions.domain.persistence.permissions.ProductFamily;
import com.wiley.permissions.domain.persistence.permissions.ProductLine;
import com.wiley.permissions.domain.persistence.permissions.ProductType;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SubjectCode;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;
import com.wiley.sf.common.monitor.PerformanceMonitor;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;


/**
 *
 * @author ttidwell
 */
public class MessageTransformerTest {

    public MessageTransformerTest() {
    }

	@BeforeClass
	public static void setUpClass()
	throws Exception
	{
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception
	{

	}

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

	@Test
	public void testMessageToXml()
	throws Exception
	{
		Message msg = generateMessage();

		String xmlMessage = messageToXML(msg);

		System.out.println(xmlMessage);
	}

	private String messageToXML(Message msg)
	throws Exception
	{
		JAXBContext jaxbContext = JAXBContext.newInstance(Message.class);

		Marshaller unmarshaller = jaxbContext.createMarshaller();

		unmarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		StringWriter writer = new StringWriter();

		unmarshaller.marshal(msg, writer);

		String xmlMessage = writer.toString();

		return xmlMessage;
	}

	private Message generateMessage()
	throws Exception
	{
		Message msg = new Message();

		msg.setId(UniqueIdentifierGenerator.getNextIdentifier());

		msg.getTargets().add(MessageEntity.CMS_US);
		msg.getTargets().add(MessageEntity.PERMISSIONS);

		MessageOperation op1 = new MessageOperation(OperationType.UPDATE_ASSET);

		Asset asset = getTestAsset();

		Product product = getTestProduct();
		CommonWork cw = new CommonWork();
		cw.getProducts().add(product);

		AssetUse au = getTestAssetUse();
		au.setAsset(asset);
		au.setCommonWork(cw);
		asset.getAssetUses().add(au);

		op1.addItem(asset);

		msg.getOperations().add(op1);

		MessageOperation op3 = new MessageOperation(OperationType.UPDATE_SOURCE);

		msg.getOperations().add(op3);

		MessageOperation op4 = new MessageOperation(OperationType.UPDATE_ASSET_USE);

		op4.addItem(au);

		msg.getOperations().add(op4);

		MessageOperation op5 = new MessageOperation(OperationType.PRODUCT_UPDATE);

		op5.addItem(product);

		msg.getOperations().add(op5);

		return msg;
	}

	public void testSchema()
	throws Exception
	{
		JAXBContext context = JAXBContext.newInstance(Message.class);

		MySchemaOutputResolver resolver = new MySchemaOutputResolver();

		context.generateSchema(resolver);

		System.out.println("Schema: " + resolver.getSchema());
	}

	@Test
	public void testXmlToMessage()
	throws Exception
	{
		Message sourceMessage = generateMessage();

		String xmlMessage = messageToXML(sourceMessage);

		JAXBContext jaxbContext = JAXBContext.newInstance(Message.class);

		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

		if (xmlMessage != null) {
			for (int i = 0; i < 100; i++) {
				PerfTimer timer = PerformanceMonitor.getInstance().startTimer("testTransformer");

				Message msg = (Message) unmarshaller.unmarshal(new InputSource(new StringReader(xmlMessage)));

				System.out.println(msg.getId());

				timer.stopTimer();
				System.out.println(PerformanceMonitor.getInstance().dumpStats());
			}
		}
		else {
			System.out.println("Ack! No Message!");

			throw new Exception("No Message Generated");
		}
	}

	private Asset getTestAsset() {
		Asset output = new Asset();

		output.setArtist("test artist");
		output.setCreditLine("Asset Credit Line");
		output.setExternalId("ext-asset-123");
		output.setId(0);
		output.setVendorId("in-001");
		output.setDescription("Test Asset");

		OwnerType ot = new OwnerType();

		ot.setCode("OT01");
		ot.setDescription("OnwerType");

		output.setOwnerType(ot);

		MediaType at = new MediaType();

		at.setCode("AT01");
		at.setDescription("AssetType");

		output.setMediaType(at);

		return output;
	}

	private Source getTestSource() {
		Source output = new Source();

		output.setCreditLine("Credit Line");
		output.setExternalId("ext-src-123");
		output.setId(0);

		DeliveryMethod dm = new DeliveryMethod();

		dm.setCode("DM01");
		dm.setDescription("DeliveryMethod");

		output.setDeliveryMethod(dm);

		return output;
	}

	private Product getTestProduct() {
		Product output = new Product();

		output.setId(0);
		output.setExternalId("ext-prod-001");
		output.setCopyrightYear(2008);
		output.setIsbn10("0123456789");
		output.setIsbn13("0123456789ABCD");
		output.setPnumber("PN1234567890");
		output.setPrintDate(new Date());
		output.setProductionDate(new Date());
		output.setShortTitle("Short Title");
		output.setSku("SKU1234567890");
		output.setTitle("LONG TITLE");
		output.setVolume("Volume 1");

		BusinessUnit bu = new BusinessUnit();
		bu.setCode("BU01");
		bu.setName("Business Unit");

		GeographicalLocation gl = new GeographicalLocation();
		gl.setCode("US");
		gl.setName("United States");

		Medium medium = new Medium();
		medium.setCode("MED01");
		medium.setName("Medium");

		PermissionPayer pp = new PermissionPayer();
		pp.setCode("PP01");
		pp.setDescription("Permission Payer");

		ProductFamily pf = new ProductFamily();
		pf.setCode("PF01");
		pf.setName("ProductFamily");

		ProductLine pl = new ProductLine();
		pl.setBusinessUnit(bu);
		pl.setCode("PL01");
		pl.setName("ProductLine");

		ProductEdition pe = new ProductEdition();
		pe.setEditionNumber(1);
		pe.setExternalId("ext-pe-123");
		pe.setId(0);
		pe.setName("Product Edition");
		pe.setProductLine(pl);

		ProductType pt = new ProductType();
		pt.setCode("PT01");
		pt.setName("Product Type");

		PublicationStatus ps = new PublicationStatus();
		ps.setCode("PS01");
		ps.setDescription("Test Publication Status");

		SubjectCode sc = new SubjectCode();
		sc.setCode("SC01");
		sc.setName("Subject Code");

		Role role1 = new Role();
		role1.setCode("AU");
		role1.setDescription("AUTHOR");

		Role role2 = new Role();
		role2.setCode("ED");
		role2.setDescription("EDITOR");

		User u1 = new User();
		//u1.setType(User.Type.AUTHOR);
		u1.setCode("AU01");
		u1.setEmail("author@wiley.com");
		u1.setFirstName("John");
		u1.setLastName("Author");

		User u2 = new User();
		//u2.setType(User.Type.EMPLOYEE);
		u2.setCode("jeditor");
		u2.setEmail("editor@wiley.com");
		u2.setFirstName("Jane");
		u2.setLastName("Editor");

		UserToRole up1 = new UserToRole();
		up1.setProduct(output);
		up1.setUser(u1);
		up1.setRole(role1);
		//u1.getProducts().add(up1);

		UserToRole up2 = new UserToRole();
		up2.setProduct(output);
		up2.setUser(u2);
		up2.setRole(role2);
		//u2.getProducts().add(up2);

		output.setBusinessUnit(bu);
		output.setEdition(pe);
		output.setLocation(gl);
		output.setMedium(medium);
		output.setPermissionPayer(pp);
		output.setProductFamily(pf);
		output.setProductLine(pl);
		output.setProductType(pt);
		output.setSubjectCode(sc);
		output.setPublicationStatus(ps);

		output.getUsers().add(up1);
		output.getUsers().add(up2);

		return output;
	}

	private AssetUse getTestAssetUse() {
		return new AssetUse();
	}

	class MySchemaOutputResolver extends SchemaOutputResolver {

		private final StringWriter swriter = new StringWriter();

		@Override
		public Result createOutput(String namespaceUri, String suggestedFileName)
		throws IOException
		{
			File tempDir = SystemUtils.getJavaIoTmpDir();
			StreamResult sr = new StreamResult(new File(tempDir, suggestedFileName));

			return sr;
		}

		public String getSchema() {
			return swriter.toString();
		}
	}
}