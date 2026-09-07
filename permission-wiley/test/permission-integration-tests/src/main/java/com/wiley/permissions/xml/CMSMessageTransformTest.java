package com.wiley.permissions.xml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.common.transformer.ObjectToXml;
import com.wiley.permissions.common.transformer.XmlToObject;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.Message.MessageEntity;
import com.wiley.permissions.domain.message.MessageError;
import com.wiley.permissions.domain.message.MessageOperation;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.RenditionType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.sf.common.lang.ResourceUtil;
import com.wiley.sf.common.xml.XSLTransform;

/**
 *
 * @author ttidwell
 */
public class CMSMessageTransformTest {

	public CMSMessageTransformTest()
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
	public void cmsXmlToMessage() throws Exception
	{
		//cmsXmlFileToMessage("/UpdateAsset.xml", "UpdateAsset");
		//cmsXmlFileToMessage("/GetAsset.xml", "GetAsset");
		//cmsXmlFileToMessage("/DeleteAsset.xml", "DeleteAsset");

		//cmsXmlFileToMessage("/Error.xml", "Error");

		//cmsXmlFileToMessage("/UpdateAssetUse.xml", "UpdateAssetUse");

		//cmsXmlFileToMessage("/GetSource.xml", "GetSource");
		//cmsXmlFileToMessage("/UpdateSource.xml", "UpdateSource");

		cmsXmlFileToMessage("/UpdateComponent.xml", "UpdateComponent");

		//cmsXmlFileToMessage("/ReportComponentList.xml", "ReportComponentList");
	}

	@Test
	public void updateAssetMessageToXml() throws Exception
	{
		Message msg = new Message();
		msg.setId("PERM.xxxx");
		msg.setSource(Message.MessageEntity.PERMISSIONS);
		msg.setType(Message.MessageType.NOTIFICATION);
		msg.setDirection(Message.MessageProcessingDirection.OUTGOING);
		// n/a in this case - for messages we receive I think
		//msg.setStatus(Message.MessageStatus.PROCESSED);
		// n/a in this case
		//msg.setReplyID("foo");
		msg.setSent(System.currentTimeMillis());
		msg.setProtocolVersion("1.0");
		List<MessageEntity> targetList = new ArrayList<MessageEntity>();
		targetList.add(MessageEntity.CMS_US);
		msg.setTargets(targetList);
		List<MessageOperation> opList = new ArrayList<MessageOperation>();
		MessageOperation msgOp = new MessageOperation();
		msgOp.setOperationType(OperationType.UPDATE_ASSET);

		List<Object> itemList = new ArrayList<Object>();
		Asset asset = createAsset();
		itemList.add(asset);
		msgOp.setItems(itemList);
		opList.add(msgOp);
		msg.setOperations(opList);
		// failure n/a in this case but put anyway for testing
		//msg.setFailureCode(MessageFailureCode.INTERNAL_SYSTEM);
		//msg.setLastFailureMessage(new Exception("Exception message"));
		ArrayList<MessageError> errorList = new ArrayList<MessageError>();
		errorList.add(new MessageError("412", "(dummy message)"));
		msg.setErrorList(errorList);

		cmsMessageToXml(msg);
	}

	private Asset createAsset()
	{
		Asset asset = new Asset();
		asset.setCreatedDate(new Date());
		asset.setCreditLine("Credit Line");
		asset.setArtist("Artist");
		asset.setDescription("Description");
		asset.setFeeRequired(true);
		asset.setId(1);
		asset.setExternalId("perm.asset.1");
		asset.setMediaType(MediaType.PHOTO);
		asset.setOwnerType(OwnerType.THIRD_PARTY);
		asset.setManaged(true);
		asset.setVendorId("vendor id");
		asset.setLastUpdatedDate(new Date());
		Asset copiedFromAsset = new Asset();
		copiedFromAsset.setExternalId("copied from external id");
		asset.setCopiedFrom(copiedFromAsset);

		Source source = new Source();
		source.setExternalId("perm.source.1");
		List<Source> sourceSet = new ArrayList<Source>();
		sourceSet.add(source);
		asset.setSources(sourceSet);

		List<AssetFile> files = new ArrayList<AssetFile>();
		AssetFile file = new AssetFile();

		byte[] data = new byte[80];
		for (int i = 0; i < data.length; i++)
		{
			data[i] = (byte) i;
		}
		file.setData(data);
		file.setObjectName("foo");
		file.setFileFormat("image/jpeg");
		file.setRenditionType(RenditionType.MEDIUM_THUMBNAIL);

		files.add(file);
		asset.setFiles(files);

		return asset;
	}

	@Test
	public void updateAssetUseMessageToXml() throws Exception
	{
		Message msg = new Message();
		msg.setId("PERM.xxxx");
		msg.setSource(Message.MessageEntity.PERMISSIONS);
		msg.setType(Message.MessageType.NOTIFICATION);
		msg.setDirection(Message.MessageProcessingDirection.OUTGOING);
		msg.setSent(System.currentTimeMillis());
		msg.setProtocolVersion("1.0");
		List<MessageEntity> targetList = new ArrayList<MessageEntity>();
		targetList.add(MessageEntity.CMS_US);
		msg.setTargets(targetList);
		List<MessageOperation> opList = new ArrayList<MessageOperation>();
		MessageOperation msgOp = new MessageOperation();
		msgOp.setOperationType(OperationType.UPDATE_ASSET_USE);

		AssetUse au = new AssetUse();
		Asset asset = createAsset();
		au.setAsset(asset);
		Product product = new Product();
		product.setExternalId("perm.product.1");
		CommonWork cw = new CommonWork();
		cw.getProducts().add(product);
		au.setCommonWork(cw);
		// add Component
		au.setCreatedDate(new Date());
		au.setId(1);
		au.setLastUpdatedDate(new Date());
		au.setPosition("Figure 1");
		au.setManuscriptPage("45");
		au.setFinalPage("55");
		//au.setSize();
		au.setReusedFromPreviousEdition(true);
		au.setPermissionComment("permission Comment");

		List<Object> itemList = new ArrayList<Object>();
		itemList.add(au);
		msgOp.setItems(itemList);
		opList.add(msgOp);
		msg.setOperations(opList);

		cmsMessageToXml(msg);
	}

	@Test
	public void updateComponentMessageToXml() throws Exception
	{
		Message msg = new Message();
		msg.setId("PERM.xxxx");
		msg.setSource(Message.MessageEntity.PERMISSIONS);
		msg.setType(Message.MessageType.NOTIFICATION);
		msg.setDirection(Message.MessageProcessingDirection.OUTGOING);
		msg.setSent(System.currentTimeMillis());
		msg.setProtocolVersion("1.0");
		List<MessageEntity> targetList = new ArrayList<MessageEntity>();
		targetList.add(MessageEntity.CMS_US);
		msg.setTargets(targetList);
		List<MessageOperation> opList = new ArrayList<MessageOperation>();
		MessageOperation msgOp = new MessageOperation();
		msgOp.setOperationType(OperationType.UPDATE_COMPONENT);

		Component comp = new Component();
		comp.setExternalId("perm.component.1");
		comp.setId(1);
		comp.setName("component name");
		Product product = new Product();
		product.setExternalId("perm.product.1");
		CommonWork cw = new CommonWork();
		cw.getProducts().add(product);
		comp.setCommonWork(cw);
		ComponentCategory cat = new ComponentCategory();
		cat.setCode("ABC");
		comp.setCategory(cat);
		comp.setCreatedDate(new Date());
		comp.setLastUpdatedDate(new Date());

		List<Object> itemList = new ArrayList<Object>();
		itemList.add(comp);
		msgOp.setItems(itemList);
		opList.add(msgOp);
		msg.setOperations(opList);

		cmsMessageToXml(msg);
	}

	private static void cmsMessageToXml(Message msg) throws Exception
	{

		String internalXml = ObjectToXml.objectToXml(msg);
		// throws JAXBException

		String type = "NO OPERATION";
		try
		{
			type = msg.getOperations().get(0).getOperationType().name();
		}
		catch (Exception e)
		{
		}
		cmsXmlToMessage(internalXml, type);
	}

	public static void cmsXmlFileToMessage(String xmlResourceName, String type) throws Exception
	{
		String externalXml = ResourceUtil.getStringResource(CMSMessageTransformTest.class, xmlResourceName);

		cmsXmlToMessage(externalXml, type);
	}

	public static void cmsXmlToMessage(String externalXml, String type) throws Exception
	{
		XSLTransform tf = new XSLTransform();
		tf.setXml(externalXml);
		tf.setXsl(CMSMessageTransformTest.class, "/xsl/cms-to-permissions.xsl");
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
		tf.setXsl(CMSMessageTransformTest.class, "/xsl/permissions-to-cms.xsl");
		// throws IOException, FileNotFoundException
		externalXml = tf.transformToString();
		// throws TransformerConfigurationException, TransformerException

		System.out.println("------ " + type + ": Permissions external format ----------");
		System.out.println(externalXml);
	}

	public static void main(String[] args) throws Exception
	{
		// just an easy way to run just one of the tests
		// (another way is to use @Ignore on the other tests)

		CMSMessageTransformTest test = new CMSMessageTransformTest();
		test.cmsXmlToMessage();
		//test.updateComponentMessageToXml();
	}

}