package com.wiley.permissions.camel.web;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.container.ContainerFactory;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.wiley.permissions.resources.AssetResource;
import com.wiley.permissions.resources.ProductResource;
import com.wiley.permissions.resources.SourceResource;
import com.wiley.permissions.resources.UserResource;

/**
 * Hosts Jersey resources on {@code rest.port.internal} (63082) the same paths as
 * Mule rest-services-mule-config. Soft-fails if the port is still held by Mule WEB
 * during the dual-run window; restart Tomcat after {@code perm-mule-web.sh stop}.
 */
public class CamelWebRestServer implements InitializingBean, DisposableBean {

	private static final Log log = LogFactory.getLog(CamelWebRestServer.class);

	private boolean enabled;
	private String bindAddress = "0.0.0.0";
	private int port = 63082;

	private ProductResource productResource;
	private UserResource userResource;
	private AssetResource assetResource;
	private SourceResource sourceResource;

	private HttpServer server;

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setProductResource(ProductResource productResource) {
		this.productResource = productResource;
	}

	public void setUserResource(UserResource userResource) {
		this.userResource = userResource;
	}

	public void setAssetResource(AssetResource assetResource) {
		this.assetResource = assetResource;
	}

	public void setSourceResource(SourceResource sourceResource) {
		this.sourceResource = sourceResource;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!enabled) {
			log.info("Camel WEB REST server not started (gps.camel.web.enabled=false)");
			return;
		}
		try {
			InetSocketAddress addr = new InetSocketAddress(bindAddress, port);
			server = HttpServer.create(addr, 0);
			mount("/service/product", productResource);
			mount("/service/user", userResource);
			mount("/service/asset", assetResource);
			mount("/service/source", sourceResource);
			server.setExecutor(null);
			server.start();
			log.info("Camel WEB REST listening on http://" + bindAddress + ":" + port
					+ "/service/{product|user|asset|source}");
		}
		catch (BindException e) {
			log.warn("Camel WEB REST could not bind " + bindAddress + ":" + port
					+ " (likely Mule WEB still holds it). Stop Mule WEB and restart Tomcat. "
					+ e.getMessage());
			server = null;
		}
		catch (IOException e) {
			log.error("Camel WEB REST failed to start on " + bindAddress + ":" + port, e);
			throw e;
		}
	}

	private void mount(String contextPath, Object resource) {
		DefaultResourceConfig config = new DefaultResourceConfig();
		Set<Object> singletons = new HashSet<Object>();
		singletons.add(resource);
		config.getSingletons().addAll(singletons);
		HttpHandler handler = ContainerFactory.createContainer(HttpHandler.class, config);
		server.createContext(contextPath, handler);
	}

	@Override
	public void destroy() {
		if (server != null) {
			server.stop(0);
			log.info("Camel WEB REST stopped");
			server = null;
		}
	}
}
