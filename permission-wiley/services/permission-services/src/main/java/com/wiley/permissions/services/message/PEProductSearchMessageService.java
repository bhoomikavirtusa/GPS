package com.wiley.permissions.services.message;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleException;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.common.dispatcher.ServiceDispatcher;
import com.wiley.permissions.domain.message.Message.MessageEntity;
import com.wiley.permissions.domain.message.Message.MessageType;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.RequiresDelayedProcessingException;
import com.wiley.permissions.domain.message.pe.ProductSearch;
import com.wiley.permissions.domain.message.pe.ProductSearchMessage;
import com.wiley.permissions.domain.message.pe.ProductSearchRequest;
import com.wiley.permissions.domain.message.pe.ProductSearchTerm;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;

public class PEProductSearchMessageService
implements MessageService<ProductSearchMessage, ProductSearchMessage>{

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PEProductSearchMessageService.class);

	// dispatcher used to communicate between WebServer and MULE_WEB mule instance.
	// This will be instantiated with the remoteWebDispatcher (MULE_WEB) remote instance
	// will send PE search requests
	protected ServiceDispatcher serviceDispatcher = null;

	// This is a default value - can be overridden by Spring config
	private final int productSearchLimit = 100;

	public ProductSearchMessage createErrorMessage(String replyId, String code, String referenceExternalId, String text)
	{
		return null;
	}

	public ProductSearchMessage createMessage()
	{
		ProductSearchMessage psm = new ProductSearchMessage();

		psm.setId(UniqueIdentifierGenerator.getNextIdentifier());

		psm.setSent(System.currentTimeMillis());

		psm.setSource(MessageEntity.PERMISSIONS);

		psm.getTargets().add(MessageEntity.PRODUCT_ENGINEERING);

		psm.setType(MessageType.REQUEST);

		ProductSearchRequest psr = new ProductSearchRequest();

		ProductSearch ps = new ProductSearch();

		ps.setMaxResults(productSearchLimit);

		psr.setProductSearch(ps);

		psm.setRequest(psr);

		return psm;
	}

	public ProductSearchMessage createMessage(List<?> items)
	{
		ProductSearchMessage psm = createMessage();

		List<ProductSearchTerm> terms = (List<ProductSearchTerm>) items;

		for (ProductSearchTerm term : terms) {
			psm.getRequest().getProductSearch().getSearchTerms().add(term);
		}

		return psm;
	}

	public void receiveMessage(ProductSearchMessage message) throws MessageException, RequiresDelayedProcessingException
	{
		// nothing
	}

	public ProductSearchMessage sendMessage(ProductSearchMessage message)
	throws MuleException, DispatcherException
	{
		return (ProductSearchMessage) getServiceDispatcher().send(
				OperationType.SEARCH_PRODUCTS, message, null);
	}

	public ServiceDispatcher getServiceDispatcher()
	{
		return serviceDispatcher;
	}

	public void setServiceDispatcher(ServiceDispatcher serviceDispatcher)
	{
		this.serviceDispatcher = serviceDispatcher;
	}
}
