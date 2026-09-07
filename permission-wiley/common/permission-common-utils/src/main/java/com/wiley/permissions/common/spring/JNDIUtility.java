package com.wiley.permissions.common.spring;

import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a very simple utility class.  It is used to bind
 * references from spring managed beans into JNDI.
 * 
 * @author ttidwell
 * @version 1.0
 */
public class JNDIUtility
{
	private final static Log log = LogFactory.getLog(JNDIUtility.class);

	public void setBindings(Map<String, Object> bindings)
	throws NamingException
	{
		try
		{
			InitialContext ic = new InitialContext();

			for (String name : bindings.keySet())
			{
				Object object = bindings.get(name);

				log.info("Looking to start binding: " + name);

				Name realName = ic.getNameParser("").parse(name);

				log.info("Parsed To: " + realName);

				Context tmpContext = ic;
				
				Object test = null;
				
				for (int x=0; x < realName.size() - 1; x++)
				{
					String tmpName = realName.get(x);
					
					log.info("Looking to check context: " + tmpName);

					test = null;

					try
					{
						test = tmpContext.lookup(tmpName);
					}
					catch (Exception e) {}

					if (test == null)
					{
						log.info("No Context Found: " + tmpName);
						
						tmpContext = tmpContext.createSubcontext(tmpName);
						
						log.info("Context Created: " + tmpName);
					}
					else
					{
						tmpContext = (Context) test;
						
						log.info("Context Existed: " + tmpName);
					}
				}

				try
				{
					test = ic.lookup(name);
				}
				catch (NamingException e) {}
				
				if (test != null)
				{
					ic.rebind(name, object);
				}
				else
				{
					ic.bind(name, object);
				}
				
				log.info("Binding: " + name);
			}

			ic.close();
		}
		catch (NamingException e)
		{
			log.error("Error Doing Naming", e);

			throw e;
		}
	}
}
