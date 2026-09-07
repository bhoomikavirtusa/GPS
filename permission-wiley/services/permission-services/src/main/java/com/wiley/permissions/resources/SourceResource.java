package com.wiley.permissions.resources;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.lifecycle.InitialisationException;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.common.transformer.TransformationException;
import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.SourceRepository;

/**
 * This class is the REST service implementation
 * Uses the ProductService to make the calls, but it also transforms the results
 * to XML or JSON
 * We need this because the version of MULE we have I tested it and it does not do the
 * JSON transformation
 * Maybe MULE 3.0 will handle the JSON transformation and then we can annotate the ProductServiceImpl
 * directly as a REST resource
 * Because of cross domain requests, we need JSONP (a callback method name is passed as QueryParam)
 * @author lnagy
 */
@Path ("/")
public class SourceResource {

	private static final Log log = LogFactory.getLog(SourceResource.class);

	private SourceRepository sourceRepository;

	@GET
	@Produces("application/json")
	@Path("/search/{term}")
	public String searchSourceView (@PathParam("term") String term, @QueryParam("callback") String callback)
			throws PersistenceException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, InitialisationException, TransformationException
	{
		log.debug("searchSourceView(): called with term = " + term);
		List<Source> sources = sourceRepository.searchSourceByName(term);
		for (int x=0; x < sources.size(); x++) {
			SourceGroup sg = sources.get(x).getSourceGroup();
			// it can be null in DB
			if (sg != null) {
				sources.get(x).setSourceGroupName(sg.getName());
			}
		}
		return ObjectToJson.doTransform(sources, callback);
	}

	@GET
	@Produces("application/json")
	@Path("/getContacts/{term}")
	public String getContactsView (@PathParam("term") String term, @QueryParam("callback") String callback)
			throws PersistenceException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, InitialisationException, TransformationException
	{
		log.debug("getContactsView(): called with term = " + term);
		Source source = null;
		List<Contact> goodContacts = new ArrayList<Contact> ();
		try {
			source = sourceRepository.lazyLoad(Source.class, new Integer(term),
					new String[] {"contacts","contacts.address", "addresses"});
			log.debug("getContactsView(): found source:" + source.getId() + ", contacts size: " + source.getContacts().size());
		
			for (Contact dContact : source.getContacts()) {
				if (null == dContact.getAddress()  && null != source.getMainAddress()) {
					log.debug("getContactsView(): contact has no address using: " + source.getMainAddress().getLineOne());
					dContact.setAddress(source.getMainAddress());
					dContact = sourceRepository.saveRequiresNew(dContact);
				}
				log.debug("email: " + dContact.getEmail());
				if (null != dContact.getAddress()  || (null != dContact.getEmail() && dContact.getEmail().indexOf("@") > -1 ) ) {
					goodContacts.add(dContact);
				}
			}
		} catch (Exception e) {
			log.error("getContactsView(): caught exception: ", e);
			return null;
		}
		
		String returnValue = ObjectToJson.doTransform(goodContacts, callback);
		log.debug("getContactsView(): returning: " + returnValue);

		return returnValue;
	}

	@GET
	@Produces("application/json")
	@Path("/getAddresses/{term}")
	public String getAddressesView (@PathParam("term") String term, @QueryParam("callback") String callback)
			throws PersistenceException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InitialisationException, TransformationException
	{
		log.debug("getAddressesView(): called with term = " + term);
		Contact contact = null;

		try {
			contact = sourceRepository.lazyLoad(Contact.class, new Integer(term), new String[] {"address"});
			//contact = sourceRepository.lazyLoad(Contact.class, new Integer(term), new String[] {"addresses"});
			log.debug("getAddressesView(): found contact: " + contact.getId() + " addresses: " + contact.getAddress().getId());
			
			ArrayList<String> list = new ArrayList<String>();
			if (StringUtils.isNotBlank(contact.getAddress().getLineOne())) {
				list.add(contact.getAddress().getLineOne());
			}
			if (StringUtils.isNotBlank(contact.getAddress().getLineTwo())) {
				list.add(contact.getAddress().getLineTwo());
			}
			if (StringUtils.isNotBlank(contact.getAddress().getLineThree())) {
				list.add(contact.getAddress().getLineThree());
			}
			if (StringUtils.isNotBlank(contact.getAddress().getCity())) {
				list.add(contact.getAddress().getCity());
			}
			if (StringUtils.isNotBlank(contact.getAddress().getProvince())) {
				list.add(contact.getAddress().getProvince());
			}
			if (StringUtils.isNotBlank(contact.getAddress().getPostalCode())) {
				list.add(contact.getAddress().getPostalCode());
			}
			// add all addresses to line one to be displayed in the custom asset
			//	create UI
			contact.getAddress().setLineOne(StringUtils.join(list, ", "));
			contact.getAddress().setLineThree("");
			if (null != contact.getEmail() && contact.getEmail().length() > 0) {
				contact.getAddress().setLineThree(contact.getEmail());
			}
		} catch (Exception e) {
			Address addr = new Address();
			if (null != contact.getEmail() && contact.getEmail().indexOf("@") > -1 ) {
			   addr.setLineThree(contact.getEmail());	
			   contact.setAddress(addr);
			} else {
				return null;
			}
		}
		
		String returnValue = ObjectToJson.doTransform(contact.getAddress(), callback);
		log.debug("getAddressesView(): returing: " + returnValue);

		return returnValue;
	}

	@GET
	@Produces("application/json")
	@Path("/nofly/{term}")
	public String searchNoflyPhotographer (@PathParam("term") String term, @QueryParam("callback") String callback)
			throws PersistenceException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, InitialisationException, TransformationException
	{
		log.debug("searchNoflyPhotographer(): called with term = " + term);
		List<Source> sources = sourceRepository.searchNoflyPhotographer(term);
		return ObjectToJson.doTransform(sources, callback);
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}
}
