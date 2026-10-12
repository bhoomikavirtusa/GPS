package com.wiley.permissions.camel.web;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.wiley.permissions.common.dispatcher.RemoteMuleDispatcher;
import com.wiley.permissions.common.dispatcher.ServiceDispatcher;

/**
 * Selects {@link CamelServiceDispatcher} or {@link RemoteMuleDispatcher} for bean id
 * {@code remoteWebDispatcher} based on {@code gps.camel.web.enabled} (default false).
 */
public class RemoteWebDispatcherFactoryBean implements FactoryBean<ServiceDispatcher>, InitializingBean {

	private static final Log log = LogFactory.getLog(RemoteWebDispatcherFactoryBean.class);

	private boolean camelWebEnabled;
	private String muleServerURL = "http://localhost:60504";
	private ProducerTemplate producerTemplate;

	private ServiceDispatcher dispatcher;

	public void setCamelWebEnabled(boolean camelWebEnabled) {
		this.camelWebEnabled = camelWebEnabled;
	}

	public void setMuleServerURL(String muleServerURL) {
		this.muleServerURL = muleServerURL;
	}

	public void setProducerTemplate(ProducerTemplate producerTemplate) {
		this.producerTemplate = producerTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (camelWebEnabled) {
			if (producerTemplate == null) {
				throw new IllegalStateException(
						"gps.camel.web.enabled=true but Camel ProducerTemplate is null");
			}
			CamelServiceDispatcher camel = new CamelServiceDispatcher();
			camel.setProducerTemplate(producerTemplate);
			dispatcher = camel;
			log.info("remoteWebDispatcher = CamelServiceDispatcher (gps.camel.web.enabled=true)");
		}
		else {
			RemoteMuleDispatcher mule = new RemoteMuleDispatcher();
			mule.setServerURL(muleServerURL);
			dispatcher = mule;
			log.info("remoteWebDispatcher = RemoteMuleDispatcher @ " + muleServerURL
					+ " (gps.camel.web.enabled=false)");
		}
	}

	@Override
	public ServiceDispatcher getObject() {
		return dispatcher;
	}

	@Override
	public Class<?> getObjectType() {
		return ServiceDispatcher.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
