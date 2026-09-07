package com.wiley.permissions.services.message;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.RequiresDelayedProcessingException;
import com.wiley.permissions.domain.message.RestMessage;
import com.wiley.permissions.domain.persistence.permissions.MessageErrorOp;
import com.wiley.permissions.domain.persistence.permissions.MessageSuccessOp;
import com.wiley.permissions.domain.persistence.permissions.Reference;

public class RestMessageService 
implements MessageService<RestMessage, RestMessage>{
	
	private static final Log log = LogFactory.getLog(RestMessageService.class);
		
	/**
	 * This method returns a success message to the calling client
	 */
	public RestMessage createSuccessMessage(String code, String referenceExternalId, String text) 
	{		
		log.debug("processing service Success for " + referenceExternalId);
		
		MessageSuccessOp success = new MessageSuccessOp();
		
		success.setText(text);
		
		success.setCode(code);
		
		Reference ref = new Reference();
		
		ref.setExternalId(referenceExternalId);
		
		success.setReference(ref);

		List<MessageSuccessOp> itemList = new ArrayList<MessageSuccessOp>();
		
	    itemList.add(success);

		return createMessage (itemList);		
	}

	/**
	 * This method returns an error message to the calling client
	 */
	public RestMessage createErrorMessage (String replyId, String code, String referenceExternalId, String text) 
	{
		log.debug("processing service error for " + referenceExternalId);
		
		MessageErrorOp error = new MessageErrorOp();
		
		error.setText(text);
		
		error.setCode(code);
		
		Reference ref = new Reference();
		
		ref.setExternalId(referenceExternalId);
		
		error.setReference(ref);

		List<MessageErrorOp> itemList = new ArrayList<MessageErrorOp>();
		
	    itemList.add(error);

		return createMessage (itemList);		
	}

	public RestMessage createMessage()
	{
		RestMessage message = new RestMessage();
		// nothing else now
		return message;
	}

	public RestMessage createMessage(List<?> items)
	{
		RestMessage msg = createMessage ();
		
		List<Object> itemList = new ArrayList<Object>();
        itemList.addAll(items);
        
        msg.setItemList(itemList);
        
        return msg;
	}

	public void receiveMessage(RestMessage message) 
	throws MessageException, RequiresDelayedProcessingException
	{
		// no implementation
	}

	public RestMessage sendMessage(RestMessage message) throws MessageException
	{
		// no implementation
		
		return null;
	}
	
}
