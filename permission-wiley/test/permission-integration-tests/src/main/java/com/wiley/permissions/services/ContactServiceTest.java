package com.wiley.permissions.services;

import com.wiley.permissions.common.dispatcher.ServiceDispatcher;
import com.wiley.permissions.it.base.BaseTestCase;

public class ContactServiceTest
extends BaseTestCase
{
	//private ContactServiceImpl contactServiceImpl;
	private ServiceDispatcher dispatcher;

	/*
	public ContactServiceImpl getContactServiceImpl() {
		return contactServiceImpl;
	}

	public void setContactService(ContactServiceImpl contactService) {
		this.contactServiceImpl = contactService;
	}
	*/
	public ServiceDispatcher getDispatcher() {
		return dispatcher;
	}

	public void setDispatcher(ServiceDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	/*	public void testCreateSource(){

	try{

	//MuleMessage message = getDispatcher().send("vm://create.source?method=createSource",PermissionsSource.class, null);
	SimpleExcelData excelData = ReadExcel.parse(new File("C://imagearchivesources.xls"));
	//System.out.println(excelData.getData());
	String[][] data = excelData.getData();

	for (int i=0;i<data.length;i++) {
	//Thread.sleep(2000);
	//for (int j=0; j<data[i].length;j++) {
	//System.out.println(data[i][j]);
	Enterprise enterprise = new Enterprise();
	Contact contact = new Contact();

	//enterprise.setId(new Integer(data[i][0]));
	if(data[i][1].length() <20){
	contact.setFirstName(data[i][1]);

	}else{
	contact.setFirstName(data[i][1].substring(0,19));
	}
	if(data[i][1].length() <50)
	{
	enterprise.setName(data[i][1]);

	}else{
	enterprise.setName(data[i][1].substring(0,49));
	contact.setFirstName(data[i][1].substring(0,19));
	}

	if(data[i][8] != null){
	enterprise.setPhone(data[i][8]);
	contact.setHomePhoneNumber(data[i][8]);
	}
	else{
	enterprise.setPhone("");
	contact.setHomePhoneNumber("");
	}
	if(data[i][9]!= null){
	enterprise.setFax(data[i][9]);


	}else{
	enterprise.setFax("");
	}
	//enterprise.set

	if(data[i][11] != null){
	enterprise.setCountryCode(data[i][11]);
	contact.setCountryCode(data[i][11]);
	}else{
	enterprise.setCountryCode("");
	}
	if(data[i][10] != null){
	enterprise.setTechContactEmail(data[i][10]);
	}else{
	enterprise.setTechContactEmail("");
	}


	Address address = new Address();

	if(data[i][3] != null){
	address.setLineOne(data[i][3]);
	}else{
	address.setLineOne("");
	}

	if(data[i][5] != null){
	address.setCity(data[i][5]);
	}else{
	address.setCity("");
	}

	if(data[i][6] != null){
	address.setProvince(data[i][6]);
	}else{
	address.setProvince("");
	}

	if(data[i][7] != null){
	address.setPostalCode(data[i][7]);
	}else{
	address.setPostalCode("");
	}
	Set<Address> addresses = new HashSet<Address>();
	addresses.add(address);

	Set<Contact> contacts = new HashSet<Contact>();
	contacts.add(contact);
	enterprise.setAddresses(addresses);
	enterprise.setContacts(contacts);


	//PermissionsSource pSource = new PermissionsSource(enterprise,address,contact);
	MuleMessage message = getDispatcher().send("vm://create.source?method=createSource", enterprise, null);
	Enterprise source = (Enterprise)message.getPayload();
	//getContactServiceImpl().createSource(enterprise);
	//log.debug("Got The Enterprise");
	}

	} catch(Exception e) {
		System.out.println(e);
	}


	}*/
/*	*//**
	 * This is Test to create 10 enterprises, 3 contacts and 2 addresses and mapping them.
	 * End of this test you should be able to see 10 enterprises in WTENTR table.
	 * 3 contacts in WTCONT table, 2 Addresses in WTADDR table. and 30 rows in WTENCT table
	 * and 20 rows in WTRADR table.
	 *//*
	public void testPersistEnterprises()
	{
		for (int i = 0; i < 10; i++) {
			Enterprise ent = new Enterprise();

			//ent.setAccessControlAcct("AccesControlAcct" + i);
			//ent.setAccountManager("accountManager" + i);
			//ent.setAccountManagerId("0000" + i);
			//ent.setAccountType("personal");
			//ent.setAdminPassword("abc123");

			ent.setApprovalPlan(true);

			Country country = new Country();

			country.setCode("US");

			ent.setCountry(country);

			ent.setName("ENTERPRISE" + i);

			for (int j = 0; j < 3; j++) {
				EnterpriseContact ec = new EnterpriseContact();

				ec.setEnterprise(ent);

				Contact cont = new Contact();
				cont.setShortName("contact" + j);
				cont.setCountry(country);
				cont.setFirstName("contact" + j);
				cont.setLastName("abc");

				ec.setContact(cont);

				ent.getContacts().add(ec);
			}

			for (int k = 0; k < 2; k++) {
				EnterpriseAddress aa = new EnterpriseAddress();

				aa.setEnterprise(ent);

				Address addr = new Address();
				addr.setLineOne("line1-" + k);
				addr.setLineTwo("line2-" + k);
				addr.setLineThree("line3-" + k);
				addr.setCity("SanFrancisco");
				addr.setCountry(country);
				addr.setPostalCode("9440" + k);
				addr.setProvince("CA");

				aa.setAddress(addr);

				AddressType type = new AddressType();

				type.setCode("0");

				aa.setType(type);
			}

			try {
				Enterprise source = (Enterprise) getDispatcher().send("vm://create.source?method=createSource", ent, null);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

	}*/
}
