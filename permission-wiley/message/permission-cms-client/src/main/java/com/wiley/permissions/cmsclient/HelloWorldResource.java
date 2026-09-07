package com.wiley.permissions.cmsclient;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.MessageOperation;
import com.wiley.permissions.domain.persistence.permissions.Source;

@Path("/")
public class HelloWorldResource {
	
	private static final Log log = LogFactory.getLog(HelloWorldResource.class);

	/*
    @GET
    @Produces("text/plain")
    @Path("/{name}")
    public String sayHelloWithUri(@PathParam("name") String name) {
        return "Hello " + name;
    }*/
	
	@GET
    @Produces("application/xml")
    @Path("/{name}")
    public Message sayHelloWithUri(@PathParam("name") String name) {
		log.debug("sayHelloWithUri(): entered...");
		Source s = new Source();
		s.setName(name);
		Message msg = new Message ();
        MessageOperation msgOp = new MessageOperation();
        msgOp.setOperationType(OperationType.GET_SOURCE);
        
        List<MessageOperation> opList = new ArrayList<MessageOperation>();
        List<Object> itemList = new ArrayList<Object>();
        itemList.add(s);
        msgOp.setItems(itemList);
        opList.add(msgOp);
        msg.setOperations(opList);
        return msg;
    }
	
}
