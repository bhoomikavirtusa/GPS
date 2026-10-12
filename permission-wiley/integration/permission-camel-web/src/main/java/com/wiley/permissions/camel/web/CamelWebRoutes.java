package com.wiley.permissions.camel.web;

import org.apache.camel.builder.RouteBuilder;

import com.wiley.permissions.camel.web.processor.CompCopyUrlProcessor;
import com.wiley.permissions.camel.web.processor.HttpGetProcessor;
import com.wiley.permissions.camel.web.processor.HttpPostXmlProcessor;
import com.wiley.permissions.camel.web.processor.ObjectToXmlProcessor;
import com.wiley.permissions.camel.web.processor.XmlToObjectProcessor;
import com.wiley.permissions.camel.web.processor.XsltProcessor;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.pe.ProductSearchMessage;
import com.wiley.permissions.domain.persistence.permissions.CompCopy;

/**
 * Camel WEB routes mirroring Mule WEB vm:// endpoints
 * (services-mule-config + pesearch-mule-config).
 */
public class CamelWebRoutes extends RouteBuilder {

	private String peHost;
	private String pePort;
	private String peTimeoutMs = "60000";
	private String compCopyUrl;
	private String compCopyAuUrl;
	private String muleHostBind = "0.0.0.0";
	private String localPePort = "63081";

	public void setPeHost(String peHost) {
		this.peHost = peHost;
	}

	public void setPePort(String pePort) {
		this.pePort = pePort;
	}

	public void setPeTimeoutMs(String peTimeoutMs) {
		this.peTimeoutMs = peTimeoutMs;
	}

	public void setCompCopyUrl(String compCopyUrl) {
		this.compCopyUrl = compCopyUrl;
	}

	public void setCompCopyAuUrl(String compCopyAuUrl) {
		this.compCopyAuUrl = compCopyAuUrl;
	}

	public void setMuleHostBind(String muleHostBind) {
		this.muleHostBind = muleHostBind;
	}

	public void setLocalPePort(String localPePort) {
		this.localPePort = localPePort;
	}

	@Override
	public void configure() throws Exception {
		ObjectToXmlProcessor objectToXml = new ObjectToXmlProcessor();
		XmlToObjectProcessor peSearchXml = new XmlToObjectProcessor(ProductSearchMessage.class);
		XmlToObjectProcessor peMessageXml = new XmlToObjectProcessor(Message.class);
		XmlToObjectProcessor compCopyXml = new XmlToObjectProcessor(CompCopy.class);
		XsltProcessor requestToPe = new XsltProcessor("/xsl/permissions-request-to-pe.xsl");
		XsltProcessor peReply = new XsltProcessor("/xsl/pe-reply-to-permissions.xsl");
		HttpGetProcessor httpGet = new HttpGetProcessor();

		int peTimeout = Integer.parseInt(peTimeoutMs);
		HttpPostXmlProcessor peSearchPost = new HttpPostXmlProcessor(
				"http://" + peHost + ":" + pePort + "/CGI-BIN/JSMDIRECT?PEPRODUCTSEARCH",
				peTimeout, peTimeout);
		HttpPostXmlProcessor peDetailPost = new HttpPostXmlProcessor(
				"http://" + peHost + ":" + pePort + "/CGI-BIN/JSMDIRECT?GETPEPRODUCTDETAIL",
				peTimeout, peTimeout);
		HttpPostXmlProcessor localPePost = new HttpPostXmlProcessor(
				"http://" + muleHostBind + ":" + localPePort + "/service/requestProduct/REQUEST",
				60000, 60000);

		from("direct:gps.camel.web.ping")
				.routeId("gps.camel.web.ping")
				.setBody(constant("pong"));

		configureBeanRoutes();
		configurePeRoutes(objectToXml, peSearchXml, peMessageXml, requestToPe, peReply,
				peSearchPost, peDetailPost, localPePost);
		configureCompCopyRoutes(compCopyXml, httpGet);
	}

	private void configureBeanRoutes() {
		beanRoute("assetService.handleUpdateAssetMessage", "services.AssetService", "handleUpdateAssetMessage");
		beanRoute("assetService.handleDeleteAssetMessage", "services.AssetService", "handleDeleteAssetMessage");
		beanRoute("assetService.handleGetAssetMessage", "services.AssetService", "handleGetAssetMessage");

		beanRoute("productService.saveProducts", "services.ProductService", "saveProducts");
		beanRoute("productService.saveMasterLists", "services.ProductService", "saveMasterLists");
		beanRoute("productService.saveProduct", "services.ProductService", "saveProduct");
		beanRoute("productService.updateCommonWork", "services.ProductService", "updateCommonWork");
		beanRoute("productService.handleErrorMessageOp", "services.ProductService", "handleErrorMessageOp");

		beanRoute("localProductService.updateCommonWorkFromPE", "services.LocalProductService", "updateCommonWorkFromPE");

		beanRoute("commonWorkService.handleUpdateComponentMessage", "services.CommonWorkService", "handleUpdateComponentMessage");
		beanRoute("commonWorkService.handleGetComponentMessage", "services.CommonWorkService", "handleGetComponentMessage");
		beanRoute("commonWorkService.handleDeleteComponentMessage", "services.CommonWorkService", "handleDeleteComponentMessage");
		beanRoute("commonWorkService.handleGetComponentListMessage", "services.CommonWorkService", "handleGetComponentListMessage");

		beanRoute("assetUseService.handleUpdateAssetUseMessage", "services.AssetUseService", "handleUpdateAssetUseMessage");
		beanRoute("assetUseService.handleDeleteAssetUseMessage", "services.AssetUseService", "handleDeleteAssetUseMessage");
		beanRoute("assetUseService.handleGetAssetUseMessage", "services.AssetUseService", "handleGetAssetUseMessage");
		beanRoute("assetUseService.handleGetPerformanceStats", "services.AssetUseService", "handleGetPerformanceStats");

		beanRoute("sourceService.handleDeleteSource", "services.SourceService", "handleDeleteSource");

		beanRoute("permissionStatusValidationService.updatePermissionStatusForAssetUseCollection",
				"services.PermissionStatusValidationService", "updatePermissionStatusForAssetUseCollection");

		from("direct:messageCache.cacheMessage")
				.routeId("messageCache.cacheMessage")
				.to("bean:repository.CachedMessageRepository?method=cacheMessage");
		from("direct:messageCache.postProcessMessage")
				.routeId("messageCache.postProcessMessage")
				.to("bean:repository.CachedMessageRepository?method=postProcessMessage");
		from("direct:messageCache.preProcessMessage")
				.routeId("messageCache.preProcessMessage")
				.to("bean:repository.CachedMessageRepository?method=preProcessMessage");
	}

	private void beanRoute(String directPath, String beanId, String method) {
		from("direct:" + directPath)
				.routeId(directPath)
				.to("bean:" + beanId + "?method=" + method);
	}

	private void configurePeRoutes(ObjectToXmlProcessor objectToXml,
			XmlToObjectProcessor peSearchXml,
			XmlToObjectProcessor peMessageXml,
			XsltProcessor requestToPe,
			XsltProcessor peReply,
			HttpPostXmlProcessor peSearchPost,
			HttpPostXmlProcessor peDetailPost,
			HttpPostXmlProcessor localPePost) {

		from("direct:services.PEProductSearch")
				.routeId("services.PEProductSearch")
				.process(objectToXml)
				.process(peSearchPost)
				.process(peSearchXml);

		from("direct:services.PEProductService")
				.routeId("services.PEProductService")
				.process(objectToXml)
				.process(requestToPe)
				.process(peDetailPost)
				.process(peReply)
				.process(peMessageXml);

		from("direct:services.LocalPEProductService")
				.routeId("services.LocalPEProductService")
				.process(objectToXml)
				.process(requestToPe)
				.process(localPePost)
				.process(peReply)
				.process(peMessageXml);
	}

	private void configureCompCopyRoutes(XmlToObjectProcessor compCopyXml, HttpGetProcessor httpGet) {
		from("direct:services.CompCopyService")
				.routeId("services.CompCopyService")
				.process(new CompCopyUrlProcessor(compCopyUrl, false))
				.process(httpGet)
				.process(compCopyXml);

		from("direct:services.CompCopyAuService")
				.routeId("services.CompCopyAuService")
				.process(new CompCopyUrlProcessor(compCopyAuUrl, true))
				.process(httpGet)
				.process(compCopyXml);

		from("direct:services.RightsLinkLicenseService")
				.routeId("services.RightsLinkLicenseService")
				.process(exchange -> {
					@SuppressWarnings("unchecked")
					java.util.Map<String, Object> params =
							exchange.getIn().getBody(java.util.Map.class);
					Object license = params != null ? params.get("licenseNumber") : null;
					String url = "https://dem100.copyright.com/svc-rightslink-rest/rest/licenses/"
							+ license + ".xml";
					exchange.getIn().setHeader(CompCopyUrlProcessor.HEADER_URL, url);
					exchange.getIn().setBody(null);
				})
				.process(httpGet);
	}
}
