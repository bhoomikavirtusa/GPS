package com.wiley.permissions.domain;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.bean.BeanProperty;
import com.wiley.permissions.common.bean.BeanUtility;

/**
 *
 * @author ttidwell
 */
public abstract class DomainObject
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(DomainObject.class);

	private final static String tab = "  ";

	private final static Object[] BLANK_ARGS =
	{

	};

//	@Override
//	public String toString()
//	{
//		StringWriter sw = new StringWriter();
//
//		PrintWriter pw = new PrintWriter(sw);
//
//		pw.println(this.getClass().getSimpleName());
//
//		pw.println(toString(new InstanceOnlySet(), 1));
//
//		return sw.toString();
//	}

	protected String toString(Set<DomainObject> rendered, int tabLevel)
	{
		DomainObject me = this;

		rendered.add(this);

		StringWriter sw = new StringWriter();

		PrintWriter pw = new PrintWriter(sw);

		String ourTab = "";

		for (int x = 0; x < tabLevel; x++)
		{
			ourTab += tab;
		}

		List<BeanProperty> props = BeanUtility.getAllProperties(getClass());

		for (BeanProperty property : props)
		{
			pw.print(ourTab + tab + property.getName() + ": ");

			String valueStr = null;

			try
			{
				Object value = property.getReadMethod().invoke(me, BLANK_ARGS);

				if (value != null)
				{
					if (value instanceof DomainObject)
					{
						DomainObject tmp = (DomainObject) value;

						boolean test = rendered.contains(tmp);

						if (!test)
						{
							rendered.add(tmp);

							pw.println();

							valueStr = tmp.toString(rendered, tabLevel + 2);
						}
						else
						{
							valueStr = "Previously Rendered";
						}
					}
					else if (value instanceof Collection)
					{
						valueStr = "Collection";
					}
					else if (value instanceof Map)
					{
						valueStr = "Map";
					}
					else if (property.getClass().isArray())
					{
						valueStr = "Array";
					}
					else
					{
						valueStr = value.toString();

						if (value instanceof String)
						{
							valueStr = "'" + valueStr + "'";
						}
					}
				}
				else
				{
					valueStr = "null";
				}
			}
			catch (Exception e)
			{
				valueStr = "Not Retrieved";
			}

			pw.println(valueStr);
		}

		return sw.toString();
	}
}
